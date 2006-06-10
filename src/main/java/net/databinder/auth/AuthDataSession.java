package net.databinder.auth;

import net.databinder.DataSession;
import net.databinder.auth.data.IUser;
import net.databinder.models.HibernateObjectModel;
import net.databinder.models.IQueryBinder;

import org.hibernate.Query;
import org.hibernate.QueryException;

import wicket.model.IModel;

public class AuthDataSession extends DataSession {
	private IModel user;
	private Class userClass;
	
	protected AuthDataSession(AuthDataApplication application) {
		super(application);
		userClass = application.getUserClass();
	}
	
	public IUser getUser() {
		
		return isSignedIn() ? (IUser) user.getObject(null) : null;
	}
	
	public boolean isSignedIn() {
		return user != null;
	}
	
	public boolean signIn(final String username, final String password) {
		String query = "from " + userClass.getCanonicalName() 
			+ " where username = :username";
		
		try {
			IModel potential = new HibernateObjectModel(query, new IQueryBinder() {
				public void bind(Query query) {
					query.setString("username", username);
				}
			});
			IUser potentialUser = (IUser) potential.getObject(null);
			if (potentialUser.checkPassword(password))
				user = potential;
		} catch (QueryException e){ }
		
		return isSignedIn();
	}
	
	@Override
	protected void detach() {
		if (user != null) user.detach();
		super.detach();
	}
}
