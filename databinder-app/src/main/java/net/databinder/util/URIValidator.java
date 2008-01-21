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
import java.util.regex.Pattern;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

/**
 * Validate that the input is a particular kind of URI.
 * @author Nathan Hamblen
 */
public abstract class URIValidator extends AbstractValidator {

	@Override
	protected void onValidate(IValidatable validatable) {
		onValidate(validatable, (URI)validatable.getValue());
	}

	public abstract void onValidate(IValidatable formComponent, URI uri);

	/**
	 * Accepts only URIs having an http or https scheme.
	 * @return validator for http and https URIs.
	 */
	public static URIValidator HttpScheme() {
		return new SchemeValidator("http(s)?", "http");
	}

	/**
	 * Accepts only URIs having an ftp scheme.
	 * @return validator for ftp URIs.
	 */
	public static URIValidator FtpScheme() {
		return new SchemeValidator("ftp", "ftp");
	}

	private static class SchemeValidator extends URIValidator {
		Pattern pattern;
		String resourceKeySuffix;

		public SchemeValidator(String pattern, String resourceKeySuffix) {
			this.pattern = Pattern.compile(pattern);
			this.resourceKeySuffix = resourceKeySuffix;
		}

		@Override
		public void onValidate(IValidatable validatable, URI uri) {
			{
				if (uri != null && (uri.getScheme() == null || !pattern.matcher(uri.getScheme()).matches()))
					error(validatable);
			}
		}
		@Override
		protected String resourceKey() {
			return "URIValidator." + resourceKeySuffix;
		}
	}

}
