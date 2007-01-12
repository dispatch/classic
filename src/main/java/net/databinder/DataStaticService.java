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

import org.hibernate.SessionFactory;
import org.hibernate.context.ManagedSessionContext;

import wicket.WicketRuntimeException;

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
	
	public static org.hibernate.classic.Session getHibernateSession() {
		return getHibernateSessionFactory().getCurrentSession();
	}
	
	/**
	 * @param sessionFactory to use for this application
	 */
	public static void setSessionFactory(SessionFactory sessionFactory) {
		hibernateSessionFactory = sessionFactory;
	}
	
	/**
	 * Wraps callback in new a Hibernate session and transaction that are closed after the callback
	 * returns. This is to be used only when a thread-bound session is not available, such as
	 * application init or an external Web service request. Uncommited transactions 
	 * are rolled back, as with DataRequestCycle. Be careful of returning detached Hibernate 
	 * objects that may not be fully loaded with data; consider using projections / scalar
	 * queries instead.
	 * @param callback
	 */
	public static Object wrapInHibernateSession(Callback callback) {
		SessionFactory sf = getHibernateSessionFactory();
		if (ManagedSessionContext.hasBind(hibernateSessionFactory))
			throw new WicketRuntimeException("This thread is already bound to a Hibernate session.");
		org.hibernate.classic.Session sess = sf.openSession();
		try {
			sess.beginTransaction();
			ManagedSessionContext.bind(sess);
			return callback.call();
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
	 * Callback for wrapInHibernateSession().
	 */
	public interface Callback {
		/** Within call, session is available from DataStaticService.getHibernateSession().  */
		Object call(); 
	}
}