package net.databinder.auth.ao;

import net.databinder.auth.AuthDataSessionBase;
import net.databinder.auth.data.DataUser;
import net.databinder.models.ao.EntityModel;
import net.java.ao.RawEntity;

import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebRequest;

/** Session to hold DataUser. */
public class AuthDataSession extends AuthDataSessionBase {

	public AuthDataSession(WebRequest request) {
		super(request);
	}
	
	@Override
	public IModel createUserModel(DataUser user) {
		return new EntityModel((RawEntity) user);
	}
}
