// Created by Steve Marney
// Copyright (C) Hewlett Packard Company 2009-2012, All rights reserved
// See the project license agreement for more details

package com.hp.QuickRDA.Plugins;

import java.io.PrintStream;

import com.hp.QuickRDA.L0.lang.ISet;
import com.hp.QuickRDA.L0.lang.TextFile;
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
	
	// XML tags for domain model elements
	public static final String ROOT_TAG = "DMI";
	public static final String DMINAME_TAG = "Name";
	public static final String QUICKRDAVERSION_TAG = "QuickRDAVersion";
	public static final String ISANINSTANCEOF_TAG = "Is An Instance Of";
	
	private PrintStream ps;
	private XMLHelper xml;
	
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
		
		System.out.print("Executing " + cmd + "... ");
		if (genInfo.itsFilePath != null) {
			String filePath = genInfo.itsFilePath;
			String filePrefix = genInfo.itsFilePrefix;
			String fileSuffix = ".xml";
			xml = new XMLHelper();
			try {
				ps = TextFile.openTheFileForCreateThrowing(filePath, filePrefix, fileSuffix);
				ps.println(xml.header());
				ps.println(xml.beginTag(ROOT_TAG, true));
				ps.println(xml.element(DMINAME_TAG, genInfo.itsFilePrefix, true));
				ps.println(xml.element(QUICKRDAVERSION_TAG, genInfo.itsQuickRDAVersion, true));
				traverseDomainModel(genInfo.itsBuilder, genInfo.itsFocusView);
				ps.println(xml.endTag(ROOT_TAG, true));
				TextFile.closeTheFile(ps);
			} catch (Exception e) {
				String file = filePath + "\\" + filePrefix + fileSuffix;
				System.out.println("Error creating file " + file + ": " + e.getCause());
			}
		}
		System.out.println("Done.");
		return new String();
	}
	
	/**
	 * Traverses the DMI graph in view, outputing all nodes and edges.
	 * 
	 * @param bldr
	 * @param vw current view in focus
	 */
	private void traverseDomainModel(Builder bldr, DMIView vw) {

		ISet<DMIElem> sV = vw.toSet ();

		// traverse nodes
		for (int i = 0; i < sV.size(); i++) {
			DMIElem m = sV.get(i);
			if (m != null && !m.isStatement()) {
				rootNode(bldr, m);
			}
		}

		// traverse edges (i.e., statements)
		for (int i = 0; i < sV.size(); i++) {
			DMIElem s = sV.get(i);
			if (s != null && s.isStatement()) {
				rootEdge(bldr, s);
			}
		}
	}
	
	/**
	 * Outputs as an XML tree the identifier information for given node
	 * {@code m} in a DMI graph; and edges (i.e., statements) as child
	 * nodes, where {@code m} is the subject.
	 * 
	 * @param bldr
	 * @param m the DMI element (node) to be output as XML
	 */
	private void rootNode(Builder bldr, DMIElem m) {
		ps.println(xml.beginTag(m, true));
		ps.println(xml.identifier(bldr, m, true));
		
		// include all statements where m is the subject
		ISet<DMIElem> stmts = m.itsUsedAsSubjectStmtSet;
		for (int i = 0; i < stmts.size(); i++) {
			DMIElem s = stmts.get(i);
			String tag = NameUtilities.firstName(s.itsVerb, bldr.itsGraph, true, true);
			if (!tag.equalsIgnoreCase(ISANINSTANCEOF_TAG)) {
				String value = xml.identifier(bldr, s.itsObject, false);
				ps.println(xml.element(tag, value, true));
			}
		}
		ps.println(xml.endTag(m, true));
	}

	/**
	 * Outputs as an XML tree a given edge's (i.e., statement's) identifier 
	 * information and its subject and object as child elements. The statement's
	 * verb is used as the tag name for the root node of the XML tree.
	 * 
	 * @param bldr
	 * @param s the DMI graph's edge (i.e., statement) to be output as XML
	 */
	private void rootEdge(Builder bldr, DMIElem s) {
		String tag = NameUtilities.firstName(s.itsVerb, bldr.itsGraph, true, true);
		ps.println(xml.beginTag(tag, true));
		ps.println(xml.identifier(bldr, s.itsVerb, true));
		ps.println(xml.element("Subject", xml.identifier(bldr, s.itsSubject, false), true));
		ps.println(xml.element("Object", xml.identifier(bldr, s.itsObject, false), true));
		ps.println(xml.endTag(tag, true));
	}
	
}
