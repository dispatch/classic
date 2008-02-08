package net.databinder.auth;

import net.databinder.auth.data.DataUser;

import org.apache.wicket.model.IModel;

public interface AuthSession {
	public boolean signIn(String username, String password);
	public boolean signIn(String username, String password, boolean setCookie);
	public void signIn(DataUser user, boolean setCookie);
	public DataUser getUser();
	public IModel getUserModel();
	public boolean isSignedIn();
	public void signOut();
}
