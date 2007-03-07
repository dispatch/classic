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

import wicket.util.convert.ConversionException;
import wicket.util.convert.converters.AbstractConverter;

/**
 * Convert color stirng to a java.awt.Color using its static decode method.
 * @author Nathan Hamblen
 * @see Color
 */
public class ColorConverter extends AbstractConverter {

	@Override
	protected Class getTargetType() {
		return Color.class;
	}

	public Object convertToObject(String obj, Locale loc) {
		try {
			return Color.decode(obj.toString());
		} catch (NumberFormatException e) {
			throw new ConversionException(e);
		}
	}
	@Override
	public String convertToString(Object value, Locale locale) {
		if (value == null) return null;
		Color c = (Color) value;
		return "#" + Integer.toHexString(c.getRGB()).substring(2);
	}
}