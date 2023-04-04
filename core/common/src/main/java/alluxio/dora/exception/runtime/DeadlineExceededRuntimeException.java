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

package alluxio.dora.exception.runtime;

import alluxio.dora.grpc.ErrorType;

import io.grpc.Status;

/**
 * Exception indicating that an operation expired before completion. Typical example is timeout.
 */
public class DeadlineExceededRuntimeException extends AlluxioRuntimeException {
  private static final Status STATUS = Status.DEADLINE_EXCEEDED;
  private static final ErrorType ERROR_TYPE = ErrorType.User;
  private static final boolean RETRYABLE = true;

  /**
   * Constructor.
   * @param message error message
   */
  public DeadlineExceededRuntimeException(String message) {
    super(STATUS, message, null, ERROR_TYPE, RETRYABLE);
  }
}
