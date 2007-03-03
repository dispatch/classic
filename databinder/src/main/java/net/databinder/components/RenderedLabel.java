package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import wicket.Component;
import wicket.Resource;
import wicket.ResourceReference;
import wicket.SharedResources;
import wicket.WicketRuntimeException;
import wicket.markup.ComponentTag;
import wicket.markup.html.image.Image;
import wicket.markup.html.image.resource.RenderedDynamicImageResource;
import wicket.model.ICompoundModel;
import wicket.model.IModel;
import wicket.util.string.Strings;

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

/**
 * Renders its model text into a PNG, using any typeface available to the JVM. The size of 
 * the image is determined by the model text and the characteristics of font selected. The
 * default font is 14pt sans, plain black on a white background. The background may be set
 * to null for alpha transparency, which will appear gray in outdated browsers. The image's 
 * alt attribute will be set to the model text, and width and height attributes will be 
 * set appropriately. 
 * <p> If told to use a shared image resource, RenderedLabel will add its image
 * to the application's shared resources and reference it from a permanent, unique, 
 * browser-chacheable URL. Note that if users might request a shared resource before a
 * page containing it has rendered (after a context reload, for example) you should load
 * that resource using loadSharedResources() as the application is starting up.
 * <p> This class is inspired by, and draws code from, Wicket's DefaultButtonImageResource. </p>
 * @author Nathan Hamblen
 * @see wicket.markup.html.image.resource.DefaultButtonImageResource
 * @see SharedResources
 */
public class RenderedLabel extends Image  {
	private static final long serialVersionUID = 1L;

	private static Font defaultFont =  new Font("sans", Font.PLAIN, 14);
	private static Color defaultColor = Color.BLACK;
	private static Color defaultBackgroundColor = Color.WHITE;
	
	private Font font = defaultFont;
	private Color color = defaultColor;
	private Color backgroundColor = defaultBackgroundColor;
	private Integer maxWidth;
	
	/** If true, resource is shared across application with a permanent URL. */
	private boolean isShared = false;
	/** Hash of the most recently displayed label attributes. -1 is initial value, 0 for blank labels. */
	private int labelHash = -1;
	
	private RenderedTextImageResource resource;
	
	/**
	 * Constructor to be used if model is derived from a compound property model.
	 * @param id Wicket id
	 */
	public RenderedLabel(String id) {
		super(id);
		init();
	}
	
	/**
	 * Constructor for compound property model and shared resource pool.
	 * @param id Wicket id
	 * @param shareResource true to add to shared resource pool
	 */
	public RenderedLabel(String id, boolean shareResource) {
		this(id);
		this.isShared = shareResource;
		init();
	}
	
	/**
	 * Constructor with explicit model.
	 * @param id Wicket id
	 * @param model model for 
	 */
	public RenderedLabel(String id, IModel model) {
		super(id, model);
		init();
	}

	/**
	 * Constructor with explicit model.
	 * @param id Wicket id
	 * @param model model for 
	 * @param shareResource true to add to shared resource pool
	 */
	public RenderedLabel(String id, IModel model, boolean shareResource) {
		this(id, model);
		this.isShared = shareResource;
		init();
	}
	
	/** Perform generic initialization. */
	protected void init() {
		setEscapeModelStrings(false);
	}
	
	@Override
	protected void onBeforeRender() {
		int curHash = getLabelHash();
		if (isShared) {
			if (labelHash != curHash) {
				String hash = Integer.toHexString(curHash);
				SharedResources shared = getApplication().getSharedResources();
				try { resource = (RenderedTextImageResource) shared.get(RenderedLabel.class, hash, null, null, false); }
				catch (ClassCastException e) {
					 // was placeholder for missing PackageResourceReference
					shared.remove(shared.resourceKey(RenderedLabel.class, hash, null, null));
				}
				if (resource == null)
					shared.add(RenderedLabel.class, hash, null, null, 
							resource = newRenderedTextImageResource(true));
				setImageResourceReference(new ResourceReference(RenderedLabel.class, hash));
			}
		} else {
			if (resource == null)
				setImageResource(resource = newRenderedTextImageResource(false));
			else if (labelHash != curHash)
				resource.setState(this);
		}
		resource.setCacheable(isShared);
		labelHash = getLabelHash();
	}
	
	/**
	 * @return false if model string is empty.
	 */
	@Override
	public boolean isVisible() {
		return getText() != null;
	}

	/** 
	 * Adds image-specific attributes including width, height, and alternate text. A hash is appended
	 * to the source URL to trigger a reload whenever drawing attributes change. 
	 */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);

		if (!isShared) {
			String url = tag.getAttributes().getString("src");
			url = url + ((url.indexOf("?") >= 0) ? "&" : "?");
			url = url + "wicket:antiCache=" + Integer.toHexString(labelHash);

			tag.put("src", url);
		}
		resource.preload();

		tag.put("width", resource.getWidth() );
		tag.put("height", resource.getHeight() );

		tag.put("alt", getText());
	}

	protected int getLabelHash() {
		return getLabelHash(getText(), font, color, backgroundColor, maxWidth);
	}

	protected static int getLabelHash(String text, Font font, Color color, Color backgroundColor, Integer maxWidth) {
		if (text == null) return 0;
		
		int hash= text.hashCode() ^ font.hashCode() ^ color.hashCode();
		if (backgroundColor != null)
			hash ^= backgroundColor.hashCode();
		if (maxWidth != null)
			hash ^= maxWidth.hashCode();
		return hash;
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
	 * Load shared resource into pool so it will be available even before a page using the
	 * rendered label is first rendered. May be needed if a page is cachable and the context 
	 * is restarted, for example.
	 * @param text
	 * @param font uses default if null
	 * @param color uses default if null
	 * @param backgroundColor uses default if null
	 * @param maxWidth
	 */
	public static void loadSharedResources(String text, Font font, Color color, Color backgroundColor, Integer maxWidth) {
		loadSharedResources(new RenderedTextImageResource(), text, font, color, backgroundColor, maxWidth);
	}
	
	/**
	 * Utility method to load a specific instance of a the rendering shared resource.
	 */
	protected static void loadSharedResources(RenderedTextImageResource res, String text, Font font, Color color, Color backgroundColor, Integer maxWidth) {
		res.setCacheable(true);
		res.backgroundColor = backgroundColor;
		res.color = color;
		res.font = font;
		res.maxWidth = maxWidth;
		res.renderedText = text;

		if (font == null) font = defaultFont;
		if (color == null) color = defaultColor;
		if (backgroundColor == null) backgroundColor = defaultBackgroundColor;
		
		String hash = Integer.toHexString(getLabelHash(text, font, color, backgroundColor, maxWidth));
		SharedResources shared = wicket.Application.get().getSharedResources();

		shared.add(RenderedLabel.class, hash, null, null, res);
	}	

	/**
	 * Create a new image resource to render this label. Override in a subclass to use a different
	 * renderer.
	 * @param isShared is a shared, cacheable resource
	 * @return new instance of RenderedTextImageResource or subclass
	 */
	protected RenderedTextImageResource newRenderedTextImageResource(boolean isShared) {
		RenderedTextImageResource res = new RenderedTextImageResource();
		res.setCacheable(isShared);
		res.setState(this);
		return res;
	}
	
	/**
	 * Inner class that renders the model text into an image  resource.
	 * @see wicket.markup.html.image.resource.DefaultButtonImageResource
	 */
	protected static class RenderedTextImageResource extends RenderedDynamicImageResource
	{
		protected Color backgroundColor;
		protected Color color;
		protected Font font;
		protected Integer maxWidth;
		protected String renderedText;

		protected RenderedTextImageResource() {
			super(1, 1,"png");	// tiny default that will resize to fit text
			setType(BufferedImage.TYPE_INT_ARGB); // allow alpha transparency
		}
		
		protected void setState(RenderedLabel label) {
			backgroundColor = label.getBackgroundColor();
			color = label.getColor();
			font = label.getFont();
			maxWidth = label.getMaxWidth();
			renderedText = label.getText();
			invalidate();
		}
		
		/** Renders text into image. */
		protected boolean render(final Graphics2D graphics)
		{
			final int width = getWidth(), height = getHeight();

			// draw background if not null, otherwise leave transparent
			if (backgroundColor != null) {
				graphics.setColor(backgroundColor);
				graphics.fillRect(0, 0, width, height);
			}

			// render as a 1x1 pixel if text is empty
			if (renderedText == null) {
				if (width == 1 && height == 1)
					return true;
				setWidth(1);
				setHeight(1);
				return false;
			}
			
			// Get size of text
			graphics.setFont(font);
			final FontMetrics metrics = graphics.getFontMetrics();
			
			List<String> lines = new LinkedList<String>();
			
			int dxText = breakLines(renderedText, metrics, lines),
				lineHeight = metrics.getHeight(),
				dyText = lineHeight * lines.size();
			
			// resize and redraw if we need to
			if (dxText !=  width || dyText != height)
			{
				setWidth(dxText);
				setHeight(dyText);
				return false;
			}
			
			// Turn on anti-aliasing
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			graphics.setColor(color);
			
			// Draw each line at its baseline
			int baseline = metrics.getAscent();
			for (String line : lines) {
				graphics.drawString(line, 0, baseline);
				baseline += lineHeight;
			}
			return true;
		}

		/**
		 * Breaks source string into lines no longer than this label's maxWidth, if not null. 
		 * @param source this label's model, previously retrieved
		 * @param metrics metrics for the font we will use for display
		 * @param outputLines list to receive lines generated by this function
		 * @return length in pixels of the longest line
		 */
		protected int breakLines(String source, FontMetrics metrics, List<String> outputLines) {
			if (maxWidth == null) {
				outputLines.add(source);
				return metrics.stringWidth(source);
			}
			String sp = " ";
			String words[] = source.split(sp);
			StringBuilder line = new StringBuilder();
			int topWidth = 0;
			for (String word : words) {
				if (line.length() >0) {
					 int curWidth = metrics.stringWidth(line + sp + word);
					 if (curWidth > maxWidth) {
						 outputLines.add(line.toString());
						 line.setLength(0);
					 } else
						 line.append(sp);
				}
				 line.append(word);
				 topWidth = Math.max(metrics.stringWidth(line.toString()), topWidth);
			}
			outputLines.add(line.toString());
			return topWidth;
		}
		
		/** 
		 * Normally, image rendering is deferred until the resource is requested, but
		 * this method allows us to render the image when its markup is rendered. This way
		 * the model will not need to be reattached when we serve the image, and we can
		 * use the size information in the IMG tag.
		 */
		public void preload() {
			getImageData();
		}
	}
	
	/** @return String to be rendered */
	protected String getText() {
		String text = getModelObjectAsString();
		return Strings.isEmpty(text) ? null : text;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	/**
	 * Specify a background color to match the page. Specify null for a transparent background blended
	 * with the alpha channel, causing IE6 to display a gray background.
	 * @param backgroundColor color or null for transparent
	 * @return this for chaining
	 */
	public RenderedLabel setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public Color getColor() {
		return color;
	}

	/** @param color Color to print text */
	public RenderedLabel setColor(Color color) {
		this.color = color;
		return this;
	}

	public Font getFont() {
		return font;
	}

	public RenderedLabel setFont(Font font) {
		this.font = font;
		return this;
	}
	

	public Integer getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Specify a maximum pixel width, causing longer renderings to wrap.
	 * @param maxWidth maximum width in pixels
	 * @return this, for chaining
	 */
	public RenderedLabel setMaxWidth(Integer maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}

	/**
	 * Utility method for creating Font objects from resources.
	 * @param fontRes Resource containing  a TrueType font descriptor.
	 * @return Plain, 16pt font derived from the resource.
	 */
	public static Font fontForResource(Resource fontRes) {
		try {
			InputStream is = fontRes.getResourceStream().getInputStream();
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			is.close();
			return font.deriveFont(Font.PLAIN, 16);
		} catch (Throwable e) {
			throw new WicketRuntimeException("Error loading font resources", e);
		}
	}
}
