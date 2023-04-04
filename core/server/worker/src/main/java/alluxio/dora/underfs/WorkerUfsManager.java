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

package alluxio.dora.underfs;

import alluxio.dora.AlluxioURI;
import alluxio.dora.conf.Configuration;
import alluxio.dora.exception.status.NotFoundException;
import alluxio.dora.exception.status.UnavailableException;
import alluxio.dora.grpc.UfsInfo;
import alluxio.dora.util.network.NetworkAddressUtils;
import alluxio.dora.worker.file.FileSystemMasterClient;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of UfsManager to manage the ufs used by different worker services.
 */
@ThreadSafe
public final class WorkerUfsManager extends AbstractUfsManager {
  private static final Logger LOG = LoggerFactory.getLogger(WorkerUfsManager.class);

  private final FileSystemMasterClient mMasterClient;

  /**
   * Constructs an instance of {@link WorkerUfsManager}.
   * @param masterClient the file system master client
   */
  @Inject
  public WorkerUfsManager(FileSystemMasterClient masterClient) {
    mMasterClient = mCloser.register(masterClient);
  }

  /**
   * {@inheritDoc}.
   *
   * If this mount id is new to this worker, this method will query master to get the
   * corresponding ufs info.
   */
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
    return super.get(mountId);
  }

  @Override
  protected void connectUfs(UnderFileSystem fs) throws IOException {
    fs.connectFromWorker(
        NetworkAddressUtils.getConnectHost(NetworkAddressUtils.ServiceType.WORKER_RPC,
            Configuration.global()));
  }
}
