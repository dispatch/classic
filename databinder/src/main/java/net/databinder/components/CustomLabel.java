package net.databinder.components;

import wicket.markup.html.basic.Label;
import wicket.model.IModel;
import wicket.util.convert.IConverter;
import wicket.util.convert.converters.AbstractConverter;

/**
 * Label that alters its contents with a specific converter before display. 
 * @author Nathan Hamblen
 */
public abstract class CustomLabel extends Label {
	private IConverter converter;
	
	/**
	 * @param id Wicket id of component
	 * @param converter specific converter to use before display
	 */
	protected CustomLabel(String id, IConverter converter) {
		super(id);
		this.converter = converter;
	}
	
	/**
	 * @param id Wicket id of component
	 * @param model model to be passed through converter
	 * @param converter specific converter to use before display
	 */
	protected CustomLabel(String id, IModel model, IConverter converter) {
		super(id, model);
		this.converter = converter;
	}
	
	/**
	 * Always returns the chosen converter.
	 */
	@Override
	public IConverter getConverter(Class type) {
		return converter;
	}
	
	/**
	 * Please override AbstractConverter directly.
	 * @deprecated
	 */
	protected abstract static class CustomConverter extends AbstractConverter {
	}
}