package net.databinder.auth.data;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import javax.persistence.Transient;

import net.databinder.auth.IAuthSettings;

import org.apache.wicket.Application;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.util.crypt.Base64;
import org.apache.wicket.util.crypt.Base64UrlSafe;

public abstract class UserBase implements IUser.CookieAuth, Serializable {
	public abstract Set<String> getRoles();
	
	public abstract String getPasswordHash();
	
	public abstract String getUsername();

	/**
	 * @return true if validRoles and this user have any
	 * roles in common
	 */
	public boolean hasAnyRole(Roles validRoles) {
		for (String role: getRoles())
			if (validRoles.hasRole(role))
				return true;
		return false;
	}
	
	protected static MessageDigest getMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA Hash algorithm not found.", e);
		}
	}
	
	/** 
	 * @param location IP address or other identifier
	 * @return restricted token for as URL-safe hash for user, password, and location parameter
	 */
	@Transient
	public String getToken(String location) {
		MessageDigest md = getMessageDigest();
		md.update(((IAuthSettings)Application.get()).getSalt());
		md.update(getPasswordHash().getBytes());
		md.update(location.getBytes());
		byte[] hash = md.digest(getUsername().getBytes());
		return new String(Base64UrlSafe.encodeBase64(hash));
	}
	
	/**
	 * Generates a hash for password using salt from AuthDataApplication.getSalt()
	 * and returns the hash encoded as a Base64 String.
	 * @see AuthDataApplication.getSalt();
	 * @param password to encode
	 * @return base64 encoded SHA hash, 28 characters 
	 */
	public static String getHash(String password) {
		MessageDigest md = getMessageDigest();
		md.update(((IAuthSettings)Application.get()).getSalt());
		byte[] hash = md.digest(password.getBytes());
		// using a Base64 string for the hash because putting a 
		// byte[] into a blob isn't working consistently.
		return new String(Base64.encodeBase64(hash));
	}
}
