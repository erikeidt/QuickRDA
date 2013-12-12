/* 
Copyright (c) 2011, 2012 Hewlett-Packard Development Company, L.P.
Created by Dave Penkler.

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

import com.hp.QuickRDA.L0.lang.IComparator;
import com.hp.QuickRDA.L0.lang.ISet;
import com.hp.QuickRDA.L0.lang.TextFile;
import com.hp.QuickRDA.L0.lang.XSetList;
import com.hp.QuickRDA.L0.lang.lang;
import com.hp.QuickRDA.L1.Core.DMIBaseVocabulary;
import com.hp.QuickRDA.L1.Core.DMIElem;
import com.hp.QuickRDA.L1.Core.DMIGraph;
import com.hp.QuickRDA.L1.Core.DMISubgraph;
import com.hp.QuickRDA.L1.Core.DMIView;
import com.hp.QuickRDA.L2.Names.NameUtilities;
import com.hp.QuickRDA.L5.ExcelTool.Start;

// QuickRDA plugin for generating a Data Dictionary file from the visible nodes in the tree.

public class DictGenerator implements IGeneratorPlugin {
	
	private ISet<Node>    itsNodes;
	private ISet<DMIElem> itsStatements;
	private ISet<DMIElem> itsElements;
	public DMIBaseVocabulary itsBaseVocab;
	private DMIGraph itsGraph;
	private PrintStream ps;
	private String version;
	private static DictComparator dictComparator = new DictComparator();
	private DictReasoner dictReasoner = new DictReasoner();
	
	@Override
	public String generate(GenerationInfo genInfo, String cmd) {
		if (genInfo.itsFilePath != null) {
			processArgs(cmd);
			version           = genInfo.itsQuickRDAVersion;
			String filePath   = genInfo.itsFilePath;
			String filePrefix = genInfo.itsFilePrefix;
			String fileSuffix = ".htm";
			String fileName   = filePath + "\\" + filePrefix + fileSuffix;
			itsGraph = genInfo.itsGraph;
			itsBaseVocab = genInfo.itsBuilder.itsBaseVocab;

			try {
				ps = TextFile.openTheFileForCreateThrowing(filePath, filePrefix, fileSuffix);
			} catch (Exception e) {
				lang.errMsg("Error creating file " + fileName + ": " + e.getCause());
				e.printStackTrace ( Start.gErrLogFile );
				return null;
			}

			try {
				generateDictionary(genInfo.itsFocusView);
			} catch (Exception e) {
				lang.errMsg ( "Exception in generateDictionary: " + e.getCause() );
				e.printStackTrace ( Start.gErrLogFile );
				return null;
			}

			printHead(genInfo.itsTitle);
			printContents();
			printNodes();
			printTail();
			TextFile.closeTheFile(ps);
		}
		lang.msgln("QUickRDA Data Dictionary Generator done.");
		return ""; 
	}

	private void processArgs(String cmd) {
		String[] args = cmd.split(",");
		for (int i=1; i < args.length; i++) {
			//if (args[i].equalsIgnoreCase("-m")) fMetaModel = true;
		}
	}

	private void generateDictionary(DMIView vw) {
		itsNodes      = new XSetList<Node> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsElements   = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsStatements = vw.toSet ();

		// traverse statements and build nodes for each unique subject and object
		for (int i = 0; i < itsStatements.size(); i++) {
			DMIElem m = itsStatements.get(i);
			if (m != null && m.isStatement()) {
				if (!itsElements.contains ( m.itsSubject )) {
					itsElements.add( m.itsSubject );
					itsNodes.add ( new Node(m.itsSubject) );
				}
				if (!itsElements.contains ( m.itsObject )) {
					itsElements.add( m.itsObject );
					itsNodes.add ( new Node(m.itsObject) );
				}
			}
		}
		
		dictReasoner.reasoner(); // add attach and group based relationships
		
		// sort the nodes
		itsNodes.sortList ( dictComparator );
	}
	
	private class Node {

		String itsName,     itsTypeName, itsQualifierName;  // Note: Qualifier of Type if not fMetaModel
		DMIElem itsElement, itsType,     itsQualifier;
		ISet<DMIElem> itsProperties;
				
		private Node (DMIElem m) {
			itsElement       = m;
			itsName          = getName(itsElement);
			itsType          = getType(itsElement);
			itsTypeName      = getName(itsType);
			itsQualifier     = getQualifier(( m.itsSubgraph.isAccessible ( DMISubgraph.SubgraphLevelEnum.kDomainModel )) ? itsElement : itsType);
			itsQualifierName = (null != itsQualifier) ?  getName(itsQualifier) : "";
			itsProperties = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand ); 
			for (int i = 0; i < itsStatements.size (); i++) {
				DMIElem n = itsStatements.get ( i );
				if (n != null && n.isStatement() && (m.equals(n.itsSubject) || m.equals ( n.itsObject ))) itsProperties.add ( n );
			}		
		}
	}
	
	private static class DictComparator implements IComparator<Node> {
		@Override
		public boolean follows ( Node m, Node n ) {
			int res = m.itsQualifierName.compareTo ( n.itsQualifierName );
			if (res > 0) return true;
			if (res < 0) return false;
			res = m.itsTypeName.compareTo ( n.itsTypeName );
			if (res > 0) return true;
			if (res < 0) return false;
			res = m.itsName.compareTo ( n.itsName );	
			if (res > 0) return true;
			return false;
		}
	}

	private class Relationship {  // Don't really need this class, exists for readability only
		DMIElem itsElement,itsType,itsSource,itsSourceType,itsTarget,itsTargetType;

		private Relationship(Node n, DMIElem m) { // n element's node , m statement containing Relationship connecting element
			itsElement    = m.itsVerb;
			itsType       = getQualifier(itsElement);
			itsSource     = m.itsSubject;
			itsSourceType = getType(itsSource);
			itsTarget     = m.itsObject;
			itsTargetType = getType(itsTarget);
		}
	}	

	private int nameComp(DMIElem m, DMIElem n) {
		return getName(m).compareTo ( getName(n));
	}
	
	private class RelComparator implements IComparator<Relationship> {
		@Override
		public boolean follows ( Relationship m, Relationship n ) {
			int res = nameComp(m.itsSourceType, n.itsSourceType);
			if (res > 0) return true;
			if (res < 0) return false;
			res = nameComp(m.itsSource,n.itsSource);
			if (res > 0) return true;
			if (res < 0) return false;
			res = nameComp(m.itsElement,n.itsElement);
			if (res > 0) return true;
			if (res < 0) return false;
			res = nameComp(m.itsTarget,n.itsTarget);
			if (res > 0) return true;
			if (res < 0) return false;
			res = nameComp(m.itsTargetType,n.itsTargetType);
			if (res > 0) return true;
			return false;
		}
	}
	
	private class DictReasoner {

		ISet<DMIElem> extraElements;

		private DictReasoner() {
			extraElements = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		}

		private void addStatements(DMIElem n, ISet<DMIElem> r, DMIElem vb, String ex) {
			//			loop over statements adding to relations to output.
			for (int k=0; k < r.size(); k++) {
				DMIElem s = r.get ( k );
				DMIElem o = s.itsSubject.equals(n) ?  s.itsObject : s.itsSubject;
				findNode(n).itsProperties.add ( s );
				findNode(o).itsProperties.add ( s );
			} 
			lang.errMsg ("\t\t" + r.size() + " " + getName(vb) + " properties added " + ex );
		}
		
		private void augment( DMIElem s, ISet<DMIElem> eels, DMIElem vb ) {
			for (int j=0; j < eels.size(); j++) {
				DMIElem n = eels.get ( j );
				lang.errMsg ( "\tAugment " + getName(n) );
				if ( !itsElements.contains ( n )  && !extraElements.contains(n)) {
					extraElements.add ( n );
					itsNodes.add ( new Node(n) );
					lang.errMsg ( "\t\tadded..");
				}
		
				addStatements(n, n.findStatementsFromUsedAsObjectBySubjectAndVerb ( s, vb, null ), vb , "by subject and verb");
				addStatements(n, n.findStatementsFromUsedAsSubjectByVerbAndObject ( vb, s, null ), vb , "by verb and object");
			}
		}

		public void reasoner () {

			for (int i=0; i<itsElements.size (); i++) {
				DMIElem n = itsElements.get ( i );
				lang.errMsg ( "Considering " + getName(n) );
	
				augment( n, n.attachedSet(),   itsBaseVocab.gAttaches );
				augment( n, n.attachedToSet(), itsBaseVocab.gIsAttachedTo );
				augment( n, n.groupedSet (),   itsBaseVocab.gGroups );
				augment( n, n.groupedToSet(),  itsBaseVocab.gIsInGroup );
			}
		
		for (int i=0; i<extraElements.size(); i++) itsElements.add(extraElements.get(i));
		
		}
	}

	private DMIElem getType(DMIElem n) {
		if (null !=  n.itsDeclaredTypeList && n.itsDeclaredTypeList.size ()>0)
			return n.itsDeclaredTypeList.get ( 0 );
		else return null;
	}

	private DMIElem getQualifier(DMIElem n) {
			if ((null != n) && (null !=  n.itsQualifiers) && (n.itsQualifiers.size () > 0))
			return n.itsQualifiers.get ( 0 );
		else return null;
	}

	private String getName(DMIElem n) {
		String res = NameUtilities.getMCText(n);
		return (res == null) ? "" : res;
	}
	
	private Node findNode( DMIElem m) {
		Node n = null;
		for (int i=0; i < itsNodes.size (); i++) {
			n = itsNodes.get ( i );
			if (m.equals ( n.itsElement )) break;
		}
		return n;
	}
	
	public void printNodes () {
		Node n;
		String lastQ = "";
		String lastI = "";
		int i = 0;
		while ( i < itsNodes.size()) {
			n = itsNodes.get ( i++ );
			if (!n.itsQualifierName.equals ( lastQ )) {
				printHeading(2,n.itsQualifier,n.itsQualifierName);
				lastQ = n.itsQualifierName;
				lastI = "";
			}
			if (!n.itsTypeName.equals ( lastI )) {
				printHeading(3,n.itsType,n.itsTypeName);
				lastI = n.itsTypeName;
			}
			printElement(n);
		}
	}
	
	
	public void printElement(Node n) {
		RelComparator relComparator = new RelComparator();
		ISet<Relationship> mRelationships = new XSetList<Relationship>(XSetList.AsSet, XSetList.HashOnDemand);
		printHeading(4,n.itsElement,n.itsName);
		startTable("");
		startRow();
		ps.println("<th>Source Type</th><th>Source</th><th>Relationship</th><th>Target</th><th>Target Type</th><th>Relationship Type</th>");
		endRow();
		for (int i = 0; i < n.itsProperties.size (); i++) {
			mRelationships.add(new Relationship(n, n.itsProperties.get ( i )));
		}
		mRelationships.sortList ( relComparator );
		for (int i = 0; i < mRelationships.size (); i++ ) {
			Relationship v = mRelationships.get ( i );
			startRow();
			printTableEntry( getName(v.itsSourceType) , v.itsSourceType,"");
			printTableEntry( getName(v.itsSource)     , n.itsElement.equals(v.itsSource) ? null : v.itsSource,"");
			printTableEntry( getName(v.itsElement)    , null ,"");
			printTableEntry( getName(v.itsTarget)     , n.itsElement.equals(v.itsTarget) ? null : v.itsTarget,"");
			printTableEntry( getName(v.itsTargetType) , v.itsTargetType,"");
			printTableEntry( getName(v.itsType)       , v.itsType,"");
			endRow();
		}
		endTable();
	}

	private void printContents() {
		Node n;
		String lastQ = "";
		String lastT = "";
		int i = 0;
		ps.println("<h3>Contents</h3>");	
		startTable("contents");	
		while ( i < itsNodes.size()) {
			n = itsNodes.get ( i++ );
			if (!n.itsQualifierName.equals ( lastQ )) {
				if (i > 0) endRow();
				startRow();
				printTableEntry(n.itsQualifierName,n.itsQualifier,"contents");
				lastQ = n.itsQualifierName;
				lastT = "";
			}
			if (!n.itsTypeName.equals ( lastT )) {
				printTableEntry(n.itsTypeName,n.itsType,"contents");
				lastT = n.itsTypeName;
			}
		}
		if (i > 0) endRow();
		endTable();
	}
	
	private void printHead(String title) {
		ps.println("<html>");	
		ps.println("<head>");
		ps.println("<meta http-equiv=Content-Type content=\"text/html; charset=windows-1252\">");
		ps.printf("<meta name=Generator content=\"QuickRDA Data Dictionary Generator Version: %s\">\n",version);
		ps.printf("<title>%s</title>",title);
		ps.println("<style>");
		ps.println("table { border: 1px solid gray; border-collapse: collapse; margin-left: 0px; margin-top: 10px; }");
		ps.println("td, th { border: 1px solid gray; padding-left: 5px; padding-right: 5px; }");
		ps.println("table.contents { border: 0; margin-left: 0px; margin-top: 10px; }");
		ps.println("td.contents { border: 0; padding-left: 0px; padding-right: 10px; }");
		ps.println("body { margin-left: 50px; }");
		ps.println("h1,h2,h3,h4 { margin-left: -40px; color: navy; font-family: sans-serif;	}");
		ps.println("</style>");
		ps.println("</head>");
		ps.println("<body lang=EN-US>");
		ps.printf("<h1>%s</h1>\n",NameUtilities.escapeTextForHTML(title));
	}
	
	private void printHeading(int level, DMIElem n, String name) {
		String desc = NameUtilities.getDescriptionFor ( n, itsGraph );
		String text = NameUtilities.escapeTextForHTML( name );
		ps.printf("<h%d><a id=\"%d\">%s</a></h%d>\n",level,n.itsIndex,text,level);
		ps.println(desc);
	}
	private void startTable(String mclass) {
		if (!"".equals ( mclass )) mclass = " class=\"" + mclass + "\"";
		ps.printf("<table %s>\n",mclass);
	}

	private void startRow() {
		ps.println("<tr>");
	}

	private void printTableEntry(String e, DMIElem link, String mclass) {
		String text = NameUtilities.escapeTextForHTML( e );
		if (!"".equals ( mclass )) mclass = " class=\"" + mclass + "\"";
		if (link != null) ps.printf("<td %s><a href=\"#%d\"</a>%s</td>",mclass,link.itsIndex, text);
		else      ps.printf("<td %s>%s</td>",mclass,text);
	}
	
	private void endRow() {
		ps.println("</tr>");
	}
	
	private void endTable() {
		ps.println("</table>");
	}

	private void printTail() {
		ps.println("</body>");
		ps.println("</html>");
	}
}