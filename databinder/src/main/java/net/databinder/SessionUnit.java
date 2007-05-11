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

/**
 * Unit of work to be used with DataStaticService.ensureSession()
 * when a session is required but might not be bound to the current thread.
 * @see DataStaticService
 * @author Nathan Hamblen
 */
public interface SessionUnit {
	/**
	 * Perform work with a thread-bound session available thorugh 
	 * DataStaticService.getHibernateSession(). Be careful of returning lazy loaded collections
	 * in objects whose sessions may have been closed.
	 * @param sess Hibernate session, for convenience
	 * @return object to be returned by DataStaticService.ensureSession()
	 * @see DataStaticService
	 */
	Object run(Session sess); 
}
