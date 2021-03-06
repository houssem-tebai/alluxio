/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.master.metrics;

import alluxio.grpc.MetricType;
import alluxio.metrics.Metric;
import alluxio.metrics.MetricInfo;
import alluxio.metrics.MetricKey;
import alluxio.metrics.MetricsSystem;
import alluxio.metrics.MetricsSystem.InstanceType;
import alluxio.resource.LockResource;

import com.codahale.metrics.Counter;
import com.google.common.base.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A store of metrics containing the metrics collected from workers and clients.
 */
@ThreadSafe
public class MetricsStore {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsStore.class);
  // The following fields are added for reducing the string processing
  // for MetricKey.WORKER_BYTES_READ_UFS and MetricKey.WORKER_BYTES_WRITTEN_UFS
  private static final String BYTES_READ_UFS = "BytesReadPerUfs";
  private static final String BYTES_WRITTEN_UFS = "BytesWrittenPerUfs";

  // The lock to guarantee that only one thread is clearing the metrics
  // and no other interaction with worker/client metrics set is allowed during metrics clearing
  private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();

  // The time of the most recent metrics store clearance.
  // This tracks when the cluster counters start aggregating from the reported metrics.
  @GuardedBy("mLock")
  private long mLastClearTime = System.currentTimeMillis();

  /**
   * A map from the cluster counter key representing the metrics to be aggregated
   * to the corresponding aggregated cluster Counter.
   * For example, Counter of cluster.BytesReadAlluxio is aggregated from
   * the worker reported worker.BytesReadAlluxio.
   *
   * Exceptions are the BytesRead/WrittenUfs metrics which records
   * the actual cluster metrics name to its Counter directly.
   */
  @GuardedBy("mLock")
  private final ConcurrentHashMap<ClusterCounterKey, Counter> mClusterCounters
      = new ConcurrentHashMap<>();

  /**
   * Put the metrics from a worker with a hostname. If all the old metrics associated with this
   * instance will be removed and then replaced by the latest.
   *
   * @param hostname the hostname of the instance
   * @param metrics the new worker metrics
   */
  public void putWorkerMetrics(String hostname, List<Metric> metrics) {
    if (metrics.isEmpty() || hostname == null) {
      return;
    }
    try (LockResource r = new LockResource(mLock.readLock())) {
      putReportedMetrics(InstanceType.WORKER, metrics);
    }
  }

  /**
   * Put the metrics from a client with a hostname.
   *
   * @param hostname the hostname of the client
   * @param metrics the new metrics
   */
  public void putClientMetrics(String hostname, List<Metric> metrics) {
    if (metrics.isEmpty() || hostname == null) {
      return;
    }
    try (LockResource r = new LockResource(mLock.readLock())) {
      putReportedMetrics(InstanceType.CLIENT, metrics);
    }
  }

  /**
   * Update the reported metrics received from a worker or client.
   *
   * Cluster metrics of {@link MetricType} COUNTER are directly incremented by the reported values.
   *
   * @param instanceType the instance type that reports the metrics
   * @param reportedMetrics the metrics received by the RPC handler
   */
  private void putReportedMetrics(InstanceType instanceType, List<Metric> reportedMetrics) {
    for (Metric metric : reportedMetrics) {
      if (metric.getHostname() == null) {
        continue; // ignore metrics whose hostname is null
      }

      // If a metric is COUNTER, the value sent via RPC should be the incremental value; i.e.
      // the amount the value has changed since the last RPC. The master should equivalently
      // increment its value based on the received metric rather than replacing it.
      if (metric.getMetricType() == MetricType.COUNTER) {
        ClusterCounterKey key = new ClusterCounterKey(instanceType, metric.getName());
        Counter counter = mClusterCounters.get(key);
        if (counter != null) {
          counter.inc((long) metric.getValue());
          continue;
        }
        if (instanceType.equals(InstanceType.CLIENT)) {
          continue;
        }
        // Need to increment two metrics: one for the specific ufs the current metric recorded from
        // and one to summarize values from all UFSes
        if (metric.getName().equals(BYTES_READ_UFS)) {
          incrementUfsRelatedCounters(metric, MetricKey.CLUSTER_BYTES_READ_UFS.getName(),
              MetricKey.CLUSTER_BYTES_READ_UFS_ALL.getName());
        } else if (metric.getName().equals(BYTES_WRITTEN_UFS)) {
          incrementUfsRelatedCounters(metric, MetricKey.CLUSTER_BYTES_WRITTEN_UFS.getName(),
              MetricKey.CLUSTER_BYTES_WRITTEN_UFS_ALL.getName());
        }
      }
    }
  }

  /**
   * Increments the related counters of a specific metric.
   *
   * @param metric the metric
   * @param perUfsMetricName the per ufs metric name prefix to increment counter value of
   * @param allUfsMetricName the all ufs metric name to increment counter value of
   */
  private void incrementUfsRelatedCounters(Metric metric,
      String perUfsMetricName, String allUfsMetricName) {
    String fullCounterName = Metric.getMetricNameWithTags(perUfsMetricName,
        MetricInfo.TAG_UFS, metric.getTags().get(MetricInfo.TAG_UFS));
    Counter perUfsCounter = mClusterCounters.computeIfAbsent(
        new ClusterCounterKey(InstanceType.CLUSTER, fullCounterName),
        n -> MetricsSystem.counter(fullCounterName));
    long counterValue = (long) metric.getValue();
    perUfsCounter.inc(counterValue);
    mClusterCounters.get(new ClusterCounterKey(InstanceType.CLUSTER, allUfsMetricName))
        .inc(counterValue);
  }

  /**
   * Inits the metrics store.
   * Defines the cluster metrics counters.
   */
  public void init() {
    try (LockResource r = new LockResource(mLock.readLock())) {
      // worker metrics
      mClusterCounters.put(new ClusterCounterKey(InstanceType.WORKER,
          MetricKey.WORKER_BYTES_READ_ALLUXIO.getMetricName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_READ_ALLUXIO.getName()));
      mClusterCounters.put(new ClusterCounterKey(InstanceType.WORKER,
          MetricKey.WORKER_BYTES_READ_DOMAIN.getMetricName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_READ_DOMAIN.getName()));
      mClusterCounters.put(new ClusterCounterKey(InstanceType.WORKER,
          MetricKey.WORKER_BYTES_WRITTEN_ALLUXIO.getMetricName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_WRITTEN_ALLUXIO.getName()));
      mClusterCounters.put(new ClusterCounterKey(InstanceType.WORKER,
          MetricKey.WORKER_BYTES_WRITTEN_DOMAIN.getMetricName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_WRITTEN_DOMAIN.getName()));

      // client metrics
      mClusterCounters.put(new ClusterCounterKey(InstanceType.CLIENT,
          MetricKey.CLIENT_BYTES_READ_LOCAL.getMetricName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_READ_LOCAL.getName()));
      mClusterCounters.put(new ClusterCounterKey(InstanceType.CLIENT,
          MetricKey.CLIENT_BYTES_WRITTEN_LOCAL.getMetricName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_WRITTEN_LOCAL.getName()));

      // special metrics that have multiple worker metrics to summarize from
      // always use the full name instead of metric name for those metrics
      mClusterCounters.put(new ClusterCounterKey(InstanceType.CLUSTER,
          MetricKey.CLUSTER_BYTES_READ_UFS_ALL.getName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_READ_UFS_ALL.getName()));
      mClusterCounters.put(new ClusterCounterKey(InstanceType.CLUSTER,
          MetricKey.CLUSTER_BYTES_WRITTEN_UFS_ALL.getName()),
          MetricsSystem.counter(MetricKey.CLUSTER_BYTES_WRITTEN_UFS_ALL.getName()));
    }
  }

  /**
   * Clears all the metrics.
   *
   * This method should only be called when starting the {@link DefaultMetricsMaster}
   * and before starting the metrics updater to avoid conflicts with
   * other methods in this class which updates or accesses
   * the metrics inside metrics sets.
   */
  public void clear() {
    try (LockResource r = new LockResource(mLock.writeLock())) {
      for (Counter counter : mClusterCounters.values()) {
        counter.dec(counter.getCount());
      }
      mLastClearTime = System.currentTimeMillis();
      MetricsSystem.resetAllMetrics();
    }
  }

  /**
   * @return the last metrics store clear time in milliseconds
   */
  public long getLastClearTime() {
    try (LockResource r = new LockResource(mLock.readLock())) {
      return mLastClearTime;
    }
  }

  /**
   * The key for cluster counter map.
   * This class is added to reduce the string concatenation of instancetype + "." + metric name.
   */
  public static class ClusterCounterKey {
    private InstanceType mInstanceType;
    private String mMetricName;

    /**
     * Construct a new {@link ClusterCounterKey}.
     *
     * @param instanceType the instance type
     * @param metricName the metric name
     */
    public ClusterCounterKey(InstanceType instanceType, String metricName) {
      mInstanceType = instanceType;
      mMetricName = metricName;
    }

    /**
     * @return the instance type
     */
    private InstanceType getInstanceType() {
      return mInstanceType;
    }

    /**
     * @return the metric name
     */
    private String getMetricName() {
      return mMetricName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ClusterCounterKey)) {
        return false;
      }
      ClusterCounterKey that = (ClusterCounterKey) o;
      return Objects.equal(mInstanceType, that.getInstanceType())
          && Objects.equal(mMetricName, that.getMetricName());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mInstanceType, mMetricName);
    }

    @Override
    public String toString() {
      return mInstanceType + "." + mMetricName;
    }
  }
}
