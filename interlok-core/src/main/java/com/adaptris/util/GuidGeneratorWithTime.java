package com.adaptris.util;

import java.nio.ByteBuffer;
import java.util.Date;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.security.util.SecurityUtil;
import com.adaptris.util.text.Base58;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Generates a unique id that is still globally unique similar to {@link GuidGenerator} but contains
 * a date/time component.
 * 
 * <p>
 * You can use this {@link IdGenerator} if you need to record the date that a GUID was generated.
 * The date that the GUID was generated can be found the {@link #computeTime(String)} method.
 * </p>
 * 
 * @config guid-generator-with-time
 * @since 3.9.1
 */
@XStreamAlias("guid-generator-with-time")
@ComponentProfile(
    summary = "unique id generator that is globally unique, but contains a time component",
    since = "3.9.1")
public class GuidGeneratorWithTime extends GuidGenerator {

  protected static final int TIME_SIGNIFICANT_BYTES_OFFSET = 3;
  protected static final int TIME_BYTE_LENGTH = 5;
  protected static final int RANDOM_BYTE_LENGTH = 11;
  protected static final int UUID_LENGTH = 16;

  public GuidGeneratorWithTime() {

  }

  @Override
  public String getUUID() {
    final byte[] uuid = binaryUUID();
    return Base58.encode(uuid);
  }

  private byte[] binaryUUID() {
    final byte[] transactionId = new byte[UUID_LENGTH];
    final byte[] timeComponent = someBytes(() ->{
      final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
      buffer.putLong(System.currentTimeMillis());
      return buffer.array();
    });

    final byte[] randomComponent = someBytes(()-> {
      byte[] bytes = new byte[RANDOM_BYTE_LENGTH];
      synchronized (bytes) {
        SecurityUtil.getSecureRandom().nextBytes(bytes);
      }
      return bytes;
    });

    System.arraycopy(timeComponent, TIME_SIGNIFICANT_BYTES_OFFSET, transactionId, 0,
        TIME_BYTE_LENGTH);
    System.arraycopy(randomComponent, 0, transactionId, TIME_BYTE_LENGTH, RANDOM_BYTE_LENGTH);
    return transactionId;
  }


  /**
   * Returns a date that contains the time portion of the UUID.
   * 
   * @param uuid the uuid generated by {@link #getUUID()}.
   * @return a {@link java.util.Date} object computed from the UUID.
   */
  public static Date computeTime(String uuid) {
    return computeTime(Base58.decode(uuid));
  }

  private static Date computeTime(byte[] byteArray) {
    long timepart = getTimepart(byteArray, 0);
    // magic number (it's 2^40), which should give us the missing 3 bytes we've discarded.
    Date date = new Date(timepart + 1099511627776L);
    return date;
  }

  private static long getTimepart(byte[] byteArray, long timepart) {
    for (int i = 0; i < 5; i++) {
      timepart = (timepart << 8) + (byteArray[i] & 0xff);
    }
    return timepart;
  }

  protected static byte[] someBytes(BytesProvider p) {
    return p.bytes();
  }

  @FunctionalInterface
  protected static interface BytesProvider {
    byte[] bytes();
  }
}
