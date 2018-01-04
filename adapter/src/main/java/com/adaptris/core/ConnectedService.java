/*
 * Copyright 2018 Adaptris Ltd.
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
package com.adaptris.core;

/**
 * An interface that represents a {@link Service} that contains a connection.
 */
public interface ConnectedService extends Service {

  /**
   * Sets the {@code AdaptrisConnection} to use.
   *
   * @param conn the {@code AdaptrisConnection} to use
   */
  void setConnection(AdaptrisConnection c);

  /**
   * Returns the {@code AdaptrisConnection} to use.
   *
   * @return the {@code AdaptrisConnection}
   */
  AdaptrisConnection getConnection();

}