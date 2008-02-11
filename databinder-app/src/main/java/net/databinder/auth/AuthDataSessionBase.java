package net.databinder.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;

import net.databinder.CookieRequestCycle;
import net.databinder.auth.data.DataUser;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.util.time.Duration;

public abstract class AuthDataSessionBase extends WebSession implements AuthSession {
	/** Effective signed in state. */
	private IModel userModel;
	private static final String CHARACTER_ENCODING = "UTF-8";

	/**
	 * Initialize new session.
	 * @see WebApplication
	 */
	public AuthDataSessionBase(Request request) {
		super(request);
	}
	
	protected static AuthApplication getApp() {
		return (AuthApplication) Application.get();
	}
	
	public static AuthDataSessionBase get() {
		return (AuthDataSessionBase) WebSession.get();
	}
	
	/**
	 * @return IUser object for current user, or null if none signed in.
	 */
	public DataUser getUser() {
		if  (isSignedIn()) {
			return (DataUser) getUserModel().getObject();
		}
		return null;
	}
	
	public IModel getUserModel() {
		return userModel;
	}
	
	/**
	 * @return model for current user
	 */
	public abstract IModel getUserModel(DataUser user);

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
		if (userModel == null)
			cookieSignIn();
		return userModel != null; 
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
		if (potential != null && (potential).getPassword().matches(password));
			signIn(potential, setCookie);
		
		return userModel != null;
	}

	/**
	 * Sign in a user whose credentials have been validated elsewhere. The user object must exist,
	 * and already have been saved, in the current request's Hibernate session.
	 * @param user validated and persisted user, must be in current Hibernate session
	 * @param setCookie if true, sets cookie to remember user
	 */
	public void signIn(DataUser user, boolean setCookie) {
		userModel = getUserModel(user);
		if (setCookie)
			setCookie();
	}
		
	/**
	 * Attempts cookie sign in, which will set usename field but not user.
	 * @return true if signed in, false if credentials incorrect or unavailable
	 */
	protected boolean cookieSignIn() {
		CookieRequestCycle requestCycle = (CookieRequestCycle) RequestCycle.get();
		Cookie userCookie = requestCycle.getCookie(getUserCookieName()),
			token = requestCycle.getCookie(getAuthCookieName());

		if (userCookie != null && token != null) {
			DataUser potential;
			try {
				potential = getUser(URLDecoder.decode(userCookie.getValue(), CHARACTER_ENCODING));
			} catch (UnsupportedEncodingException e) {
				throw new WicketRuntimeException(e);
			}
			if (potential != null && potential instanceof DataUser) {
				AuthApplication app = (AuthApplication)getApplication();
				String correctToken = app.getToken((DataUser)potential);
				if (correctToken.equals(token.getValue()))
					signIn(potential, false);
			}
		}
		return userModel != null;
	}
		
	/**
	 * Looks for a persisted IUser object matching the given username. Uses the user class
	 * and criteria builder returned from the application subclass implementing IAuthSettings.
	 * @param username
	 * @return user object from persistent storage
	 * @see IAuthSettings
	 */
	protected DataUser getUser(final String username) {
		return getApp().getUser(username);
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
		if (userModel == null)
			throw new WicketRuntimeException("User must be signed in when calling this method");
		
		DataUser cookieUser = (DataUser) getUser();
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
	
	@Override
	protected void detach() {
		if (userModel != null)
			userModel.detach();
	}
	
	/** Detach user from session */
	public void signOut() {
		userModel = null;
		CookieRequestCycle requestCycle = (CookieRequestCycle) RequestCycle.get();
		requestCycle.clearCookie(getUserCookieName());
		requestCycle.clearCookie(getAuthCookieName());
	}

}
