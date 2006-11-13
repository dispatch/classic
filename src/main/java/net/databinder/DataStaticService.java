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
	 * @param sessionFactory to use for this application
	 */
	public static void setSessionFactory(SessionFactory sessionFactory) {
		hibernateSessionFactory = sessionFactory;
	}
}
