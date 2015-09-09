package com.adaptris.util.datastore;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author lchan
 */
public class TestSimpleDataStore {

  private static String type = "type";
  private static String id = "id";
  private static String data = "This is the data part";
  private boolean initialised = false;
  private static Log logR = LogFactory.getLog(TestSimpleDataStore.class);

  private DataStore dataStore = null;
  private Properties dataStoreProperties;


  @Before
  public void setUp() throws Exception {
    dataStoreProperties = createProperties();
    dataStore = new SimpleDataStore(dataStoreProperties);
  }

  @After
  public void tearDown() throws Exception {
    File f = new File(dataStoreProperties.getProperty(SimpleDataStore.FILE_PROPERTY));
    FileUtils.deleteQuietly(f);
    f = new File(dataStoreProperties.getProperty(SimpleDataStore.LOCK_PROPERTY));
    FileUtils.deleteQuietly(f);

  }

  @Test
  public void testSimpleDataStoreExists() throws Exception {
    dataStore.persist(id, type, data);
    assertEquals(dataStore.exists(id, type), true);
  }

  @Test
  public void testSimpleDataStoreRetrieve() throws Exception {
    dataStore.persist(id, type, data);
    String s = (String) dataStore.retrieve(id, type);
    assertEquals(s, data);
  }

  @Test
  public void testSimpleDataStoreRemove() throws Exception {
    dataStore.persist(id, type, data);
    String s = (String) dataStore.retrieve(id, type);
    assertEquals(s, data);
    dataStore.remove(id, type);
    assertEquals(dataStore.exists(id, type), false);
  }

  private static Properties createProperties() throws IOException {
    Properties sp = new Properties();
    File dataFile = File.createTempFile("junitsds", ".dat");
    File lockFile = File.createTempFile("junitsds", ".lock");
    dataFile.delete();
    lockFile.delete();
    sp.setProperty(SimpleDataStore.FILE_PROPERTY, dataFile.getCanonicalPath());
    sp.setProperty(SimpleDataStore.LOCK_PROPERTY, lockFile.getCanonicalPath());
    sp.setProperty(SimpleDataStore.MAXLOCK_PROPERTY, "10");
    return sp;
  }
}
