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

package net.databinder.conv;

import net.databinder.DataRequestCycle;
import net.databinder.DataStaticService;
import net.databinder.conv.components.IConversationPage;

import org.hibernate.FlushMode;
import org.hibernate.classic.Session;
import org.hibernate.context.ManagedSessionContext;

import wicket.Page;
import wicket.Response;
import wicket.WicketRuntimeException;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebSession;

/**
 * Supports extended Hibernate sessions for long conversations. This is useful for a page or
 * a series of pages where changes are made to an entity that can not be immediately
 * committed. Using a "conversation" session, HibernateObjectModels are used normally, but
 * until the session is flushed the changes are not made to persistent storage.   
 * @author Nathan Hamblen
 */
public class DataConversationRequestCycle extends DataRequestCycle {
	public DataConversationRequestCycle(final WebSession session, final WebRequest request, final Response response) {
		super(session, request, response);
	}
	
	/**
	 * Does nothing; The session is open or retreived only when the request target is known.
	 */
	@Override
	protected void onBeginRequest() {
	}
	
	/** Called by DataStaticService when a session is needed. */
	public void openHibernateSessionForPage() {
		Page page = getResponsePage();
		if (page == null)
			page = getRequest().getPage();
		if (page == null)
			throw new WicketRuntimeException("Tried to load object before before response page is available");

		// if continuing a conversation page
		if (page instanceof  IConversationPage) {
			// look for existing session
			org.hibernate.classic.Session sess = ((IConversationPage)page).getConversationSession();

			// if usable session exists, bind and return
			if (sess != null && sess.isOpen()) {
					sess.beginTransaction();
					ManagedSessionContext.bind(sess);
					return;
			}
			// else start new one and set in page
			sess = openHibernateSession();
			sess.setFlushMode(FlushMode.MANUAL);
			((IConversationPage)page).setConversationSession(sess);
			return;
		}
		// start new standard session
		openHibernateSession();
	}
	
	/**
	 * Inspects responding page to determine if current Hibernate session should be closed
	 * or left open and stored in the page.
	 */
	@Override
	protected void onEndRequest() {
		if (!ManagedSessionContext.hasBind(DataStaticService.getHibernateSessionFactory()))
			return;
		org.hibernate.classic.Session sess = DataStaticService.getHibernateSession();
		boolean transactionComitted = false;
		if (sess.getTransaction().isActive())
			sess.getTransaction().rollback();
		else
			transactionComitted = true;
		
		Page page = getResponsePage() ;
		
		if (page != null) {
			// check for current conversational session
			if (page instanceof IConversationPage) {
				IConversationPage convPage = (IConversationPage)page;
				// close if not dirty contains no changes
				if (transactionComitted && !sess.isDirty()) {
					sess.close();
					sess = null;
				}
				convPage.setConversationSession(sess);
			} else
				sess.close();
		}		
		ManagedSessionContext.unbind(DataStaticService.getHibernateSessionFactory());
	}

	/** 
	 * Closes and reopens Hibernate session for this Web session. Unrelated models may try to load 
	 * themselves after this point. 
	 */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e) {
		Session sess = DataStaticService.getHibernateSession();
		sess.getTransaction().rollback();
		sess.close();
		openHibernateSession();
		return null;
	}

}
