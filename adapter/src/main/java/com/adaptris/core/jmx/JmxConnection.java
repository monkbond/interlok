/*
 * Copyright 2016 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adaptris.core.jmx;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.validation.Valid;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AllowsRetriesConnection;
import com.adaptris.core.CoreException;
import com.adaptris.core.util.Args;
import com.adaptris.core.util.ExceptionHelper;
import com.adaptris.core.util.JmxHelper;
import com.adaptris.security.exc.AdaptrisSecurityException;
import com.adaptris.security.password.Password;
import com.adaptris.util.KeyValuePairBag;
import com.adaptris.util.KeyValuePairSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("jmx-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to a JMX MBeanServer instance", tag = "connections,jmx")
@DisplayOrder(order = {"jmxServiceUrl", "jmxProperties", "username", "password"})
public class JmxConnection extends AllowsRetriesConnection {
  private String jmxServiceUrl;
  @Valid
  private KeyValuePairSet jmxProperties;
  private String username;
  @InputFieldHint(style = "PASSWORD")
  private String password;
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean additionalDebug;

  private transient MBeanServerConnection connection;

  public JmxConnection() {
    setJmxProperties(new KeyValuePairSet());
  }

  @Override
  protected void prepareConnection() throws CoreException {
  }

  @Override
  protected void initConnection() throws CoreException {
    try {
      doConnect();
    } catch (Exception e) {
      throw ExceptionHelper.wrapCoreException(e);
    }
  }

  @Override
  protected void startConnection() throws CoreException {
  }

  @Override
  protected void stopConnection() {
  }

  @Override
  protected void closeConnection() {
    connection = null;
  }

  public MBeanServerConnection mbeanServerConnection() {
    return connection;
  }


  private void doConnect() throws Exception {
    int attemptCount = 0;
    while (connection == null) {
      try {
        attemptCount++;
        if (additionalDebug()) {
          log.trace("Attempting connection to [{}]", jmxServiceUrlForLogging());
        }
        connection = createConnection();
      } catch (IOException e) {
        if (attemptCount == 1) {
          log.warn("Cannot connect to [{}]", jmxServiceUrlForLogging(), e);
        }
        if (connectionAttempts() != -1 && attemptCount >= connectionAttempts()) {
          log.error("Failed to connect to [{}]", getJmxServiceUrl(), e);
          throw ExceptionHelper.wrapCoreException(e);
        } else {
          log.warn("Attempt [{}] failed for broker [{}], retrying", attemptCount, jmxServiceUrlForLogging());
          log.info(createLoggingStatement(attemptCount));
          Thread.sleep(connectionRetryInterval());
          continue;
        }
      }
    }
  }


  private MBeanServerConnection createConnection() throws IOException, AdaptrisSecurityException {
    MBeanServerConnection result = null;
    if (!isBlank(getJmxServiceUrl())) {
      Map env = KeyValuePairBag.asMap(getJmxProperties());
      if (!isBlank(getUsername())) {
        if (!env.containsKey("jmx.remote.profiles")) {
          env.put("jmx.remote.profiles", "SASL/PLAIN");
          env.put(JMXConnector.CREDENTIALS, new String[] {getUsername(), Password.decode(getPassword())});
        }
      }
      JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(getJmxServiceUrl()), env.size() == 0 ? null : env);
      result = connector.getMBeanServerConnection();
    } else {
      result = JmxHelper.findMBeanServer();
    }
    return result;
  }

  /**
   * @return the jmxServiceUrl
   */
  public String getJmxServiceUrl() {
    return jmxServiceUrl;
  }

  /**
   * Set the JMX ServiceURL.
   * 
   * @param s the jmxServiceUrl to set; if not specified, then a local JMX connector is assumed.
   */
  public void setJmxServiceUrl(String s) {
    this.jmxServiceUrl = s;
  }

  /**
   * @return the jmxProperties
   */
  public KeyValuePairSet getJmxProperties() {
    return jmxProperties;
  }

  /**
   * @param s the additional jmx properties to set
   */
  public void setJmxProperties(KeyValuePairSet s) {
    this.jmxProperties = Args.notNull(s, "jmxProperties");
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set the username for accessing JMX.
   *
   * <p>
   * If both the username / password are set, then a {@code jmx.remote.profiles="SASL/PLAIN"} is added to the environment
   * if it doesn't already exist.
   * </p>
   * 
   * @param s the username to set
   */
  public void setUsername(String s) {
    this.username = s;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the password.
   * <p>
   * If both the username / password are set, then a {@code jmx.remote.profiles="SASL/PLAIN"} is added to the environment
   * if it doesn't already exist.
   * </p>
   * 
   * @param s the password to set
   */
  public void setPassword(String s) {
    this.password = s;
  }

  public Boolean getAdditionalDebug() {
    return additionalDebug;
  }

  /**
   * Whether or not to generate additional TRACE level debug when attempting connections.
   * 
   * @param b true to enable additional logging; default false.
   */
  public void setAdditionalDebug(Boolean b) {
    additionalDebug = b;
  }

  protected boolean additionalDebug() {
    return getAdditionalDebug() != null ? getAdditionalDebug().booleanValue() : false;
  }

  private String jmxServiceUrlForLogging() {
    return !isBlank(getJmxServiceUrl()) ? getJmxServiceUrl() : "Local JMX";
  }
}
