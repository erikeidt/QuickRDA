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


package com.hp.QuickRDA.Plugins;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.hp.QuickRDA.L0.lang.ISet;
import com.hp.QuickRDA.L0.lang.TextFile;
import com.hp.QuickRDA.L0.lang.lang;
import com.hp.QuickRDA.L1.Core.DMIElem;
import com.hp.QuickRDA.L1.Core.DMIView;
import com.hp.QuickRDA.L2.Names.NameUtilities;
import com.hp.QuickRDA.L4.Build.XML.XMLHelper;
import com.hp.QuickRDA.L5.ExcelTool.Builder;

/**
 * This class provides a QuickRDA plugin for exporting the DMI elements in view
 * as an XML document file.
 *
 * @author Steve Marney
 *
 */
public class XMLGenerator implements IGeneratorPlugin {

	// XML tag and attribute names for DMI elements
	public static final String ROOT_TAG = "DMI";
	public static final String NODE_TAG = "DMIElem";
	public static final String DMINAME_TAG = "name";
	public static final String QUICKRDAVERSION_TAG = "QuickRDAVersion";
	public static final String SUBJECT_TAG = "Subject";
	public static final String OBJECT_TAG = "Object";

	private Builder bldr;
	private PrintStream ps;
	private XMLHelper xml;
	private boolean abbrev;

	/**
	 * Provides the main entry point for this plugin; traverses the DMI graph
	 * for the view currently in focus and outputs the nodes and edges to an
	 * XML document file.
	 *
	 * @param  genInfo contains generation information such as domain model file
	 *                 path, focus view, etc.
	 * @param  cmd     plugin command line (no args for this plugin)
	 * @return empty string
	 */
	@Override
	public String generate(GenerationInfo genInfo, String cmd) {

		lang.msg("Executing " + cmd + " ... ");
		if (genInfo.itsFilePath != null) {
			processArgs(cmd);

			String filePath   = genInfo.itsFilePath;
			String filePrefix = genInfo.itsFilePrefix;
			String fileSuffix = ".xml";

			bldr = genInfo.itsBuilder;
			xml = new XMLHelper(bldr);
			List<String> attrs = new ArrayList<String>();
			attrs.add(xml.attribute(DMINAME_TAG, filePrefix));
			attrs.add(xml.attribute(QUICKRDAVERSION_TAG, genInfo.itsQuickRDAVersion));
			try {
				ps = TextFile.openTheFileForCreateThrowing(filePath, filePrefix, fileSuffix);
				ps.println(xml.header());
				ps.println(xml.beginTag(ROOT_TAG, attrs, false, true));
				traverseDomainModel(genInfo.itsFocusView);
				ps.println(xml.endTag(ROOT_TAG, true));
				TextFile.closeTheFile(ps);
			} catch (Exception e) {
				String file = filePath + "\\" + filePrefix + fileSuffix;
				lang.errMsg( "Error creating file " + file + ": " + e.getCause() );
			}
		}
		lang.msgln( "Done." );
		return new String();
	}
	
	/**
	 * Iterates over the arguments in the command string {$code cmd}, processing
	 * each command argument.
	 * 
	 * @param cmd command string including arguments
	 */
	private void processArgs(String cmd) {
		abbrev = false;
		String[] args = cmd.split(" ");
		for (int i=1; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-a")) {
				abbrev = true;
			}
		}
	}

	/**
	 * Traverses the DMI graph in view, outputing all nodes and edges.
	 *
	 * @param vw current view in focus
	 */
	private void traverseDomainModel(DMIView vw) {

		ISet<DMIElem> sV = vw.toSet ();

		// traverse nodes
		for (int i = 0; i < sV.size(); i++) {
			DMIElem m = sV.get(i);
			if (m != null && !m.isStatement()) {
				rootNode(m);
			}
		}

		// traverse edges (i.e., statements)
		for (int i = 0; i < sV.size(); i++) {
			DMIElem s = sV.get(i);
			if (s != null && s.isStatement()) {
				rootEdge(s);
			}
		}
	}

	/**
	 * Outputs as an XML tree the identifier information for a given node
	 * {@code m} in a DMI graph; and edges (i.e., statements) as child
	 * nodes, where {@code m} is the subject.
	 *
	 * @param m the DMI element (node) to be output as XML
	 */
	private void rootNode(DMIElem m) {

		ISet<DMIElem> stmts = m.itsUsedAsSubjectStmtSet;

		// output a node for each type of which m is an instance
		if (!abbrev) {
			for (int i = 0; i < m.itsDeclaredTypeList.size(); i++) {
				DMIElem type = m.itsDeclaredTypeList.get(i);
				String tag = NameUtilities.firstName(type, m.itsSubgraph.itsGraph, true, true);
				if (isMetamodelType(m, tag)) {
					ps.println(xml.beginTag(tag, m, true, true));
				}
			}
		}

		// output a DMIElem node that includes all statements where m is the subject
		ps.println(xml.beginTag(NODE_TAG, m, false, true));
		for (int j = 0; j < stmts.size(); j++) {
			DMIElem s = stmts.get(j);
			String tag2 = NameUtilities.firstName(s.itsVerb, bldr.itsGraph, true, true);
			ps.println(xml.element(tag2, s.itsObject, null, true));
		}
		ps.println(xml.endTag(NODE_TAG, true));
	}
	
	/**
	 * Determines whether {@code t} is a type defined in QuickRDA's Metamodel
	 * (versus a type that was defined within a domain model).
	 * 
	 * @param  m the DMI element
	 * @param  t a type name
	 * @return true if {@code t} is defined in the QuickRDA Metamodel
	 */
	private boolean isMetamodelType(DMIElem m, String t) {

		/* 
		 * Note: Because {@code m.itsDeclaredTypeList} includes domain-model defined
		 * types as well as metamodel defined types, it would be helpful if QuickRDA 
		 * maintain a list (perhaps on the Builder class) that includes only DMI 
		 * elements defined as part of the QuickRDA Metamodel. Then we could iterate
		 * over that list and return {@code true} only if {@code t} is in that list.
		 * This method could then be refactored to be metamodel independent.
		 */

		ISet<DMIElem> stmts = m.itsUsedAsSubjectStmtSet;

		for (int i = 0; i < stmts.size(); i++) {
			DMIElem s = stmts.get(i);
			String v = NameUtilities.firstName(s.itsVerb, bldr.itsGraph, true, true);
			String o = NameUtilities.firstName(s.itsObject, bldr.itsGraph, true, true);
			if (t.equalsIgnoreCase(o) &&
				! "Is A Kind Of (FO-FO)".equalsIgnoreCase(v) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Outputs as an XML tree a given edge's (i.e., statement's) identifier
	 * information and its subject and object as child elements. The statement's
	 * verb is used as the tag name for the root node of the XML tree.
	 *
	 * @param s the DMI graph's edge (i.e., statement) to be output as XML
	 */
	private void rootEdge(DMIElem s) {
		String tag = NameUtilities.firstName(s.itsVerb, bldr.itsGraph, true, true);
		ps.println(xml.beginTag(tag, s.itsVerb, false, true));
		ps.println(xml.element(SUBJECT_TAG, s.itsSubject, null, true));
		ps.println(xml.element(OBJECT_TAG, s.itsObject, null, true));
		ps.println(xml.endTag(tag, true));
	}
}
