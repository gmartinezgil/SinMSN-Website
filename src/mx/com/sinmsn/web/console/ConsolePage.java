/**
 * 
 */
package mx.com.sinmsn.web.console;

import mx.com.sinmsn.web.IndexPage;
import mx.com.sinmsn.web.MainApplication;
import mx.com.sinmsn.web.commons.FooterPanel;
import mx.com.sinmsn.web.commons.HeaderPanel;
import mx.com.sinmsn.web.session.AuthenticatedPage;
import mx.com.sinmsn.web.session.SignInSession;
import net.sf.jml.MsnMessenger;
import net.sf.jml.event.MsnMessengerListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.wicketstuff.dojo.skin.manager.SkinManager;
import org.wicketstuff.dojo.skin.windows.WindowsDojoSkin;

/**
 * @author jerry
 *
 */
public final class ConsolePage extends AuthenticatedPage implements MsnMessengerListener {
	//the log...
	private static final Log log = LogFactory.getLog(ConsolePage.class);
	
	//the connection...
	private transient boolean logout;
	
	private transient MsnMessenger messenger = ((MainApplication)getApplication()).getMessengerService().getMessengerUser(((SignInSession)getSession()).getUser().getLogin()); 
	
	//test
	private Label mainTitle;

	/**
	 * 
	 */
	public ConsolePage() {
		init();
	}

	private void init()  {
		log.info("protocol - "+messenger.getActualMsnProtocol());
		log.info("connection - "+messenger.getConnection().getConnectionType());
		log.info("external ip - "+messenger.getConnection().getExternalIP());
		log.info("external port - "+messenger.getConnection().getExternalPort());
		log.info("internal ip - "+messenger.getConnection().getInternalIP());
		log.info("internal port - "+messenger.getConnection().getInternalPort());
		log.info("remote ip - "+messenger.getConnection().getRemoteIP());
		log.info("remote port - "+messenger.getConnection().getRemotePort());
		messenger.addMessengerListener(this);
		
		//MAIN TITLE
		mainTitle = new Label("title", new Model("sinmsn.com - " + messenger.getOwner().getDisplayName()));
		add(mainTitle);

		//HEADER
		HeaderPanel headerPanel = new HeaderPanel("headerPanel");
		add(headerPanel);
		
		//default skin...
		SkinManager.getInstance().setupSkin(new WindowsDojoSkin());
		
		//MESSENGER
		MessengerPanel messengerPanel = new MessengerPanel("messengerPanel");
		add(messengerPanel);

		add(new AbstractAjaxTimerBehavior(Duration.seconds(10)){
			private static final long serialVersionUID = 1L;
			protected void onTimer(AjaxRequestTarget target) {
				if(logout)  {
					PageParameters parameters = new PageParameters();
					parameters.put("m", "disconnected");
					messenger.logout();
					setResponsePage(IndexPage.class, parameters);
				}
			}
		});
		
		//FOOTER
		FooterPanel footerPanel = new FooterPanel("footerPanel");
		add(footerPanel);
	}
	
	
	//MESSENGER LISTENER
	public void exceptionCaught(MsnMessenger messenger, Throwable e) {
		log.error("messenger send an error - "+e.getMessage(), e);
	}

	public void loginCompleted(MsnMessenger messenger) {
		log.info("loged - "+messenger.getOwner().getEmail());
	}

	public void logout(MsnMessenger messenger) {
		log.info("logout - "+((messenger.getOwner() != null)?messenger.getOwner().getEmail():""));
		logout = true;
	}

}//END OF FILE