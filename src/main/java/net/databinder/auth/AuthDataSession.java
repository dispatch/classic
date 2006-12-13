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
package net.databinder.auth;

/**
 * Holds IUser instance for signed in users. Remembering the user with a browser cookie
 * allows that user to bypass login for the length of time specified in getSignInCookieMaxAge().
 * <p> In general the sematics here expect users to have a username and password, though the 
 * IUser interface itself does not require it. In most cases it should not be necessary to
 * subclass AuthDataSession; use your <tt>AuthDataApplication</tt> subclass to specify
 * a user class and criteria builder as needed.</p>
 */
import javax.servlet.http.Cookie;

import net.databinder.DataRequestCycle;
import net.databinder.DataSession;
import net.databinder.auth.data.IUser;
import net.databinder.models.HibernateObjectModel;

import org.hibernate.HibernateException;

import wicket.Application;
import wicket.RequestCycle;
import wicket.WicketRuntimeException;
import wicket.model.IModel;
import wicket.protocol.http.WebApplication;
import wicket.protocol.http.WebResponse;
import wicket.util.time.Duration;

public class AuthDataSession extends DataSession {
	private IModel user;
	public static final String AUTH_COOKIE = "AUTH", USERNAME_COOKIE = "USER";

	/**
	 * Initialize new session. Retains user class from AuthDataApplication instance.
	 * @param application must be WebApplication subclass
	 * @see WebApplication
	 */
	protected AuthDataSession(IAuthSettings application) {
		super((WebApplication)application);
	}
	
	protected AuthDataSession(IAuthSettings application, boolean useConversationSession) {
		super((WebApplication)application, useConversationSession);
	}
	
	/**
	 * @return current session casted to AuthDataSession
	 * @throws ClassCastException if available session is not of this class
	 */
	public static AuthDataSession get() {
		return (AuthDataSession) DataSession.get();
	}
	
	/**
	 * @return IUser object for current user, or null if none signed in.
	 */
	public IUser getUser() {
		return isSignedIn() ? (IUser) user.getObject(null) : null;
	}
	
	/**
	 * @return model for current user
	 */
	public IModel getUserModel() {
		return isSignedIn() ? user : null;
	}
	
	/**
	 * @return length of time sign-in cookie should persist, defined here as one month
	 */
	protected Duration getSignInCookieMaxAge() {
		return Duration.days(31);
	}
	
	/**
	 * @return true if signed in or cookie sign in is possible and successful
	 */
	public boolean isSignedIn() {
		return user != null || (cookieSignInSupported() && cookieSignIn());
	}
	
	/** 
	 * @return true if application's user class implements <tt>IUser.CookieAuthentication</tt>.  
	 */
	protected boolean cookieSignInSupported() {
		return IUser.CookieAuth.class.isAssignableFrom(((IAuthSettings)Application.get()).getUserClass());
	}

	/**
	 * @return true if signed in, false if credentials incorrect
	 */
	public boolean signIn(String username, String password) {
		IModel potential = getUser(username);
		if (potential != null && ((IUser)potential.getObject(null)).checkPassword(password))
			user =  potential;
		
		return user != null;
	}
	
	/**
	 * @param setCookie if true, sets cookie to remember user
	 * @return true if signed in, false if credentials incorrect
	 */
	public boolean signIn(final String username, final String password, boolean setCookie) {
		if (!signIn(username, password))
			return false;

		if (setCookie) {
			setCookie();
		}
		return true;
	}

	/**
	 * Sign in a user whose credentials have been validated elsewhere. The user object must exist,
	 * and already have been saved, in the current request's Hibernate session.
	 * @param user validated and persisted user, must be in current Hibernate session
	 * @param setCookie if true, sets cookie to remember user
	 */
	public void signIn(IUser user, boolean setCookie) {
		this.user = new HibernateObjectModel (user);
		if (setCookie)
			setCookie();
	}
	/**
	 * @return true if signed in, false if credentials incorrect or unavailable
	 */
	protected boolean cookieSignIn() {
		DataRequestCycle requestCycle = (DataRequestCycle) RequestCycle.get();
		Cookie username = requestCycle.getCookie(USERNAME_COOKIE),
			token = requestCycle.getCookie(AUTH_COOKIE);

		if (username != null && token != null) {
			IModel potential = getUser(username.getValue());
			if (potential != null && potential.getObject(null) instanceof IUser.CookieAuth) {
				String correctToken = ((IUser.CookieAuth)potential.getObject(null)).getToken();
				if (correctToken.equals(token.getValue()))
					user =  potential;
			}
		}
		return user != null;
	}
	
	/**
	 * Looks for a persisted IUser object matching the given username. Uses the user class
	 * and criteria builder returned from the application subclass implementing IAuthSettings.
	 * @param username
	 * @return user object from persistent storage
	 * @see IAuthSettings
	 */
	protected IModel getUser(final String username) {
		try {
			IAuthSettings app = (IAuthSettings)getApplication();
			IModel user = new HibernateObjectModel(app.getUserClass(), 
					app.getUserCriteriaBuilder(username)); 
			if (user.getObject(null) != null)
				return user;
			return null;	// no results
		} catch (HibernateException e){
			throw new WicketRuntimeException("Multiple users returned for query", e); 
		}
	}
	
	/**
	 * Sets cookie to remember the currently signed-in user.
	 */
	protected void setCookie() {
		if (user == null)
			throw new WicketRuntimeException("User must be signed in when calling this method");
		if (!cookieSignInSupported())
			throw new UnsupportedOperationException("Must use an implementation of IUser.CookieAuth");
		
		IUser.CookieAuth cookieUser = (IUser.CookieAuth) user.getObject(null);
		WebResponse resp = (WebResponse) RequestCycle.get().getResponse();
		
		int  maxAge = (int) getSignInCookieMaxAge().seconds();
		
		Cookie name = new Cookie(USERNAME_COOKIE, cookieUser.getUsername()),
			auth = new Cookie(AUTH_COOKIE, cookieUser.getToken());
		
		name.setMaxAge(maxAge);
		auth.setMaxAge(maxAge);
		
		resp.addCookie(name);
		resp.addCookie(auth);
	}
	
	/** Detach user from session */
	public void signOut() {
		user = null;
		DataRequestCycle requestCycle = (DataRequestCycle) RequestCycle.get();
		requestCycle.clearCookie(AUTH_COOKIE);
		requestCycle.clearCookie(USERNAME_COOKIE);
	}
	
	/**
	 * Deatch our user model, which would not get the message otherwise.
	 */
	@Override
	protected void detach() {
		if (user != null) user.detach();
		super.detach();
	}
}
