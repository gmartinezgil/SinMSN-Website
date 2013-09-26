/**
 * 
 */
package mx.com.sinmsn.services.msn;

import java.util.HashMap;
import java.util.Map;

import net.sf.jml.MsnMessenger;

/**
 * @author jerry
 *
 */
public final class MSNMessengerService {

	private Map messengers; //TODO:it should be in the database and cached here used by the cache manager...
	/**
	 * 
	 */
	public MSNMessengerService() {
		messengers = new HashMap();
	}
	
	/**
	 * 
	 * @param username
	 * @param messenger
	 */
	public void addMessengerUser(final String username, final MsnMessenger messenger)  {
		messengers.put(username, messenger);
	}
	
	public MsnMessenger getMessengerUser(final String username)  {
		return (MsnMessenger)messengers.get(username);
	}
	
	public void removeMessengerUser(final String username)  {
		messengers.remove(username);
	}

}//END OF FILE