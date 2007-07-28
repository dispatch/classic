package net.databinder.dispatch.components;

import org.apache.wicket.model.IModel;


/**
 * XmlRpcLabel Factory that my be helpful when model text format is determined at runtime. 
 * @author Nathan Hamblen
 */
public class TextFormattedLabel {
	public static enum TextFormat { 
		/** creates RedClothLabel */
		Textile, 
		/** creates MarukuLabel */
		Markdown, 
		/** creates RubyPantsLabel */
		Smartypants 
	}
	public static XmlRpcLabel create(String id, TextFormat format) {
		return create(id, null, format);
	}
	public static XmlRpcLabel create(String id, IModel model, TextFormat format) {
		switch (format) {
		case Textile: return new RedClothLabel(id, model);
		case Markdown: return new MarukuLabel(id, model);
		case Smartypants: return new RubyPantsLabel(id, model);
		}
		throw new UnsupportedOperationException("format was invalid");
	}
}
