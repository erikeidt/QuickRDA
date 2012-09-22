/* 
Copyright (c) 2011, 2012 Hewlett-Packard Development Company, L.P.
Created by Erik L. Eidt.

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

package com.hp.QuickRDA.L4.Build;

import java.io.PrintStream;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;

public class DMIExport {

	private DMIElem []		itsReferenceClasses;
	private DMIElem []		itsReferenceSerializations;

	private ISet<DMIElem>	itsExportList;

	private PrintStream		itsFileNum;

	private int				itsNextDMIPos;
	private int				itsConceptStart;
	private int				itsStatementStart;

	private int				itsXTBase;
	private int []			itsXDMIPos;

	public void buildNamesAndSerializationsForExport ( DMIView vw, DMIBaseVocabulary baseVocab, ConceptManager conceptMgr, DMISubgraph nameSG ) {
		DMISubgraph xg = conceptMgr.setDefaultSubgraph ( nameSG );

		//
		// For DMI we need to export limited metamodel metadata (names) that is not as rich as
		// full type information
		// so we need to discover what types are needed and generate metadata for those
		// when we do same for domain model metadata
		//

		itsExportList = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		itsReferenceClasses = new DMIElem [] { baseVocab.gName, baseVocab.gHasName, baseVocab.gHasNameString, baseVocab.gStringSerialization };
		itsReferenceSerializations = new DMIElem [ itsReferenceClasses.length ];
		for ( int i = 0; i < itsReferenceClasses.length; i++ ) {
			itsReferenceSerializations [ i ] = buildNamesForNode ( itsReferenceClasses [ i ], true, baseVocab, conceptMgr );
		}

		// want metamodel metadata to come out first, and, just names of concepts, really
		ISet<DMIElem> qV = vw.toSet ();
		qV.trim ();

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isStatement () ) {
				buildNamesForNode ( m.itsVerb, true, baseVocab, conceptMgr );
			}
		}

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			buildNamesForNode ( m, false, baseVocab, conceptMgr );
			if ( m.isStatement () ) {
				if ( !vw.inView ( m.itsSubject ) )
					buildNamesForNode ( m.itsSubject, true, baseVocab, conceptMgr );
				if ( !vw.inView ( m.itsObject ) )
					buildNamesForNode ( m.itsObject, true, baseVocab, conceptMgr );
			}
		}

		itsExportList.mergeListIntoSet ( nameSG.toSet () );
		itsExportList.mergeListIntoSet ( vw.toSet () );
		/*
		 * vw.AddSubgraph(nameSG); vw.AddElementsFromSet(exportList);
		 */
		conceptMgr.setDefaultSubgraph ( xg );
		// graph.trim();
	}

	private DMIElem buildNamesForNode ( DMIElem m, boolean isImport, DMIBaseVocabulary baseVocab, ConceptManager conceptMgr ) {
		DMIElem ans = null;
		if ( m.instanceOf ( baseVocab.gName ) ) {
			// don't build names for names; we'll already do that
		} else {
			if ( isImport )
				itsExportList.addToSet ( m );

			DMISharedNameList snl = NameUtilities.getFirstName ( m );
			if ( snl != null )
				ans = buildNamesAndSerializations ( snl.itsMCText, isImport, m, baseVocab, conceptMgr );

		}
		return ans;
	}

	private DMIElem buildNamesAndSerializations ( String nameStr, boolean isImport, DMIElem m, DMIBaseVocabulary baseVocab, ConceptManager conceptMgr ) {
		DMIElem nm = conceptMgr.enterName ( nameStr, false );
		DMIElem x = conceptMgr.enterStatement ( m, baseVocab.gHasName, nm, false );
		DMIElem sr = conceptMgr.enterSerialization ( nameStr, null );
		DMIElem y = conceptMgr.enterStatement ( nm, baseVocab.gHasNameString, sr, false );
		if ( isImport ) {
			itsExportList.addToSet ( nm );
			itsExportList.addToSet ( x );
			itsExportList.addToSet ( sr );
			itsExportList.addToSet ( y );
		}
		return sr;
	}

	public void exportToDMI ( DMIGraph graph, String filePath, String filePrefix, String fileSuffix, String label ) {
		itsXTBase = graph.getMinIndex ();
		int u = graph.getMaxIndex ();
		itsXDMIPos = new int [ u - itsXTBase ];

		itsFileNum = TextFile.openTheFileForCreate ( filePath, filePrefix, fileSuffix );

		setDMIPositions ();

		itsFileNum.println ( "{" );
		itsFileNum.println ();

		itsFileNum.println ( "\"DMI Version\" : \"1.0\"," );
		itsFileNum.println ();

		itsFileNum.println ( "\"Label\" : \"" + BuildUtilities.makeJSONEscapeText ( label ) + "\"," );
		itsFileNum.println ();

		exportSerializations ( graph.itsBaseVocab );
		itsFileNum.println ();

		exportConcepts ( graph.itsBaseVocab );
		itsFileNum.println ();

		itsFileNum.println ( "\"Unasserted Statements\" : [ ]," );
		itsFileNum.println ();

		exportStatements ();
		itsFileNum.println ();

		exportReference ();
		itsFileNum.println ();

		itsFileNum.println ( "}" );

		TextFile.closeTheFile ( itsFileNum );
	}

	private void exportSerializations ( DMIBaseVocabulary baseVocab ) {
		itsFileNum.println ( "\"Serializations\" : [" );

		int ssDMIPos = getDMIPos ( baseVocab.gStringSerialization );
		String mid = " */\t[ " + ssDMIPos + ", \"";

		int cnt = 0;
		ISet<DMIElem> qV = itsExportList;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isSerialization () ) {
				if ( cnt > 0 )
					itsFileNum.println ( "," );
				itsFileNum.print ( "/* " + padNum ( getDMIPos ( m ) ) + "\t(" + m.itsIndex + ")" + mid + BuildUtilities.makeJSONEscapeText ( m.value () ) + "\" ]" );
				cnt++;
			}
		}
		if ( cnt > 0 )
			itsFileNum.println ();
		itsFileNum.println ( "  ],\t/* Serializations */" );
	}

	private void exportConcepts ( DMIBaseVocabulary baseVocab ) {
		itsFileNum.println ( "\"Simple Concept Count\" : " + (itsStatementStart - itsConceptStart) + "," );
		itsFileNum.println ( "/*" );

		ISet<DMIElem> qV = itsExportList;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isSimpleConcept () ) {
				String content = " * " + padNum ( getDMIPos ( m ) ) + "\t(" + m.itsIndex + ")";
				if ( m.instanceOf ( baseVocab.gName ) )
					content += "\tN\t";
				else
					content += "\t \t";
				content += NameUtilities.getMCText ( m );
				itsFileNum.println ( content );
			}
		}
		itsFileNum.println ( " */" );
	}

	private void exportStatements () {
		itsFileNum.println ( "\"Asserted Statements\" : [" );

		int cnt = 0;
		ISet<DMIElem> qV = itsExportList;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isStatement () ) {
				if ( cnt > 0 )
					itsFileNum.println ( "," );
				itsFileNum.print ( "/* " + padNum ( getDMIPos ( m ) ) + "\t(" + m.itsIndex + ") */\t[ " +
						padNum ( getDMIPos ( m.itsSubject ) ) + ", " + padNum ( getDMIPos ( m.itsVerb ) ) + ", " + padNum ( getDMIPos ( m.itsObject ) ) + " ]" +
						" \t/* " + NameUtilities.getMCText ( m.itsSubject ) + " - " + NameUtilities.getMCText ( m.itsVerb ) + " - " + NameUtilities.getMCText ( m.itsObject ) + " */" );
				cnt++;
			}
		}
		if ( cnt > 0 )
			itsFileNum.println ();
		itsFileNum.println ( "  ],\t/* AssertedStatements */" );
	}

	private void exportReference () {
		itsFileNum.println ( "\"Initial Reference\" : {" );
		itsFileNum.println ( "\t\"Scheme\" : \"SS\"," );
		itsFileNum.println ( "\t\"Concepts\" : [" );
		for ( int i = 0; i < itsReferenceClasses.length; i++ ) {
			DMIElem m = itsReferenceClasses [ i ];
			DMIElem s = itsReferenceSerializations [ i ];
			String oc = ((i == itsReferenceClasses.length - 1) ? "" : ",");
			itsFileNum.println ( "/*\t(" + m.itsIndex + "-" + s.itsIndex + ")\t*/\t[ " +
					padNum ( getDMIPos ( m ) ) + ", " + padNum ( getDMIPos ( s ) ) +
					" ]" + oc + "\t/* " + NameUtilities.getMCText ( m ) + " */" );
		}

		itsFileNum.println ( "\t  ]\t/* Concepts */" );
		itsFileNum.println ( "  }\t/* Reference */" );
	}

	private void setDMIPositions () {
		ISet<DMIElem> qV = itsExportList;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isSerialization () ) {
				setDMIPosition ( m );
			}
		}
		itsConceptStart = itsNextDMIPos;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isSimpleConcept () ) {
				setDMIPosition ( m );
			}
		}
		itsStatementStart = itsNextDMIPos;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m.isStatement () ) {
				setDMIPosition ( m );
			}
		}
	}

	private void setDMIPosition ( DMIElem m ) {
		setDMIPos ( m, itsNextDMIPos );
		itsNextDMIPos++;
	}

	private String padNum ( int n ) {
		String ans = "" + n;
		int c = 5 - ans.length ();
		if ( c > 0 ) {
			ans = Strings.Mid ( "        ", 1, c ) + ans;
		}
		return ans;
	}

	/*
	 * private void PrintProvenances() { DMIElem[] qL; qL = gGraph.FullGraph; int i; for (
	 * i=1-1; i < qL.length; i++ ) { DMIElem m; m = qL(i); if ( m.itsToDOT ) { DMIElem[] pSL;
	 * pSL = m.ProvenanceSet; DMIElem[] pL; ReDim pL(0); int j; for ( j=1-1; j < pSL.length; j++
	 * ) { DMIElem ps; ps = pSL(j); if ( FindPositionInList(m, pL) == 0 ) { AddToSet m, pL;
	 * String QN; QN = PrintQualifiedName(fileNum, ps, false); String content; if ( AsNode(m) )
	 * { content = "[ """ + QN + """, ""Asserts"", """ + m.itsGraphID + ".1"" ]"; Print
	 * #fileNum, content; content = "[ """ + QN + """, ""Asserts"", """ + m.itsGraphID +
	 * ".2"" ]"; Print #fileNum, content; content = "[ """ + QN + """, ""Asserts"", """ +
	 * m.itsGraphID + ".3"" ]"; Print #fileNum, content; } if ( AsEdge(m) ) { content = "[ """ +
	 * QN + """, ""Asserts"", """ + m.itsGraphID + """ ]"; Print #fileNum, content; } } } } } }
	 */

	private int getDMIPos ( DMIElem m ) {
		return itsXDMIPos [ m.itsIndex - itsXTBase ];
	}

	private void setDMIPos ( DMIElem m, int id ) {
		itsXDMIPos [ m.itsIndex - itsXTBase ] = id;
	}

}
