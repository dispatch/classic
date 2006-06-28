package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import wicket.Component;
import wicket.WicketRuntimeException;
import wicket.markup.ComponentTag;
import wicket.markup.html.image.Image;
import wicket.markup.html.image.resource.RenderedDynamicImageResource;
import wicket.model.ICompoundModel;
import wicket.model.IModel;

/**
 * Renders its model text into a PNG, using any typeface available to the JVM. The size of 
 * the image is determined by the model text and the characteristics of font selected. The
 * default font is 14pt sans, plain black on a white background. The image's alt attribute
 * will be set to the model text. <p>This class is inspired by, and draws code from, Wicket's 
 * DefaultButtonImageResource. </p>
 * @author Nathan Hamblen
 * @see wicket.markup.html.image.resource.DefaultButtonImageResource
 */
public class RenderedLabel extends Image {

	private Color backgroundColor = Color.WHITE;
	private Color color = Color.BLACK;
	private Font font = new Font("sans", Font.PLAIN, 14);
	
	private RenderedTextImageResource resource;
	
	/**
	 * Constructor to be used if model is derived from a compound property model. The 
	 * model object <b>must</b> be a string.
	 * @param id
	 */
	public RenderedLabel(String id) {
		super(id);
		setImageResource(resource = new RenderedTextImageResource());
	}
	
	/**
	 * Constructor with explicit model. The model object <b>must</b> be a string.
	 * @param id Wicket id
	 * @param model model for 
	 */
	public RenderedLabel(String id, IModel model) {
		super(id, model);
		setImageResource(resource = new RenderedTextImageResource());
	}
	
	/** Adds alt attribute for accessibility. */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		tag.put("alt", getText());
	}
	
	/** Restores  compound model resolution that is disabled in  the Image superclass. */
	@Override
	protected IModel initModel() {
		// c&p'd from Component 
		for (Component current = getParent(); current != null; current = current.getParent())
		{
			final IModel model = current.getModel();
			if (model instanceof ICompoundModel)
			{
				setVersioned(false);
				return model;
			}
		}
		return null;
	}
	
	/**
	 * Inner class that renders the model text into an image  resource.
	 * @see wicket.markup.html.image.resource.DefaultButtonImageResource
	 */
	protected class RenderedTextImageResource extends RenderedDynamicImageResource
	{
		public RenderedTextImageResource()
		{
			super(10, 10,"png");	// tiny default that will resize to fit text
		}
		
		/** Renders text into image. */
		protected boolean render(final Graphics2D graphics)
		{
			String text = getText(); // get text from outer class model
			final int width = getWidth(), height = getHeight();

			// Fill background
			graphics.setColor(backgroundColor);
			graphics.fillRect(0, 0, width, height);

			if (text == null)
				return true;	// no text? we're done here
			
			// Get size of text
			graphics.setFont(font);
			final FontMetrics fontMetrics = graphics.getFontMetrics();
			final int dxText = fontMetrics.stringWidth(text);
			final int dyText = fontMetrics.getHeight();
			
			if (dxText > width || dyText > height)
			{
				setWidth(dxText);
				setHeight(dyText);
				return false;
			}
			else
			{
				// Turn on anti-aliasing
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				
				// Draw text at baseline
				graphics.setColor(color);
				graphics.drawString(text, 0, fontMetrics.getAscent());
				return true;
			}
		}
	}

	/** @return String to be rendered */
	protected String getText() {
		try {
			return (String) getModelObject();
		} catch (ClassCastException e) {
			throw new WicketRuntimeException("A RenderedLabel's model object MUST be a String.", e);
		}
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	public RenderedLabel setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
		resource.invalidate();
		return this;
	}

	public Color getColor() {
		return color;
	}

	/** @param color Color to print text */
	public RenderedLabel setColor(Color color) {
		this.color = color;
		resource.invalidate();
		return this;
	}

	public Font getFont() {
		return font;
	}

	public RenderedLabel setFont(Font font) {
		this.font = font;
		resource.invalidate();
		return this;
	}
}