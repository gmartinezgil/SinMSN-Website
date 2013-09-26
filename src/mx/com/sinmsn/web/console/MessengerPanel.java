/**
 * 
 */
package mx.com.sinmsn.web.console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import mx.com.sinmsn.web.MainApplication;
import mx.com.sinmsn.web.session.SignInSession;
import mx.com.sinmsn.web.util.WebUtil;
import net.sf.jml.DisplayPictureListener;
import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnGroup;
import net.sf.jml.MsnList;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnObject;
import net.sf.jml.MsnOwner;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.MsnUserStatus;
import net.sf.jml.event.MsnContactListListener;
import net.sf.jml.event.MsnMessageListener;
import net.sf.jml.event.MsnSwitchboardListener;
import net.sf.jml.message.MsnControlMessage;
import net.sf.jml.message.MsnDatacastMessage;
import net.sf.jml.message.MsnInstantMessage;
import net.sf.jml.message.MsnSystemMessage;
import net.sf.jml.message.MsnUnknownMessage;
import net.sf.jml.message.p2p.DisplayPictureRetrieveWorker;
import net.sf.jml.message.p2p.MsnP2PMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkIconPanel;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.wicketstuff.dojo.markup.html.floatingpane.DojoFloatingPane;

import wicket.contrib.tinymce.TinyMceBehavior;
import wicket.contrib.tinymce.settings.ContextMenuPlugin;
import wicket.contrib.tinymce.settings.EmotionsPlugin;
import wicket.contrib.tinymce.settings.SpellCheckPlugin;
import wicket.contrib.tinymce.settings.TinyMCESettings;

/**
 * @author jerry
 *
 */
public final class MessengerPanel extends Panel {
	private static final long serialVersionUID = 1L;
	
	//the reference to the messenger...
	private transient MsnMessenger messenger = ((MainApplication)getApplication()).getMessengerService().getMessengerUser(((SignInSession)getSession()).getUser().getLogin()); 
	//the reusable dialog...
	private ContactDialogPane contactDialog;
	
	//the log...
	private static final Log log = LogFactory.getLog(MessengerPanel.class);
	
	/**
	 * @param id
	 */
	//TODO: pass the messenger reference as the model...
	public MessengerPanel(String id/*, IModel model*/) {
		super(id);
		init();
	}
	
	private void init()  {
		//CONTACT LIST
		contactDialog = new ContactDialogPane("contactDialog");
		add(contactDialog);
		final ContactListPane contacts = new ContactListPane("contactsFloatingPane");
		add(contacts);
	}
	
	/**
	 * 
	 * ContactListPane
	 *
	 */
	final class ContactListPane extends DojoFloatingPane implements MsnContactListListener {
		private static final long serialVersionUID = 1L;

		//the status...
		private transient boolean contactStatusChanged;
		/**
		 * @param id
		 */
		public ContactListPane(String id) {
			super(id);
			init();
		}
		
		private void init()  {
			//to see the changes in the contact list...
			messenger.addContactListListener(this);
			
			//the default initialization...
			setDisplayCloseAction(false);
			setHeight("700px");
			setWidth("300px");
			setTitle("Messenger - "+messenger.getOwner().getDisplayName());
			setHasShadow(true);
			
			//define the list of contacts...
			TreeModel treeModel = getContactListTreeModel(messenger.getOwner(), Arrays.asList(messenger.getContactList().getContactsInList(MsnList.AL)));
			final BaseTree tree = new LinkTree("msnContacts", treeModel) {
				private static final long serialVersionUID = 1L;
				protected Component newNodeComponent(String id, IModel model) {
					return new LinkIconPanel(id, model, this) {
						private static final long serialVersionUID = 1L;
						protected void onNodeLinkClicked(TreeNode node, BaseTree tree, AjaxRequestTarget target) {
							tree.getTreeState().selectNode(node, !tree.getTreeState().isNodeSelected(node));
							onClicked(node, tree, target);
						}
						protected Component newContentComponent(String componentId, BaseTree tree, IModel model) {
							DefaultMutableTreeNode node = (DefaultMutableTreeNode)model.getObject();
							if (node.getUserObject() instanceof MsnContact)  {
								final MsnContact contact = (MsnContact)node.getUserObject();
								return new Label(componentId, contact.getDisplayName());
							}
							else  {
								return new Label(componentId, getNodeTextModel(model));
							}
						}
						protected Component newImageComponent(String componentId, BaseTree tree, IModel model) {
							DefaultMutableTreeNode node = (DefaultMutableTreeNode)model.getObject();
							if(node.getUserObject() instanceof MsnContact)  {
								final MsnContact contact = (MsnContact)node.getUserObject();
								Image contactIcon = new Image(componentId);
								if("ONLINE".equals(contact.getStatus().getDisplayStatus()))  {
									contactIcon.setImageResource(WebUtil.ONLINE_CONTACT_IMAGE);
									return contactIcon;
								}
								if("BUSY".equals(contact.getStatus().getDisplayStatus()) || "ON THE PHONE".equals(contact.getStatus().getDisplayStatus()))  {
									contactIcon.setImageResource(WebUtil.BUSY_CONTACT_IMAGE);
									return contactIcon;
								}
								if("AWAY".equals(contact.getStatus().getDisplayStatus()) || "BE RIGHT BACK".equals(contact.getStatus().getDisplayStatus()) || "OUT TO LUNCH".equals(contact.getStatus().getDisplayStatus())) {
									contactIcon.setImageResource(WebUtil.AWAY_CONTACT_IMAGE);
									return contactIcon;
								}
								if("OFFLINE".equals(contact.getStatus().getDisplayStatus()))  {
									contactIcon.setImageResource(WebUtil.OFFLINE_CONTACT_IMAGE);
									return contactIcon;
								}
								else {
									return super.newImageComponent(componentId, tree, model);
								}
							}
							else  {
								return super.newImageComponent(componentId, tree, model);
							}
						}
					};
				}
				protected void onClicked(TreeNode node, BaseTree tree, AjaxRequestTarget target) {
					if (!node.isLeaf()) {
						if (tree.getTreeState().isNodeExpanded(node)) {
							collapseAll(tree, node);
						} else {
							expandAll(tree, node);
						}
						tree.updateTree(target);
					} else {
						final MsnContact contactToTalk = (MsnContact)((DefaultMutableTreeNode) node).getUserObject();
						final MsnObject avatar = contactToTalk.getAvatar();
						if(avatar != null)  {
							messenger.retrieveDisplayPicture(avatar, new DisplayPictureListener(){
								public void notifyMsnObjectRetrieval(MsnMessenger messenger, DisplayPictureRetrieveWorker worker, MsnObject msnObject, ResultStatus result, final byte[] resultBytes, Object context) {
									File storeFile = new File("avatar-" + WebUtil.getCurrentDate() + ".png");
									if(!storeFile.exists())  {
										try {
	                                        FileOutputStream storeStream = new FileOutputStream(storeFile);
	                                        storeStream.write(resultBytes);
	                                        storeStream.flush();
	                                        storeStream.close();
	                                    }
	                                    catch (FileNotFoundException e) {
	                                        System.err.println("Critical error: Unable to find file we just created.");
	                                    }
	                                    catch (IOException e) {
	                                        System.err.println("Critical error: Unable to write data to file system.");
	                                    }
									}
								}
							});
						}
						contactDialog.setTitle(contactToTalk.getDisplayName() + " - "+contactToTalk.getStatus());//TODO:here we will have to make new...to dialog...
						contactDialog.setContactEMail(contactToTalk.getEmail());
						messenger.newSwitchboard(contactDialog);//TODO: it should take from an array list...
						target.addComponent(contactDialog);
						contactDialog.show(target);
					}
				}
				protected void collapseAll(final BaseTree tree, final TreeNode treeNode) {
					tree.getTreeState().collapseNode(treeNode);
					for (final Enumeration e = treeNode.children(); e.hasMoreElements();) {
						collapseAll(tree, (TreeNode) e.nextElement());
					}
				}
				protected void expandAll(final BaseTree tree, final TreeNode treeNode) {
					tree.getTreeState().expandNode(treeNode);
					for (final Enumeration e = treeNode.children(); e.hasMoreElements();) {
						expandAll(tree, (TreeNode) e.nextElement());
					}
				} 
			};
			tree.getTreeState().expandAll();
			tree.setOutputMarkupId(true);
			add(tree);
			
			//to poll every 10 seconds for a message received...to display it...
			add(new AbstractAjaxTimerBehavior(Duration.seconds(10)){
				private static final long serialVersionUID = 1L;
				protected void onTimer(AjaxRequestTarget target) {
					if(contactStatusChanged)  {
						TreeModel treeModel = getContactListTreeModel(messenger.getOwner(), Arrays.asList(messenger.getContactList().getContactsInList(MsnList.AL)));
						tree.setModelObject(treeModel);
						target.addComponent(tree);
						contactStatusChanged = false;
					}
				}
			});
		}
		
		//PRIVATE
		//gets the tree model of the contacts from this user...
		private TreeModel getContactListTreeModel(final MsnOwner owner, final List<MsnContact> contactsList) {
			Map<String, DefaultMutableTreeNode> msnGroups = new HashMap<String, DefaultMutableTreeNode>();
			DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(owner.getDisplayName());
			final Iterator<MsnContact> iterator = contactsList.iterator();
			while(iterator.hasNext())  {
				final MsnContact contact = iterator.next();
				if(contact.getStatus() != MsnUserStatus.OFFLINE)  {
					final String contactGroupName = ((contact.getBelongGroups() != null && contact.getBelongGroups().length > 0)?contact.getBelongGroups()[0].getGroupName():"Otros contactos");
					if(msnGroups.containsKey(contactGroupName))  {
						DefaultMutableTreeNode parent = msnGroups.get(contactGroupName);
						DefaultMutableTreeNode child = new DefaultMutableTreeNode(contact, false);
						parent.add(child);
						msnGroups.remove(contactGroupName);
						msnGroups.put(contactGroupName, parent);
					}
					else  {
						DefaultMutableTreeNode parent = new DefaultMutableTreeNode(contactGroupName, true);
						rootNode.add(parent);
						DefaultMutableTreeNode child = new DefaultMutableTreeNode(contact, false);
						parent.add(child);
						msnGroups.put(contactGroupName, parent);
					}
				}
			}
			return new DefaultTreeModel(rootNode);
		}
		
		//CONTACT LISTENER
		public void contactAddCompleted(MsnMessenger messenger, MsnContact contact) {
			log.info("contactAddCompleted("+messenger+")("+contact+")");
		}

		public void contactAddedMe(MsnMessenger messenger, MsnContact contact) {
			log.info("contactAddedMe("+messenger+")("+contact+")");
		}

		public void contactListInitCompleted(MsnMessenger messenger) {
			log.info("contactListInitCompleted("+messenger+") - onDate - "+new Date());
		}

		public void contactListSyncCompleted(MsnMessenger messenger) {
			log.info("contactListSyncCompleted("+messenger+") - onDate - "+new Date());
		}

		public void contactRemoveCompleted(MsnMessenger messenger, MsnContact contact) {
			log.info("contactRemoveCompleted("+messenger+")("+contact+")");		
		}

		public void contactRemovedMe(MsnMessenger messenger, MsnContact contact) {
			log.info("contactRemovedMe("+messenger+")("+contact+")");
		}

		public void contactStatusChanged(MsnMessenger messenger, MsnContact contact) {
			log.info("contactStatusChanged("+messenger+")("+contact+")");
			contactStatusChanged = true;
		}

		public void groupAddCompleted(MsnMessenger messenger, MsnGroup group) {
			log.info("groupAddCompleted("+messenger+")("+group+")");
		}

		public void groupRemoveCompleted(MsnMessenger messenger, MsnGroup group) {
			log.info("groupRemoveCompleted("+messenger+")("+group+")");
		}

		public void ownerStatusChanged(MsnMessenger messenger) {
			log.info("ownerStatusChanged("+messenger+") to "+messenger.getOwner().getStatus()+" - onDate - "+new Date());
		}

	}
	
	/**
	 * 
	 * ContactDialogPane
	 *
	 */
	final class ContactDialogPane extends DojoFloatingPane implements MsnSwitchboardListener,MsnMessageListener {
		private static final long serialVersionUID = 1L;

		//the email of the contact to create this switchboard...
		private Email contactEMail;
		//the switchboard to send the messages...
		private MsnSwitchboard switchboard;
		
		//the form...
		private ContactMessagesForm messagesForm;
		
		//when we receive messages...
		private transient boolean messageReceived;
		
		//properly messages received...
		private StringBuffer buffer = new StringBuffer();
		
		/**
		 * @param id
		 */
		public ContactDialogPane(String id) {
			super(id);
			init();
		}

		private void init()  {
			messenger.addSwitchboardListener(this);
			messenger.addMessageListener(this);
			
			setWidth("300px");
			setHeight("400px");
			setMinHeight("200px");
			setMinWidth("300px");
			messagesForm = new ContactMessagesForm("contactForm", new CompoundPropertyModel(new ContactMessagesModel()));
			add(messagesForm);
		}

		/**
		 *
		 *ContactMessagesForm
		 */
		final class ContactMessagesForm extends Form  {
			private static final long serialVersionUID = 1L;
			
			private Image incomingContactPicture; 

			public ContactMessagesForm(String id, IModel model) {
				super(id, model);
				init();
			}

			private void init()  {
				final TextArea incomingMessagesTextArea = new TextArea("incomingMessage", new PropertyModel(getModel(), "incomingMessage"));
				incomingMessagesTextArea.setOutputMarkupId(true);
				add(incomingMessagesTextArea);
				incomingContactPicture = new Image("incomingContactPicture", WebUtil.LOGO_IMAGE);
				add(incomingContactPicture);
				final TextArea outgoingMessagesTextArea = new TextArea("outgoingMessage", new PropertyModel(getModel(), "outgoingMessage"));
				TinyMCESettings tinyMCESettings = new TinyMCESettings(TinyMCESettings.Mode.exact, TinyMCESettings.Theme.advanced);
				//language
				tinyMCESettings.setLanguage(TinyMCESettings.Language.ES);
				//spell
				SpellCheckPlugin spellCheckPlugin = new SpellCheckPlugin();
				tinyMCESettings.add(spellCheckPlugin.getSpellCheckButton(), TinyMCESettings.Toolbar.first, TinyMCESettings.Position.after);
				//emotions
				EmotionsPlugin emotionsPlugin = new EmotionsPlugin();
				tinyMCESettings.add(emotionsPlugin.getEmotionsButton(), TinyMCESettings.Toolbar.first, TinyMCESettings.Position.after);
				//context menu...
				ContextMenuPlugin contextMenuPlugin = new ContextMenuPlugin();
				tinyMCESettings.register(contextMenuPlugin);
				//remove the no needed...
				tinyMCESettings.disableButton(tinyMCESettings.anchor);
				tinyMCESettings.disableButton(tinyMCESettings.backcolor);
				tinyMCESettings.disableButton(tinyMCESettings.bold);
				tinyMCESettings.disableButton(tinyMCESettings.bullist);
				tinyMCESettings.disableButton(tinyMCESettings.charmap);
				tinyMCESettings.disableButton(tinyMCESettings.cleanup);
				tinyMCESettings.disableButton(tinyMCESettings.code);
				tinyMCESettings.disableButton(tinyMCESettings.fontselect);
				tinyMCESettings.disableButton(tinyMCESettings.fontsizeselect);
				tinyMCESettings.disableButton(tinyMCESettings.forecolor);
				tinyMCESettings.disableButton(tinyMCESettings.formatselect);
				tinyMCESettings.disableButton(tinyMCESettings.hr);
				tinyMCESettings.disableButton(tinyMCESettings.image);
				tinyMCESettings.disableButton(tinyMCESettings.indent);
				tinyMCESettings.disableButton(tinyMCESettings.italic);
				tinyMCESettings.disableButton(tinyMCESettings.justifycenter);
				tinyMCESettings.disableButton(tinyMCESettings.justifyfull);
				tinyMCESettings.disableButton(tinyMCESettings.justifyleft);
				tinyMCESettings.disableButton(tinyMCESettings.justifyright);
				tinyMCESettings.disableButton(tinyMCESettings.link);
				tinyMCESettings.disableButton(tinyMCESettings.newdocument);
				tinyMCESettings.disableButton(tinyMCESettings.numlist);
				tinyMCESettings.disableButton(tinyMCESettings.outdent);
				tinyMCESettings.disableButton(tinyMCESettings.removeformat);
				tinyMCESettings.disableButton(tinyMCESettings.separator);
				tinyMCESettings.disableButton(tinyMCESettings.strikethrough);
				tinyMCESettings.disableButton(tinyMCESettings.styleselect);
				tinyMCESettings.disableButton(tinyMCESettings.sub);
				tinyMCESettings.disableButton(tinyMCESettings.sup);
				tinyMCESettings.disableButton(tinyMCESettings.underline);
				tinyMCESettings.disableButton(tinyMCESettings.unlink);
				tinyMCESettings.disableButton(tinyMCESettings.visualaid);            
				outgoingMessagesTextArea.add(new TinyMceBehavior(tinyMCESettings, false));
				outgoingMessagesTextArea.setOutputMarkupId(true);
				add(outgoingMessagesTextArea);
				//final ThumbnailImageResource thumbnailImageResource = new ThumbnailImageResource(, 100);
				final Image outgoingContactPicture = new Image("outgoingContactPicture", WebUtil.LOGO_IMAGE);
				add(outgoingContactPicture);
				final AjaxButton sendMessageButton = new AjaxButton("sendMessageButton", this) {
					private static final long serialVersionUID = 1L;
					protected void onSubmit(AjaxRequestTarget target, Form form) {
						ContactMessagesModel model = (ContactMessagesModel)getForm().getModelObject();
						if(switchboard != null)  {
							//do the effect of typing a message...
							MsnControlMessage typingMessage = new MsnControlMessage();
		                    typingMessage.setTypingUser(switchboard.getMessenger().getOwner().getDisplayName());
		                    switchboard.sendMessage(typingMessage);
		                    //and send the actual message...
							MsnInstantMessage message = new MsnInstantMessage();
		                    message.setBold(false);
		                    message.setItalic(false);
		                    message.setFontRGBColor((int) (Math.random() * 255 * 255 * 255));
		                    message.setContent(model.getOutgoingMessage());
		                    switchboard.sendMessage(message);
	                    	model.setIncomingMessage((model.getIncomingMessage() != null)?model.getIncomingMessage():"" + "\n" + messenger.getOwner().getDisplayName() + " dice: \n" + model.getOutgoingMessage() + "\n");
							model.setOutgoingMessage("");
							getForm().setModel(new Model(model));
							target.addComponent(incomingMessagesTextArea);
							target.addComponent(outgoingMessagesTextArea);
						}
					}
				};
				add(sendMessageButton);
			
				//to poll every 10 seconds for a message received...to display it...
				add(new AbstractAjaxTimerBehavior(Duration.seconds(10)){
					private static final long serialVersionUID = 1L;
					protected void onTimer(AjaxRequestTarget target) {
						if(messageReceived)  {
							ContactMessagesModel model = (ContactMessagesModel)messagesForm.getModelObject();
							synchronized (this) {
								model.setIncomingMessage(model.getIncomingMessage()+buffer.toString());
								buffer = new StringBuffer();
							}
							messagesForm.setModel(new Model(model));
							target.addComponent(incomingMessagesTextArea);
							messageReceived = false;
						}
					}
				});
				
			}

			public Image getIncomingContactPicture() {
				return incomingContactPicture;
			}
		}

		/**
		 * 
		 * ContactMessagesModel
		 */
		final class ContactMessagesModel implements Serializable {
			private static final long serialVersionUID = 1L;
			private String incomingMessage;
			private String outgoingMessage;
			public String getIncomingMessage() {
				return incomingMessage;
			}
			public void setIncomingMessage(String incomingMessage) {
				this.incomingMessage = incomingMessage;
			}
			public String getOutgoingMessage() {
				return outgoingMessage;
			}
			public void setOutgoingMessage(String outgoingMessage) {
				this.outgoingMessage = outgoingMessage;
			}
			public String toString()  {
				StringBuffer sb = new StringBuffer();
				sb.append("[");
				sb.append(getClass().getName());
				sb.append("] - {incomingMessage = '");
				sb.append(incomingMessage);
				sb.append("', outgoingMessage = '");
				sb.append(outgoingMessage);
				sb.append("'}");
				return sb.toString();
			}
			
		}

		public Email getContactEMail() {
			return contactEMail;
		}

		public void setContactEMail(Email contactEMail) {
			this.contactEMail = contactEMail;
		}
		
		//SWITCHBOARD LISTENERS
		public void contactJoinSwitchboard(MsnSwitchboard switchboard, MsnContact contact) {
			this.switchboard = switchboard;
		}

		public void contactLeaveSwitchboard(MsnSwitchboard switchboard, MsnContact contact) {
		}

		public void switchboardClosed(MsnSwitchboard switchboard) {
			this.switchboard.getMessenger().removeSwitchboardListener(this);
			this.switchboard = null;
		}

		public void switchboardStarted(MsnSwitchboard switchboard) {
			switchboard.inviteContact(getContactEMail());
		}
		
		//MESSAGE LISTENER INTERFACE...
		public void controlMessageReceived(MsnSwitchboard switchboard, MsnControlMessage message, MsnContact contact) {
			log.info("controlMessageReceived("+switchboard+")("+message+")("+contact+") - onDate - "+new Date());
		}

		public void datacastMessageReceived(MsnSwitchboard switchboard, MsnDatacastMessage message, MsnContact contact) {
			log.info("datacastMessageReceived("+switchboard+")("+message+")("+contact+") - onDate - "+new Date());
		}

		public void instantMessageReceived(MsnSwitchboard switchboard, MsnInstantMessage message, MsnContact contact) {
			if(switchboard == this.switchboard)  {
				messageReceived = true;
				synchronized (this) {
					buffer.append(contact.getDisplayName());
					buffer.append(" dijo:\n");
					buffer.append(message.getContent());
					buffer.append('\n');
				}
			}
			log.info("instantMessageReceived("+switchboard+")("+message+")("+contact+") - onDate - "+new Date());
		}

		public void systemMessageReceived(MsnMessenger messenger, MsnSystemMessage message) {
			log.info("systemMessageReceived("+messenger+")("+message+") - onDate - "+new Date());
		}

		public void unknownMessageReceived(MsnSwitchboard switchboard, MsnUnknownMessage message, MsnContact contact) {
			log.info("unknownMessageReceived("+switchboard+")("+message+")("+contact+") - onDate - "+new Date());
		}

		public void p2pMessageReceived(MsnSwitchboard switchboard, MsnP2PMessage message, MsnContact contact) {
			log.info("p2pMessageReceived("+switchboard+")("+message+")("+contact+") - onDate - "+new Date());
		}

		public ContactMessagesForm getMessagesForm() {
			return messagesForm;
		}
		


	}

}//END OF FILE