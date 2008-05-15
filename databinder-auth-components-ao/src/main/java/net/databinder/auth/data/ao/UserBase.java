package net.databinder.auth.data.ao;

import net.databinder.auth.data.DataPassword;
import net.databinder.auth.data.DataUser;
import net.java.ao.RawEntity;
import net.java.ao.schema.Ignore;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Unique;

import org.apache.wicket.authorization.strategies.role.Roles;

/** 
 * Optional DataUser extension with ActiveObjects field annotations. 
 * Client applications may extend this interface with an @Implementation
 * that is an extension of UserHelper.
 */
public interface UserBase extends DataUser {

	@Ignore
	public DataPassword getPassword();
	
	@NotNull
	public byte[] getPasswordHash();
	public void setPasswordHash(byte[] passwordHash);
	
	@NotNull
	@Unique
	public String getUsername();
	public void setUsername(String username);
	
	public String getRoleString();
	public void setRoleString(String roleString);
	
	@Ignore
	public Roles getRoles();
	@Ignore
	public void setRoles(Roles roles);
}
