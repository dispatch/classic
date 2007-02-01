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

import java.net.URI;
import java.net.URL;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import net.databinder.components.PageExpiredCookieless;
import net.databinder.util.URIConverter;
import net.databinder.util.URLConverter;
import net.databinder.web.NorewriteWebResponse;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import wicket.Request;
import wicket.Session;
import wicket.markup.html.pages.PageExpiredErrorPage;
import wicket.protocol.http.WebApplication;
import wicket.protocol.http.WebResponse;
import wicket.util.convert.Converter;
import wicket.util.convert.IConverterFactory;

/**
 * Optional Databinder base Application class for configuration and session management. 
 * Independent WebApplication subclasses will need to establish both Wicket and 
 * Hibernate session factories.
 * @author Nathan Hamblen
 */
public abstract class DataApplication extends WebApplication {
	/** true if cookieless use is supported through URL rewriting(defaults to true). */
	private boolean cookielessSupported = true;
	
	/**
	 * Configures this application for development or production, turns off 
	 * default page versioning, and <strong>initializes Hibernate session factory</strong>. 
	 * Development mode is the default; set a JVM property or context/init parameter of 
	 * wicket.configuration=deployment to enable production defaults. If you override this 
	 * method, be sure to call super() or initialize the Hibernate session factory yourself.
	 * @see DataStaticService 
	 */
	@Override
	protected void init() {
		// this databinder-specific parameter will eventually be dropped in favor
		// of wicket.configuration
		String configuration = System.getProperty("net.databinder.configuration");
		if (configuration != null)
			configure(configuration);
		
		// we find versioning less useful for simple, data-driven pages
		getPageSettings().setVersionPagesByDefault(false);

		// register URL converters
		getApplicationSettings().setConverterFactory(new IConverterFactory() {
			/** Registers URLConverter and URIConverter in addition to the Wicket defaults. */
			public wicket.util.convert.IConverter newConverter(Locale locale) {
				Converter conv = new Converter(locale);
				conv.set(URL.class, new URLConverter());
				conv.set(URI.class, new URIConverter());
				return conv;
			}
		});

		DataStaticService.setSessionFactory(buildHibernateSessionFactory());
	}
	
	/**
	 * Called by init to create  Hibernate session factory, triggering a general Hibernate
	 * initialization. Override if using a custom session factory.
	 */
	public SessionFactory buildHibernateSessionFactory() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			configureHibernate(config);
			return config.buildSessionFactory();
		} catch (Throwable ex) {
				throw new ExceptionInInitializerError(ex);
		}
	}
	
	/**
	 * Override to add annotated classes for persistent storage by Hibernate, but don't forget
	 * to call this super-implementation. If running in a development environment,
	 * the session factory is set for hbm2ddl auto-updating to create and add columns to tables 
	 * as required. Otherwise, it is configured for C3P0 connection pooling. (At present the two
	 * seem to be incompatible.) If you don't want this behaviour, don't call the 
	 * super-implementation.
	 * @param config used to build Hibernate session factory
	 */
	protected  void configureHibernate(AnnotationConfiguration config) {
		config.setProperty("hibernate.current_session_context_class","managed");

    	if (isDevelopment())
    		config.setProperty("hibernate.hbm2ddl.auto", "update");
    	else {
    		config.setProperty("hibernate.c3p0.max_size", "20")
    		.setProperty("hibernate.c3p0.timeout","3000")
    		.setProperty("hibernate.c3p0.idle_test_period", "300");
    	}
	}
	
	/**
	 * Returns a new instance of a DataSession. Override if your application uses
	 * its own DataSession subclass. 
	 * @return new instance of DataSession
	 */
	@Override
	public Session newSession(Request request) {
		return new DataSession(DataApplication.this, request);
	}
	
	/**
	 * Reports if the program is running in a development environment, as determined by the
	 * "wicket.configuration" environment variable or context/init parameter. If that variable 
	 * is unset or set to "development", the app is considered to be running in development.  
	 * @return true if running in a development environment
	 */
	protected boolean isDevelopment() {
		return  getConfigurationType().equalsIgnoreCase(DEVELOPMENT);
	}
	
	/**
	 * If <code>isCookielessSupported()</code> returns false, this method returns
	 * a custom WebResponse that disables URL rewriting.
	 */
	@Override
	protected WebResponse newWebResponse(final HttpServletResponse servletResponse)
	{
		if (isCookielessSupported())
			return super.newWebResponse(servletResponse);
		return NorewriteWebResponse.getNew(this, servletResponse);
	}

	/**
	 * @return  true if cookieless use is supported through URL rewriting.
	 */
	public boolean isCookielessSupported() {
		return cookielessSupported;
	}

	/**
	 * Set to false to disable URL rewriting and consequentally hamper cookieless 
	 * browsing.  Users with cookies disabled, and more importantly search engines, 
	 * will still be able to browse the application through bookmarkable URLs. Because
	 * rewriting is disabled, these URLs will have no jsessionid appended and will 
	 * remain static.
	 * <p> The Application's "page expired" error page will be set to PageExpiredCookieless
	 * if cookielessSupported is false, unless an alternate error page has already been
	 * specified. This page will appear when cookieless users try to follow a link or 
	 * form-submit that requires a session, informing them that cookies are required.
	 * </p>
	 * @param cookielessSupported  true if cookieless use is supported through 
	 * URL rewriting
	 * @see net.databinder.components.PageExpiredCookieless
	 */
	protected void setCookielessSupported(boolean cookielessSupported) {
		Class expected = this.cookielessSupported ? 
				PageExpiredErrorPage.class : PageExpiredCookieless.class;
		
		this.cookielessSupported = cookielessSupported;
		
		if (getApplicationSettings().getPageExpiredErrorPage().equals(expected))
			getApplicationSettings().setPageExpiredErrorPage(cookielessSupported ?
					PageExpiredErrorPage.class : PageExpiredCookieless.class);
	}
}
