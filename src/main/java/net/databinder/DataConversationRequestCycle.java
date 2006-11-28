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

import org.hibernate.Session;
import org.hibernate.context.ManagedSessionContext;

import wicket.Page;
import wicket.Response;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebSession;

public class DataConversationRequestCycle extends DataRequestCycle {

	public DataConversationRequestCycle(final WebSession session, final WebRequest request, final Response response) {
		super(session, request, response);
	}

	@Override
	protected void onBeginRequest() {
		DataConversationSession webSession = (DataConversationSession)getSession();
		
		org.hibernate.classic.Session sess = webSession.getConversataionSession();
		if (sess == null)
			sess = webSession.openConversationSession();
		
		sess.beginTransaction();
		ManagedSessionContext.bind(sess);
	}

	@Override
	protected void onEndRequest() {
		Session sess = DataStaticService.getHibernateSession();
		if (sess.getTransaction().isActive())
			sess.getTransaction().rollback();
		ManagedSessionContext.unbind(DataStaticService.getHibernateSessionFactory());
	}
	
	@Override
	protected void closeSession() {
		((DataConversationSession)getSession()).closeConversationSession();
	}

	/** Roll back active transactions and close session. */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e) {
		closeSession();	// close session; another one will open if models load themselves
		return null;
	}

}
