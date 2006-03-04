package net.databinder.components;

import java.util.Locale;

import wicket.markup.html.basic.Label;
import wicket.model.IModel;
import wicket.util.convert.IConverter;

/**
 * Label that alters its contents with a specific converter before display. 
 * @author Nathan Hamblen
 */
public abstract class CustomLabel extends Label {
	private CustomConverter converter;
	
	/**
	 * @param id Wicket id of component
	 * @param converter specific convert to use before display
	 */
	protected CustomLabel(String id, CustomConverter converter) {
		super(id);
		converter.setLocale(getLocale());
		this.converter = converter;
	}
	
	/**
	 * @param id Wicket id of component
	 * @param model model to be passed through converter
	 * @param converter specific convert to use before display
	 */
	protected CustomLabel(String id, IModel model, CustomConverter converter) {
		super(id, model);
		converter.setLocale(getLocale());
		this.converter = converter;
	}
	
	/**
	 * Always returns the chosen converter.
	 */
	@Override
	public final IConverter getConverter() {
		return converter;
	}
	
	/**
	 * Provides basic Locale support so those IConverter methods will not need to be
	 * overridden.
	 */
	protected abstract static class CustomConverter implements IConverter {
		private Locale locale;
		public Locale getLocale() {
			return locale;
		}
		public void setLocale(Locale locale) {
			this.locale = locale;
		}
	}
}