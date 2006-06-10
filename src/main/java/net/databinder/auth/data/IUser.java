package net.databinder.auth.data;

import wicket.authorization.strategies.role.Roles;

public interface IUser {
	public boolean hasAnyRole(Roles roles);
	public boolean checkPassword(String password);
}
