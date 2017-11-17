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

package com.adaptris.util.text.xml;

import static org.apache.commons.lang.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang.BooleanUtils.toBooleanObject;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * @author Stuart Ellidge
 * 
 */
// * Class which provides convenience methods for executing XPath queries.
// * <b>!!!IMPORTANT!!! The underlying Xalan apis create a new entry in the DTM
// * (Document Table Model) for each document processed. Therefore you MUST use a
// * new instance of this class for each document processed otherwise you will
// * create a memory leak </b>
public class XPath {

  private NamespaceContext context = null;

  /**
   * {@value #SYSPROP_USE_SAXON_XPATH} controls whether Saxon is explicitly enabled as an {@link XPathFactory} (defaults to true).
   * <p>
   * From the <a href="https://www.saxonica.com/html/documentation/xpath-api/jaxp-xpath/factory.html">saxon documentation</a> :
   * <strong>Saxon therefore no longer identifies itself (in the JAR file manifest) as a JAXP XPath supplier. If you want to load
   * Saxon as your XPath engine, you need to select it explicitly; it's not enough to just put it on the classpath.</strong>. If set
   * to true, then we attempt to use {@code com.saxonica.config.EnterpriseXPathFactory},
   * {@code com.saxonica.config.ProfessionalXPathFactory} and {@code net.sf.saxon.xpath.XPathFactoryImpl} as XPathFactory instances
   * (in that order).
   * <p>
   */
  public static final String SYSPROP_USE_SAXON_XPATH = "interlok.useSaxonXPath";

  private static final boolean useSaxonXpath = toBooleanDefaultIfNull(
      toBooleanObject(System.getProperty(SYSPROP_USE_SAXON_XPATH, "true")), true);

  private static final String[] SAXON_XPATH_FACTORIES =
  {
      // Narrow down in terms of license... enterpise, then pro, then HE.
      "com.saxonica.config.EnterpriseXPathFactory", "com.saxonica.config.ProfessionalXPathFactory",
      "net.sf.saxon.xpath.XPathFactoryImpl"
  };

  private XPathFactory xpathFactory;
  
  public XPath() {
    xpathFactory = newXPathFactory();
  }

  public XPath(NamespaceContext ctx) {
    this();
    context = ctx;
  }

  private javax.xml.xpath.XPath createXpath() {
    javax.xml.xpath.XPath result = xpathFactory.newXPath();
    if (context != null) {
      result.setNamespaceContext(context);
    }
    return result;
  }

  /**
   * returns the string value contained in an element returned by an XPath
   *
   * @param context the node to apply the XPath to
   * @param xpath the xpath to apply
   * @return the string extracted
   * @throws XPathExpressionException on error
   */
  public String selectSingleTextItem(Node context, String xpath)
      throws XPathExpressionException {
    return (String) createXpath().evaluate(xpath, context, XPathConstants.STRING);
  }

  /**
   * returns an array of string values taken from a list of elements returned by
   * an xpath
   *
   * @param context the node to apply the XPath to
   * @param xpath the xpath to apply
   * @return the strings extracted
   * @throws XPathExpressionException on error
   */
  public String[] selectMultipleTextItems(Node context, String xpath)
      throws XPathExpressionException {
    NodeList list = selectNodeList(context, xpath);
    String[] retArray = new String[list.getLength()];

    for (int i = 0; i < list.getLength(); i++) {
      Node node = list.item(i);
      if (node != null) {
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
          retArray[i] = node.getNodeValue();
        }
        else if (node.getNodeType() == Node.TEXT_NODE) {
          retArray[i] = node.getNodeValue();
        }
        else {
          node.normalize();
          Node text = node.getFirstChild();
          retArray[i] = text.getNodeValue();
        }
      }
    }
    return retArray;
  }

  /**
   * selects a list of Nodes from the context node using the supplied xpath
   *
   * @param context the root node to query
   * @param xpath the xpath to apply
   * @return NodeList of returned Nodes
   * @throws XPathExpressionException on error.
   */
  public NodeList selectNodeList(Node context, String xpath)
      throws XPathExpressionException {
    return (NodeList) createXpath().evaluate(xpath, context, XPathConstants.NODESET);
  }

  /**
   * Selects a single Node based on the supplied Xpath
   *
   * @param context the root node to query
   * @param xpath the xpath to apply
   * @return the Node extracted
   * @throws XPathExpressionException on error.
   */
  public Node selectSingleNode(Node context, String xpath)
      throws XPathExpressionException {
    return (Node) createXpath().evaluate(xpath, context, XPathConstants.NODE);
  }

  /**
   * Convenience method to create a new {@link XPathFactory}.
   * 
   * @return either a Saxon based XPathFactory or one auto-found by {@link XPathFactory#newInstance()}
   */
  public static XPathFactory newXPathFactory() {
    return build(useSaxonXpath);
  }

  static XPathFactory build(boolean useSaxon) {
    if (useSaxon) {
      for (String clazz : SAXON_XPATH_FACTORIES) {
        try {
          return (XPathFactory) Class.forName(clazz).newInstance();
        } catch (Exception e) {
          
        }
      }
    }
    return XPathFactory.newInstance();
  }

}
