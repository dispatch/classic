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
 * Holds IUser identifier for signed in users. Remembering the user with a browser cookie
 * allows that user to bypass login for the length of time specified in getSignInCookieMaxAge().
 * <p> In general the semantics here expect users to have a username and password, though the 
 * IUser interface itself does not require it. Use your <tt>AuthDataApplication</tt> subclass to specify
 * a user class and criteria builder as needed.</p>
 */
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.AuthSession;
import net.databinder.auth.data.DataUser;
import net.databinder.hib.DataRequestCycle;
import net.databinder.hib.Databinder;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.util.time.Duration;
import org.hibernate.NonUniqueResultException;

public class AuthDataSession extends WebSession implements AuthSession {
	/** Effective signed in state. */
	private Serializable userId;
	private static final String CHARACTER_ENCODING = "UTF-8";

	/**
	 * Initialize new session.
	 * @see WebApplication
	 */
	public AuthDataSession(Request request) {
		super(request);
	}
	
	/**
	 * @return current session casted to AuthDataSession
	 * @throws ClassCastException if available session is not of this class
	 */
	public static AuthDataSession get() {
		return (AuthDataSession) WebSession.get();
	}
	
	protected static AuthApplication getApp() {
		return (AuthApplication) Application.get();
	}
	
	/**
	 * @return IUser object for current user, or null if none signed in.
	 */
	public DataUser getUser() {
		if  (isSignedIn()) {
			DataUser user = getUser(userId);
			return user;
		}
		return null;
	}
	
	/**
	 * @return model for current user
	 */
	public HibernateObjectModel getUserModel() {
		return isSignedIn() ? new HibernateObjectModel(getApp().getUserClass(), userId) : null;
	}
	
	/**
	 * @return length of time sign-in cookie should persist, defined here as one month
	 */
	protected Duration getSignInCookieMaxAge() {
		return Duration.days(31);
	}
	
	/**
	 * Determine if user is signed in, or can be via cookie.
	 * @return true if signed in or cookie sign in is possible and successful
	 */
	public boolean isSignedIn() {
		if (userId == null && cookieSignInSupported())
			cookieSignIn();
		return userId != null; 
	}
	
	/** 
	 * @return true if application's user class implements <tt>IUser.CookieAuthentication</tt>.  
	 */
	protected boolean cookieSignInSupported() {
		return DataUser.CookieAuth.class.isAssignableFrom(getApp().getUserClass());
	}

	/**
	 * @return true if signed in, false if credentials incorrect
	 */
	public boolean signIn(String username, String password) {
		return signIn(username, password, false);
	}
	
	/**
	 * @param setCookie if true, sets cookie to remember user
	 * @return true if signed in, false if credentials incorrect
	 */
	public boolean signIn(final String username, final String password, boolean setCookie) {
		signOut();
		DataUser potential = getUser(username);
		if (potential != null && (potential).checkPassword(password))
			signIn(potential, setCookie);
		
		return userId != null;
	}

	/**
	 * Sign in a user whose credentials have been validated elsewhere. The user object must exist,
	 * and already have been saved, in the current request's Hibernate session.
	 * @param user validated and persisted user, must be in current Hibernate session
	 * @param setCookie if true, sets cookie to remember user
	 */
	public void signIn(DataUser user, boolean setCookie) {
		userId = Databinder.getHibernateSession().getIdentifier(user);
		if (setCookie)
			setCookie();
	}
	
	/**
	 * Attempts cookie sign in, which will set usename field but not user.
	 * @return true if signed in, false if credentials incorrect or unavailable
	 */
	protected boolean cookieSignIn() {
		DataRequestCycle requestCycle = (DataRequestCycle) RequestCycle.get();
		Cookie userCookie = requestCycle.getCookie(getUserCookieName()),
			token = requestCycle.getCookie(getAuthCookieName());

		if (userCookie != null && token != null) {
			DataUser potential;
			try {
				potential = getUser(URLDecoder.decode(userCookie.getValue(), CHARACTER_ENCODING));
			} catch (UnsupportedEncodingException e) {
				throw new WicketRuntimeException(e);
			}
			if (potential != null && potential instanceof DataUser.CookieAuth) {
				AuthApplication app = (AuthApplication)getApplication();
				String correctToken = app.getToken((DataUser.CookieAuth)potential);
				if (correctToken.equals(token.getValue()))
					signIn(potential, false);
			}
		}
		return userId != null;
	}
		
	/**
	 * Looks for a persisted IUser object matching the given username. Uses the user class
	 * and criteria builder returned from the application subclass implementing IAuthSettings.
	 * @param username
	 * @return user object from persistent storage
	 * @see IAuthSettings
	 */
	protected DataUser getUser(final String username) {
		try {
			return getApp().getUser(username);
		} catch (NonUniqueResultException e){
			throw new WicketRuntimeException("Multiple users returned for query", e); 
		}
	}

	/**
	 * @param userId Hibernate entity identifier
	 * @return user with given userId
	 */
	protected DataUser getUser(final Serializable userId) {
		return (DataUser) Databinder.getHibernateSession().load(getApp().getUserClass(), userId);
	}
	
	public static String getUserCookieName() {
		return Application.get().getClass().getSimpleName() + "_USER";
	}
	
	public static String getAuthCookieName() {
		return Application.get().getClass().getSimpleName() + "_AUTH";
	}

	/**
	 * Sets cookie to remember the currently signed-in user.
	 */
	protected void setCookie() {
		if (userId == null)
			throw new WicketRuntimeException("User must be signed in when calling this method");
		if (!cookieSignInSupported())
			throw new UnsupportedOperationException("Must use an implementation of IUser.CookieAuth");
		
		DataUser.CookieAuth cookieUser = (DataUser.CookieAuth) getUser();
		WebResponse resp = (WebResponse) RequestCycle.get().getResponse();
		
		int  maxAge = (int) getSignInCookieMaxAge().seconds();
		
		Cookie name, auth;
		try {
			name = new Cookie(getUserCookieName(), 
					URLEncoder.encode(cookieUser.getUsername(), CHARACTER_ENCODING));
			auth = new Cookie(getAuthCookieName(), getApp().getToken(cookieUser));
		} catch (UnsupportedEncodingException e) {
			throw new WicketRuntimeException(e);
		}
		
		name.setPath("/");
		auth.setPath("/");

		name.setMaxAge(maxAge);
		auth.setMaxAge(maxAge);
		
		resp.addCookie(name);
		resp.addCookie(auth);
	}
	
	/** Detach user from session */
	public void signOut() {
		userId = null;
		DataRequestCycle requestCycle = (DataRequestCycle) RequestCycle.get();
		requestCycle.clearCookie(getUserCookieName());
		requestCycle.clearCookie(getAuthCookieName());
	}
}
