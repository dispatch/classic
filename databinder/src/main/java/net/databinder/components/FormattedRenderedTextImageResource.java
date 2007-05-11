package net.databinder.components;

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.databinder.components.RenderedLabel.RenderedTextImageResource;

import org.apache.wicket.util.string.Strings;

/**
 * Base class for rendered labels formated with a Markdown subset including **bold**
 * __bold__ *italic* _italic_ and [link] appearance, as well as hard returns (space-space-newline)
 * and paragraphs (newline-newline). Subclasses apply attributes to
 * an AttributedString in the abstract attributeBold/Italic/Link methods. 
 * @see AttributedString
 * @author Nathan Hamblen
 */
public abstract class FormattedRenderedTextImageResource extends RenderedTextImageResource {
    //matches links patters like `[foo]: http://example.com/  "Optional Title Here"` 
    private static Pattern footnoteLinks = Pattern.compile("^ *\\[.+\\]\\:\\s.+\n", Pattern.MULTILINE);

	// matches single newlines that do not have two spaces before them
	private static Pattern strayNewlines = Pattern.compile("(?<!(  )|\n)\n(?!\n)");

	// group 1: either beginning of string or not a \
	// group 2: beginning format element, to be expelled
	// group 3: reluuctantly matched string inside formatters
	// group 4: ending format element, to be expelled
	private static Pattern boldFormat = Pattern.compile("(\\A|[^\\\\])(_{2}|\\*{2})(.+?)(\\2)", Pattern.DOTALL);
	private static Pattern italicFormat = Pattern.compile("(\\A|[^\\\\])(\\*|_)(.+?)(\\2)", Pattern.DOTALL);
    private static Pattern linkFormat = Pattern.compile("(\\A|[^\\\\])(\\[)(.+?)(\\](\\(|\\[).+?(\\)|\\]))", Pattern.DOTALL);
	
	// matches a slash used for escaping, to be expelled
	private static Pattern escapedCharacter = Pattern.compile("(\\\\)[^\\\\]");

	private enum Style {BOLD, ITALIC, LINK};
	
	private static class Range{
		Style style;
		int start;
		int end;
	}
	
	private static class MutableRangeString {
		List<Range> ranges = new ArrayList<Range>(10);
		StringBuilder string;
		public MutableRangeString(String str) {
			string = new StringBuilder(str);
		}
		void expell(int start, int end) {
			string.delete(start, end);
			for(Range r : ranges) {
				if (r.end > start) {
					r.end = r.end + start - end;
					if (r.start >= start)
						r.start = r.start + start - end;
				}
			}
		}
	}
	
	/** Apply style markers to ranges matching the given format pattern. */
	private static void process(MutableRangeString rangeStr, Pattern p, Style style) {
		int delta = 0;
		Matcher m = p.matcher(rangeStr.string.toString());
		while (m.find()) {
			Range r = new Range();
			r.style = style;
			r.start = m.start(3) - delta;
			r.end = m.end(3) - delta;

			rangeStr.ranges.add(r);

			rangeStr.expell(m.start(2) - delta, m.end(2) - delta);
			delta += m.end(2) - m.start(2);
			rangeStr.expell(m.start(4) - delta, m.end(4) - delta);
			delta += m.end(4) - m.start(4);
		}
	}
	
	/** @return string formatted with markdown subset */
	protected String getFormattedTextString() {
		return text;
	}
	
	/** @return string with attributes derived from formatting in getFormattedTextString() */
	@Override
	protected List<AttributedCharacterIterator> getAttributedLines() {
		String markedtext = getFormattedTextString();
		if (Strings.isEmpty(markedtext))
			return null;

        markedtext = footnoteLinks.matcher(markedtext).replaceAll("");
		markedtext = strayNewlines.matcher(markedtext.trim()).replaceAll("");
				
		MutableRangeString rangeStr = new MutableRangeString(markedtext);
		
		process(rangeStr, boldFormat, Style.BOLD);
		process(rangeStr, italicFormat, Style.ITALIC);
		process(rangeStr, linkFormat, Style.LINK);
		
		int delta = 0;
		Matcher m = escapedCharacter.matcher(rangeStr.string.toString());
		while (m.find()) {
			rangeStr.expell(m.start(1) - delta, m.end(1) - delta);
			delta++;
		}
		
		String text = rangeStr.string.toString();
		AttributedString attributedText = new AttributedString(text);
		attributedText.addAttribute(TextAttribute.FONT, font);
		
		for (Range r : rangeStr.ranges) {
			if (r.style == Style.BOLD)
				attributeBold(attributedText, r.start, r.end);
			else if (r.style == Style.ITALIC)
				attributeItalic(attributedText, r.start, r.end);
			else if (r.style == Style.LINK)
				attributeLink(attributedText, r.start, r.end);
		}
		return splitAtNewlines(attributedText, text);
	}
	
	abstract void attributeBold(AttributedString string, int start, int end);
	abstract void attributeItalic(AttributedString string, int start, int end);
	abstract void attributeLink(AttributedString string, int start, int end);
}
