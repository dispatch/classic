package net.databinder.auth;

import net.databinder.auth.data.DataUser;

import org.apache.wicket.model.IModel;

/** Required interface for user session of applications using Databinder authentication. */
public interface AuthSession {
	/** Sign in without setting cookie. */
	public boolean signIn(String username, String password);
	public boolean signIn(String username, String password, boolean setCookie);
	/** Sign in without checking password (here). */
	public void signIn(DataUser user, boolean setCookie);
	public DataUser getUser();
	public IModel getUserModel();
	public boolean isSignedIn();
	/** Sign out and remove any authentication cookies. */
	public void signOut();
}
