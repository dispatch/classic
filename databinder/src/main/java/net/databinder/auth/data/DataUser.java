package net.databinder.auth.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.wicket.authorization.strategies.role.Roles;
import org.hibernate.annotations.CollectionOfElements;

/**
 * This class is deprecated.
 * @deprecated Please extend UserBase or implement IUser directly.
 * @author Nathan Hamblen
 */
@Entity
public class DataUser extends UserBase implements IUser.CookieAuth, Serializable {
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
	 * Password is not retained, but this method satifies some bean utils.
	 * @return always null
	 */
	public String getPassword() {
		return null;
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
	 * @return username
	 */
	@Override
	public String toString() {
		return username;
	}
}
