package net.databinder.components;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import wicket.model.IModel;

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

	/**
	 * @param id Wicket id
	 * @param model source model (must be a Date)
	 * @param formatString SimpleDateFormat format string
	 * @see SimpleDateFormat
	 */
	public DateLabel(String id, IModel model, String formatString) {
		super(id, model, new DateConverter(new SimpleDateFormat(formatString)));
	}

	/**
	 * Format Date as a String determined by its configuration. 
	 */
	protected static class DateConverter extends CustomConverter {
		private DateFormat df;
		public DateConverter(DateFormat df) {
			this.df = df;
		}
		public Object convert(Object source, Class cl) {
			if (source instanceof Date && cl.equals(String.class))
				try{
					return df.format((Date) source);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return new UnsupportedOperationException("Can only convert Dates to Strings");
		}
	}
}
