package net.databinder.data;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.CollectionOfElements;

import wicket.authorization.strategies.role.Roles;

@Entity
public class User implements IUser{
	private Integer id;
	private String password;
	private String username;
	private Set<String> roles;
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}
	protected void setId(Integer id) {
		this.id = id;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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
