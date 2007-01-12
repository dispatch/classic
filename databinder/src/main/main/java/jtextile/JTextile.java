/*

This is Textile
A Humane Web Text Generator

Original PHP Version
Version 1.0
21 Feb, 2003

Copyright (c) 2003, Dean Allen, www.textism.com
All rights reserved.

This java version by Gareth Simpson 
1.0 April 2003
1.1 mid 2004
1.2 March 2006
_______
LICENSE

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name Textile nor the names of its contributors may be used to
  endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

_____________
USING TEXTILE

Block modifier syntax:

Header: hn. 
Paragraphs beginning with 'hn. ' (where n is 1-6) are wrapped in header tags.
Example: <h1>Text</h1>

Header with CSS class: hn(class).
Paragraphs beginning with 'hn(class). ' receive a CSS class attribute. 
Example: <h1 class="class">Text</h1>

Paragraph: p. (applied by default)
Paragraphs beginning with 'p. ' are wrapped in paragraph tags.
Example: <p>Text</p>

Paragraph with CSS class: p(class).
Paragraphs beginning with 'p(class). ' receive a CSS class attribute. 
Example: <p class="class">Text</p>

Blockquote: bq.
Paragraphs beginning with 'bq. ' are wrapped in block quote tags.
Example: <blockquote>Text</blockquote>

Blockquote with citation: bq(citeurl).
Paragraphs beginning with 'bq(citeurl). ' recieve a citation attribute. 
Example: <blockquote cite="citeurl">Text</blockquote>

Numeric list: #
Consecutive paragraphs beginning with # are wrapped in ordered list tags.
Example: <ol><li>ordered list</li></ol>

Bulleted list: *
Consecutive paragraphs beginning with * are wrapped in unordered list tags.
Example: <ul><li>unordered list</li></ul>


Phrase modifier syntax:

_emphasis_             <em>emphasis</em>
__italic__             <i>italic</i>
*strong*               <strong>strong</strong>
**bold**               <b>bold</b>
??citation??           <cite>citation</cite>
-deleted text-         <del>deleted</del>
+inserted text+        <ins>inserted</ins>
^superscript^          <sup>superscript</sup>
~subscript~            <sub>subscript</sub>
@code@                 <code>computer code</code>

==notextile==          leave text alone (do not format)

"linktext":url         <a href="url">linktext</a>
"linktext(title)":url  <a href="url" title="title">linktext</a>

!imageurl!             <img src="imageurl">
!imageurl(alt text)!   <img src="imageurl" alt="alt text" />
!imageurl!:linkurl     <a href="linkurl"><img src="imageurl" /></a>

ABC(Always Be Closing) <acronym title="Always Be Closing">ABC</acronym>

*/

package jtextile;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JTextile 
	{
		@SuppressWarnings("unused")
		private static final int ENT_COMPAT = 0;
		private static final int ENT_NOQUOTES = 2;
		private static final int ENT_QUOTES = 3;
		
		
		
		public JTextile() 
		{
		} 
		
		
		public static String textile(String text) throws Exception
		{
			
			//$text = stripslashes($text);
			
			//# turn any incoming ampersands into a dummy character for now.
			//#  This uses a negative lookahead for alphanumerics followed by a semicolon,
			//#  implying an incoming html entity, to be skipped 
			text = preg_replace("&(?![#a-zA-Z0-9]+;)","x%x%",text);
			
			//# unentify angle brackets and ampersands
			text = replace(text,"&gt;", ">");
			text = replace(text,"&lt;", "<");
			text = replace(text,"&amp;", "&");
			
			
			//# zap carriage returns
			text = replace(text,"\r\n", "\n");
			
			
			//# zap tabs
			text = replace(text,"\t", "" );
			
			//  trim each line
			StringBuffer splitBuffer = new StringBuffer();
			
			String[] sList = text.split("/\n/");
			for(int i = 0; i < sList.length; i++)
			{
				splitBuffer.append(sList[i].trim());
				splitBuffer.append("\n");
			}
			
			text = splitBuffer.toString();
			
			//### Find and replace quick tags
			
			//# double equal signs mean <notextile>
			text = preg_replace("(^|\\s)==(.*?)==([^\\w]{0,2})","$1<notextile>$2</notextile>$3$4",text);
			
			//# image qtag
			text = preg_replace("!([^!\\s\\(=]+?)\\s?(\\(([^\\)]+?)\\))?!","<img src=\"$1\" alt=\"$3\" />",text);
			
			//# image with hyperlink
			text = preg_replace("(<img.+ \\/>):(\\S+)","<a href=\"$2\">$1</a>",text);
			
			//# hyperlink qtag
			text = preg_replace("\"([^\"\\(]+)\\s?(\\(([^\\)]+)\\))?\":(\\S+?)([^\\w\\s\\/;]|[1-9]*?)(\\s|$)","<a href=\"$4\" title=\"$3\">$1</a>$5$6",text);
			
			//# arrange qtag delineators and replacements in an array
			String[] srcTags = {"\\*\\*","\\*","\\?\\?","-","\\+","~","@"};
			String[] replaceTags = {"b","strong","cite","del","ins","sub","code"};
			
			//# loop through the array, replacing qtags with html
			for(int i = 0; i < srcTags.length; i++)
			{
				//text = preg_replace("(^|\\s|>)" + srcTags[i] + "\\b(.+?)\\b([^\\w\\s]*?)" + srcTags[i] + "([^\\w\\s]{0,2})(\\s|$)","$1<" + replaceTags[i] + ">$2$3</" + replaceTags[i] + ">$4$5",text);
				text = preg_replace("(^|\\s|>)" + srcTags[i] + "([^ ])(.+?)?([^\\w\\s]*?)([^ ])" + srcTags[i] + "([^\\w\\s]{0,2})(\\s|$)","$1<" + replaceTags[i] + ">$2$3$4$5</" + replaceTags[i] + ">$6$7",text);
			}
			
			//# some weird bs with underscores and \b word boundaries, 
			//#  so we'll do those on their own
			
			text = preg_replace("(^|\\s)__(.*?)__([^\\w\\s]{0,2})","$1<i>$2</i>$3",text);   
			
			text = preg_replace("(^|\\s)_(.*?)_([^\\w\\s]{0,2})","$1<em>$2</em>$3",text); 
			
			text = preg_replace("\\^(.*?)\\^","<sup>$1</sup>",text);
			
			// ### Find and replace typographic chars and special tags
			
			//# small problem with double quotes at the end of a string
			
			text = preg_replace("\"$","\" ",text);
			
			//# NB: all these will wreak havoc inside <html> tags
			
			String[] glyph_search = {
//					"([^\\s[{<])?\\'([dmst]\\b|ll\\b|ve\\b|\\s|$)",  // escape [
					"([^\\s\\[{<])?\\'([dmst]\\b|ll\\b|ve\\b|\\s|$)",  // single closing
					"\\'", // single opening
//					"([^\\s[{])?\"(\\s|$)", // escape [
					"([^\\s\\[{])?\"(\\s|$)", // # double closing
					"\"", // double opening
					"\\b( )?\\.{3}", // # ellipsis
					"\\b([A-Z][A-Z0-9]{2,})\\b(\\(([^\\)]+)\\))", // # 3+ uppercase acronym
					"(^|[^\"][>\\s])([A-Z][A-Z0-9 ]{2,})([^<a-z0-9]|$)", // # 3+ uppercase caps
					"\\s?--\\s?", // # em dash
					"\\s-\\s", // # en dash
					"(\\d+)-(\\d+)", // # en dash
					"(\\d+) ?x ?(\\d+)", //# dimension sign
					"\\b ?(\\((tm|TM)\\))", // trademark
					"\\b ?(\\([rR]\\))", // # registered
					"\\b ?(\\([cC]\\))" // # registered     
			};
			
			
			String[] glyph_replace = {     
					"$1&#8217;$2",              //# single closing
					"&#8216;",                //# single opening
					"$1&#8221;$2",              //# double closing
					"&#8220;",                //# double opening
					"$1&#8230;",              //# ellipsis
					"<acronym title=\"$2\">$1</acronym>", //# 3+ uppercase acronym
					//"$1<span class=\"caps\">$2</span>$3", //# 3+ uppercase caps
					"$1$2$3", //# 3+ uppercase caps
					"&#8212;",                //# em dash
					" &#8211; ",              //# en dash
					"$1&#8211;$2",              //# en dash
					"$1&#215;$2",             //# dimension sign
					"&#8482;",                //# trademark
					"&#174;",               //# registered
					"&#169;"                //# copyright
			};
			
			
			
			
			//    # set toggle for turning off replacements between <code> or <pre>
			boolean codepre = false;
			boolean notextile = false;
			
			//# if there is no html, do a simple search and replace
			
			if(!preg_match("<.[^<]*>",text))
			{
				text = preg_replace(glyph_search,glyph_replace,text);
			}
			else 
			{
				
				StringBuffer out = new StringBuffer();
				//# else split the text into an array at <.*>
				//$text = preg_split("/(<.*>)/U",$text,-1,PREG_SPLIT_DELIM_CAPTURE);
				String[] textSplit = preg_split("<.[^<]*>",text);
				for(int i = 0; i < textSplit.length; i++)
				{
					
					//  # matches are off if we're between <code>, <pre> etc. 
					if(preg_match("<(code|pre|kbd)>",textSplit[i].toLowerCase()))
					{
						codepre = true; 
					}
					if(preg_match("<notextile>",textSplit[i].toLowerCase()))
					{
						codepre = true;
						notextile = true;
					}
					else if(preg_match("</(code|pre|kbd)>",textSplit[i].toLowerCase()))
					{
						codepre = false; 
					}
					else if(preg_match("</notextile>",textSplit[i].toLowerCase()))
					{
						codepre = false; 
						notextile = false;
					}
					
					if(!preg_match("<.[^<]*?>",textSplit[i]) && codepre == false)
					{
						textSplit[i] = preg_replace(glyph_search,glyph_replace,textSplit[i]);
					}
					
					//# convert htmlspecial if between <code>
					if (codepre == true && notextile == false){
						textSplit[i] = htmlspecialchars(textSplit[i],ENT_NOQUOTES);
						textSplit[i] = replace(textSplit[i],"&lt;pre&gt;","<pre>");
						textSplit[i] = replace(textSplit[i],"&lt;code&gt;","<code>");
						textSplit[i] = replace(textSplit[i],"&lt;notextile&gt;","<notextile>");
					}
					
					if(notextile == true)
					{
						textSplit[i] = replace(textSplit[i],"\n","({)(})");
					}
					
					//# each line gets pushed to a new array
					out.append( textSplit[i]);
				}
				
				text = out.toString();
				
				
			}
			
			//### Block level formatting
			
			//# deal with forced breaks; this is going to be a problem between
			//#  <pre> tags, but we'll clean them later
			
			
			//////!!! not working 
			//text = preg_replace("(\\S)(_*)([[:punct:]]*) *\n([^#*\\s])", "$1$2$3<br />$4", text);
			//text = preg_replace("(\\S)(_*)([:punct:]*) *\\n([^#*\\s])", "$1$2$3<br />$4", text);
			
			
			text = preg_replace("(\\S)(_*)([:punct:]*) *\\n([^#*\\s])", "$1$2$3<br />$4", text);
			
			
			//# might be a problem with lists
			text = replace(text,"l><br />", "l>\n");
			
			boolean pre = false;
			
			
			String[] block_find = {
					"^\\s?\\*\\s(.*)",            //# bulleted list *
					"^\\s?#\\s(.*)",              //# numeric list #
					"^bq\\. (.*)",                //# blockquote bq.
					"^bq\\((\\S+?)\\). (.*)",                //# blockquote bq(cite-url).
					"^h(\\d)\\(([\\w]+)\\)\\.\\s(.*)",  //# header hn(class).  w/ css class
					"^h(\\d)\\. (.*)",            //# plain header hn.
					"^p\\(([[:alnum:]]+)\\)\\.\\s(.*)",   //# para p(class).  w/ css class
					"^p\\. (.*)",                 //# plain paragraph
					"^([^\\t ]+.*)"               //# remaining plain paragraph
			};
			
			/*
			 String[]  block_find = {
			 "/^\\s?\\*\\s(.*)/",                         //                      # bulleted list *
			 "/^\\s?#\\s(.*)/",                       //                         # numeric list #
			 "/^bq\\. (.*)/",                         //                        # blockquote bq.
			 "/^h(\\d)\\(([[:alnum:]]+)\\)\\.\\s(.*)/", //  # header hn(class).  w/ css class
			 "/^h(\\d)\\. (.*)/",                     //                         # plain header hn.
			 "/^p\\(([[:alnum:]]+)\\)\\.\\s(.*)/",      //         # para p(class).  w/ css class
			 "/^p\\. (.*)/i",                       //                          # plain paragraph
			 "/^([^\\t ]+.*)/i"                     //                          # remaining plain paragraph
			 };      
			 */
			String[] block_replace = {
//					"\t<liu>$1</liu>$2",
//					"\t<lio>$1</lio>$2",
					"\t<liu>$1</liu>",
					"\t<lio>$1</lio>",
					"\t<blockquote>$1</blockquote>",
					"\t<blockquote cite=\"$1\">$2</blockquote>",
					"\t<h$1 class=\"$2\">$3</h$1>$4",
//					"\t<h$1>$2</h$1>$3",
					"\t<h$1>$2</h$1>",
					"\t<p class=\"$1\">$2</p>$3",
					"\t<p>$1</p>",
//					"\t<p>$1</p>$2"
					"\t<p>$1</p>"
			};
			
			
			StringBuffer blockBuffer = new StringBuffer();
			
			String list = "";
			
			//  This done to ensure that lists close after themselves
			text += " \n";
			
			
			//# split the text into an array by newlines
			String[] bList = text.split("\n");
			for(int i = 0; i <= bList.length; i++)
			{
				String line = " ";
				if(i < bList.length)
					line = bList[i];
				
				
				//#make sure the line isn't blank
				if (true || line.length() > 0 ) // actually i think we want blank lines
				{
					
					//# matches are off if we're between <pre> or <code> tags 
					if(line.toLowerCase().indexOf("<pre>") > -1)
					{ 
						pre = true; 
					}
					
					//# deal with block replacements first, then see if we're in a list
					if (!pre)
					{
						line = preg_replace(block_find,block_replace,line);
					}
					
					//# kill any br tags that slipped in earlier
					if (pre == true)
					{
						line = replace(line,"<br />","\n");
					} 
					
					//# matches back on after </pre> 
					if(line.toLowerCase().indexOf("</pre>") > -1)
					{ 
						pre = false; 
					}
					
					//# at the beginning of a list, $line switches to a value
					if (list.length() == 0 && preg_match("\\t<li",line))
					{
						line = preg_replace("^(\\t<li)(o|u)","\n<$2l>\n$1$2",line);
						list = line.substring(2,3);
					} 
					//# at the end of a list, $line switches to empty
					else if (list.length() > 0 && !preg_match("\\t<li" + list,line))
					{
						line = preg_replace("^(.*)$","</" + list + "l>\n$1",line); 
						list = "";
					}
				}
				// push each line to a new array once it's processed
				blockBuffer.append(line);
				blockBuffer.append("\n");
				
			}
			text = blockBuffer.toString();
			
			
			
			//#clean up <notextile>
			text = preg_replace("<\\/?notextile>", "",text);  
			
			//#clean up <notextile>
			text = replace(text,"({)(})", "\n");  
			
			//# clean up liu and lio
			text = preg_replace("<(\\/?)li(u|o)>", "<$1li>",text);
			
			//# turn the temp char back to an ampersand entity
			text = replace(text,"x%x%","&#38;");
			
			//# Newline linebreaks, just for markup tidiness
			text = replace(text,"<br />","<br />\n");   
			
			return text;
		} 
		
		
		
		/**
		 * Does just that.
		 * 
		 * @param source      The string to start with
		 * @param searchFor   The string we are looking for
		 * @param replaceWith The replacement
		 * 
		 * @return  The reformatted string
		 * 
		 */
		private static String replace ( String source , String searchFor , String replaceWith )
		{
			if (source == null || "".equals(source)) {
				return source;
			}
			
			if (replaceWith == null) {
				return source;
			}
			
			if ("".equals(searchFor)) {
				return source;
			}
			
			int s = 0;
			int e = 0;
			StringBuffer result = new StringBuffer();
			
			while ((e = source.indexOf(searchFor, s)) >= 0) 
			{
				result.append(source.substring(s, e));
				result.append(replaceWith);
				s = e + searchFor.length();
			}
			result.append(source.substring(s));
			return result.toString();
			
		}
		
		private static String htmlspecialchars(String text, int mode)
		{
			text = replace(text,"&", "&amp;");
			if (mode != ENT_NOQUOTES)
				text = replace(text,"\"", "&quot;");
			if (mode == ENT_QUOTES)
				text = replace(text,"'", "&#039;");
			text = replace(text,"<", "&lt;");
			text = replace(text,">", "&gt;");
			return text ;
		}
		
		private static String preg_replace(String pattern,String replace,String text) throws Exception
		{
			
//			gnu.regexp.RE r = new gnu.regexp.RE(pattern);
//			return r.substituteAll(text,replace);
			return Pattern.compile(pattern).matcher(text).replaceAll(replace);
		}
		
		private static String preg_replace(String[] pattern,String[] replace,String text) throws Exception
		{
			for(int i = 0; i < pattern.length; i++)
			{
				text = preg_replace(pattern[i],replace[i],text);
			}
			return text;
		}
		
		private static boolean preg_match(String pattern,String text) throws Exception
		{
//			gnu.regexp.RE r = new gnu.regexp.RE(pattern);
//			return r.getMatch(text) != null;
			return Pattern.compile(pattern).matcher(text).find();
		}
		
		private static String[] preg_split(String pattern,String text) throws Exception
		{
			int startAt = 0;
			ArrayList<String> tempList = new ArrayList<String>();
			
//			gnu.regexp.RE r = new gnu.regexp.RE(pattern);
			
			Matcher m = Pattern.compile(pattern).matcher(text);
			m.find();
//			gnu.regexp.REMatch match = r.getMatch(text);
			
			while(m.find())
			{                  
				String beforeMatch = text.substring(startAt, m.start());      
				tempList.add(beforeMatch);
				tempList.add(text.substring(m.start(), m.end()));         
				startAt = m.end();
			}
			
			tempList.add(text.substring(startAt));
			
			//  copy out our templist to an array of strings which is what we return
			String[] ret = new String[tempList.size()];
			
			for(int i = 0; i < ret.length; i++)
			{
				ret[i] = tempList.get(i);
			}
			
			return ret;
		}
		
	}