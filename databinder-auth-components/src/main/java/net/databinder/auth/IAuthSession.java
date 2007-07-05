package net.databinder.auth;

import net.databinder.auth.data.IUser;
import net.databinder.models.HibernateObjectModel;

public interface IAuthSession {
	public boolean signIn(String username, String password);
	public boolean signIn(String username, String password, boolean setCookie);
	public IUser getUser();
	public HibernateObjectModel getUserModel();
	public boolean isSignedIn();
	public void signOut();
}
