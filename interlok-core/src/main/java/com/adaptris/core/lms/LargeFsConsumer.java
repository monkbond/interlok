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

package com.adaptris.core.lms;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreConstants;
import com.adaptris.core.CoreException;
import com.adaptris.core.NullConnection;
import com.adaptris.core.fs.FsConsumer;
import com.adaptris.fs.FsException;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.FileDeleteStrategy;

import java.io.File;
import java.io.IOException;

import static com.adaptris.core.AdaptrisMessageFactory.defaultIfNull;

/**
 * <p>
 * File system implementation of <code>AdaptrisMessageConsumer</code> with large message support.
 * </p>
 * <p>
 * The configured <code>Base Directory URL</code> may return a string in one of two formats
 * </p>
 * <ul>
 * <li>
 * Supports URLs with both the {@code file scheme} and without.
 * </li>
 * <li>
 * If you define a directory without any leading slash or
 * if it starts with a slash is deemed to be an <strong>absolute</strong> path.
 * </li>
 * <li>
 * If "./" or "../" is used at the start of your definition then
 * the path is deemed to be <strong>relative</strong>.</li>
 * <li>
 * This is true whether using the {@code file scheme} or not.
 * </li>
 * <li>
 * With Windows systems the above is above is true, plus if you simply define the <strong>absolute</strong> path including the drive letter
 * e.g. 'c://my/path' this is also valid.
 * </li>
 * <li>
 * Both / and \ slashes are supported.
 * </li>
 * </ul>
 * <p>
 * On windows based platforms, you should always use a file based url.
 * </p>
 * <p>
 * Additionally the behaviour of this consumer is subtly different from the standard {@link FsConsumer} :
 * </p>
 * <ul>
 * <li>Encoding is supported if you use a {@link FileBackedMimeEncoder}.</li>
 * <li>The default AdaptrisMessageFactory implementation is {@link FileBackedMessageFactory}</li>
 * <li>If, at runtime, the MessageFactory implementation is not FileBackedMessageFactory, then behaviour changes to be identical to
 * to the existing {@link FsConsumer} and uses the configured FsWorker</li>
 * </ul>
 *
 * @config large-fs-consumer
 *
 */
@XStreamAlias("large-fs-consumer")
@AdapterComponent
@ComponentProfile(summary = "Pickup messages from the filesystem with large message support", tag = "consumer,fs,filesystem", metadata =
{
        CoreConstants.ORIGINAL_NAME_KEY, CoreConstants.FS_FILE_SIZE,
        CoreConstants.FILE_LAST_MODIFIED_KEY, CoreConstants.FS_CONSUME_DIRECTORY,
        CoreConstants.MESSAGE_CONSUME_LOCATION, CoreConstants.FS_CONSUME_PARENT_DIR
}, recommended =
{
    NullConnection.class
})
@DisplayOrder(order = {"baseDirectoryUrl", "poller", "createDirs", "filterExpression",
    "fileFilterImp", "fileSorter", "wipSuffix", "resetWipFiles"})
public class LargeFsConsumer extends FsConsumer {

  private transient FileCleaningTracker tracker;

  public LargeFsConsumer() {
    setMessageFactory(new FileBackedMessageFactory());
  }

  @Override
  public void init() throws CoreException {
    super.init();
    tracker = new FileCleaningTracker();
  }

  @Override
  public void start() throws CoreException {
    super.start();
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public void close() {
    super.close();
    if (tracker != null) {
      tracker.exitWhenFinished();
    }
  }

  @Override
  protected int processFile(File originalFile) throws CoreException {
    int rc = 0;
    try {
      if (originalFile.getName().endsWith(getWipSuffix())) {
        log.debug("ignoring part-processed file [{}]", originalFile.getName());
      }
      else {
        if (checkModified(originalFile) && isFileAccessible(originalFile)) {
          File wipFile = renameFile(originalFile);
          AdaptrisMessage msg = decode(wipFile);
          addStandardMetadata(msg, originalFile, wipFile);
          retrieveAdaptrisMessageListener().onAdaptrisMessage(msg);
          rc++;
          tracker.track(wipFile, msg, FileDeleteStrategy.FORCE);
        }
        else {
          log.trace("{} not deemed safe to process", originalFile.getName());
        }
      }
    }
    catch (FsException | IOException e) {
      throw new CoreException(e);
    }
    return rc;
  }

  private AdaptrisMessage decode(File wip) throws FsException, IOException, CoreException {
    AdaptrisMessage msg = null;
    if (getEncoder() != null && getEncoder() instanceof FileBackedMimeEncoder) {
      msg = ((FileBackedMimeEncoder) getEncoder()).readMessage(wip);
    } else {
      msg = defaultIfNull(getMessageFactory()).newMessage();
      if (getMessageFactory() instanceof FileBackedMessageFactory) {
        ((FileBackedMessage) msg).initialiseFrom(wip);
      } else {
        msg.setPayload(fsWorker.get(wip));
      }
    }
    return msg;
  }
}
