package net.databinder;

import net.databinder.data.IUser;
import net.databinder.models.HibernateObjectModel;
import net.databinder.models.IQueryBinder;

import org.hibernate.Query;
import org.hibernate.QueryException;

import wicket.authorization.strategies.role.Roles;
import wicket.model.IModel;

public class AuthDataSession extends DataSession {
	private IModel user;
	private Class userClass;
	
	protected AuthDataSession(AuthDataApplication application) {
		super(application);
		userClass = application.getUserClass();
	}
	
	public boolean hasAnyRole(Roles roles) {
		return isSignedIn() ? getUser().hasAnyRole(roles) : false;
	}

	public IUser getUser() {
		
		return isSignedIn() ? (IUser) user.getObject(null) : null;
	}
	
	public boolean isSignedIn() {
		return user != null;
	}
	
	public boolean signIn(final String username, final String password) {
		String query = "from " + userClass.getCanonicalName() 
			+ " where username = :username and password = :password";
		
		try {
			user = new HibernateObjectModel(query, new IQueryBinder() {
				public void bind(Query query) {
					query.setString("username", username)
						.setString("password", password);
				}
			});
		} catch (QueryException e){
			return false;
		}
		return true;
	}
	
	@Override
	protected void detach() {
		if (user != null) user.detach();
		super.detach();
	}
}
