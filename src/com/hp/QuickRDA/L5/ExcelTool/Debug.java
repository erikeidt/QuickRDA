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

package com.hp.QuickRDA.L5.ExcelTool;

import java.io.PrintStream;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;

public class Debug {

	public static boolean	gDebugFileCreated	= false;

	public static PrintStream OpenDebugFile () {
		PrintStream ans = null;
		if ( gDebugFileCreated )
			ans = TextFile.openTheFileForAppend ( Start.gQuickRDATEMPPath, "DMI", ".txt" );
		else {
			ans = TextFile.openTheFileForCreate ( Start.gQuickRDATEMPPath, "DMI", ".txt" );
			gDebugFileCreated = true;
		}
		return ans;
	}

	public static PrintStream OpenDebugFile ( String filePath, String filePrefix, String fileSuffix ) {
		if ( Start.gQuickRDATEMPPath.equals ( filePath ) && "DMI".equals ( filePrefix ) && ".txt".equals ( fileSuffix ) )
			return OpenDebugFile ();
		return TextFile.openTheFileForCreate ( filePath, filePrefix, fileSuffix );
	}

	public static void exportDump ( ConceptManager cm ) {
		Debug.exportForDebug ( cm, cm.itsGraph, false, Start.gQuickRDATEMPPath, "DMI", ".txt" );
	}

	public static void exportDump ( String name, ISet<DMIElem> v, ConceptManager cm ) {
		Debug.exportView ( name, v, cm.itsGraph, Start.gQuickRDATEMPPath, "DMI", ".txt" );
	}

	public static void exportForDebug ( ConceptManager cm, DMIGraph g, boolean domain, String filePath, String filePrefix, String fileSuffix ) {
		//On Error GoTo 9999

		PrintStream fileNum = OpenDebugFile ( filePath, filePrefix, fileSuffix );

		MemRefCount mrc = new MemRefCount ();

		fileNum.println ( "==== Whole Graph ====" );
		hashAnalysis ( fileNum, "Serialization Hash", cm.getHT () );

		cm.memUsage ( mrc );

		// for ( int ix = 0; ix < g.subgraphCount (); ix++ ) {
		for ( DMISubgraph s : g.getSubgraphList () ) {
			// DMISubgraph s = g.Subgraph (ix);
			s.memUsage ( mrc );

			if ( !domain || s.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
				fileNum.println ( "==== Subgraph " + s.itsName + " ====" );
				DMIView vw = new DMIView ();
				vw.addSubgraph ( s );
				exportView ( fileNum, "Subgraph: " + s.itsName, vw.toSet (), g, mrc );
			}
		}

		fileNum.println ( "ObjCount = " + mrc.objCount + "; PtrCount = " + mrc.ptrCount + "; AtrCount = " + mrc.atrCount + "; StrCount = " + mrc.strCount + "; StrSize = " + mrc.strSize );

		fileNum.println ( "." );
		TextFile.closeTheFile ( fileNum );
	}

	public static void exportView ( String theName, DMIView vw, DMIGraph g, String filePath, String filePrefix, String fileSuffix ) {
		exportView ( theName, vw.toSet (), g, filePath, filePrefix, fileSuffix );
	}

	public static void exportView ( String theName, ISet<DMIElem> v, DMIGraph g, String filePath, String filePrefix, String fileSuffix ) {
		//On Error GoTo 9999
		PrintStream fileNum = TextFile.openTheFileForAppend ( filePath, filePrefix, fileSuffix );

		MemRefCount mrc = new MemRefCount ();

		exportView ( fileNum, "View: " + theName, v, g, mrc );

		fileNum.println ( "ObjCount = " + mrc.objCount + "; PtrCount = " + mrc.ptrCount + "; AtrCount = " + mrc.atrCount + "; StrCount = " + mrc.strCount + "; StrSize = " + mrc.strSize );
		fileNum.println ( "." );

		TextFile.closeTheFile ( fileNum );
	}

	private static void exportView ( PrintStream fileNum, String theName, ISet<DMIElem> qV, DMIGraph g, MemRefCount mrc ) {
		fileNum.println ( "---- Nulls ----\t\t(" + theName + ")" );
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m == null ) {
				fileNum.println ( "[" + i + "] is null" );
			}
		}

		fileNum.println ( "" );
		fileNum.println ( "---- Serializations ----\t(" + theName + ")" );
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && m.isSerialization () ) {
				printElement ( fileNum, m, g );
				m.memUsage ( mrc );
			}
		}
		fileNum.println ( "" );

		fileNum.println ( "---- Simple Concepts ----\t(" + theName + ")" );
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && m.isSimpleConcept () ) {
				printElement ( fileNum, m, g );
				m.memUsage ( mrc );
			}
		}
		fileNum.println ( "" );

		fileNum.println ( "---- Relationship Concepts ----\t(" + theName + ")" );
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && m.isStatement () ) {
				printElement ( fileNum, m, g );
				m.memUsage ( mrc );
			}
		}
		fileNum.println ();
	}

	private static void printElement ( PrintStream fileNum, DMIElem m, DMIGraph g ) {
		String msg;
		if ( m == null ) {
			msg = "... nil";
		} else {
			boolean R1 = false;
			boolean R2 = false;
			//StrStr("Description='", m.itsDescription, "'; ") + _
			//StrStr("Group=", m.Group, "; ") + _
			//ItemStr("Qual=", m.itsQualifier, "; ") + _
			//StrStr("URL='", m.itsURL, "'; ") + _

			//ItemStr("Diag=", m.itsDiagParent, "; ") + _
			//StrStr("GLink=", m.itsGraphLinkage, "; ") + _
			//SetStr("ProvenanceSet=", m.ProvenanceSet, "; ") + _
			//NumSetStr("Rows=", m.ProvenanceRows, "; ") + _
			//SetStr("nl='", m.NameList, "'; ", g) + _

			//msg = m.itsIndex + " [+" + m.itsRefCount + "]: "
			msg = m.itsIndex + ": " +
					strStr ( "N='", NameUtilities.firstName ( m, g, true, true ), "';" ) +
					strStr ( "V='", m.value (), "';" ) +
					itemStr ( "{ ", m.itsSubject, " ", g ) +
					itemStr ( " ", m.itsVerb, " ", g ) +
					itemStr ( " ", m.itsObject, " } ", g );

			if ( m.isStatement () ) {
				ISet<DMIElem> tV = m.itsDeclaredTypeList;
				if ( tV.size () > 0 && tV.get ( 0 ) == m.itsVerb ) {
					ISet<DMIElem> xV = m.itsFullTypeSet;
					if ( xV.size () > 0 && (xV.indexOf ( m.itsVerb ) >= 0) ) {} else {
						R2 = true;
					}
				} else {
					msg = msg + setStr ( "D=", m.itsDeclaredTypeList, "; ", g );
					R1 = true;
				}
			} else {
				R1 = true;
			}

			if ( R1 ) {
				msg += setStr ( "D=", m.itsDeclaredTypeList, "; ", g );
			}

			if ( R1 || R2 ) {
				if ( !m.itsDeclaredTypeList.sameList ( m.itsFullTypeSet ) ) {
					msg = msg + setStr ( "T=", m.itsFullTypeSet, "; ", g );
				}
			}

			msg = msg + setStr ( "SS=", m.superclassSet (), "; ", g );
			msg = msg +
					setStr ( "DX=", m.domainSet (), "; ", g ) +
					setStr ( "RX=", m.rangeSet (), "; ", g ) +
					setStr ( "At=", m.attachedSet (), "; ", g ) +
					setStr ( "Gp=", m.groupedSet (), "; ", g ) +
					setStr ( "GpP=", m.groupedToSet (), "; ", g ) +
					setStr ( "AtP=", m.attachedToSet (), ". ", g );
		}

		fileNum.println ( msg );
	}

	private static String strStr ( String f, String s, String t ) {
		if ( s == null || "".equals ( s ) ) {
			return "";
		} else {
			return f + s + t;
		}
	}

	private static String itemStr ( String f, DMIElem m, String t, DMIGraph g ) {
		if ( m == null ) {
			return "";
		} else {
			return f + m.itsIndex + strStr ( "<", NameUtilities.firstName ( m, g, true, true ), ">" ) + t;
		}
	}

	private static String setStr ( String f, ISet<DMIElem> mV, String t, DMIGraph g ) {
		String a = "";
		for ( int i = 0; i < mV.size (); i++ ) {
			DMIElem m = mV.get ( i );
			if ( m != null )
				a += "," + m.itsIndex + strStr ( "<", NameUtilities.firstName ( m, g, true, true ), ">" );
		}
		if ( "".equals ( a ) ) {
			return "";
		} else {
			return f + Strings.Mid ( a, 2 ) + t;
		}
	}

	/*
	private static String NumSetStr(String f, int[] lL, String t) {
		String a = "";
		for ( int i = 0; i < lL.length; i++ ) {
			a = a + "," + lL[i];
		}
		if ( "".equals(a) ) {
			return "";
		} else {
			return f + Strings.Mid(a, 2) + t;
		}
	}
	*/

	private static void hashAnalysis ( PrintStream fileNum, String title, DMIHashTable ht ) {
		DMIHashBucketCollisionList [] hcb = ht.hashCollisionBuckets ();

		int mtCount = 0;
		int ttCount = 0;
		int lgCount = 0;
		int ccCount = 0;

		Double avg;

		for ( int i = 0; i < hcb.length; i++ ) {
			DMIHashBucketCollisionList bc = hcb [ i ];
			if ( bc == null ) {
				mtCount = mtCount + 1;
			} else {
				int n = bc.getSNL ().size (); // bc.Count ();
				ttCount += n;
				ccCount += n - 1;
				if ( n > lgCount ) {
					lgCount = n;
				}
			}
		}

		if ( ttCount > 0 ) {
			avg = (double) ttCount;
			avg = avg / (hcb.length - mtCount);
		} else {
			avg = 0.0;
		}

		fileNum.println ( "---- HashTable: " + title + " ----" );
		fileNum.println ( "  Bucket Count  = " + hcb.length );
		fileNum.println ( "  Empty Buckets = " + mtCount );
		fileNum.println ( "  Total Count   = " + ttCount );
		fileNum.println ( "  Collision Cnt = " + ccCount );
		fileNum.println ( "  Longest Chain = " + lgCount );
		fileNum.println ( "  Average Chain = " + avg );
		fileNum.println ( "" );
	}


}
