/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jml.impl;

import java.io.Serializable;
import java.util.Collection;

import net.sf.cindy.util.CopyOnWriteCollection;
import net.sf.jml.DisplayPictureListener;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnFileTransfer;
import net.sf.jml.MsnGroup;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnObject;
import net.sf.jml.MsnProtocol;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.event.MsnAdapter;
import net.sf.jml.event.MsnContactListListener;
import net.sf.jml.event.MsnEmailListener;
import net.sf.jml.event.MsnFileTransferListener;
import net.sf.jml.event.MsnMessageListener;
import net.sf.jml.event.MsnMessengerListener;
import net.sf.jml.event.MsnSwitchboardListener;
import net.sf.jml.exception.JmlException;
import net.sf.jml.message.MsnControlMessage;
import net.sf.jml.message.MsnDatacastMessage;
import net.sf.jml.message.MsnEmailActivityMessage;
import net.sf.jml.message.MsnEmailInitEmailData;
import net.sf.jml.message.MsnEmailInitMessage;
import net.sf.jml.message.MsnEmailNotifyMessage;
import net.sf.jml.message.MsnInstantMessage;
import net.sf.jml.message.MsnSystemMessage;
import net.sf.jml.message.MsnUnknownMessage;
import net.sf.jml.message.p2p.DisplayPictureDuelManager;
import net.sf.jml.message.p2p.DisplayPictureRetrieveWorker;
import net.sf.jml.message.p2p.MsnP2PMessage;
import net.sf.jml.protocol.MsnOutgoingMessage;

/**
 * Implement MsnMessenger basic method.
 * 
 * @author Roger Chen
 */
public abstract class AbstractMessenger implements MsnMessenger, Serializable {

    private Object attachment;
    
    /**
     * @see MsnMessenger#getAttachment()
     */
    public Object getAttachment() {
        return attachment;
    }

    /**
     * @see MsnMessenger#setAttachment(Object)
     */
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    ////////////////////////////////////////////////////////////////////////////

    private boolean logIncoming;
    
    /**
     * @see MsnMessenger#isLogIncoming()
     */
    public boolean isLogIncoming() {
        return logIncoming;
    }

    /**
     * @see MsnMessenger#setLogIncoming(boolean)
     */
    public void setLogIncoming(boolean logIncoming) {
        this.logIncoming = logIncoming;
    }

    ////////////////////////////////////////////////////////////////////////////

    private boolean logOutgoing;
    
    /**
     * @see MsnMessenger#isLogOutgoing()
     */
    public boolean isLogOutgoing() {
        return logOutgoing;
    }

    /**
     * @see MsnMessenger#setLogOutgoing(boolean)
     */
    public void setLogOutgoing(boolean logOutgoing) {
        this.logOutgoing = logOutgoing;
    }

    ////////////////////////////////////////////////////////////////////////////

	/**
	 * The Protocol that MsnMessenger supported.
	 */
    private MsnProtocol[] supportedProtocol = 
    	MsnProtocol.getAllSupportedProtocol();
    
    /**
     * @see MsnMessenger#getSupportedProtocol()
     */
    public MsnProtocol[] getSupportedProtocol() {
        return supportedProtocol;
    }

    /**
     * @see MsnMessenger#setSupportedProtocol(MsnProtocol[])
     */
    public void setSupportedProtocol(MsnProtocol[] supportedProtocol) {
        if (supportedProtocol != null && supportedProtocol.length > 0)
            this.supportedProtocol = supportedProtocol;
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * The protocol that used.
     */
    private MsnProtocol actualMsnProtocol; 
    
    /**
     * @see MsnMessenger#getActualMsnProtocol()
     */
    public MsnProtocol getActualMsnProtocol() {
        return actualMsnProtocol;
    }

    /**
     * Sets the actual version of the protocol used.
     * 
     * @param protocol Instance of the protocol version used.
     */
    public void setActualMsnProtocol(MsnProtocol protocol) {
        this.actualMsnProtocol = protocol;
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * @see MsnMessenger#send(MsnOutgoingMessage)
     */
    public void send(MsnOutgoingMessage message) {
        send(message, false);
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * @see MsnMessenger#retrieveDisplayPicture(MsnObject, DisplayPictureListener)
     */
    public void retrieveDisplayPicture(MsnObject displayPicture,
    		                           DisplayPictureListener listener)
    throws JmlException {
    	
    	// Check that the MsnObject is not null
    	if (displayPicture == null) {
    		throw new JmlException();
    	}
    	
    	// Check that the MsnObject is a display picture or an emoticon
    	if (displayPicture.getType() != MsnObject.TYPE_DISPLAY_PICTURE &&
    		displayPicture.getType() != MsnObject.TYPE_CUSTOM_EMOTICON) {
    		throw new JmlException();
    	}
    	
    	// Create a new worker for the MsnObject retrieval
		DisplayPictureRetrieveWorker worker = new DisplayPictureRetrieveWorker(
				this,
				displayPicture,
				listener);
		
		// Start the worker
		worker.start();

		// TODO: Optimize the use of switchboards
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Instance of the duel manager for this session.
     */
    private DisplayPictureDuelManager duelManager = 
    	new DisplayPictureDuelManager();
    
    /**
     * @see MsnMessenger#getDisplayPictureDuelManager()
     */
    public DisplayPictureDuelManager getDisplayPictureDuelManager() {
    	return duelManager;
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * @see MsnMessenger#addListener(MsnAdapter)
     */
    public void addListener(MsnAdapter listener) {
        addMessengerListener(listener);
        addMessageListener(listener);
        addContactListListener(listener);
        addSwitchboardListener(listener);
        addFileTransferListener(listener);
        addEmailListener(listener);
    }

    /**
     * @see MsnMessenger#removeListener(MsnAdapter)
     */
    public void removeListener(MsnAdapter listener) {
        removeMessengerListener(listener);
        removeMessageListener(listener);
        removeContactListListener(listener);
        removeSwitchboardListener(listener);
        removeFileTransferListener(listener);
        removeEmailListener(listener);
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Collection of listeners for messenger events.
     */
    @SuppressWarnings("unchecked") 
    private final Collection<MsnMessengerListener> messengerListeners = 
    	new CopyOnWriteCollection();
    
    /**
     * @see MsnMessenger#addMessengerListener(MsnMessengerListener)
     */
    public void addMessengerListener(MsnMessengerListener listener) {
        if (listener != null) {
            messengerListeners.add(listener);
        }
    }

    /**
     * @see MsnMessenger#removeMessengerListener(MsnMessengerListener)
     */
    public void removeMessengerListener(MsnMessengerListener listener) {
        if (listener != null) {
            messengerListeners.remove(listener);
        }
    }

    /**
     * Notify the listeners about login completion.
     */
    public void fireLoginCompleted() {
    	for (MsnMessengerListener listener : messengerListeners) {
            listener.loginCompleted(this);
        }
    }

    /**
     * Notify the listeners about logout completion.
     */
    public void fireLogout() {
    	for (MsnMessengerListener listener : messengerListeners) {
            listener.logout(this);
        }
    }

    /**
     * Notify the listeners about an exception.
     * 
     * @param throwable Instance of the exception. 
     */
    public void fireExceptionCaught(Throwable throwable) {
    	for (MsnMessengerListener listener : messengerListeners) {
            listener.exceptionCaught(this, throwable);
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Collection of listeners for messages events.
     */
    @SuppressWarnings("unchecked") 
    private final Collection<MsnMessageListener> messageListeners = 
    	new CopyOnWriteCollection();
    
    /**
     * @see MsnMessenger#addMessageListener(MsnMessageListener)
     */
    public void addMessageListener(MsnMessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    /**
     * @see MsnMessenger#removeMessageListener(MsnMessageListener)
     */
    public void removeMessageListener(MsnMessageListener listener) {
        if (listener != null) {
            messageListeners.remove(listener);
        }
    }

    /**
     * Notify the listeners about an instant message arrival.
     * 
     * @param switchboard Instance of the switchboard where the message was 
     * received. 
     * @param message Instance of the received message.  
     * @param contact MsnContact that has sended the instant message.
     */
    public void fireInstantMessageReceived(MsnSwitchboard switchboard,
                                           MsnInstantMessage message, 
                                           MsnContact contact) {
    	for (MsnMessageListener listener : messageListeners) {
            listener.instantMessageReceived(switchboard, message, contact);
        }
    }

    /**
     * Notify the listeners about a control message arrival.
     * 
     * @param switchboard Instance of the switchboard where the message was 
     * received. 
     * @param message Instance of the received message.  
     * @param contact MsnContact that has sended the instant message.
     */
    public void fireControlMessageReceived(MsnSwitchboard switchboard,
                                           MsnControlMessage message, 
                                           MsnContact contact) {
    	for (MsnMessageListener listener : messageListeners) {
            listener.controlMessageReceived(switchboard, message, contact);
        }
    }

    /**
     * Notify the listeners about a system message arrival.
     * 
     * @param message Instance of the received message.  
     */
    public void fireSystemMessageReceived(MsnSystemMessage message) {
    	for (MsnMessageListener listener : messageListeners) {
            listener.systemMessageReceived(this, message);
        }
    }

    /**
     * Notify the listeners about a datacast message arrival.
     * 
     * @param switchboard Instance of the switchboard where the message was 
     * received. 
     * @param message Instance of the received message.  
     * @param contact MsnContact that has sended the instant message.
     */
    public void fireDatacastMessageReceived(MsnSwitchboard switchboard,
                                            MsnDatacastMessage message, 
                                            MsnContact contact) {
    	for (MsnMessageListener listener : messageListeners) {
            listener.datacastMessageReceived(switchboard, message, contact);
        }
    }

    /**
     * Notify the listeners about a control message arrival.
     * 
     * @param switchboard Instance of the switchboard where the message was 
     * received. 
     * @param message Instance of the received message.  
     * @param contact MsnContact that has sended the instant message.
     */
    public void fireUnknownMessageReceived(MsnSwitchboard switchboard,
                                           MsnUnknownMessage message, 
                                           MsnContact contact) {
    	for (MsnMessageListener listener : messageListeners) {
            listener.unknownMessageReceived(switchboard, message, contact);
        }
    }

    /**
     * Notify the listeners about a P2P message arrival.
     * 
     * @param switchboard Instance of the switchboard where the message was 
     * received. 
     * @param message Instance of the received message.  
     * @param contact MsnContact that has sended the instant message.
     */
    public void fireP2PMessageReceived(MsnSwitchboard switchboard,
    		                           MsnP2PMessage message,
    		                           MsnContact contact) {
    	for (MsnMessageListener listener : messageListeners) {
            listener.p2pMessageReceived(switchboard, message, contact);
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Collection of listeners for email events.
     */
    @SuppressWarnings("unchecked") 
    private final Collection<MsnEmailListener> emailListeners =
    	new CopyOnWriteCollection();

    public void addEmailListener(MsnEmailListener listener) {
        if (listener != null) {
            emailListeners.add(listener);
        }
    }

    public void removeEmailListener(MsnEmailListener listener) {
        if (listener != null) {
            emailListeners.remove(listener);
        }
    }

    public void fireInitialEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailInitMessage message, MsnContact contact) {
        for (MsnEmailListener listener : emailListeners) {
            listener.initialEmailNotificationReceived(switchboard, message, contact);
        }
    }

    public void fireInitialEmailDataReceived(MsnSwitchboard switchboard,
            MsnEmailInitEmailData message, MsnContact contact) {
        for (MsnEmailListener listener : emailListeners) {
            listener.initialEmailDataReceived(switchboard, message, contact);
        }
    }

    public void fireNewEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailNotifyMessage message, MsnContact contact) {
        for (MsnEmailListener listener : emailListeners) {
            listener.newEmailNotificationReceived(switchboard, message, contact);
        }
    }

    public void fireEmailActivityNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailActivityMessage message, MsnContact contact) {
        for (MsnEmailListener listener : emailListeners) {
            listener.activityEmailNotificationReceived(switchboard, message, contact);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Collection of listeners for contact list events.
     */
    @SuppressWarnings("unchecked") 
    private final Collection<MsnContactListListener> contactListListeners = 
    	new CopyOnWriteCollection();
    
    /**
     * @see MsnMessenger#addContactListListener(MsnContactListListener)
     */
    public void addContactListListener(MsnContactListListener listener) {
        if (listener != null) {
            contactListListeners.add(listener);
        }
    }

    /**
     * @see MsnMessenger#removeContactListListener(MsnContactListListener)
     */
    public void removeContactListListener(MsnContactListListener listener) {
        if (listener != null) {
            contactListListeners.remove(listener);
        }
    }

    /**
     * Notify the listeners about the contact list synchronization completion.
     */
    public void fireContactListSyncCompleted() {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.contactListSyncCompleted(this);
        }
    }

    /**
     * Notify the listeners about the contact list initialization completion.
     */
    public void fireContactListInitCompleted() {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.contactListInitCompleted(this);
        }
    }

    /**
     * Notify the listeners about the status change of a contact.
     * 
     * @param contact Instance of the contact that has changed its status. 
     */
    public void fireContactStatusChanged(MsnContact contact) {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.contactStatusChanged(this, contact);
        }
    }

    /**
     * Notify the listeners about the change of the owner status.
     */
    public void fireOwnerStatusChanged() {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.ownerStatusChanged(this);
        }
    }

    /**
     * Notify the listeners because someone has added me to his contact list.
     * 
     * @param contact Instance of the contact that added me. 
     */
    public void fireContactAddedMe(MsnContact contact) {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.contactAddedMe(this, contact);
        }
    }

    /**
     * Notify the listeners because someone has removed me from his contact 
     * list.
     * 
     * @param contact Instance of the contact that removed me. 
     */
    public void fireContactRemovedMe(MsnContact contact) {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.contactRemovedMe(this, contact);
        }
    }

    /**
     * Notify the listeners about the completion of a contact addition.
     * 
     * @param contact Instance of the added contact. 
     */
    public void fireContactAddCompleted(MsnContact contact) {
    for (MsnContactListListener listener : contactListListeners) {
            listener.contactAddCompleted(this, contact);
        }
    }

    /**
     * Notify the listeners about the completion of a contact removal.
     * 
     * @param contact Instance of the removed contact. 
     */
    public void fireContactRemoveCompleted(MsnContact contact) {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.contactRemoveCompleted(this, contact);
        }
    }

    /**
     * Notify the listeners about the completion of a group addition.
     * 
     * @param group Instance of the added group. 
     */
    public void fireGroupAddCompleted(MsnGroup group) {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.groupAddCompleted(this, group);
        }
    }

    /**
     * Notify the listeners about the completion of a group removal.
     * 
     * @param group Instance of the removed group. 
     */
    public void fireGroupRemoveCompleted(MsnGroup group) {
    	for (MsnContactListListener listener : contactListListeners) {
            listener.groupRemoveCompleted(this, group);
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Collection of listeners for switchboard events.
     */
    @SuppressWarnings("unchecked") 
    private final Collection<MsnSwitchboardListener> switchboardListeners = 
    	new CopyOnWriteCollection();
    
    /**
     * @see MsnMessenger#addSwitchboardListener(MsnSwitchboardListener)
     */
    public void addSwitchboardListener(MsnSwitchboardListener listener) {
        if (listener != null) {
            switchboardListeners.add(listener);
        }
    }

    /**
     * @see MsnMessenger#removeSwitchboardListener(MsnSwitchboardListener)
     */
    public void removeSwitchboardListener(MsnSwitchboardListener listener) {
        if (listener != null) {
            switchboardListeners.remove(listener);
        }
    }

    /**
     * Notify the listeners about the start of a new Switchboard.
     * 
     * @param switchboard Instance of the started switchboard.
     */
    public void fireSwitchboardStarted(MsnSwitchboard switchboard) {
    	for (MsnSwitchboardListener listener : switchboardListeners) {
            listener.switchboardStarted(switchboard);
        }
    }

    /**
     * Notify the listeners about the finalization of a Switchboard.
     * 
     * @param switchboard Instance of the closed switchboard.
     */
    public void fireSwitchboardClosed(MsnSwitchboard switchboard) {
    	for (MsnSwitchboardListener listener : switchboardListeners) {
            listener.switchboardClosed(switchboard);
        }
    }

    /**
     * Notify the listeners about the addition of a contact to a Switchboard.
     * 
     * @param switchboard Instance of the switchboard.
     * @param contact Instance of the added contact.
     */
    public void fireContactJoinSwitchboard(MsnSwitchboard switchboard,
                                           MsnContact contact) {
    	for (MsnSwitchboardListener listener : switchboardListeners) {
            listener.contactJoinSwitchboard(switchboard, contact);
        }
    }

    /**
     * Notify the listeners about the removal of a contact from a Switchboard.
     * 
     * @param switchboard Instance of the switchboard.
     * @param contact Instance of the removed contact.
     */
    public void fireContactLeaveSwitchboard(MsnSwitchboard switchboard,
                                            MsnContact contact) {
    	for (MsnSwitchboardListener listener : switchboardListeners) {
            listener.contactLeaveSwitchboard(switchboard, contact);
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Collection of listeners for file transfer events.
     */
    @SuppressWarnings("unchecked") 
    private final Collection<MsnFileTransferListener> fileTransferListeners = 
    	new CopyOnWriteCollection();
    
    /**
     * @see MsnMessenger#addFileTransferListener(MsnFileTransferListener)
     */
    public void addFileTransferListener(MsnFileTransferListener listener) {
        if (listener != null) {
            fileTransferListeners.add(listener);
        }
    }

    /**
     * @see MsnMessenger#removeFileTransferListener(MsnFileTransferListener)
     */
    public void removeFileTransferListener(MsnFileTransferListener listener) {
        if (listener != null) {
            fileTransferListeners.remove(listener);
        }
    }

    /**
     * Notify the listeners that a request for a file transfer has been 
     * received.
     * 
     * @param transfer Instance of the file transfer.
     */
    public void fireFileTransferRequestReceived(MsnFileTransfer transfer) {
    	for (MsnFileTransferListener listener : fileTransferListeners) {
            listener.fileTransferRequestReceived(transfer);
        }
    }

    /**
     * Notify the listeners that a file transfer has started.
     * 
     * @param transfer Instance of the file transfer.
     */
    public void fireFileTransferStarted(MsnFileTransfer transfer) {
    	for (MsnFileTransferListener listener : fileTransferListeners) {
            listener.fileTransferStarted(transfer);
        }
    }

    /**
     * Notify the listeners about the process of a file transfer. 
     * 
     * @param transfer Instance of the file transfer.
     */
    public void fireFileTransferProcess(MsnFileTransfer transfer) {
    	for (MsnFileTransferListener listener : fileTransferListeners) {
            listener.fileTransferProcess(transfer);
        }
    }

    /**
     * Notify the listeners that a file transfer has finished.
     * 
     * @param transfer Instance of the file transfer.
     */
    public void fireFileTransferFinished(MsnFileTransfer transfer) {
    	for (MsnFileTransferListener listener : fileTransferListeners) {
            listener.fileTransferFinished(transfer);
        }
    }

}
