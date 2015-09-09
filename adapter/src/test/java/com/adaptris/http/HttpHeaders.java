package com.adaptris.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.internet.InternetHeaders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.adaptris.util.text.Conversion;

/** Fields used during an HTTP Conversation.
 *  <p>This class allows the manipulation of an HTTP conversation.  
 *  Both the client and server side of the conversation can be handled using 
 *  this class
 */
public class HttpHeaders implements DataTransfer {

  private InternetHeaders headers;
  private static Log logR = null;

  /** @see Object#Object()
   */
  public HttpHeaders() {
    if (logR == null) {
      logR = LogFactory.getLog(this.getClass());
    }
    headers = new InternetHeaders();
  }

  /**
   * Add an entry into the collection, if values is null, then the entry is 
   * removed
   * @param key the header
   * @param value the values to associated with this key
   */
  public void put(String key, String value) {
    if (value == null) {
      removeHeader(key);
    } else {
      if (this.containsHeader(key)) {
        removeHeader(key);
      }
      headers.setHeader(key, value);
    }
    return;
  }

  /**
   * remove an entry from the collection, the search for the key is 
   * case-insensitive.
   * @param key the key
   */
  public void removeHeader(String key) {
    if (key != null) {
      headers.removeHeader(key);
    }
  }

  /** Get an enumeration on the existing keys in this set of headers.
   * @return an enumeration containing javax.mail.Header objects.
   * @see javax.mail.Header
   */
  public Enumeration getAllHeaders() {
    return (headers.getAllHeaders());
  }

  /** Return an enumeration containing all the headers as strings.
   *  @return an enumeration containing strings in the form 
   *  <code>Content-Length: 5555</code>
   */
  public Enumeration getAllHeaderLines() {
    return (headers.getAllHeaderLines());
  }
  /**
   * Get the values associated with this key
   * @return the value associated with the key or null
   * @param key the key.
   */
  public String get(String key) {

    String rc = null;
    if ((key != null) && this.containsHeader(key)) {
      String[] s = headers.getHeader(key);
      rc = s[0];
    }
    return rc;
  }
  
  /** Add all the header elements to this set of headers.
   *  <p>Any existing headers will be replaced.
   * 
   * @param other the HttpHeaders object containing the headers to be added.
   */
  public void putAll(HttpHeaders other) {
    Enumeration e = other.getAllHeaders();
    while (e.hasMoreElements()) {
      Header h = (Header) e.nextElement();
      this.put(h.getName(), h.getValue());
    }    
  }

  /**
   * Check if this key currently exists in the collection.
   * @return true if key is present
   * @param key the key
   */
  public boolean containsHeader(String key) {
    String[] keys = { key };
    Enumeration e = headers.getMatchingHeaders(keys);
    return (e.hasMoreElements());
  }

  /** Return the content length associated with this set of headers.
   *  @return the content length, -1 if it is not specified, or does not exist
   */
  public int getContentLength() {
    return (
      containsHeader(Http.CONTENT_LENGTH)
        ? Integer.parseInt(get(Http.CONTENT_LENGTH).trim())
        : -1);
  }

  /** Read the header portion of a HTTP converstion.
   * @see DataTransfer#load(InputStream)
   */
  public void load(InputStream in) throws HttpException {
    headers = new InternetHeaders();
    synchronized (in) {
      try {
        headers.load(in);
      } catch (Exception e) {
        throw new HttpException(e);
      }
    }
    return;
  }

  /** Write this set of headers to the supplied outputstream.
   *  @see DataTransfer#writeTo(OutputStream)
   */
  public void writeTo(OutputStream out) throws HttpException {
    try {

      PrintStream p = new PrintStream(out);
      p.print(toString());
      p.flush();
    } catch (Exception e) {
      throw new HttpException(e);
    }
  }

  /** @see Object#toString()
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    Enumeration e = this.getAllHeaderLines();
    while (e.hasMoreElements()) {
      sb.append(e.nextElement().toString());
      sb.append(Http.CRLF);
    }
    sb.append(Http.CRLF);
    return (sb.toString());
  }

  /** Return a string representation of this set of headers adding the specified
   *  content length to the headers.
   *  @see #toString()
   */
  public String toString(int contentLength) {
    this.put(Http.CONTENT_LENGTH, String.valueOf(contentLength));
    return toString();
  }

  /** Build a RFC2617 basic authorisation string.
   * 
   * @param user the user
   * @param password the password
   * @return the RFC2617 auth
   */
  public static String buildBasicRfc2617(String user, String password) {

    String authString = "";
    if ((user != null) && (user.length() > 0)) {

      String source = user + ":" + password;
      authString =
        "Basic " + Conversion.byteArrayToBase64String(source.getBytes());
    }
    return authString;
  }
}