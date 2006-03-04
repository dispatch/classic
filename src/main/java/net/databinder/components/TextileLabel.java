package net.databinder.components;

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
	 * Passes all source objects through JTextile, checks that conversion is String-String.
	 * @see jtextile.JTextile
	 */
	protected static class TextileConverter extends CustomConverter {
		public Object convert(Object source, Class cl) {
			if (source instanceof String && cl.equals(String.class))
				try{
					return JTextile.textile((String) source);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return new UnsupportedOperationException("Can only convert Strings to Strings");
		}
	}
}
