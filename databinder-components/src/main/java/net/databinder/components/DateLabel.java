/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

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
package net.databinder.components;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.converters.AbstractConverter;

/**
 * Label with a specific date format (rather than a globally defined one). 
 * @author Nathan Hamblen
 */
public class DateLabel extends CustomLabel {
	/**
	 * @param id Wicket id (must map to a Date property)
	 * @param formatString SimpleDateFormat format string
	 * @see SimpleDateFormat
	 */
	public DateLabel(String id, String formatString) {
		super(id, new DateConverter(new SimpleDateFormat(formatString)));
	}

	public DateLabel(String id, DateFormat format) {
		super(id, new DateConverter(format));
	}

	/**
	 * @param id Wicket id
	 * @param model source model (must be a Date)
	 * @param formatString SimpleDateFormat format string
	 * @see SimpleDateFormat
	 */
	public DateLabel(String id, IModel model, String formatString) {
		super(id, model, new DateConverter(new SimpleDateFormat(formatString)));
	}

	public DateLabel(String id, IModel model, DateFormat dateFormat) {
		super(id, model, new DateConverter(dateFormat));
		
	}
	/**
	 * Format Date as a String determined by its configuration. 
	 */
	protected static class DateConverter extends AbstractConverter {
		private DateFormat df;
		public DateConverter(DateFormat df) {
			this.df = df;
		}
		public Object convertToObject(final String value, Locale locale)
		{
			return parse(df, value, locale);
		}
		@Override
		protected Class getTargetType() {
			return Date.class;
		}
		public String convertToString(Object source, Locale locale) {
			if (source instanceof Date)
				try{
					return df.format((Date) source);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				throw new UnsupportedOperationException("Can only convert Dates to Strings");
		}
	}
}
