/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.protocol.record.intent;

public enum BatchOperationIntent implements Intent {
  CREATE((short) 0),
  CREATED((short) 1),
  START((short) 2),
  STARTED((short) 3),
  FAIL((short) 4),
  FAILED((short) 5),
  CANCEL((short) 6),
  CANCELED((short) 7),
  SUSPEND((short) 8),
  SUSPENDED((short) 9),
  RESUME((short) 10),
  RESUMED((short) 11),
  COMPLETE((short) 12),
  COMPLETED((short) 13),
  COMPLETE_PARTITION((short) 14),
  PARTITION_COMPLETED((short) 15),
  FAIL_PARTITION((short) 16),
  PARTITION_FAILED((short) 17),
  CONTINUE_INITIALIZATION((short) 18),
  INITIALIZATION_CONTINUED((short) 19);

  private final short value;

  BatchOperationIntent(final short value) {
    this.value = value;
  }

  public short getIntent() {
    return value;
  }

  public static Intent from(final short value) {
    switch (value) {
      case 0:
        return CREATE;
      case 1:
        return CREATED;
      case 2:
        return START;
      case 3:
        return STARTED;
      case 4:
        return FAIL;
      case 5:
        return FAILED;
      case 6:
        return CANCEL;
      case 7:
        return CANCELED;
      case 8:
        return SUSPEND;
      case 9:
        return SUSPENDED;
      case 10:
        return RESUME;
      case 11:
        return RESUMED;
      case 12:
        return COMPLETE;
      case 13:
        return COMPLETED;
      case 14:
        return COMPLETE_PARTITION;
      case 15:
        return PARTITION_COMPLETED;
      case 16:
        return FAIL_PARTITION;
      case 17:
        return PARTITION_FAILED;
      case 18:
        return CONTINUE_INITIALIZATION;
      case 19:
        return INITIALIZATION_CONTINUED;
      default:
        return Intent.UNKNOWN;
    }
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    switch (this) {
      case CREATED:
      case STARTED:
      case FAILED:
      case SUSPENDED:
      case CANCELED:
      case RESUMED:
      case COMPLETED:
      case PARTITION_COMPLETED:
      case PARTITION_FAILED:
      case INITIALIZATION_CONTINUED:
        return true;
      default:
        return false;
    }
  }
}
