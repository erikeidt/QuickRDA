/* 
Copyright (c) 2011, 2012 Hewlett-Packard Development Company, L.P.
Created by Steve Marney.

This file is part of QuickRDA.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.hp.QuickRDA.L4.Build.XML;

import com.hp.QuickRDA.L1.Core.DMIElem;
import com.hp.QuickRDA.L2.Names.NameUtilities;
import com.hp.QuickRDA.L5.ExcelTool.Builder;

/**
 * This class provides helper functions for rendering DMI elements and strings
 * into XML strings.
 * 
 * @author Steve Marney
 *
 */
public class XMLHelper {

	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	// XML tags for DMI elements
	public static final String ID_TAG = "Id";
	public static final String NAME_TAG = "Name";
	public static final String MAP_TAG = "Map";
	public static final String KEY_TAG = "Key";
	public static final String VALUE_TAG = "Value";

	// XML indentation related variables
	public static final String INDENTATION = "  ";
	private int level;
	
	
	/**
	 * Default constructor.
	 */
	public XMLHelper() {
		resetLevel();
	}

	/**
	 * Resets the indentation level.
	 */
	public void resetLevel() {
		level = 0;
	}
	
	/**
	 * Increases the indentation level by 1.
	 */
	public void indent() {
		level++;
	}
	
	/**
	 * Decreases the indentation level by 1.
	 */
	public void outdent() {
		level--;
	}
	
	/**
	 * Indents {@code str} to the current indentation level.
	 * 
	 * @param  str the string to be indented
	 * @return indented string
	 */
	public String indent(String str) {
		String ind = "";
		for ( int i=0; i < level; i++ ) {
			ind += INDENTATION;
		}
		return ind + str;
	}

	/**
	 * Formats, creates and indents an XML element's "begin" tag as appropriate.
	 * 
	 * @param  tag the tag name
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "begin" tag
	 */
	public String beginTag(String tag, boolean indent) {
		String str = "<" + formatTag(tag) + ">";
		if (indent) {
			str = indent(str);
			indent();
		}
		return str;
	}
	
	/**
	 * Formats, creates and indents an XML element's "begin" tag as appropriate.
	 * 
	 * @param  m the DMI element whose name will be used for the tag
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "begin" tag
	 */
	public String beginTag(DMIElem m, boolean indent) {
		return beginTag(getTag(m), indent);
	}
	
	/**
	 * Formats, creates and indents an XML element's "end" tag as appropriate.
	 * 
	 * @param  tag the tag name
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "end" tag
	 */
	public String endTag(String tag, boolean indent) {
		String str = "</" + formatTag(tag) + ">";
		if (indent) {
			outdent();
			str = indent(str);
		}
		return str;
	}
	
	/**
	 * Formats, creates and indents an XML element's "end" tag as appropriate.
	 * 
	 * @param  m the DMI element whose name will be used for the tag
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "end" tag
	 */
	public String endTag(DMIElem m, boolean indent) {
		return endTag(getTag(m), indent);
	}
	
	/**
	 * Creates a tag name based on the specified DMI element's name. 
	 * 
	 * @param  m the DMI element whose name will be used for the tag
	 * @return tag name
	 */
	public String getTag(DMIElem m) {
		DMIElem type = m.itsDeclaredTypeList.get(m.itsDeclaredTypeList.size() - 1);
		String tag = NameUtilities.firstName(type, m.itsSubgraph.itsGraph, true, true);
		tag = tag.replaceAll("\\<.*", "");	// keep everything before the first occurence of "<"
		return tag;
	}
	
	/**
	 * Formats a tag name to ensure it is syntactically valid.
	 * 
	 * @param  tag tag to be formatted
	 * @return formatted tag
	 */
	public String formatTag(String tag) {
		String t = tag.replaceAll("-", "");
		t = t.replaceAll(" ", "");
		t = t.replaceAll("\\(", "_");
		t = t.replaceAll("\\)", "");
		t = t.replaceAll("\\.\\.", "_");	// convert occurrences of ".." to "_"
		t = t.replaceAll("\\.", "");		// remove remaining occurrences of "."
		return t;
	}
	
	/**
	 * Creates an XML element with its value/subtree bracketed by begin and end tags.
	 * 
	 * @param  tag the tag name
	 * @param  value the XML element value/subtree
	 * @param  indent if {@code true}, indent the XML element
	 * @return XML element
	 */
	public String element(String tag, String value, boolean indent) {
		String xml = new String();
		if (!expandable(value)) {
			xml = beginTag(tag, false) + value + endTag(tag, false);
		}
		else {
			String name = (value.substring(0, value.indexOf("["))).trim();
			xml = element(tag, name, false) + expand(value);
		}
		if (indent) {
			xml = indent(xml);
		}
		return xml;
	}
	
	/**
	 * Creates an XML element with its integer value bracketed by begin and end tags.
	 * 
	 * @param  tag the tag name
	 * @param  value the XML element value
	 * @param  indent if {@code true}, indent the XML element
	 * @return XML element
	 */
	public String element(String tag, int value, boolean indent) {
		String xml = beginTag(tag, false) + value + endTag(tag, false);
		if (indent) {
			xml = indent(xml);
		}
		return xml;
	}
	
	/**
	 * Creates an XML comment string indented as appropriate.
	 * 
	 * @param  value the comment text
	 * @return XML comment string
	 */
	public String comment(String value) {
		return indent("<!-- " + format(value) + " -->");
	}
	
	/**
	 * Creates an XML heading, which is essentially an XML comment preceded by
	 * two blank lines.
	 * 
	 * @param  value the heading text
	 * @return XML heading string
	 */
	public String heading(String value) {
		return "\n\n" + comment(value);
	}
	
	/**
	 * @return XML header string indicating XML version and encoding
	 */
	public String header() {
		return XML_HEADER;
	}
	
	/**
	 * Creates a string that contains formatted {@code <Name>} and {@code <Id>}
	 * XML elements for the specified DMI element.
	 * 
	 * @param  bldr DMI builder
	 * @param  m DMI element whose Name and Id are to be rendered into XML elements
	 * @param  indent if {@code true}, indent the identifier string
	 * @return identifier formatted as XML
	 */
	public String identifier(Builder bldr, DMIElem m, boolean indent) {
		String xml = new String();
		xml += element(NAME_TAG, format(NameUtilities.firstName(m, bldr.itsGraph, true, true)), false);
		xml += element(ID_TAG, m.itsIndex, false);
		if (indent) {
			xml = indent(xml);
		}
		return xml;
	}
	
	/**
	 * Formats special characters in the specified string for XML. Chevrons
	 * ("<<" and ">>") are removed and special characters '&', '<', and '>' 
	 * are escaped as necessary.
	 * 
	 * @param  str string to be formatted for XML
	 * @return formatted string
	 */
	public String format(String str) {
		String xml = str;
		if (!xml.contains("&amp;")) {
			xml = xml.replaceAll("&", "&amp;");
		}
		xml = xml.replaceAll("<<", "");
		xml = xml.replaceAll(">>", "");
		xml = xml.replaceAll("<", "&lt;");
		xml = xml.replaceAll(">", "&gt;");
		return xml;
	}
	
	/**
	 * Determines if the specified string is expandable to an XML tree structure.
	 * 
	 * @param  str
	 * @return true if {@code str} contains '[' and ']' characters
	 */
	public boolean expandable(String str) {
		return (str.contains("[") && str.contains("]"));
	}
	
	/**
	 * Expands the specified string into a list of XML nodes named {@code Map}, each of
	 * which has child nodes named {@code Key} and {@code Value}.
	 * 
	 * @param  str string whose format is {@code "[ key1 : value1 , key2 : value2 , ... ]"}
	 * @return {@code str} expanded to a list of XML nodes
	 * 
	 * @see    #expandPairs(String[])
	 */
	public String expand(String str) {
		String xml = str;
		if (xml.contains("[")) {
			xml = xml.substring(xml.indexOf("[")+1, xml.indexOf("]")-1).trim();
			String[] pairs = xml.split(",");
			xml = expandPairs(pairs);
		}
		return xml;
	}
	
	/**
	 * Expands the specified list of key/value pairs into a list of XML nodes 
	 * named Map, each of which has child nodes named Key and Value.
	 * 
	 * @param  pairs a list of strings, each of which has the format {@code "key : value"}
	 * @return concatenated list of strings, each of which has the format  
	 *         {@code "<Map><Key>key</Key><Value>value</Value></Map>"}
	 */
	public String expandPairs(String[] pairs) {
		String xml = new String();
		for (int i=0; i < pairs.length; i++) {
			int colon = pairs[i].indexOf(":");
			if (colon > 0) {
				String key = element(KEY_TAG, pairs[i].substring(0,colon).trim(), false);
				String value = element(VALUE_TAG, pairs[i].substring(colon+1).trim(), false);
				xml += element(MAP_TAG, key + value, false);
			}
		}
		return xml;
	}
}
