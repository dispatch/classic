package net.databinder;

import org.hibernate.SessionFactory;

/**
 * Databinder application interface. DataStaticService expects the current Wicket
 * application to conform to this interface and supply a session factory as needed.
 * @see DataStaticService
 * @author Nathan Hamblen
 */
public interface IDataApplication {
	/**
	 * Supply the session factory for the given key. Applications needing only one
	 * session factory may return it without inspecting the key parameter.
	 * @param factory key, or null for the default factory
	 * @return configured Hibernate session factory
	 */
	SessionFactory getHibernateSessionFactory(Object key);
	
	/**
	 * Determines when the data browser page should be accessible. Normally this
	 * is allowed in development but disabled for live sites.
	 * @return true if data browser should be accessible
	 */
	boolean isDataBrowserAllowed();
	/* Note: considered using IAuthorizationStrategy, but didn't want to risk a user linking
	 * DataBrowser's jar without extending DataApplication and neglecting
	 * to add any authorization strategy to prevent the browser's  use in production. */ 
}
