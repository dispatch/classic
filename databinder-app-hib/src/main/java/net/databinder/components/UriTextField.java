/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2007  Nathan Hamblen nathan@technically.us
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
package net.databinder.components;

import java.util.Locale;

import net.databinder.util.URIConverter;
import net.databinder.util.URIValidator;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;

/**
 * TextField for URIs with a default scheme. This field establishes a default URI scheme
 * and equates it to a null URI. For the UriTextField.Http subclass, the field is initialized with 
 * "http://" and if unchanged the model will be left null. 
 * @author Nathan Hamblen
 */
public class UriTextField extends TextField {
	
	private String scheme;
	
	/** Costructor called by nested subclasses. */
	private UriTextField(String id, String scheme) {
		super(id, java.net.URI.class);
		this.scheme = scheme;
	}
	
	private String defaultValue() { return  scheme + "://"; }
	
	/**
	 * Return default value when null. Note that converter is not called from base if null.
	 */
	@Override
	protected String getModelValue() {
		String value = super.getModelValue();
		if (Strings.isEmpty(value))
			return defaultValue();
		return value;
	}
	
	/** @return specialized converter that equates a default string value to null */
	@Override
	public IConverter getConverter(Class type) {
		return new URIConverter() {
			@Override
			public Object convertToObject(String value, Locale locale) {
				if (value == null || value.equals(defaultValue()))
						return null;
				return super.convertToObject(value, locale);
			}
		};
	}
	
	public static class Http extends UriTextField {
		public Http(String id) {
			super(id, "http");
			add(URIValidator.HttpScheme());
		}
	}
	
	public static class Ftp extends UriTextField {
		public Ftp(String id) {
			super(id, "ftp");
			add(URIValidator.FtpScheme());
		}
	}
}
