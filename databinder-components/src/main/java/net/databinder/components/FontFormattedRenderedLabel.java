package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

import org.apache.wicket.model.IModel;

/**
 * Uses specified fonts for bold and italic text. By default bold and italic fonts will be derived
 * from the RenderedLabel base font, but if that font is loaded from a resource
 * these must be as well. Links are attributed as underlined plain-weight text in Color.BLUE.
 * @author Nathan Hamblen
 */
public class FontFormattedRenderedLabel extends RenderedLabel {
	private Font italicFont = getFont().deriveFont(Font.ITALIC);
	private Font boldFont = getFont().deriveFont(Font.BOLD);

	public FontFormattedRenderedLabel(String id) {
		super(id);
	}
	
	public FontFormattedRenderedLabel(String id, IModel model) {
		super(id, model);
	}

	public FontFormattedRenderedLabel(String id, boolean shareResource) {
		super(id, shareResource);
	}
	
	public FontFormattedRenderedLabel(String id, IModel model, boolean shareResource) {
		super(id, model, shareResource);
	}
	
	public static void loadSharedResources(String text, Font font, Font boldFont, Font italicFont, Color color, Color backgroundColor, Integer maxWidth) {
		loadSharedResources(new FontFormattedRenderedImageResource(), text, font, boldFont, italicFont, color, backgroundColor, maxWidth);
	}

	protected static void loadSharedResources(FontFormattedRenderedImageResource res, String text, Font font, Font boldFont, Font italicFont, Color color, Color backgroundColor, Integer maxWidth) {
		res.boldFont = boldFont;
		res.italicFont = italicFont;
		RenderedLabel.loadSharedResources(res, text, font, color, backgroundColor, maxWidth);
	}
	
	protected FontFormattedRenderedImageResource newRenderedTextImageResource(boolean isShared) {
		FontFormattedRenderedImageResource res = new FontFormattedRenderedImageResource();
		res.setCacheable(isShared);
		res.setState(this);
		return res;
	}



	protected static class FontFormattedRenderedImageResource extends FormattedRenderedTextImageResource {
		protected Font boldFont, italicFont;

		@Override
		public void setState(RenderedLabel label) {
			FontFormattedRenderedLabel ffLabel = (FontFormattedRenderedLabel) label;
			boldFont = ffLabel.getBoldFont();
			italicFont = ffLabel.getItalicFont();
			super.setState(label);
		}
	
		@Override
		void attributeBold(AttributedString string, int start, int end) {
			string.addAttribute(TextAttribute.FONT, boldFont, start, end);
		}
		@Override
		void attributeItalic(AttributedString string, int start, int end) {
			string.addAttribute(TextAttribute.FONT, italicFont, start, end);
		}
		/** Renders as underlined plain-weight text in Color.BLUE; override for other attributes. */
		@Override
		void attributeLink(AttributedString string, int start, int end) {
			string.addAttribute(TextAttribute.UNDERLINE,TextAttribute.UNDERLINE_ON, start, end); 
			string.addAttribute(TextAttribute.FOREGROUND,Color.BLUE, start, end); 
		}
	}
	
	public Font getItalicFont() {
		return italicFont;
	}

	public void setItalicFont(Font italicFont) {
		this.italicFont = italicFont;
	}

	public Font getBoldFont() {
		return boldFont;
	}

	public void setBoldFont(Font boldFont) {
		this.boldFont = boldFont;
	}
}