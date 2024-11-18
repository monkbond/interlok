package com.adaptris.core.services;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.CoreException;
import com.adaptris.core.ServiceException;
import com.adaptris.core.ServiceImp;
import com.adaptris.core.util.MessageHelper;

/**
 * This service generates a new message unique id and sets it on the message
 */
public class MessageUniqueIdService extends ServiceImp {

    @Override
    protected void initService() throws CoreException {

    }

    @Override
    protected void closeService() {

    }

    @Override
    public void doService(AdaptrisMessage msg) throws ServiceException {
        String newUniqueId = MessageHelper.generateNewMessageUniqueId(msg);
        msg.setUniqueId(newUniqueId);
    }

    @Override
    public void prepare() throws CoreException {

    }
}
