package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.font.AWTGVTFont;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.renderer.StrokingTextPainter;
import org.apache.batik.gvt.text.TextPaintInfo;

import wicket.model.IModel;

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

	protected RenderedTextImageResource newRenderedTextImageResource(RenderedLabel label, boolean isShared) {
		RenderedTextImageResource res = new BatikRenderedTextImageResource();
		res.setCacheable(isShared);
		res.setState(label);
		return res;
	}

	
	protected static class BatikRenderedTextImageResource extends RenderedTextImageResource {
		@Override
		protected boolean render(Graphics2D graphics) {
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
			
			graphics.setColor(color);
			TextNode node = new TextNode();
			AttributedString ats = new AttributedString(renderedText);
			
			List<GVTFont> fonts = new ArrayList<GVTFont>(1);
			fonts.add(new AWTGVTFont(font));
			ats.addAttribute(StrokingTextPainter.GVT_FONTS, fonts);
			
			TextPaintInfo tpi = new TextPaintInfo();
			tpi.visible = true;
			tpi.fillPaint = color;
	        
			ats.addAttribute(StrokingTextPainter.PAINT_INFO, tpi);
			
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
					RenderingHints.VALUE_STROKE_PURE);

			node.setLocation(new Point(0, metrics.getAscent()));
			node.setAttributedCharacterIterator(ats.getIterator());
			node.getTextPainter().paint(node, graphics);
			
//			// Turn on anti-aliasing
//			
//			graphics.setColor(color);
//			
//			// Draw each line at its baseline
//			int baseline = metrics.getAscent();
//			for (String line : lines) {
//				graphics.drawString(line, 0, baseline);
//				baseline += lineHeight;
//			}
			return true;		
		}
	}

}
