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

import java.util.List;
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
	public static final String ID_TAG = "id";
	public static final String NAME_TAG = "name";

	// XML indentation related variables
	public static final String INDENTATION = "  ";
	private int level;
	
	private Builder bldr;
	
	
	/**
	 * Default constructor.
	 */
	public XMLHelper(Builder bldr) {
		this.bldr = bldr;
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
	 * Creates and indents an XML element's "begin" tag and attributes
	 * as appropriate.
	 * 
	 * @param  tag the tag name
	 * @param  attrs list of attribute name/value pairs
	 * @param  close if {@code true}, close with "/>" instead of ">"
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "begin" tag
	 */
	public String beginTag(String tag, List<String> attrs, boolean close, boolean indent) {
		String str = "<" + formatTag(tag) + attributes(attrs) + (close ? "/>" : ">");
		if (indent) {
			str = indent(str);
			if (!close) {
				indent();
			}
		}
		return str;
	}

	/**
	 * Creates and indents an XML element's "begin" tag and attributes
	 * as appropriate.
	 * 
	 * @param  tag the tag name
	 * @param  attrs if not {@code null}, DMI element metadata are included as attributes
	 * @param  close if {@code true}, close with "/>" instead of ">"
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "begin" tag
	 * @see    #attributes(DMIElem)
	 */
	public String beginTag(String tag, DMIElem attrs, boolean close, boolean indent) {
		String str = "<" + formatTag(tag) + attributes(attrs) + (close ? "/>" : ">");
		if (indent) {
			str = indent(str);
			if (!close) {
				indent();
			}
		}
		return str;
	}
	
	/**
	 * Creates and indents an XML element's "begin" tag and attributes
	 * as appropriate.
	 * 
	 * @param  m the DMI element whose name will be used for the tag
	 * @param  attrs if not {@code null}, DMI element metadata are included as attributes
	 * @param  close if {@code true}, close with "/>" instead of ">"
	 * @param  indent if {@code true}, indent the XML tag
	 * @return XML "begin" tag
	 * @see    #attributes(DMIElem)
	 */
	public String beginTag(DMIElem m, DMIElem attrs, boolean close, boolean indent) {
		return beginTag(getTag(m), attrs, close, indent);
	}
	
	/**
	 * Creates and indents an XML element's "end" tag as appropriate.
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
	 * Creates and indents an XML element's "end" tag as appropriate.
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
		String tag = new String();
		DMIElem type = m.itsDeclaredTypeList.get(0);
		tag = NameUtilities.firstName(type, m.itsSubgraph.itsGraph, true, false);
		tag = tag.replaceAll("\\<.*", "");	// keep everything before the first "<"
		return tag;
	}
	
	/**
	 * Formats {@code attrs} into a list of {@code name="value"} pairs,
	 * separated by spaces.
	 * 
	 * @param  attrs list of formatted attribute {@code name="value"} pairs
	 * @return formatted attribute list
	 * @see    #attribute(String, String)
	 */
	public String attributes(List<String> attrs) {
		String str = new String();
		if (attrs != null) {
			for (String attr : attrs) {
				str += attr;
			}
		}
		return str;
	}
	
	/**
	 * Creates a string containing XML element attributes that identify the DMI
	 * element {@code m} by name and id.
	 * 
	 * @param  m DMI element whose name and index are formatted as XML element attributes
	 * @return formatted {@code name} and {@code id} attributes
	 * @see    #attribute(String, String)
	 */
	public String attributes(DMIElem m) {
		String str = new String();
		if (m != null) {
			str += attribute(NAME_TAG, NameUtilities.firstName(m, bldr.itsGraph, true, true));
			str += attribute(ID_TAG, String.valueOf(m.itsIndex));
		}
		return str;
	}
	
	/**
	 * Creates a name/value pair representing an XML element attribute.
	 * 
	 * @param  name attribute name
	 * @param  value attribute value
	 * @return {@code name="value"} preceded by a space character
	 */
	public String attribute(String name, String value) {
		return " " + formatTag(name) + "=\"" + format(value) + "\"";
	}
	
	/**
	 * Creates an XML element with its value/subtree bracketed by begin and end tags.
	 * 
	 * @param  tag the tag name
	 * @param  attrs if not {@code null}, DMI element metadata are included as attributes
	 * @param  value the XML element value/subtree
	 * @param  indent if {@code true}, indent the XML element
	 * @return XML element
	 */
	public String element(String tag, DMIElem attrs, String value, boolean indent) {
		String xml;
		if (value == null) {
			xml = beginTag(tag, attrs, true, indent);
		} else {
			xml = beginTag(tag, attrs, false, indent) + value + endTag(tag, false);
		}
		return xml;
	}
	
	/**
	 * Creates an XML element with its integer value bracketed by begin and end tags.
	 * 
	 * @param  tag the tag name
	 * @param  attrs if not {@code null}, DMI element metadata are included as attributes
	 * @param  value the XML element value
	 * @param  indent if {@code true}, indent the XML element
	 * @return XML element
	 */
	public String element(String tag, DMIElem attrs, int value, boolean indent) {
		return element(tag, attrs, String.valueOf(value), indent);
	}
	
	/**
	 * Creates an XML comment string indented as appropriate.
	 * 
	 * @param  value the comment text
	 * @return XML comment string
	 */
	public String comment(String value) {
		return indent("<!-- " + value + " -->");
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
	 * Formats special characters in the specified string for XML. Special characters 
	 * '&', '<', '>', single quote ('), and double quote (") are escaped as necessary.
	 * 
	 * @param  str string to be formatted for XML
	 * @return formatted string
	 */
	public String format(String str) {
		String xml = str;
		if (!xml.contains("&amp;")) {
			xml = xml.replaceAll("&", "&amp;");
		}
		xml = xml.replaceAll("<", "&lt;");
		xml = xml.replaceAll(">", "&gt;");
		xml = xml.replaceAll("'", "&apos;");
		xml = xml.replaceAll("\"", "&quot;");
		return xml;
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
		t = t.replaceAll("\\<.*", "");		// keep everything before the first "<"
		return t;
	}
}
