/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.databinder.auth;

/**
 * Holds IUser instance for signed in users. Expects IUser implementation to be an annotated
 * class where with a unique username property.
 */
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
	
	/**
	 * Initialize new session. Retains user class from AuthDataApplication instance.
	 */
	protected AuthDataSession(AuthDataApplication application) {
		super(application);
		userClass = application.getUserClass();
	}
	
	/**
	 * @return IUser object for current user, or null if none signed in.
	 */
	public IUser getUser() {
		return isSignedIn() ? (IUser) user.getObject(null) : null;
	}
	
	public boolean isSignedIn() {
		return user != null;
	}

	/**
	 * @return true if signed in, false if credentials incorrect
	 */
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
	
	/** Detach user from session */
	public void signOut() {
		user = null;
	}
	
	/**
	 * Deatch our user model, which would not get the message otherwise.
	 */
	@Override
	protected void detach() {
		if (user != null) user.detach();
		super.detach();
	}
}
