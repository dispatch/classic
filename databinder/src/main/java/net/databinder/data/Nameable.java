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
package net.databinder.data;

import java.util.regex.Pattern;

/**
 * Foundation for objects that generate names based on string properties.
 * @author Nathan Hamblen
 */
public class Nameable {
	protected static Pattern spaces = Pattern.compile("\\s+");
	protected static Pattern nonWords = Pattern.compile("[^-\\w]");

	/**
	 * Converts string to lowercase with spaces as hyphens and other non-word charters
	 * removed.
	 * @return string in all lowercase with no whitespace or other non-word characters
	 */
	public static String stringIdFor(String string) {
		if (string == null) return null;
		return nonWords.matcher(spaces.matcher(string.toLowerCase()).replaceAll("-")).replaceAll("");
	}
}
