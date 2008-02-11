package net.databinder.auth.data;

import java.security.MessageDigest;

public interface DataPassword {
	/** @return true if the given password matches this object's */
	boolean matches(String password);
	
	/** Change this object's password to the given one. */
	void change(String newPassword);
	
	/** Update digest with password, to bind it to the cookie token. */
	void update(MessageDigest digest);
}
