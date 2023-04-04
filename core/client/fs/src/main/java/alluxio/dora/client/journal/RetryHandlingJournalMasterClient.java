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

package alluxio.dora.client.journal;

import alluxio.dora.AbstractJobMasterClient;
import alluxio.dora.Constants;
import alluxio.dora.exception.status.AlluxioStatusException;
import alluxio.dora.grpc.GetQuorumInfoPRequest;
import alluxio.dora.grpc.GetQuorumInfoPResponse;
import alluxio.dora.grpc.GetTransferLeaderMessagePRequest;
import alluxio.dora.grpc.GetTransferLeaderMessagePResponse;
import alluxio.dora.grpc.JournalMasterClientServiceGrpc;
import alluxio.dora.grpc.NetAddress;
import alluxio.dora.grpc.RemoveQuorumServerPRequest;
import alluxio.dora.grpc.ResetPrioritiesPRequest;
import alluxio.dora.grpc.ServiceType;
import alluxio.dora.grpc.TransferLeadershipPRequest;
import alluxio.dora.master.MasterClientContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for the gRPC client to interact with the journal master, used by alluxio clients.
 * It would talk to both JobMaster and Master, so it should inherit AbstractJobMasterClient
 */
public class RetryHandlingJournalMasterClient extends AbstractJobMasterClient
    implements JournalMasterClient {
  private static final Logger RPC_LOG = LoggerFactory.getLogger(JournalMasterClient.class);
  private JournalMasterClientServiceGrpc.JournalMasterClientServiceBlockingStub mClient = null;

  /**
   * Creates a new journal master client.
   *
   * @param conf master client configuration
   */
  public RetryHandlingJournalMasterClient(MasterClientContext conf) {
    super(conf);
  }

  @Override
  protected ServiceType getRemoteServiceType() {
    return ServiceType.JOURNAL_MASTER_CLIENT_SERVICE;
  }

  @Override
  protected String getServiceName() {
    return Constants.JOURNAL_MASTER_CLIENT_SERVICE_NAME;
  }

  @Override
  protected long getServiceVersion() {
    return Constants.JOURNAL_MASTER_CLIENT_SERVICE_VERSION;
  }

  @Override
  protected void afterConnect() {
    mClient = JournalMasterClientServiceGrpc.newBlockingStub(mChannel);
  }

  @Override
  public GetQuorumInfoPResponse getQuorumInfo() throws AlluxioStatusException {
    return retryRPC(() -> mClient.getQuorumInfo(GetQuorumInfoPRequest.getDefaultInstance()),
        RPC_LOG, "GetQuorumInfo",  "");
  }

  @Override
  public void removeQuorumServer(NetAddress serverAddress) throws AlluxioStatusException {
    retryRPC(() -> mClient.removeQuorumServer(
        RemoveQuorumServerPRequest.newBuilder().setServerAddress(serverAddress).build()),
        RPC_LOG, "RemoveQuorumServer",  "serverAddress=%s", serverAddress);
  }

  @Override
  public String transferLeadership(NetAddress newLeaderNetAddress) throws AlluxioStatusException {
    return retryRPC(() -> mClient.transferLeadership(
        TransferLeadershipPRequest.newBuilder()
        .setServerAddress(newLeaderNetAddress).build()).getTransferId(),
        RPC_LOG, "TransferLeadership", "serverAddress=%s", newLeaderNetAddress);
  }

  @Override
  public void resetPriorities() throws AlluxioStatusException {
    retryRPC(() -> mClient.resetPriorities(ResetPrioritiesPRequest.getDefaultInstance()),
            RPC_LOG, "ResetPriorities", "");
  }

  @Override
  public GetTransferLeaderMessagePResponse getTransferLeaderMessage(String transferId)
          throws AlluxioStatusException {
    return retryRPC(() ->
        mClient.getTransferLeaderMessage(
           GetTransferLeaderMessagePRequest.newBuilder().setTransferId(transferId).build()),
        RPC_LOG, "GetTransferLeaderMessage",  "");
  }
}
