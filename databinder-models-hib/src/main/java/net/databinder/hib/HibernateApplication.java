package net.databinder.hib;


import org.hibernate.SessionFactory;

/**
 * Databinder application interface. DataStaticService expects the current Wicket
 * application to conform to this interface and supply a session factory as needed.
 * @see Databinder
 * @author Nathan Hamblen
 */
public interface HibernateApplication {
	/**
	 * Supply the session factory for the given key. Applications needing only one
	 * session factory may return it without inspecting the key parameter.
	 * @param factory key, or null for the default factory
	 * @return configured Hibernate session factory
	 */
	SessionFactory getHibernateSessionFactory(Object key);
}