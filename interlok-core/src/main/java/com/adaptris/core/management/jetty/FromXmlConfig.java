/*
 * Copyright 2017 Adaptris Ltd.
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
package com.adaptris.core.management.jetty;

import static com.adaptris.core.util.PropertyHelper.asMap;
import static com.adaptris.core.util.PropertyHelper.getPropertySubset;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import com.adaptris.interlok.util.Args;

/**
 * Build a jetty server from xml.
 * <p>
 * The {@link XmlConfiguration} will be configured with a property set that contains all the system properties and local properties
 * (from bootstrap.properties) that are prefixed {@code jetty.}. Properties defined in bootstrap.properties override system
 * properties.
 * </p>
 * 
 */
class FromXmlConfig extends ServerBuilder {

  protected static final String JETTY_PREFIX = "jetty.";

  public FromXmlConfig(Properties initialConfig) {
    super(initialConfig);
  }

  @Override
  protected Server build() throws Exception {
    log.trace("Create Server from XML");
    final XmlConfiguration xmlConfiguration = new XmlConfiguration(getJettyConfigResource());
    Map<String, String> configProperties = mergeWithSystemProperties();
    log.trace("Additional properties for XML Config {}", configProperties);
    xmlConfiguration.getProperties().putAll(configProperties);
    return (Server) xmlConfiguration.configure();
  }

  protected Map<String, String> mergeWithSystemProperties() {
    Map<String, String> result = asMap(getPropertySubset(System.getProperties(), JETTY_PREFIX));
    result.putAll(asMap(getPropertySubset(getConfig(), JETTY_PREFIX)));
    return result;
  }

  protected Resource getJettyConfigResource() throws Exception {
    // if we get here, and WEB_SERVER_CONFIG_FILE_NAME_CGF_KEY is null, we're in trouble
    String jettyConfigUrl = Args.notBlank(getConfigItem(WEB_SERVER_CONFIG_FILE_NAME_CGF_KEY), WEB_SERVER_CONFIG_FILE_NAME_CGF_KEY);
    final URL url = createUrlFromString(jettyConfigUrl);
    log.trace("Connecting to configured URL {}", url.toString());
    return Resource.newResource(url);
  }

  protected static URL createUrlFromString(String s) throws Exception {
    String destToConvert = s.replace('\\', '/');
    URI configuredUri = null;
    try {
      configuredUri = new URI(destToConvert);
    }
    catch (URISyntaxException e) {
      // Specifically here to cope with file:///c:/ (which is
      // technically illegal according to RFC2396 but we need
      // to support it
      if (destToConvert.split(":").length >= 3) {
        configuredUri = new URI(URLEncoder.encode(destToConvert, "UTF-8"));
      }
      else {
        throw e;
      }
    }
    return configuredUri.getScheme() == null ? relativeConfig(configuredUri) : new URL(configuredUri.toString());
  }

  private static URL relativeConfig(URI uri) throws Exception {
    String pwd = backslashToSlash(Args.notNull(System.getProperty("user.dir"), "user.dir"));
    String path = pwd + "/" + uri;
    URL result = new URL("file:///" + new URI(null, path, null).toASCIIString());
    return result;
  }

  private static String backslashToSlash(String url) {
    return url.replace('\\', '/');
  }
}
