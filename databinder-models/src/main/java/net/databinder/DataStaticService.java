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


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.ManagedSessionContext;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;

/**
 * Holds a static reference to Hibernate session factory.
 * @author Nathan Hamblen
 */
public class DataStaticService {
	private static SessionFactory hibernateSessionFactory;
	
	/**
	 * @return session factory, as configured by the application
	 * @throws WicketRuntimeException if session factory was not previously set 
	 */
	public static SessionFactory getHibernateSessionFactory() {
		if (hibernateSessionFactory == null)
			throw new WicketRuntimeException("The Hibernate session factory has not been " +
					"initialized. This is normally done in DataApplication.init().");
		return hibernateSessionFactory;
	}
	
	/**
	 * @return Hibernate session bound to current thread
	 */
	public static org.hibernate.classic.Session getHibernateSession() {
		dataSessionRequested();
		return getHibernateSessionFactory().getCurrentSession();
	}
	
	/**
	 * Notifies current request cycle that a data session was requested, if a session factory
	 * was not already bound for this thread and the request cycle is a listener.
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
	 * @param sessionFactory to use for this application
	 */
	public static void setSessionFactory(SessionFactory sessionFactory) {
		hibernateSessionFactory = sessionFactory;
	}
	
	/**
	 * Wraps SessionUnit callback in a new thread-bound Hibernate session if one is not present.
	 * This is to be used only when a thread-bound session may not be available, such as
	 * application init or an external Web service request. The new session and transaction, if 
	 * created, are closed after the callback returns and uncommited transactions are rolled 
	 * back. (Existing sessions are left as they were.) Be careful 
	 * of returning detached Hibernate objects that may not be fully loaded with data; 
	 * consider using projections / scalar queries instead.
	 * @param unit work to be performed in thread-bound session
	 * @see SessionUnit
	 */
	public static Object ensureSession(SessionUnit unit) {
		dataSessionRequested();
		SessionFactory sf = getHibernateSessionFactory();
		if (ManagedSessionContext.hasBind(hibernateSessionFactory))
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
	/**
	 * Please use ensureSession(SessionUnit unit) instead.
	 * @deprecated
	 */
	public static Object wrapInHibernateSession(final Callback callback) {
		return ensureSession(new SessionUnit() {
			public Object run(Session sess) {
				return callback.call();
			}
		});
	}
	/**
	 * Please use SessionUnit instead.
	 * @deprecated
	 */
	public interface Callback {
		/** Within call, session is available from DataStaticService.getHibernateSession().  */
		Object call(); 
	}
}