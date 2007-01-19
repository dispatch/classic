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

import net.databinder.conv.DataConversationRequestCycle;

import org.hibernate.SessionFactory;
import org.hibernate.context.ManagedSessionContext;

import wicket.RequestCycle;
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
	
	/**
	 * @return Hibernate session bound to current thread
	 */
	public static org.hibernate.classic.Session getHibernateSession() {
		checkConversationalSession();
		return getHibernateSessionFactory().getCurrentSession();
	}
	
	/**
	 * Checks if current request cycle is for conversational sessions and notifies it that a
	 * session is requested if appropriate.
	 */
	protected static void checkConversationalSession() {
		if (!ManagedSessionContext.hasBind(DataStaticService.getHibernateSessionFactory())) {
			// if session is unavailable, it could be a late-loaded conversational cycle
			RequestCycle cycle = RequestCycle.get();
			if (cycle instanceof DataConversationRequestCycle)
				((DataConversationRequestCycle)cycle).openHibernateSessionForPage();
		}
	}

	
	/**
	 * @param sessionFactory to use for this application
	 */
	public static void setSessionFactory(SessionFactory sessionFactory) {
		hibernateSessionFactory = sessionFactory;
	}
	
	/**
	 * Wraps callback in new a Hibernate session and transaction that are closed after the callback
	 * returns. This is to be used when a thread-bound session may not be available, such as
	 * application init or an external Web service request. (If a thread-bound session is found,
	 * it is used to perform the callback.) Uncommited transactions begun by this method 
	 * are rolled back, as with DataRequestCycle. Be careful of returning detached Hibernate 
	 * objects that may not be fully loaded with data; consider using projections / scalar
	 * queries instead.
	 * @param callback
	 */
	public static Object wrapInHibernateSession(Callback callback) {
		checkConversationalSession();
		SessionFactory sf = getHibernateSessionFactory();
		if (ManagedSessionContext.hasBind(hibernateSessionFactory))
			return callback.call();
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