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
package net.databinder.web;

import javax.servlet.http.HttpServletResponse;

import net.databinder.DataApplication;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.BufferedWebResponse;
import org.apache.wicket.protocol.http.WebResponse;

/**
 * Creates web response objects that do not rewrite URLs for cookieless support. Buffered or
 * basic responses are created according to the application configuration. This factory is
 * used by DataApplication when cookieless support is off, but may also be used independently.
 * @see DataApplication
 * @author Nathan Hamblen
 */
public class NorewriteWebResponse {

	public static WebResponse getNew(Application app, final HttpServletResponse servletResponse) {
		return app.getRequestCycleSettings().getBufferResponse() ?
				new Buffered(servletResponse) : new Unbuffered(servletResponse);
	}
	
	static class Buffered extends BufferedWebResponse {
		public Buffered(final HttpServletResponse httpServletResponse)
		{ 
			super(httpServletResponse); 
		}
		@Override
		public CharSequence encodeURL(CharSequence url) {
			return url;
		}
	}

	static class Unbuffered extends WebResponse {
		public Unbuffered(final HttpServletResponse httpServletResponse)
		{ 
			super(httpServletResponse); 
		}
		@Override
		public CharSequence encodeURL(CharSequence url) {
			return url;
		}
	}
}
