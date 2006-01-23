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

import java.net.URL;

import net.databinder.util.URLConverter;

import org.hibernate.cfg.AnnotationConfiguration;

import wicket.ApplicationPages;
import wicket.ISessionFactory;
import wicket.Session;
import wicket.protocol.http.WebApplication;
import wicket.util.convert.Converter;
import wicket.util.convert.IConverterFactory;

/**
 * Databinder Application subclass for request cycle hooks and a basic configuration.
 * @author Nathan Hamblen
 */
public abstract class DataApplication extends WebApplication {
	private boolean development;
	
	/**
	 * Configures this application for development or production, sets a home page,
	 * turns off default page versioning, and sets a session factory. Override for 
	 * customization.
	 */
	@Override
	protected void init() {
		String configuration = System.getProperty("net.databinder.configuration", "development");
		development = configuration.equalsIgnoreCase("development");
		getSettings().configure(configuration);
		getPages().setHomePage(getHomePage());
		getPages().setHomePageRenderStrategy(ApplicationPages.BOOKMARK_REDIRECT);
		// versioning doesn't do so much for database driven pages 
		getSettings().setVersionPagesByDefault(false);

		setSessionFactory(new ISessionFactory() {
			public Session newSession()
			{
				return newDataSession();
			}
    	});
	}
	
	/**
	 * A factory that registers URLConverter in addition to the Wicket defaults.
	 */
	@Override
	public IConverterFactory getConverterFactory() {
		return new IConverterFactory() {
			public wicket.util.convert.IConverter newConverter(java.util.Locale locale) {
				Converter conv = new Converter(locale);
				conv.set(URL.class, new URLConverter());
				return conv;
			}
		};
	}
	
	/**
	 * Returns a new instance of a DataSession. Override if your application uses
	 * its own DataSession subclass. 
	 * @return new instance of DataSession
	 */
	protected DataSession newDataSession() {
		return new DataSession(DataApplication.this);
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
	 * @param config used to build Hibernate session factory
	 */
	protected  void configureHibernate(AnnotationConfiguration config) {
    	if (isDevelopment())
    		config.setProperty("hibernate.hbm2ddl.auto", "update");
    	else {
    		config.setProperty("hibernate.c3p0.max_size", "20")
    		.setProperty("hibernate.c3p0.timeout","3000")
    		.setProperty("hibernate.c3p0.idle_test_period", "300");
    	}
	}
	
	/**
	 * Return your application's default page.
	 */
	protected abstract Class getHomePage();
}