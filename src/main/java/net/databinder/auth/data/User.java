package net.databinder.auth.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import net.databinder.auth.AuthDataApplication;

import org.hibernate.annotations.CollectionOfElements;

import wicket.Application;
import wicket.authorization.strategies.role.Roles;

@Entity
public class User implements IUser{
	private Integer id;
	private byte[] passwordHash;
	private String username;
	private Set<String> roles;
	
	public User() {
	}
	
	public User(String username, String password) {
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
	
	protected byte[] getHash(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(((AuthDataApplication)Application.get()).getSalt());
			return md.digest(password.getBytes());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(
					"Hash algorithm not found. Make available, or override this method.", e);
		}
	}

	@Column(length = 20, nullable = false)
	public byte[] getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(byte[] passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	public boolean checkPassword(String password){
		return passwordHash.equals(getHash(password));
	}
	
	/**
	 * @return true if authorized and this user have any
	 * roles in common
	 */
	public boolean hasAnyRole(Roles authorized) {
		for (String role: roles)
			if (authorized.hasRole(role))
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
}
