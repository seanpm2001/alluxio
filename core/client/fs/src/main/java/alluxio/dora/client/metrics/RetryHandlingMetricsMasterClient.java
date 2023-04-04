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

package alluxio.dora.client.metrics;

import alluxio.dora.AbstractMasterClient;
import alluxio.dora.Constants;
import alluxio.dora.exception.status.AlluxioStatusException;
import alluxio.dora.exception.status.UnavailableException;
import alluxio.dora.grpc.ClearMetricsPRequest;
import alluxio.dora.grpc.ClientMetrics;
import alluxio.dora.grpc.GetMetricsPOptions;
import alluxio.dora.grpc.MetricValue;
import alluxio.dora.grpc.MetricsHeartbeatPOptions;
import alluxio.dora.grpc.MetricsHeartbeatPRequest;
import alluxio.dora.grpc.MetricsMasterClientServiceGrpc;
import alluxio.dora.grpc.ServiceType;
import alluxio.dora.master.MasterClientContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A wrapper for the gRPC client to interact with the metrics master.
 */
@ThreadSafe
public class RetryHandlingMetricsMasterClient extends AbstractMasterClient
    implements MetricsMasterClient {
  private static final Logger LOG =
      LoggerFactory.getLogger(RetryHandlingMetricsMasterClient.class);
  private MetricsMasterClientServiceGrpc.MetricsMasterClientServiceBlockingStub mClient = null;

  /**
   * Creates a new metrics master client.
   *
   * @param conf master client configuration
   */
  public RetryHandlingMetricsMasterClient(MasterClientContext conf) {
    super(conf);
  }

  @Override
  protected ServiceType getRemoteServiceType() {
    return ServiceType.METRICS_MASTER_CLIENT_SERVICE;
  }

  @Override
  protected String getServiceName() {
    return Constants.METRICS_MASTER_CLIENT_SERVICE_NAME;
  }

  @Override
  protected long getServiceVersion() {
    return Constants.METRICS_MASTER_CLIENT_SERVICE_VERSION;
  }

  @Override
  protected void afterConnect() {
    mClient = MetricsMasterClientServiceGrpc.newBlockingStub(mChannel);
  }

  @Override
  public void clearMetrics() throws IOException {
    retryRPC(() -> mClient.clearMetrics(ClearMetricsPRequest.newBuilder().build()),
        LOG, "ClearMetrics", "");
  }

  @Override
  public void heartbeat(final List<ClientMetrics> metrics) throws IOException {
    connect();
    try {
      MetricsHeartbeatPRequest.Builder request = MetricsHeartbeatPRequest.newBuilder();
      request.setOptions(MetricsHeartbeatPOptions.newBuilder()
          .addAllClientMetrics(metrics).build());
      mClient.metricsHeartbeat(request.build());
    } catch (io.grpc.StatusRuntimeException e) {
      disconnect();
      throw new UnavailableException(e);
    }
  }

  @Override
  public Map<String, MetricValue> getMetrics() throws AlluxioStatusException {
    return retryRPC(
        () -> mClient.getMetrics(GetMetricsPOptions.getDefaultInstance()).getMetricsMap(),
        LOG, "GetMetrics", "");
  }
}
