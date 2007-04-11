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

package net.databinder.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.converters.AbstractConverter;

/**
 * Convert an object to a java.net.URI.
 * @author Nathan Hamblen
 */
public class URIConverter extends AbstractConverter {

	@Override
	protected Class getTargetType() {
		return URI.class;
	}
	public Object convertToObject(String value, Locale locale) {
		try {
			return new URI(value);
		} catch (URISyntaxException e) {
			throw new ConversionException(e);
		}
	}
}