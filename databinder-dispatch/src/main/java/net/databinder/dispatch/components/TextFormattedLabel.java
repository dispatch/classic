package net.databinder.dispatch.components;

import org.apache.wicket.model.IModel;


/**
 * XmlRpcLabel Factory that my be helpful when model text format is determined at runtime. 
 * @author Nathan Hamblen
 */
public class TextFormattedLabel extends XmlRpcLabel {
	public static enum TextFormat { 
		/** creates RedClothLabel */
		Textile("redcloth.to_html"), 
		/** creates MarukuLabel */
		Markdown("maruku.to_html"), 
		/** creates RubyPantsLabel */
		Smartypants("rubypants.to_html"); 
		
		protected String methodName;

		TextFormat(String methodName) {
			this.methodName = methodName;
		}
	}
	public TextFormattedLabel(String id, TextFormat textFormat) {
		super(id, new TextFormatConverter(textFormat));
		setEscapeModelStrings(false);
	}

	public TextFormattedLabel(String id, IModel model, TextFormat textFormat) {
		super(id, model, new TextFormatConverter(textFormat));
		setEscapeModelStrings(false);
	}

	public TextFormattedLabel(String id, TextFormatConverter converter) {
		super(id, converter);
		setEscapeModelStrings(false);
	}

	public TextFormattedLabel(String id, IModel model, TextFormatConverter converter) {
		super(id, model, converter);
		setEscapeModelStrings(false);
	}

	public static class TextFormatConverter extends XmlRpcConverter {
		private TextFormat textFormat;
		public TextFormatConverter(TextFormat textFormat) {
			this.textFormat = textFormat;
		}
		public TextFormatConverter() { }
		protected TextFormat getTextFormat() {
			return textFormat;
		}
		@Override
		protected String getMethodName() {
			return getTextFormat().methodName;
		}
	}
}