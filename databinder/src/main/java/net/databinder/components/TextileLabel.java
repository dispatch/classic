package net.databinder.components;

import java.util.Locale;

import wicket.model.IModel;
import jtextile.JTextile;

/**
 * Filters its model through JTextile before rendering.
 * @see jtextile.JTextile
 * @author Nathan Hamblen
 */
public class TextileLabel extends CustomLabel {	
	/**
	 * @param id Wicket id
	 */
	public TextileLabel(String id) {
		super(id, new TextileConverter());
		setEscapeModelStrings(false); // since the contents will be in HTML
	}	
	
	/**
	 * @param Wicket id
	 * @param String model
	 */
	public TextileLabel(String id, IModel model) {
		super(id, model, new TextileConverter());
		setEscapeModelStrings(false); // since the contents will be in HTML
	}	

	/**
	 * Passes all source objects through JTextile, checks that conversion is String-String.
	 * @see jtextile.JTextile
	 */
	protected static class TextileConverter extends CustomConverter {
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
