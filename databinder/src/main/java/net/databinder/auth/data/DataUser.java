package net.databinder.auth.data;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import net.databinder.auth.IAuthSettings;

import org.hibernate.annotations.CollectionOfElements;

import wicket.Application;
import wicket.authorization.strategies.role.Roles;
import wicket.util.crypt.Base64;
import wicket.util.crypt.Base64UrlSafe;

/**
 * Basic implementation of IUser.CookieAuth. Stores no passwords in memory or persistent
 * storage, only a hash. Subclass as needed, uses default inheritance strategy of single 
 * table per class hierarchy. Please use your own IUser implementation.  An updated version of
 * this class is available in the auth-data-app Maven archetype.
 * @deprecated
 * @author Nathan Hamblen
 */
@Entity
public class DataUser implements IUser.CookieAuth, Serializable {
	private Integer id;
	private String passwordHash;
	private String username;
	private Set<String> roles;
	
	public DataUser() {
	}
	
	public DataUser(String username, String password) {
		this.username = username;
		this.passwordHash = getHash(password);
		roles = new HashSet<String>(1);
		roles.add(Roles.USER);
	}
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}
	
	protected void setId(Integer id) {
		this.id = id;
	}
	
	@Column(unique = true, nullable = false)
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Generates a hash for password using salt from AuthDataApplication.getSalt()
	 * and returns the hash encoded as a Base64 String.
	 * @see AuthDataApplication.getSalt();
	 * @param password to encode
	 * @return base64 encoded SHA hash, 28 characters 
	 */
	public static String getHash(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(((IAuthSettings)Application.get()).getSalt());
			byte[] hash = md.digest(password.getBytes());
			// using a Base64 string for the hash because putting a 
			// byte[] into a blob isn't working consistently.
			return new String(Base64.encodeBase64(hash));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(
					"SHA Hash algorithm not found. Make available, or override this method.", e);
		}
	}

	@Column(length = 28, nullable = false)
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	/**
	 * @param password new password for user
	 */
	public void setPassword(String password) {
		passwordHash = getHash(password);
	}

	/**
	 * Performs hash on given password and compares it to the correct hash.
	 * @true if hashed password is correct
	 */
	public boolean checkPassword(String password){
		return passwordHash.equals(getHash(password));
	}
	
	/**
	 * @return true if validRoles and this user have any
	 * roles in common
	 */
	public boolean hasAnyRole(Roles validRoles) {
		for (String role: roles)
			if (validRoles.hasRole(role))
				return true;
		return false;
	}

	@CollectionOfElements
	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}
	
	/** 
	 * @return salted hash that is determined by both username and password hash. 
	 */
	@Transient
	public String getToken() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(((IAuthSettings)Application.get()).getSalt());
			md.update(passwordHash.getBytes());
			byte[] hash = md.digest(username.getBytes());
			return new String(Base64UrlSafe.encodeBase64(hash));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(
					"SHA Hash algorithm not found. Make available, or override this method.", e);
		}
	}
	
	/**
	 * @return username
	 */
	@Override
	public String toString() {
		return username;
	}
}
