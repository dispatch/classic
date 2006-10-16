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

/*
 * Note: this class contains code adapted from wicket-contrib-database. 
 */

package net.databinder;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import wicket.Page;
import wicket.RequestCycle;
import wicket.Response;
import wicket.WicketRuntimeException;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebRequestCycle;
import wicket.protocol.http.WebResponse;
import wicket.protocol.http.WebSession;

/**
 * <p>Opens Hibernate sessions as required and closes (but does not flush) them at a request's
 * end. Creates a Hibernate session factory in a static initialization block, configuring it with
 * annotatied classes in a DataApplication subclass.</p>
 * <p>If you want to use a custom session factory, you will need to override initHibernate()
 * and openSession(). Both of these refer to this class's private static session factory, which
 * would remain null in such a configuration. </p>
 * @see DataApplication.initDataRequestCycle()
 * @author Nathan Hamblen
 */
public class DataRequestCycle extends WebRequestCycle {
	private Session hibernateSession;
	/** cache of cookies from request */ 
	private Map<String, Cookie> cookies;


	public DataRequestCycle(final WebSession session, final WebRequest request, final Response response) {
		super(session, request, response);
	}

	/**
	 * Will open a session if one is not already open for this request.
	 * @return the open Hibernate session for the current request cycle.
	 */
	public static Session getHibernateSession() {
		RequestCycle cycle = get();
		if (!(cycle instanceof DataRequestCycle))
			throw new WicketRuntimeException("Current request cycle not managed by Databinder. " +
					"Your application session factory must return a DataSession or some other session " +
			"that produces DataRequestCycle.");
		return ((DataRequestCycle)cycle).getCycleHibernateSession();
	}

	/**
	 * Opens a session and a transaction if a session is not already associated with
	 * this request cycle.
	 * @return the open Hibernate session for this request cycle.
	 */
	protected Session getCycleHibernateSession() {
		if(hibernateSession == null) {
			hibernateSession = openSession();
			hibernateSession.beginTransaction();
		}
		return hibernateSession;
	}

	/**
	 * @return a newly opened session
	 */
	protected Session openSession()
	throws HibernateException {
		return DataStaticService.getHibernateSessionFactory().openSession();
	}

	/** Roll back active transactions and close session. */
	protected void closeSession() {
		if (hibernateSession != null) {
			try {
				if (hibernateSession.getTransaction().isActive())
					hibernateSession.getTransaction().rollback();
			} finally {
				try {
					hibernateSession.close();
				} finally {
					hibernateSession = null;
				}
			}
		}
	}

	/**
	 * Closes the Hibernate session, if one was open for this request. If a transaction has
	 * not been committed, it will be rolled back before cloing the session.
	 * @see net.databinder.components.DataForm#onSubmit()
	 */
	@Override
	protected void onEndRequest() {
		closeSession();
	}

	/** Roll back active transactions and close session. */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e) {
		closeSession();	// close session; another one will open if models load themselves
		return null;
	}

	/**
	 * Return or build cache of cookies cookies from request.
	 */
	protected Map<String, Cookie> getCookies() {
		if (cookies == null) {
			Cookie ary[] = ((WebRequest)getRequest()).getCookies();
			cookies = new HashMap<String, Cookie>(ary == null ? 0 : ary.length);
			if (ary != null)
				for (Cookie c : ary)
					cookies.put(c.getName(), c);
		}
		return cookies;
	}

	/**
	 * Retrieve cookie from request, so long as it hasn't been cleared. Cookies  cleared by
	 * clearCookie() are still contained in the current request's cookie array, but this method
	 * will not return them.
	 * @param name cookie name
	 * @return cookie requested, or null if unavailable
	 */
	public Cookie getCookie(String name) {
		return getCookies().get(name);
	}

	/**
	 * Sets a new a cookie with an expiration time of zero to an clear an old one from the 
	 * browser, and removes any copy from this request's cookie cache. Subsequent calls to 
	 * <tt>getCookie(String name)</tt> during this request will not return a cookie of that name. 
	 * @param name cookie name
	 */
	public void clearCookie(String name) {
		getCookies().remove(name);
		((WebResponse)getResponse()).clearCookie(new Cookie(name, ""));
	}
}
