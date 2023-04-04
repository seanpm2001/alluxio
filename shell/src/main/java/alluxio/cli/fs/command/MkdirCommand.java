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

package alluxio.cli.fs.command;

import alluxio.dora.AlluxioURI;
import alluxio.dora.annotation.PublicApi;
import alluxio.dora.cli.CommandUtils;
import alluxio.dora.client.file.FileSystemContext;
import alluxio.dora.exception.AlluxioException;
import alluxio.dora.exception.status.InvalidArgumentException;
import alluxio.dora.grpc.CreateDirectoryPOptions;

import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Creates a new directory specified by the path in args, including any parent folders that are
 * required. This command fails if a directory or file with the same path already exists.
 */
@ThreadSafe
@PublicApi
public final class MkdirCommand extends AbstractFileSystemCommand {

  /**
   * Constructs a new instance to create a new directory.
   *
   * @param fsContext the filesystem of Alluxio
   */
  public MkdirCommand(FileSystemContext fsContext) {
    super(fsContext);
  }

  @Override
  public String getCommandName() {
    return "mkdir";
  }

  @Override
  public int run(CommandLine cl) throws AlluxioException, IOException {
    String[] args = cl.getArgs();
    for (String path : args) {
      AlluxioURI inputPath = new AlluxioURI(path);

      CreateDirectoryPOptions options =
          CreateDirectoryPOptions.newBuilder().setRecursive(true).build();
      mFileSystem.createDirectory(inputPath, options);
      System.out.println("Successfully created directory " + inputPath);
    }
    return 0;
  }

  @Override
  public String getUsage() {
    return "mkdir <path1> [path2] ... [pathn]";
  }

  @Override
  public String getDescription() {
    return "Creates the specified directories, including any parent directories that are required.";
  }

  @Override
  public void validateArgs(CommandLine cl) throws InvalidArgumentException {
    CommandUtils.checkNumOfArgsNoLessThan(this, cl, 1);
  }
}
