package net.databinder.auth.data.ao;

import java.security.MessageDigest;
import java.util.Arrays;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.data.DataPassword;

import org.apache.wicket.Application;
import org.apache.wicket.authorization.strategies.role.Roles;

/**
 * Helper for UserBase, handles password hashing and virtual collection of roles (from roleString).
 * Client applications must extend this class to use it, because as an @Implementation it must have
 * a contructor matching the mapped entity interface.
 */
public class UserHelper {
	UserBase user;
	
	public UserHelper(UserBase user) {
		this.user = user;
	}
	
	public static byte[] getHash(String string) {
		MessageDigest md = ((AuthApplication)Application.get()).getDigest();
		return md.digest(string.getBytes());
	}
	
	public DataPassword getPassword() {
		return new DataPassword() {
			public void change(String password) {
				user.setPasswordHash(getHash(password));
			}
			public boolean matches(String password) {
				return Arrays.equals(getHash(password), user.getPasswordHash());
			}
			public void update(MessageDigest digest) {
				digest.update(user.getPasswordHash());
			}
		}; 
	}
	
	public void setRoleString(String roleString) {
		user.setRoleString(roleString);
	}
	
	public Roles getRoles() {
		String roleString = user.getRoleString();
		if (roleString == null) roleString = "";
		return new Roles(roleString);
	}
	
	public void setRoles(Roles roles) {
		user.setRoleString(roles.toString());
	}
	
	public boolean hasRole(String role) {
		return getRoles().contains(role);
	}
	
	public void update(MessageDigest digest) {
		digest.update(user.getPasswordHash());
	}
}
