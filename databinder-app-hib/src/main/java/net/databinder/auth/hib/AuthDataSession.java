/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.databinder.auth.hib;

/**
 * Holds DataUser identifier for signed in users. Remembering the user with a browser cookie
 * allows that user to bypass login for the length of time specified in getSignInCookieMaxAge().
 * <p> In general the semantics here expect users to have a username and password, though the 
 * DataUser interface itself does not require it. Use your <tt>AuthDataApplication</tt> subclass to specify
 * a user class and criteria builder as needed.</p>
 */
import net.databinder.auth.AuthDataSessionBase;
import net.databinder.auth.AuthSession;
import net.databinder.auth.data.DataUser;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.Request;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;

/** Session to hold DataUser. */
public class AuthDataSession extends AuthDataSessionBase implements AuthSession {
	/**
	 * Initialize new session.
	 * @see WebApplication
	 */
	public AuthDataSession(Request request) {
		super(request);
	}
	
	@Override
	public IModel getUserModel(DataUser user) {
		return new HibernateObjectModel(user);
	}
}
