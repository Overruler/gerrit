// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.index;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;

/**
 * Implementation-specific configuration for secondary indexes.
 * <p>
 * Contains configuration that is tied to a specific index implementation but is
 * otherwise global, i.e. not tied to a specific {@link ChangeIndex} and schema
 * version.
 */
@AutoValue
public abstract class IndexConfig {
  public static IndexConfig createDefault() {
    return create(Integer.MAX_VALUE);
  }

  public static IndexConfig create(int maxLimit) {
    checkArgument(maxLimit > 0, "maxLimit must be positive: %s", maxLimit);
    return new AutoValue_IndexConfig(maxLimit);
  }

  public abstract int maxLimit();
}
