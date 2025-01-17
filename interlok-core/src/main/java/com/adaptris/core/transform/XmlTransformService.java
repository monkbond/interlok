/*
 * Copyright 2015 Adaptris Ltd.
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

package com.adaptris.core.transform;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.transform.Transformer;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.interlok.config.DataInputParameter;
import com.adaptris.util.TimeInterval;
import com.adaptris.util.text.xml.XmlTransformer;
import com.adaptris.util.text.xml.XmlTransformerFactory;
import com.adaptris.util.text.xml.XsltTransformerFactory;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * <p>
 * Implementation of <code>Service</code> which provides transformation of XML payloads.
 * </p>
 * <p>
 * You are required to configure the XML transformer factory; see the javadoc and implementations of {@link XmlTransformerFactory} for
 * details on the supported transformer factories.
 * </p>
 * <p>
 * Configuration including allow over-ride behaviour matches previous implementation.
 * </p>
 * <p>
 * Cache transforms functionality only works if <code>url</code> is used. Caching is not supported with <code>mappingSource</code>.
 * </p>
 * <p>
 * File path context when using XSL import functionality only works if <code>url</code> is used. Caching is not supported with <code>mappingSource</code>.
 * </p>
 * <p>
 * If you wish to call an external mapping source when using <code>mappingSource</code> such as via http you can use
 * <strong>FileDataInputParameter</strong>. However that will not keep the file path context when using XSL import.
 * </p>
 *
 * @config xml-transform-service
 *
 */
@XStreamAlias("xml-transform-service")
@AdapterComponent
@ComponentProfile(summary = "Execute an XSLT transform", tag = "service,transform,xml")
@DisplayOrder(order = { "url", "outputMessageEncoding", "cacheTransforms", "allowOverride", "metadataKey", "transformParameter",
    "xmlTransformerFactory", "mappingSource" })
public class XmlTransformService extends ServiceImp {
  private static final int DEFAULT_MAX_CACHE_SIZE = 16;
  private static final TimeInterval DEFAULT_EXPIRATION = new TimeInterval(1L, TimeUnit.HOURS);

  private String url;

  @AdvancedConfig
  private String metadataKey;

  /**
   * Use mapping-source to use an inline mapping instead of a URL
   */
  @Getter
  @Setter
  @AutoPopulated
  @Valid
  private DataInputParameter<String> mappingSource;

  @AdvancedConfig
  @InputFieldDefault(value = "true")
  private Boolean cacheTransforms;
  @AdvancedConfig
  @InputFieldDefault(value = "false")
  private Boolean allowOverride;

  // Default to null is fine
  private String outputMessageEncoding;
  @NotNull
  @AutoPopulated
  @Valid
  private XmlTransformerFactory xmlTransformerFactory;
  @Valid
  private XmlTransformParameter transformParameter;

  private transient Map<String, Transformer> transforms = null;
  // This is the override value which is set to true if url is null
  private transient Boolean overrideAllowOverride;

  protected transient Boolean isUrlMappingSource;

  /**
   * <p>
   * Creates a new instance. Defaults to caching transforms and not allowing over-rides. Default metadata key is <code>transformurl</code>.
   * </p>
   */
  public XmlTransformService() {
    setMetadataKey(CoreConstants.TRANSFORM_OVERRIDE);
    xmlTransformerFactory = new XsltTransformerFactory();
  }

  @Override
  protected void initService() throws CoreException {
    if (isEmpty(getUrl())) {
      overrideAllowOverride = Boolean.TRUE;
    }
    if (isEmpty(getUrl()) && isEmpty(getMetadataKey()) && getMappingSource() == null) {
      throw new CoreException("metadata-key, mapping source & url are empty, cannot initialise");
    }
  }

  @Override
  protected void closeService() {
  }

  /**
   * @see com.adaptris.core.Service#doService (com.adaptris.core.AdaptrisMessage)
   */
  @Override
  public void doService(AdaptrisMessage msg) throws ServiceException {
    doTransform(msg, obtainMappingContent(msg));
  }

  @Override
  public void prepare() throws CoreException {
    transforms = ExpiringMap.builder().maxSize(DEFAULT_MAX_CACHE_SIZE)
        .expirationPolicy(ExpirationPolicy.ACCESSED)
        .expiration(DEFAULT_EXPIRATION.toMilliseconds(), TimeUnit.MILLISECONDS).build();
  }

  /**
   * <p>
   * If <code>this.getAllowOverride()</code> is true and <code>msg.containsKey(getMetadataKey())</code> is true then the value of
   * <code>msg.getMetadataValue(this.getMetadataKey()</code> is the URL to use, otherwise the value of <code>this.getUrl()</code> is used,
   * if <code>this.getUrl()</code> is empty or null then it checks <code>this.getMappingSource()</code> if this is also null a
   * <code>ServiceException</code> is thrown.
   * </p>
   */
  String obtainMappingContent(AdaptrisMessage msg) throws ServiceException {
    try {
      if (!isEmpty(mappingUrlSource(msg))) {
        isUrlMappingSource = true;
        return mappingUrlSource(msg);
      } else if (getMappingSource() != null) {
        isUrlMappingSource = false;
        return getMappingSource().extract(msg);
      } else {
        throw new ServiceException(
            "no URL or mapping source has been configured and metadata key [" + getMetadataKey() + "] returned null");
      }

    } catch (Exception e) {
      throw new ServiceException(e);
    }
  }

  private String mappingUrlSource(AdaptrisMessage msg) {
    String mappingUrl = null;
    if (!isEmpty(getUrl())) {
      mappingUrl = getUrl();
    }
    if (allowOverride() && msg.headersContainsKey(getMetadataKey())) {
      mappingUrl = isEmpty(msg.getMetadataValue(getMetadataKey())) ? getUrl() : msg.getMetadataValue(getMetadataKey());
    }
    return mappingUrl;
  }

  private void doTransform(AdaptrisMessage msg, String xslSourceToUse) throws ServiceException {
    XmlTransformer xmlTransformerImpl = new XmlTransformer();
    Transformer transformer = null;

    try {
      if (isUrlMappingSource) {
        if (cacheTransforms()) {
          transformer = cacheAndGetTransformer(xslSourceToUse, getXmlTransformerFactory());
        } else {
          transformer = getXmlTransformerFactory().createTransformerFromUrl(xslSourceToUse);
        }
      } else {
        transformer = getXmlTransformerFactory().createTransformerFromRawXsl(xslSourceToUse);
      }

      getXmlTransformerFactory().configure(xmlTransformerImpl);
    } catch (Exception ex) {
      throw new ServiceException(ex);
    }
    // INTERLOK-2022 Let the XML parser do its thing, rather than using a reader/writer.
    try (InputStream input = msg.getInputStream(); OutputStream output = msg.getOutputStream()) {
      Map<Object, Object> parameters = getParameterBuilder().createParameters(msg, null);
      xmlTransformerImpl.transform(transformer, input, output, xslSourceToUse, parameters);
      if (!StringUtils.isBlank(getOutputMessageEncoding())) {
        msg.setContentEncoding(getOutputMessageEncoding());
      }
    } catch (Exception e) {
      throw new ServiceException("failed to transform message", e);
    }
  }

  protected Transformer cacheAndGetTransformer(String xslToUse, XmlTransformerFactory xmlTransformerFactory) throws Exception {
    if (getTransforms().containsKey(xslToUse)) {
      return getTransforms().get(xslToUse);
    } else {
      Transformer transformer = xmlTransformerFactory.createTransformerFromUrl(xslToUse);
      getTransforms().put(xslToUse, transformer);
      return transformer;
    }
  }

  // properties...

  /**
   * <p>
   * Returns the URL of the XSLT to use.
   * </p>
   *
   * @return the URL of the XSLT to use
   */
  public String getUrl() {
    return url;
  }

  /**
   * <p>
   * Sets the URL of the XSLT to use. May not be empty.
   * Using a URL will keep file path context when using XSL import. 
   * </p>
   *
   * @param s the URL of the XSLT to use
   */
  public void setUrl(String s) {
    if ("".equals(s)) {
      throw new IllegalArgumentException("null param");
    }
    url = s;
  }

  /**
   * <p>
   * Returns the metadata key against which an over-ride XSLT URL may be stored.
   * </p>
   *
   * @return the metadata key against which an over-ride XSLT URL may be stored
   */
  public String getMetadataKey() {
    return metadataKey;
  }

  /**
   * <p>
   * Sets the metadata key against which an over-ride XSLT URL may be stored. May not be empty.
   * </p>
   *
   * @param s the metadata key against which an over-ride XSLT URL may be stored, defaults to
   *          {@value com.adaptris.core.CoreConstants#TRANSFORM_OVERRIDE}
   */
  public void setMetadataKey(String s) {
    if ("".equals(s)) {
      throw new IllegalArgumentException("null param");
    }
    metadataKey = s;
  }

  /**
   * <p>
   * Returns true if XSLTs should be cached. This functionality only works if {@link XmlTransformService#getUrl()} is used.
   * </p>
   *
   * @see #setCacheTransforms(Boolean)
   * @return true if XSLTs should be cached, otherwise false
   */
  public Boolean getCacheTransforms() {
    return cacheTransforms;
  }

  /**
   * <p>
   * Sets whether XSLTs should be cached or not. If this is false the XSLT will be read for each message processed. Therefore while any
   * changes to the XSLT will be picked up immediately, processing will take significantly longer, particularly if the XSLT is on a remote
   * machine. This functionality only works if {@link XmlTransformService#getUrl()} is used.
   * </p>
   *
   * @param b whether XSLTs should be cached or not, defaults to true.
   */
  public void setCacheTransforms(Boolean b) {
    cacheTransforms = b;
  }

  boolean cacheTransforms() {
    return BooleanUtils.toBooleanDefaultIfNull(getCacheTransforms(), true);
  }

  /**
   * <p>
   * Returns true if a configured XSLT URL may be over-ridden by one stored against a metadata key.
   * </p>
   *
   * @return true if a configured XSLT URL may be over-ridden by one stored against a metadata key, otherwise false
   */
  public Boolean getAllowOverride() {
    return allowOverride;
  }

  /**
   * <p>
   * Sets whether the configured XSLT URL may be over-ridden by one stored against a metaddata key. If URL is configured this is implicitly
   * true.
   * </p>
   *
   * @param b whether the configured XSLT URL may be over-ridden by one stored against a metadata key, defaults to null (false)
   */
  public void setAllowOverride(Boolean b) {
    allowOverride = b;
  }

  boolean allowOverride() {
    if (overrideAllowOverride != null) {
      return overrideAllowOverride.booleanValue();
    }
    return BooleanUtils.toBooleanDefaultIfNull(getAllowOverride(), false);
  }

  public String getOutputMessageEncoding() {
    return outputMessageEncoding;
  }

  /**
   * Force the output message encoding to be a particular encoding.
   * <p>
   * If specified then the underlying {@link com.adaptris.core.AdaptrisMessage#setCharEncoding(String)} is changed to match the encoding
   * specified here before attempting any write operations.
   * </p>
   * <p>
   * This is only useful if the underlying message is encoded in one way, and you wish to force the encoding directly in your stylesheet;
   * e.g. the message is physically encoded using ISO-8859-1; but your xslt has &lt;xsl:output method="xml" encoding="UTF-8"
   * indent="yes"/&gt; and you need to ensure that the message is physically encoded using UTF-8 after the transform.
   * </p>
   *
   * @param s the output encoding; if null, the the existing encoding specified by
   *          {@link com.adaptris.core.AdaptrisMessage#getCharEncoding()} is used.
   */
  public void setOutputMessageEncoding(String s) {
    outputMessageEncoding = s;
  }

  public XmlTransformerFactory getXmlTransformerFactory() {
    return xmlTransformerFactory;
  }

  public void setXmlTransformerFactory(XmlTransformerFactory xmlTransformerFactory) {
    this.xmlTransformerFactory = xmlTransformerFactory;
  }

  public XmlTransformParameter getTransformParameter() {
    return transformParameter;
  }

  public void setTransformParameter(XmlTransformParameter param) {
    transformParameter = param;
  }

  protected XmlTransformParameter getParameterBuilder() {
    return getTransformParameter() != null ? getTransformParameter() : new IgnoreMetadataParameter();
  }

  Map<String, Transformer> getTransforms() {
    return transforms;
  }
}
