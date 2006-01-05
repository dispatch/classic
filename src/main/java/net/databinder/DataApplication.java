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

package net.databinder;

import org.hibernate.cfg.AnnotationConfiguration;

import wicket.IRequestCycleFactory;
import wicket.ISessionFactory;
import wicket.Request;
import wicket.RequestCycle;
import wicket.Response;
import wicket.Session;
import wicket.protocol.http.WebApplication;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebResponse;
import wicket.protocol.http.WebSession;

/**
 * Databinder Application subclass for request cycle hooks and a basic configuration.
 * @author Nathan Hamblen
 */
public abstract class DataApplication extends WebApplication {
	private boolean development;
	/**
	 * Configures this application for development or production, sets a home page,
	 * and turns of default page versioning. Override for customization.
	 */
	@Override
	protected void init() {
		String configuration = System.getProperty("net.databinder.configuration", "development");
		development = configuration.equalsIgnoreCase("development");
		getSettings().configure(configuration);
		getPages().setHomePage(getHomePage());
		// versioning doesn't do so much for database driven pages 
		getSettings().setVersionPagesByDefault(false);
	}
	
	/**
	 * Reports if the program is running in a development environment, as determined by the
	 * "net.databinder.configuration" environment variable. If that variable is unset or set to 
	 * "development", the app is considered to be running in development.  
	 * @return true if running in a development environment
	 */
	protected boolean isDevelopment() {
		return development;
	}
	
	/**
	 * Override to add annotated classes for persistent storage by Hibernate, but don't forget
	 * to call this super-implementation. If running in a development environment,
	 * the session factory is set for hbm2ddl auto updating to create and add columns to tables 
	 * as required. Otherwise, it is configured for C3P0 connection pooling. (At present the two
	 * seem to be incompatible.) If you don't want this behaviour, don't call the 
	 * super-implementation.
	 * 
	 * @param config used to build Hibernate session factory
	 */
	protected  void configureHibernate(AnnotationConfiguration config) {
    	if (isDevelopment())
    		config.setProperty("hibernate.hbm2ddl.auto", "update");
    	else
    		config.setProperty("hibernate.c3p0.max_size", "20");
	}
	
	/**
	 * Instatiates DataRequestCycle objects through custom request cycle and session factories.
	 */
	@Override
	protected final ISessionFactory getSessionFactory() {
		return sessionFactory;
	}

	private ISessionFactory sessionFactory = new ISessionFactory()
	{
		public Session newSession()
		{
			return new WebSession(DataApplication.this) {
				@Override
				protected IRequestCycleFactory getRequestCycleFactory() {
					return new IRequestCycleFactory() {
						public RequestCycle newRequestCycle(Session session, Request request, Response response) {
							return new DataRequestCycle((WebSession)session, (WebRequest)request, (WebResponse)response);
						};
					};
				}
			};
		}
	};
	/**
	 * Return your application's default page.
	 */
	protected abstract Class getHomePage();
}