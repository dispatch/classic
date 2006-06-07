package net.databinder.data;

import wicket.authorization.strategies.role.Roles;

public interface IUser {
	public boolean hasAnyRole(Roles roles);
}
