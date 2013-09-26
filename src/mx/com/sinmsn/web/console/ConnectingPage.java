/**
 * 
 */
package mx.com.sinmsn.web.console;

import java.util.Date;

import mx.com.sinmsn.web.IndexPage;
import mx.com.sinmsn.web.MainApplication;
import mx.com.sinmsn.web.commons.HeaderPanel;
import mx.com.sinmsn.web.session.AuthenticatedPage;
import mx.com.sinmsn.web.session.SignInSession;
import net.sf.jml.MsnMessenger;
import net.sf.jml.event.MsnMessengerListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

/**
 * @author jerry
 *
 */
public final class ConnectingPage extends AuthenticatedPage implements MsnMessengerListener {

	//the log...
    private static final Log log = LogFactory.getLog(ConnectingPage.class);
    //to see if can be loged in...
    private boolean loged = false;
    private boolean logout = false;
    private boolean error = false;
    
    private transient MsnMessenger messenger = ((MainApplication)getApplication()).getMessengerService().getMessengerUser(((SignInSession)getSession()).getUser().getLogin());

	/**
	 * 
	 */
	public ConnectingPage() {
		super();
		init();
	}
	
	private void init()  {
		final long start = System.currentTimeMillis();
		log.info("start = "+new Date(start));
		messenger.addMessengerListener(this);
		
		HeaderPanel headerPanel = new HeaderPanel("headerPanel");
		add(headerPanel);
		
		final Label status = new Label("status", new Model("trying to connect..."));
		status.add(new AbstractAjaxTimerBehavior(Duration.seconds(10)){
			private static final long serialVersionUID = 1L;
			protected void onTimer(AjaxRequestTarget target) {
				long updatedTime = (System.currentTimeMillis() - start) / 1000;
				if(updatedTime > 60 || logout == true) {//TODO:this is only empirical...try another way...
					setResponsePage(IndexPage.class);
				}
				if(error == true) {
					setResponsePage(IndexPage.class);
				}
				if(loged == true)  setResponsePage(ConsolePage.class);
				if(target != null)  {
					status.setModel(new Model(updatedTime+" seconds. - trying to connect..."));
					target.addComponent(status);
				}
			}
		});
		status.setOutputMarkupId(true);
		add(status);
	}

	/* (non-Javadoc)
	 * @see net.sf.jml.event.MsnMessengerListener#exceptionCaught(net.sf.jml.MsnMessenger, java.lang.Throwable)
	 */
	public void exceptionCaught(MsnMessenger messenger, Throwable throwable) {
		log.error("messenger send an error - "+throwable.getMessage(), throwable);
		if (throwable instanceof net.sf.jml.exception.IncorrectPasswordException) {
			
		}
		error = true;
	}

	/* (non-Javadoc)
	 * @see net.sf.jml.event.MsnMessengerListener#loginCompleted(net.sf.jml.MsnMessenger)
	 */
	public void loginCompleted(MsnMessenger messenger) {
		log.info("loged - "+messenger.getOwner().getEmail()+" on date -"+new Date());
		loged = true;
	}

	/* (non-Javadoc)
	 * @see net.sf.jml.event.MsnMessengerListener#logout(net.sf.jml.MsnMessenger)
	 */
	public void logout(MsnMessenger messenger) {
		log.info("logout - "+((messenger.getOwner() != null)?messenger.getOwner().getEmail():"")+" on date -"+new Date());
		logout = true;
	}

}//END OF FILE