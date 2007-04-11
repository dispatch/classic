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

import net.databinder.DataApplication;
import net.databinder.auth.data.IUser;
import net.databinder.models.ICriteriaBuilder;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.WebPage;
import org.hibernate.Criteria;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Restrictions;

import org.apache.wicket.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authorization.strategies.role.Roles;

/**
 * Adds basic authentication fuctionality to DataApplication. This class is a derivative
 * of Wicket's AuthenticatedWebApplication, brought into the DataApplication hierarchy. Unlike
 * that implementation, this one supplies a default User entity.. To use a different IUser class,
 * or a User subclass, override getUserClass(). (This class, whatever it is, will be added to 
 * the Hibernate Annotations configuration automatically. It is also possible to use Databinder
 * authentication without extending this base class by implementing IAuthSettings. 
 * @see IAuthSettings
 * @see IUser
 * @author Nathan Hamblen
 */
public abstract class AuthDataApplication extends DataApplication 
implements IUnauthorizedComponentInstantiationListener, IRoleCheckingStrategy, IAuthSettings {

	/**
	 * Calls configuration in super-implementation, then sets Wicket's security strategy for role
	 * authorization and appoints this class as the unauthorized instatiation listener.
	 * @see DataApplication.init()
	 */
	@Override
	protected void init() {
		super.init();
		getSecuritySettings().setAuthorizationStrategy(new RoleAuthorizationStrategy(this));
		getSecuritySettings().setUnauthorizedComponentInstantiationListener(this);
	}

	/**
	 * @return new AuthDataSession
	 * @see AuthDataSession
	 */
	@Override
	public Session newSession(Request request, Response response) {
		return new AuthDataSession(this, request);
	}
	/**
	 * Adds to the configuration whatever IUser class is defined.
	 */
	@Override
	protected void configureHibernate(AnnotationConfiguration config) {
		super.configureHibernate(config);
		config.addAnnotatedClass(getUserClass());
	}
	
	/**
	 * Sends to sign in page if not signed in, otherwise throws UnauthorizedInstantiationException.
	 */
	public void onUnauthorizedInstantiation(Component component) {
		if (((AuthDataSession)Session.get()).isSignedIn()) {
			throw new UnauthorizedInstantiationException(component.getClass());
		}
		else {
			throw new RestartResponseAtInterceptPageException(getSignInPageClass());
		}	
	}
	
	/**
	 * Passes query on to the IUser object if signed in.
	 */
	public final boolean hasAnyRole(Roles roles) {
		IUser user = ((AuthDataSession)Session.get()).getUser();
		return user == null ? false : user.hasAnyRole(roles);
	}

	/**
	 * Please override to use your own IUser implementation. This base implementation 
	 * will be removed in a future version.
	 * @return class to be used for signed in users
	 */
	@SuppressWarnings("deprecation")
	public Class< ? extends IUser> getUserClass() {
		return net.databinder.auth.data.DataUser.class;
	}
	
	/** Default user criteria builder, binds to "username" property. */
	private static class UsernameCriteriaBuilder implements ICriteriaBuilder {
		private String username;
		public UsernameCriteriaBuilder(String username) { this.username = username; }
		public void build(Criteria criteria) {
			criteria.add(Restrictions.eq("username", username));
		}
	}
	/**
	 * Get a criteria builder to find users by username, needed for retrieving users in 
	 * 	<tt>AuthDataSession</tt>. The default implementation matches on a
	 * "username" property. Override to match on an e-mail address or other 
	 * property name.
	 * @param username username to look up
	 * @return builder to match on the username
	 * @see AuthDataSession
	 */
	public ICriteriaBuilder getUserCriteriaBuilder(String username) {
		return new UsernameCriteriaBuilder(username);
	}

	/**
	 * Please override to return your own sign in page. This implementation will be removed
	 * in a future version.
	 * @return page to sign in users
	 */
	@SuppressWarnings("deprecation")
	public Class< ? extends WebPage> getSignInPageClass() {
		return net.databinder.auth.components.DataSignInPage.class;
	}
	
	/**
	 * Cryptographic salt to be used in authentication. The default IUser
	 * implementation uses this value. If your imlementation does not require
	 * a salt value (!), return null.
	 * @return
	 */
	public abstract byte[] getSalt();
}
