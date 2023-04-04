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

package alluxio.underfs;

import alluxio.dora.AlluxioURI;
import alluxio.dora.ClientContext;
import alluxio.dora.conf.Configuration;
import alluxio.dora.exception.status.NotFoundException;
import alluxio.dora.exception.status.UnavailableException;
import alluxio.dora.underfs.AbstractUfsManager;
import alluxio.dora.underfs.UnderFileSystem;
import alluxio.dora.underfs.UnderFileSystemConfiguration;
import alluxio.dora.grpc.UfsInfo;
import alluxio.dora.master.MasterClientContext;
import alluxio.master.file.FileSystemMasterClient;
import alluxio.dora.resource.CloseableResource;
import alluxio.dora.util.network.NetworkAddressUtils;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Implementation of UfsManager to manage the ufs used by different job service processes.
 */
// TODO(jiri): Avoid duplication of logic with WorkerUfsManager.
@ThreadSafe
public final class JobUfsManager extends AbstractUfsManager {
  private static final Logger LOG = LoggerFactory.getLogger(JobUfsManager.class);

  private final FileSystemMasterClient mMasterClient;

  /**
   * Constructs an instance of {@link JobUfsManager}.
   */
  public JobUfsManager() {
    mMasterClient =
        mCloser.register(new FileSystemMasterClient(MasterClientContext
            .newBuilder(ClientContext.create(Configuration.global())).build()));
  }

  @Override
  protected void connectUfs(UnderFileSystem fs) throws IOException {
    fs.connectFromWorker(
        NetworkAddressUtils.getConnectHost(NetworkAddressUtils.ServiceType.JOB_WORKER_RPC,
            Configuration.global()));
  }

  @Override
  public UfsClient get(long mountId) throws NotFoundException, UnavailableException {
    try {
      return super.get(mountId);
    } catch (NotFoundException e) {
      // Not cached locally, let's query master
    }

    UfsInfo info;
    try {
      info = mMasterClient.getUfsInfo(mountId);
    } catch (IOException e) {
      throw new UnavailableException(
          String.format("Failed to create UFS info for mount point with id %d", mountId), e);
    }
    Preconditions.checkState((info.hasUri() && info.hasProperties()), "unknown mountId");
    super.addMount(mountId, new AlluxioURI(info.getUri()),
        new UnderFileSystemConfiguration(
            Configuration.global(), info.getProperties().getReadOnly())
            .createMountSpecificConf(info.getProperties().getPropertiesMap()));
    UfsClient ufsClient = super.get(mountId);
    try (CloseableResource<UnderFileSystem> ufsResource = ufsClient.acquireUfsResource()) {
      UnderFileSystem ufs = ufsResource.get();
      ufs.connectFromWorker(
          NetworkAddressUtils.getConnectHost(NetworkAddressUtils.ServiceType.WORKER_RPC,
              Configuration.global()));
    } catch (IOException e) {
      removeMount(mountId);
      throw new UnavailableException(
          String.format("Failed to connect to UFS %s with id %d", info.getUri(), mountId), e);
    }
    return ufsClient;
  }
}
