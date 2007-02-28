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

import java.awt.Color;
import java.util.Locale;

import wicket.util.convert.converters.AbstractConverter;

/**
 * Convert a color object to a #xxxxxx string value.
 * @author Nathan Hamblen
 */
public class ColorToStringConverter extends AbstractConverter {

	@Override
	protected Class getTargetType() {
		return String.class;
	}

	public Object convert(Object value, Locale locale) {
		if (value == null) return null;
		Color c = (Color) value;
		return "#" + Integer.toHexString(c.getRGB()).substring(2);
	}

}
