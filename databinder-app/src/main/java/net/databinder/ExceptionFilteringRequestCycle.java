/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2008  Nathan Hamblen nathan@technically.us
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

import java.util.regex.Pattern;

import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request cycle that logs runtime exceptions as warnings if their origin matches
 * a pattern.
 * @author Nathan Hamblen
 */
public abstract class ExceptionFilteringRequestCycle extends WebRequestCycle {
	
	private static final Logger log = LoggerFactory.getLogger(ExceptionFilteringRequestCycle.class);
	/** Default pattern is ".*UrlCodingStrategy" */
	private static Pattern warnOnlySource = Pattern.compile(".*UrlCodingStrategy");
	
	public ExceptionFilteringRequestCycle(WebApplication application, WebRequest request, Response response) {
		super(application, request, response);
	}
	
	/**
	 * Logs runtime exception as warning if it matches the warnOnlySource pattern.
	 */
	@Override
	protected void logRuntimeException(RuntimeException e) {
		if (warnOnlySource.matcher(e.getStackTrace()[0].getClassName()).matches())
			log.warn(e.getMessage(), e);
		else
			super.logRuntimeException(e);
	}
	
	/**
	 * Change the warn testing pattern used by all instances of this class.
	 * @param pattern to match against fully qualified class name of exception origin
	 */
	public static void setWarnOnlySource(Pattern pattern) {
		warnOnlySource = pattern;
	}
}
