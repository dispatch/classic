package net.databinder.auth.data;

import org.apache.wicket.authorization.strategies.role.Roles;

/**
 * Base user interface.
 * @author Nathan Hamblen
 */
public interface IUser {
	/** @return ture if user has any role matching those given */
	public boolean hasAnyRole(Roles roles);
	/** @return true if password is valid for this user. */
	public boolean checkPassword(String password);
	
	/**
	 * Sub-interface for user classes supporting cookie authentication.
	 */
	public interface CookieAuth extends IUser {
		/**
		 * @return value used to identify user; may be e-mail or other identifier.
		 */
		public String getUsername();

		/** 
		 * @return URL-safe unique value for this user and password that can not be externally determined
		 */
		public String getToken();
	}
}
