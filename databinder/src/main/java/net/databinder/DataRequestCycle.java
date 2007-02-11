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

import org.hibernate.Session;
import org.hibernate.context.ManagedSessionContext;

import wicket.Page;
import wicket.Response;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebRequestCycle;
import wicket.protocol.http.WebSession;

/**
 * <p>Opens Hibernate sessions and transactions as required and closes them at a request's
 * end. Uncomitted transactions are rolled back. Uses Hibernate session factory from
 * DataStaticService..</p>
 * @see DataStaticService
 * @author Nathan Hamblen
 */
public class DataRequestCycle extends WebRequestCycle {
	/** cache of cookies from request */ 
	private Map<String, Cookie> cookies;


	public DataRequestCycle(final WebSession session, final WebRequest request, final Response response) {
		super(session, request, response);
	}

	/**
	 * Will open a session if one is not already open for this request.
	 * @return the open Hibernate session for the current request cycle.
	 * @deprecated
	 */
	public static Session getHibernateSession() {
		return DataStaticService.getHibernateSession();
	}
	
	/** Roll back active transactions and close session. */
	protected void closeSession() {
		Session sess = DataStaticService.getHibernateSession();
		
		if (sess.isOpen())
			try {
				if (sess.getTransaction().isActive())
					sess.getTransaction().rollback();
			} finally {
				sess.close();
			}
	}

	@Override
	protected void onBeginRequest() {
		openHibernateSession();
	}
	
	protected org.hibernate.classic.Session openHibernateSession() {
		org.hibernate.classic.Session sess = DataStaticService.getHibernateSessionFactory().openSession();
		sess.beginTransaction();
		ManagedSessionContext.bind(sess);
		return sess;
	}

	/**
	 * Closes the Hibernate session, if one was open for this request. If a transaction has
	 * not been committed, it will be rolled back before closing the session.
	 * @see net.databinder.components.DataForm#onSubmit()
	 */
	@Override
	protected void onEndRequest() {
		if (!ManagedSessionContext.hasBind(DataStaticService.getHibernateSessionFactory()))
			return;
		closeSession();
		ManagedSessionContext.unbind(DataStaticService.getHibernateSessionFactory());
	}

	/** 
	 * Closes and reopens session for this request cycle. Unrelated models may try to load 
	 * themselves after this point. 
	 */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e) {
		onEndRequest();
		onBeginRequest();
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
		Cookie empty = new Cookie(name, "");
		empty.setPath("/");
		getWebResponse().clearCookie(empty);
	}
}
