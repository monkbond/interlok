package com.adaptris.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.adaptris.core.stubs.LicenseStub;
import com.adaptris.core.stubs.MockChannel;
import com.adaptris.core.stubs.MockWorkflowInterceptor;
import com.adaptris.core.stubs.StubAdapterStartUpEvent;
import com.adaptris.util.TimeInterval;
import com.adaptris.util.license.License;

/**
 * <p>
 * Extension to <code>BaseCase</code> for <code>Service</code>s which provides a
 * method for marshaling sample XML config.
 * </p>
 */
public abstract class ExampleWorkflowCase extends ExampleConfigCase {

  /**
   * Key in unit-test.properties that defines where example goes unless overriden {@link #setBaseDir(String)}.
   * 
   */
  public static final String BASE_DIR_KEY = "WorkflowCase.baseDir";
  protected static final String PAYLOAD_1 = "The quick brown fox jumps over "
    + "the lazy dog";
  protected static final String PAYLOAD_2 = "Sixty zippers were quickly picked "
    + "from the woven jute bag";

  public ExampleWorkflowCase(String name) {
    super(name);

    if (PROPERTIES.getProperty(BASE_DIR_KEY) != null) {
      setBaseDir(PROPERTIES.getProperty(BASE_DIR_KEY));
    }
  }

  @Override
  protected String createExampleXml(Object object) throws Exception {
    String result = getExampleCommentHeader(object);

    Channel w = (Channel) object;

    result = result + configMarshaller.marshal(w);
    return result;
  }

  protected static void execute(Workflow w, AdaptrisMessage m)
      throws CoreException {
    w.requestStart();
    w.onAdaptrisMessage(m);
    w.requestClose();
  }

  public void testSetServiceCollection() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    ServiceCollection obj = wf.getServiceCollection();
    try {
      wf.setServiceCollection(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals(obj, wf.getServiceCollection());
  }

  public void testSetSendEvents() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    assertNull(wf.getSendEvents());
    assertTrue(wf.sendEvents());
    wf.setSendEvents(Boolean.FALSE);
    assertNotNull(wf.getSendEvents());
    assertEquals(Boolean.FALSE, wf.getSendEvents());
    assertEquals(false, wf.sendEvents());
    wf.setSendEvents(null);
    assertNull(wf.getSendEvents());
    assertTrue(wf.sendEvents());
  }

  public void testSetLogPayload() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    assertNull(wf.getLogPayload());
    assertFalse(wf.logPayload());
    wf.setLogPayload(Boolean.TRUE);
    assertNotNull(wf.getLogPayload());
    assertEquals(Boolean.TRUE, wf.getLogPayload());
    assertEquals(true, wf.logPayload());
    wf.setLogPayload(null);
    assertNull(wf.getLogPayload());
    assertFalse(wf.logPayload());
  }

  public void testSetChannelUnavailableWait() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    TimeInterval defaultInterval = new TimeInterval(30L, TimeUnit.SECONDS);
    TimeInterval interval = new TimeInterval(10L, TimeUnit.SECONDS);

    assertNull(wf.getChannelUnavailableWaitInterval());
    assertEquals(defaultInterval.toMilliseconds(), wf.channelUnavailableWait());

    wf.setChannelUnavailableWaitInterval(interval);
    assertEquals(interval, wf.getChannelUnavailableWaitInterval());
    assertNotSame(defaultInterval.toMilliseconds(), wf.channelUnavailableWait());
    assertEquals(interval.toMilliseconds(), wf.channelUnavailableWait());

    wf.setChannelUnavailableWaitInterval(null);

    assertNull(wf.getChannelUnavailableWaitInterval());
    assertEquals(defaultInterval.toMilliseconds(), wf.channelUnavailableWait());
  }

  public void testSetConsumer() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    AdaptrisMessageConsumer obj = wf.getConsumer();
    try {
      wf.setConsumer(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals(obj, wf.getConsumer());
  }

  public void testSetProducer() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    AdaptrisMessageProducer obj = wf.getProducer();
    try {
      wf.setProducer(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals(obj, wf.getProducer());
  }

  public void testSetProduceExceptionHandler() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    ProduceExceptionHandler obj = wf.getProduceExceptionHandler();
    try {
      wf.setProduceExceptionHandler(null);
      fail();
    }
    catch (IllegalArgumentException e) {

    }
    assertEquals(obj, wf.getProduceExceptionHandler());
  }

  public void testRegisterObjects() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    wf.registerChannel(new Channel());
    try {
      wf.registerChannel(null);
      fail();
    }
    catch (IllegalArgumentException e) {
      ;
    }

    wf.registerActiveMsgErrorHandler(new NullProcessingExceptionHandler());
    try {
      wf.registerActiveMsgErrorHandler(null);
      fail();
    }
    catch (IllegalArgumentException e) {
      ;
    }

    wf.registerActiveMsgErrorHandler(new NullProcessingExceptionHandler());
    try {
      wf.registerActiveMsgErrorHandler(null);
      fail();
    }
    catch (IllegalArgumentException e) {
      ;
    }

    wf.registerEventHandler(new DefaultEventHandler());
    try {
      wf.registerEventHandler(null);
      fail();
    }
    catch (IllegalArgumentException e) {
      ;
    }

  }


  public void testLicenseCombinations() throws Exception {
    assertEquals(false, createWorkflowLicenseCombo(false, true, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, true, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, false, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, false, false).isEnabled(new LicenseStub()));

    assertEquals(true, createWorkflowLicenseCombo(true, true, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, true, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, false, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, false, true).isEnabled(new LicenseStub()));

    assertEquals(true, createWorkflowLicenseCombo(true, true, true, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, true, true, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, false, true, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, false, false, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, false, false, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, true, false, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, true, false, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(false, true, true, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, false, true, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, false, false, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, false, false, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, true, false, true).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, true, false, false).isEnabled(new LicenseStub()));
    assertEquals(false, createWorkflowLicenseCombo(true, true, true, false).isEnabled(new LicenseStub()));
  }

  public void testSetInterceptors() throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    wf.setInterceptors(new ArrayList(Arrays.asList(new WorkflowInterceptor[]
    {
      new MockWorkflowInterceptor()
    })));
    assertEquals(1, wf.getInterceptors().size());
    try {
      wf.addInterceptor(null);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(1, wf.getInterceptors().size());
    wf.addInterceptor(new MockWorkflowInterceptor());
    assertEquals(2, wf.getInterceptors().size());
    try {
      wf.setInterceptors(null);
      fail();
    }
    catch (IllegalArgumentException expected) {
    }
    assertEquals(2, wf.getInterceptors().size());
  }

  protected WorkflowImp createWorkflowLicenseCombo(boolean consumerLicensed, boolean producerLicensed, boolean servicesLicensed,
                                       boolean messageErrorHandlerLicensed) throws Exception {
    WorkflowImp wf = createWorkflowLicenseCombo(consumerLicensed, producerLicensed, servicesLicensed);
    if (!messageErrorHandlerLicensed) {
      wf.setMessageErrorHandler(new StandardProcessingExceptionHandler() {
        @Override
        public boolean isEnabled(License l) {
          return false;
        }
      });

    }
    else {
      wf.setMessageErrorHandler(new StandardProcessingExceptionHandler());
    }
    return wf;
  }

  protected WorkflowImp createWorkflowLicenseCombo(boolean consumerLicensed, boolean producerLicensed, boolean servicesLicensed)
      throws Exception {
    WorkflowImp wf = createWorkflowForGenericTests();
    if (!consumerLicensed) {
      wf.setConsumer(new NullMessageConsumer() {
        @Override
        public boolean isEnabled(License l) {
          return false;
        }
      });
    }
    if (!producerLicensed) {
      wf.setProducer(new NullMessageProducer() {
        @Override
        public boolean isEnabled(License l) {
          return false;
        }
      });
    }
    if (!servicesLicensed) {
      wf.setServiceCollection(new ServiceList() {
        @Override
        public boolean isEnabled(License l) {
          return false;
        }
      });
    }
    return wf;
  }

  protected Adapter createAdapter(String uniqueId, Channel... channels) throws Exception {
    Adapter adapter = new Adapter();
    adapter.setUniqueId(uniqueId);
    adapter.getChannelList().addAll(new ArrayList(Arrays.asList(channels)));
    adapter.setStartUpEventImp(StubAdapterStartUpEvent.class.getCanonicalName());
    adapter.registerLicense(new LicenseStub());
    return adapter;
  }

  protected Channel createChannel(String uniqueId, boolean autostart, WorkflowImp... workflows) throws Exception {
    Channel channel = new MockChannel();
    channel.setUniqueId(uniqueId);
    channel.setAutoStart(autostart);
    channel.getWorkflowList().addAll(new ArrayList(Arrays.asList(workflows)));
    return channel;
  }

  protected abstract WorkflowImp createWorkflowForGenericTests() throws Exception;
}
