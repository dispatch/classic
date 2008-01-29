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

package net.databinder.hib;



import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.hibernate.SessionFactory;
import org.hibernate.context.ManagedSessionContext;

/**
 * Provides access to application-bound Hibernate session factories and current sessions.
 * This class will work with a
 * <a href="http://www.hibernate.org/hib_docs/v3/api/org/hibernate/context/ManagedSessionContext.html">ManagedSessionContext</a>
 * and IDataRequestCycle listener when present, but neither is required so long as a
 * "current" session is available from the session factory supplied by the application.
 * @see HibernateApplication
 * @author Nathan Hamblen
 */
public class Databinder {
	@Deprecated
	private static SessionFactory hibernateSessionFactory;
	
	/**
	 * @return default session factory, as returned by the application
	 * @throws WicketRuntimeException if session factory can not be found 
	 * @see HibernateApplication
	 */
	public static SessionFactory getHibernateSessionFactory() {
		return getHibernateSessionFactory(null);
	}
	/**
	 * @param key object, or null for the default factory
	 * @return session factory, as returned by the application
	 * @throws WicketRuntimeException if session factory can not be found 
	 * @see HibernateApplication
	 */
	public static SessionFactory getHibernateSessionFactory(Object key) {
		Application app = Application.get();
		if (app instanceof HibernateApplication)
			return ((HibernateApplication)app).getHibernateSessionFactory(key);
		if (hibernateSessionFactory != null)
			return hibernateSessionFactory;
		throw new WicketRuntimeException("Please implement IDataApplication in your Application subclass.");
	}
	
	/**
	 * @return default Hibernate session bound to current thread
	 */
	public static org.hibernate.classic.Session getHibernateSession() {
		return getHibernateSession(null);
	}
	/**
	 * @param factory key, or null for the default factory
	 * @return Hibernate session bound to current thread
	 */
	public static org.hibernate.classic.Session getHibernateSession(Object key) {
		dataSessionRequested(key);
		return getHibernateSessionFactory(key).getCurrentSession();
	}
	/**
	 * @return true if a session is bound for the default factory
	 */
	public static boolean hasBoundSession() {
		return hasBoundSession(null);
	}
	
	/**
	 * @param factory key, or null for the default factory
	 * @return true if a session is bound for the keyed factory
	 */
	public static boolean hasBoundSession(Object key) {
		return ManagedSessionContext.hasBind(getHibernateSessionFactory(key));
	}
	
	/**
	 * Notifies current request cycle that a data session was requested, if a session factory
	 * was not already bound for this thread and the request cycle is an IDataRequestCycle.
	 * @param factory key, or null for the default factory
	 * @see HibernateRequestCycle
	 */
	private static void dataSessionRequested(Object key) {
		if (!hasBoundSession(key)) {
			// if session is unavailable, it could be a late-loaded conversational cycle
			RequestCycle cycle = RequestCycle.get();
			if (cycle instanceof HibernateRequestCycle)
				((HibernateRequestCycle)cycle).dataSessionRequested(key);
		}
	}
	
	/**
	 * Please implement IDataApplication in your application class instead of calling this method.
	 * @deprecated
	 * @see HibernateApplication
	 */
	public static void setSessionFactory(SessionFactory sessionFactory) {
		hibernateSessionFactory = sessionFactory;
	}
	
	/**
	 * Wraps SessionUnit callback in a temporary thread-bound Hibernate session from the default
	 * factory if necessary. This is to be used outside of a regular a session-handling request cycle,
	 * such as during application init or an external Web service request. 
	 * The temporary session and transaction, if created, are closed after the callback returns and 
	 * uncommited transactions are rolled back. Be careful of returning detached Hibernate 
	 * objects that may not be fully loaded with data; consider using projections / scalar 
	 * queries instead.<b>Note</b> This method uses a ManagedSessionContext. With JTA
	 * or other forms of current session lookup a wrapping session will not be
	 * detected and a new one will always be created.
	 * @param unit work to be performed in thread-bound session
	 * @see SessionUnit
	 */
	public static Object ensureSession(SessionUnit unit) {
		return ensureSession(unit, null);
	}
	/**
	 * Wraps SessionUnit callback in a temporary thread-bound Hibernate session from the keyed
	 * factory if necessary. This is to be used outside of a regular a session-handling request cycle,
	 * such as during application init or an external Web service request. 
	 * The temporary session and transaction, if created, are closed after the callback returns and 
	 * uncommited transactions are rolled back. Be careful of returning detached Hibernate 
	 * objects that may not be fully loaded with data; consider using projections / scalar 
	 * queries instead. <b>Note</b> This method uses a ManagedSessionContext. With JTA
	 * or other forms of current session lookup a wrapping session will not be
	 * detected and a new one will always be created. 
	 * @param unit work to be performed in thread-bound session
	 * @param factory key, or null for the default factory
	 * @see SessionUnit
	 */
	public static Object ensureSession(SessionUnit unit, Object key) {
		dataSessionRequested(key);
		SessionFactory sf = getHibernateSessionFactory(key);
		if (ManagedSessionContext.hasBind(sf))
			return unit.run(getHibernateSession());
		org.hibernate.classic.Session sess = sf.openSession();
		try {
			sess.beginTransaction();
			ManagedSessionContext.bind(sess);
			return unit.run(sess);
		} finally {
			try {
				if (sess.getTransaction().isActive())
					sess.getTransaction().rollback();
			} finally {
				sess.close();
				ManagedSessionContext.unbind(sf);
			}
		}
	}
}