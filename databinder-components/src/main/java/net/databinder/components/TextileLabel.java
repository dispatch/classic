package net.databinder.components;

import java.util.Locale;

import jtextile.JTextile;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converters.AbstractConverter;

/**
 * Filters its model through JTextile before rendering.
 * @see jtextile.JTextile
 * @author Nathan Hamblen
 */
public class TextileLabel extends Label {	
	/**
	 * @param id Wicket id
	 */
	public TextileLabel(String id) {
		super(id);
		setEscapeModelStrings(false); // since the contents will be in HTML
	}	
	
	/**
	 * @param id Wicket id
	 * @param model String model
	 */
	public TextileLabel(String id, IModel model) {
		super(id, model);
		setEscapeModelStrings(false); // since the contents will be in HTML
	}
	
	@Override
	public IConverter getConverter(Class type) {
		return new TextileConverter();
	}

	/**
	 * Passes all source objects through JTextile, checks that conversion is String-String.
	 * @see jtextile.JTextile
	 */
	protected static class TextileConverter extends AbstractConverter {
		@Override
		protected Class getTargetType() {
			return String.class;
		}
		@Override
		public String convertToString(Object source, Locale locale) {
			if (source instanceof String)
				try{
					return JTextile.textile((String) source);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				throw new UnsupportedOperationException("Can only convert Strings to Strings");
		}
		public Object convertToObject(String value, Locale locale) {
			return null;
		}
	}
}
