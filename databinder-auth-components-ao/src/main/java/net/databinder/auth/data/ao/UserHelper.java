package net.databinder.auth.data.ao;

import java.security.MessageDigest;
import java.util.Arrays;

import net.databinder.auth.AuthApplication;

import org.apache.wicket.Application;
import org.apache.wicket.authorization.strategies.role.Roles;

public class UserHelper {
	UserBase user;
	
	public UserHelper(UserBase user) {
		this.user = user;
	}
	
	public String getPassword() {
		return null;
	}
		
	public void setPassword(String password) {
		user.setPasswordHash(getHash(password));
	}
	
	public static byte[] getHash(String string) {
		MessageDigest md = ((AuthApplication)Application.get()).getDigest();
		return md.digest(string.getBytes());
	}
	
	public boolean checkPassword(String password) {
		return Arrays.equals(user.getPasswordHash(), getHash(password));
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
	
	public boolean hasAnyRole(Roles roles) {
		return getRoles().hasAnyRole(roles);
	}
	
	public void update(MessageDigest digest) {
		digest.update(user.getPasswordHash());
	}
}
