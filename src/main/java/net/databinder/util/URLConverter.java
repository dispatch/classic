package net.databinder.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import wicket.util.convert.ConversionException;
import wicket.util.convert.converters.AbstractConverter;

/**
 * Convert an object to a java.net.URL. Will be deprecated if a similar converter is
 * ever built into Wicket.
 * 
 * @author Nathan Hamblen
 */
public class URLConverter extends AbstractConverter {

	@Override
	protected Class getTargetType() {
		return URL.class;
	}

	public Object convert(Object obj, Locale arg1) {
		try {
			return new URL(obj.toString());
		} catch (MalformedURLException e) {
			throw new ConversionException(e);
		}
	}
}