/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mx.com.sinmsn.web.session;

import mx.com.sinmsn.web.MainApplication;

import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnUserStatus;
import net.sf.jml.impl.MsnMessengerFactory;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;

/**
 * Session class for signin example. Holds and authenticates users.
 * 
 * @author Jonathan Locke
 */
public final class SignInSession extends WebSession {
	private static final long serialVersionUID = 1L;
	/** Trivial user representation */
	private UserSessionModel user;
	
	/**
	 * Constructor
	 * 
	 * @param application
	 *            The application
	 * @param request
	 *            The current request object
	 */
	public SignInSession(final WebApplication application, Request request) {
		super(application, request);
	}

	/**
	 * Checks the given username and password, returning a User object if if the username and
	 * password identify a valid user.
	 * 
	 * @param username
	 *            The username
	 * @param password
	 *            The password
	 * @return True if the user was authenticated
	 */
	public final boolean authenticate(final String username, final String password) {
		if (user == null) {
			MsnMessenger messenger = MsnMessengerFactory.createMsnMessenger(username, password);
			//messenger.setLogIncoming(true);
			//messenger.setLogOutgoing(true);
			messenger.getOwner().setInitStatus(MsnUserStatus.ONLINE);
			try {
				messenger.login();
				user = new UserSessionModel();
				user.setLogin(username);
				((MainApplication)RequestCycle.get().getApplication()).getMessengerService().addMessengerUser(username, messenger);
			} catch(Exception e) {
				user = null;
				e.printStackTrace();
			}
		}
		return user != null;
	}

	/**
	 * @return True if user is signed in
	 */
	public boolean isSignedIn() {
		return user != null;
	}

	/**
	 * @return User
	 */
	public UserSessionModel getUser() {
		return user;
	}

	/**
	 * @param user
	 *            New user
	 */
	public void setUser(final UserSessionModel user) {
		this.user = user;
	}
	
}//END OF FILE