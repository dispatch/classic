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


import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.hibernate.SessionFactory;
import org.hibernate.context.ManagedSessionContext;

/**
 * Provides access to the Hibernate session factory and current sessions.
 * @author Nathan Hamblen
 */
public class DataStaticService {
	@Deprecated
	private static SessionFactory hibernateSessionFactory;
	
	/**
	 * @return session factory, as returned by the application
	 * @throws WicketRuntimeException if session factory can not be found 
	 * @see IDataApplication
	 */
	public static SessionFactory getHibernateSessionFactory() {
		Application app = Application.get();
		if (app instanceof IDataApplication)
			return ((IDataApplication)app).getHibernateSessionFactory();
		if (hibernateSessionFactory != null)
			return hibernateSessionFactory;
		throw new WicketRuntimeException("Please implement IDataApplication in your Application subclass.");
	}
	
	/**
	 * @return Hibernate session bound to current thread
	 */
	public static org.hibernate.classic.Session getHibernateSession() {
		dataSessionRequested();
		return getHibernateSessionFactory().getCurrentSession();
	}
	
	public static boolean hasBoundSession() {
		return ManagedSessionContext.hasBind(getHibernateSessionFactory());
	}
	
	/**
	 * Notifies current request cycle that a data session was requested, if a session factory
	 * was not already bound for this thread and the request cycle is an IDataRequestCycle.
	 * @see IDataRequestCycle
	 */
	private static void dataSessionRequested() {
		if (!ManagedSessionContext.hasBind(DataStaticService.getHibernateSessionFactory())) {
			// if session is unavailable, it could be a late-loaded conversational cycle
			RequestCycle cycle = RequestCycle.get();
			if (cycle instanceof IDataRequestCycle)
				((IDataRequestCycle)cycle).dataSessionRequested();
		}
	}

	
	/**
	 * Please implement IDataApplication in your application class instead of calling this method.
	 * @deprecated
	 * @see IDataApplication
	 */
	public static void setSessionFactory(SessionFactory sessionFactory) {
		hibernateSessionFactory = sessionFactory;
	}
	
	/**
	 * Wraps SessionUnit callback in a temporary thread-bound Hibernate session if
	 * necessary. This is to be used outside of a regular a session-handling request cycle,
	 * such as during application init or an external Web service request. 
	 * The temporary session and transaction, if created, are closed after the callback returns and 
	 * uncommited transactions are rolled back. Be careful of returning detached Hibernate 
	 * objects that may not be fully loaded with data; consider using projections / scalar 
	 * queries instead.
	 * @param unit work to be performed in thread-bound session
	 * @see SessionUnit
	 */
	public static Object ensureSession(SessionUnit unit) {
		dataSessionRequested();
		SessionFactory sf = getHibernateSessionFactory();
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