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
import net.databinder.auth.components.DataSignInPage;
import net.databinder.auth.data.IUser;
import net.databinder.auth.data.DataUser;

import org.hibernate.cfg.AnnotationConfiguration;

import wicket.Component;
import wicket.RestartResponseAtInterceptPageException;
import wicket.Session;
import wicket.authorization.IUnauthorizedComponentInstantiationListener;
import wicket.authorization.UnauthorizedInstantiationException;
import wicket.authorization.strategies.role.IRoleCheckingStrategy;
import wicket.authorization.strategies.role.RoleAuthorizationStrategy;
import wicket.authorization.strategies.role.Roles;
import wicket.markup.html.WebPage;

/**
 * Adds basic authentication fuctionality to DataApplication. This class is largely a derivative
 * of Wicket's AuthenticatedWebApplication brought into the DataApplication hierarchy. Unlike
 * that implementation, this one supplies a default annotated User class. To use a different IUser class,
 * or a User subclass, override getUserClass(). (This class, whatever it is, will be added to 
 * the Hibernate Annotations configuration automatically.
 * @see DataUser
 * @see IUser
 * @author Nathan Hamblen
 */
public abstract class AuthDataApplication extends DataApplication implements IUnauthorizedComponentInstantiationListener, IRoleCheckingStrategy {

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
	 * Overrides to return an AuthDataSession. You may override to return an AuthDataSession
	 * subclass.
	 * @see AuthDataSession
	 */
	@Override
	protected AuthDataSession newDataSession() {
		return new AuthDataSession(this);
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
	 * Override to use your own IUser implementation. 
	 * @return class to be used for signed in users
	 */
	public Class< ? extends IUser> getUserClass() {
		return DataUser.class;
	}

	/**
	 * @return page to sign in users
	 */
	public Class< ? extends WebPage> getSignInPageClass() {
		return DataSignInPage.class;
	}
	
	/**
	 * Cryptographic salt to be used in authentication. The default IUser
	 * implementation uses this value. If your imlementation does not require
	 * a salt value (!), return null.
	 * @return
	 */
	public abstract byte[] getSalt();}
