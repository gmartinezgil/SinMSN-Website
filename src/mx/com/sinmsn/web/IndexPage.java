/**
 * 
 */
package mx.com.sinmsn.web;

import mx.com.sinmsn.web.commons.FooterPanel;
import mx.com.sinmsn.web.commons.HeaderPanel;
import mx.com.sinmsn.web.commons.MenuPanel;
import mx.com.sinmsn.web.commons.SidebarPanel;
import mx.com.sinmsn.web.session.SignInPanel;
import mx.com.sinmsn.web.session.SignInSession;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;


/**
 * @author jerry
 *
 */
public final class IndexPage extends WebPage {

	private String message;
	/**
	 * 
	 */
	public IndexPage() {
		super();
		init();
	}
	
	public IndexPage(PageParameters parameters) {
		super(parameters);
		message = parameters.getString("m");
		init();
	}

	private void init()  {
		final HeaderPanel headerPanel = new HeaderPanel("headerPanel");
		final MenuPanel menuPanel = new MenuPanel("menuPanel");
		final SignInPanel contentPanel = new SignInPanel("contentPanel", message) {
			private static final long serialVersionUID = 1L;
			public boolean signIn(final String username, final String password) {
				return ((SignInSession)getSession()).authenticate(username, password);
			}
		};
		final SidebarPanel sidebarPanel = new SidebarPanel("sidebarPanel");
		final FooterPanel footerPanel = new FooterPanel("footerPanel");
		add(headerPanel);
		add(menuPanel);
		add(contentPanel);
		add(sidebarPanel);
		add(footerPanel);
	}
	
}//END OF FILE