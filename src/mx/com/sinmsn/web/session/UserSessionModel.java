/**
 * 
 */
package mx.com.sinmsn.web.session;

import java.io.Serializable;

import net.sf.jml.MsnMessenger;

/**
 * @author jerry
 *
 */
public final class UserSessionModel implements Serializable{
	private static final long serialVersionUID = 1L;
	private String login;
	private MsnMessenger messenger;

	public MsnMessenger getMessenger() {
		return messenger;
	}

	public void setMessenger(MsnMessenger messenger) {
		this.messenger = messenger;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

}//END OF FILE