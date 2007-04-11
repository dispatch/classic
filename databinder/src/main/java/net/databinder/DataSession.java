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
import org.apache.wicket.IRequestCycleFactory;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;

/**
 * WebSession subclass whose request cycle factory instantiates DataRequestCycle
 * objects, or if requested, DataConversationRequestCycle objects.
 *
 *@see DataRequestCycle
 *@see DataConversationRequestCycle
 * @author Nathan Hamblen
 * @author Timothy Bennett
 */
public class DataSession extends WebSession {
	private boolean supportConversationSession = false;

	/**
	 * Constructor for standard Hibernate request cycle binding.
	 * @param application the application this applies to
	 */
	public DataSession(WebApplication application, Request request) {
		super(application, request);
	}

	/**
	 * Constructor for conversational Hibernate request cycle binding.
	 * @param application the application this applies to
	 * @param supportConversationSession if true, enable conversational binding
	 */
	public DataSession(WebApplication application, Request request, boolean supportConversationSession) {
		super(application, request);
		this.supportConversationSession = supportConversationSession;
	}

	/**
	 * Factory for DataRequestCycle or DataConversationRequestCycle objects, depending
	 * on how this DataSession was constructed.
	 */
	@Override
	public IRequestCycleFactory getRequestCycleFactory() {
		return new IRequestCycleFactory() {
			public RequestCycle newRequestCycle(Session session, Request request, Response response) {
				if (supportConversationSession)
				    return new DataConversationRequestCycle((WebSession)session, (WebRequest)request, (WebResponse)response);
				else
					return new DataRequestCycle((WebSession)session, (WebRequest)request, (WebResponse)response);
			};
		};
	}
}
