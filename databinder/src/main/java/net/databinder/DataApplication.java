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

import java.awt.Color;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import net.databinder.components.PageExpiredCookieless;
import net.databinder.components.hibernate.DataBrowser;
import net.databinder.util.ColorConverter;
import net.databinder.util.URIConverter;
import net.databinder.util.URLConverter;
import net.databinder.web.NorewriteWebResponse;

import org.apache.wicket.Application;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.pages.PageExpiredErrorPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.convert.ConverterLocator;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * Optional Databinder base Application class for configuration and session management. 
 * Supports multiple session factories with key objects.
 * @author Nathan Hamblen
 */
public abstract class DataApplication extends WebApplication implements IDataApplication {
	/** true if cookieless use is supported through URL rewriting(defaults to true). */
	private boolean cookielessSupported = true;
	
	/** App-wide session factory */
	private HashMap<Object, SessionFactory>hibernateSessionFactories = new HashMap<Object, SessionFactory>();
	
	/**
	 * Initializes a default Hibernate session factory. If you override this 
	 * method, be sure to call super() or initialize a Hibernate session factory yourself. 
	 * This method also turns off exceptions for missing resources in deployment mode 
	 * (as search engines will request those long after they are gone) and mounts the
	 * data browser.
	 * @see DataStaticService 
	 */
	@Override
	protected void init() {
		if (!isDevelopment())
			getResourceSettings().setThrowExceptionOnMissingResource(false);
		buildHibernateSessionFactory(null);
		if (isDataBrowserAllowed())
			mountDataBrowser();
	}
	
	/**
	 * Bookmarkable subclass of DataBrowser. Access to the page is permitted
	 * only if the current application is assignable to DataApplication
	 * and returns true for isDataBrowserAllowed().
	 * @see DataBrowser
	 */
	public static class BmarkDataBrowser extends DataBrowser {
		public BmarkDataBrowser() {
			super(((DataApplication)Application.get()).isDataBrowserAllowed());
		}
	}
	
	/**
	 * Mounts Data Diver to /dbrowse. Override to mount elsewhere, or not mount at all.
	 * This method is only called if isDataBrowserAllowed() returns true in init().
	 */
	protected void mountDataBrowser() {
		mountBookmarkablePage("/dbrowse", BmarkDataBrowser.class);
	}

	/**
	 * Called by init to create Hibernate session factory and load a configuration. Passes
	 * a new AnnotationConfiguration to buildHibernateSessionFactory(key, config) by 
	 * default. Override if creating a configuration externally.
	 */
	public void buildHibernateSessionFactory(Object key) {
		buildHibernateSessionFactory(key, new AnnotationConfiguration());
	}
	
	/**
	 * Builds and retains a session factory with the given configuration after passing it to 
	 * configureHibernate methods.
	 */
	final public void buildHibernateSessionFactory(Object key, AnnotationConfiguration config) {
		configureHibernate(config, key);
		setHibernateSessionFactory(key, config.buildSessionFactory());
	}
	
	/**
	 * Configure the session factory associated with the key. The default implementation
	 * calls the key-neutral configureHibernate(config) method.
	 * @param config configuration to update
	 * @param key object, or null for the default factory
	 */
	protected  void configureHibernate(AnnotationConfiguration config, Object key) {
		configureHibernate(config);
	}
		
	/**
	 * Override to add annotated classes to all session factories, but don't forget
	 * to call this super-implementation. If running in a development environment,
	 * the session factory is set for hbm2ddl auto-updating to create and add columns to tables 
	 * as required. Otherwise, it is configured for C3P0 connection pooling. 
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
	 * @param key object, or null for the default factory
	 * @return the retained session factory
	 */
	public SessionFactory getHibernateSessionFactory(Object key) {
		SessionFactory sf = hibernateSessionFactories.get(key);
		if (sf == null)
			if (key == null)
				throw new WicketRuntimeException("The default Hibernate session factory has not been " +
						"initialized. This is normally done in DataApplication.init().");
			else
				throw new WicketRuntimeException("Session factory not found for key: " + key);
		return sf;
	}
	
	/**
	 * @param key object, or null for the default factory
	 * @param sf session factory to retain
	 */
	protected void setHibernateSessionFactory(Object key, SessionFactory sf) {
		hibernateSessionFactories.put(key, sf);
	}
	
	/**
	 * Adds converters to Wicket's base locator.
	 */
	@Override
	protected IConverterLocator newConverterLocator() {
		// register converters
		ConverterLocator converterLocator = new ConverterLocator();
		converterLocator.set(URL.class, new URLConverter());
		converterLocator.set(URI.class, new URIConverter());
		converterLocator.set(Color.class, new ColorConverter());
		return converterLocator;
	}
	
	/**
	 * @return a DataRequestCycle
	 * @see DataRequestCycle
	 */
	@Override
	public RequestCycle newRequestCycle(Request request, Response response) {
		return new DataRequestCycle(this, (WebRequest) request, response);
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
	 * Returns true if development mode is enabled. Override for other behavior.
	 * @return true if the Data Browser page should be enabled
	 */
	protected boolean isDataBrowserAllowed() {
		return isDevelopment();
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
