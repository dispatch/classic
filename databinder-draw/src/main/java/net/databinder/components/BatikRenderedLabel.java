package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.font.AWTGVTFont;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.renderer.StrokingTextPainter;
import org.apache.batik.gvt.text.TextPaintInfo;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

public class BatikRenderedLabel extends RenderedLabel {
	public BatikRenderedLabel(String id) {
		super(id);
	}
	public BatikRenderedLabel(String id, boolean shareResource) {
		super(id, shareResource);
	}
	public BatikRenderedLabel(String id, IModel model) {
		super(id, model);
	}
	public BatikRenderedLabel(String id, IModel model, boolean shareResource) {
		super(id, model, shareResource);
	}


	public static void loadSharedResources(String text, Font font, Color color, Color backgroundColor, Integer maxWidth) {
		loadSharedResources(new BatikRenderedTextImageResource(), text, font, color, backgroundColor, maxWidth);
	}

	@Override
	protected RenderedTextImageResource newRenderedTextImageResource(boolean isShared) {
		RenderedTextImageResource res = new BatikRenderedTextImageResource();
		res.setCacheable(isShared);
		res.setState(this);
		return res;
	}

	
	protected static class BatikRenderedTextImageResource extends RenderedTextImageResource {
		
		protected List<AttributedCharacterIterator> getAttributedLines() {
			if (Strings.isEmpty(text))
				return null;
			AttributedString attributedText = new AttributedString(text);
			
			List<GVTFont> fonts = new ArrayList<GVTFont>(1);
			fonts.add(new AWTGVTFont(font));
			attributedText.addAttribute(StrokingTextPainter.GVT_FONTS, fonts);
			
			TextPaintInfo tpi = new TextPaintInfo();
			tpi.visible = true;
			tpi.fillPaint = color;
			attributedText.addAttribute(StrokingTextPainter.PAINT_INFO, tpi);

			return splitAtNewlines(attributedText, text);
		}
		
		@Override
		protected boolean render(Graphics2D graphics) {
			final int width = getWidth(), height = getHeight();

			// draw background if not null, otherwise leave transparent
			if (backgroundColor != null) {
				graphics.setColor(backgroundColor);
				graphics.fillRect(0, 0, width, height);
			}

			// render as a 1x1 pixel if text is empty
			if (Strings.isEmpty(text)) {
				if (width == 1 && height == 1)
					return true;
				setWidth(1);
				setHeight(1);
				return false;
			}
			
			// Get size of text
			graphics.setFont(font);
			final FontMetrics fontMetrics = graphics.getFontMetrics();

			List<AttributedCharacterIterator> attributedLines = getAttributedLines();
			
			// each one of these is needed for a unhinted, anti-aliased display
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
					RenderingHints.VALUE_STROKE_PURE);

			 // TODO: maxwidth wrapping layout, format string processing

			float lineHeight = graphics.getFontMetrics().getHeight(),
				neededHeight = attributedLines.size() * lineHeight + fontMetrics.getMaxDescent(),
				neededWidth = 0f, 
				y = lineHeight;
	
			for (AttributedCharacterIterator line : attributedLines) {
				TextNode node = new TextNode();
				node.setLocation(new Point(0, (int) y));
				node.setAttributedCharacterIterator(line);
				node.getTextPainter().paint(node, graphics);
				
				float w = (float) node.getTextPainter().getBounds2D(node).getWidth() + 4f;
				if (w > neededWidth)
					neededWidth = w;
				
				y += lineHeight;
			}
			if (neededWidth > width || neededHeight > height) {
				setWidth((int)Math.ceil(neededWidth));
				setHeight((int)Math.ceil(neededHeight));
				return false;
			}

			return true;		
		}
	}

}
