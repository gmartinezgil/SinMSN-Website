/**
 * 
 */
package mx.com.sinmsn.web;

import mx.com.sinmsn.services.msn.MSNMessengerService;
import mx.com.sinmsn.web.console.ConnectingPage;
import mx.com.sinmsn.web.console.ConsolePage;
import mx.com.sinmsn.web.session.AuthenticatedPage;
import mx.com.sinmsn.web.session.SignInPage;
import mx.com.sinmsn.web.session.SignInSession;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.protocol.http.request.CryptedUrlWebRequestCodingStrategy;
import org.apache.wicket.protocol.http.request.WebRequestCodingStrategy;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;

/**
 * @author jerry
 *
 */
public final class MainApplication extends WebApplication {
	
	private MSNMessengerService messengerService;

	/**
	 * @see org.apache.wicket.protocol.http.WebApplication#newSession(org.apache.wicket.Request,
	 *      Response)
	 */
	public Session newSession(Request request, Response response) {
		return new SignInSession(MainApplication.this, request);
	}

	/**
	 * @see org.apache.wicket.examples.WicketExampleApplication#init()
	 */
	protected void init() {
		super.init();
		
		messengerService = new MSNMessengerService();
		
		mountBookmarkablePage("/home", IndexPage.class);
		mountBookmarkablePage("/connecting", ConnectingPage.class);
		mountBookmarkablePage("/console", ConsolePage.class);

		getSecuritySettings().setAuthorizationStrategy(new IAuthorizationStrategy() {
			public boolean isActionAuthorized(Component component, Action action) {
				return true;
			}

			public boolean isInstantiationAuthorized(Class componentClass) {
				if (AuthenticatedPage.class.isAssignableFrom(componentClass)) {
					// Is user signed in?
					if (((SignInSession)Session.get()).isSignedIn()) {
						// okay to proceed
						return true;
					}

					// Force sign in
					throw new RestartResponseAtInterceptPageException(SignInPage.class);
				}
				return true;
			}
		});
		
		getApplicationSettings().setPageExpiredErrorPage(IndexPage.class);
	}

	/**
	 * @see org.apache.wicket.protocol.http.WebApplication#newRequestCycleProcessor()
	 */
	protected IRequestCycleProcessor newRequestCycleProcessor() {
		return new WebRequestCycleProcessor() {
			protected IRequestCodingStrategy newRequestCodingStrategy() {
				return new CryptedUrlWebRequestCodingStrategy(new WebRequestCodingStrategy());
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	public Class getHomePage() {
		return IndexPage.class;
	}

	/*
	 * 
	 */
	public MSNMessengerService getMessengerService() {
		return messengerService;
	}

}//END OF FILE