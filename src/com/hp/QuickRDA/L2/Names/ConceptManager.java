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

package com.hp.QuickRDA.L2.Names;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;

public class ConceptManager {

	public static final boolean	kSingleProv	= true;

	public enum EntryRequestTypeEnum {
		kSerialization, // name string
		kName, // concept that is a Name
		kConcept
		// concept that has a Name
	}

	public DMISubgraph			itsCurrSG;					//current/main subgraph for additions to the graph
	public DMIView				itsCurrVW;					//current/main view for additions to the graph

	public DMISubgraph			itsNamesSG;
	public DMISubgraph			itsAbstrSG;				//abstract property subgraph
	public DMISubgraph			itsProvSG;					//provenance info subgraph
	public DMISubgraph			itsMetaSG;					// crosses domain model and domain language boundaries
	public DMISubgraph			itsInvisiblexSG;			//invisible, can be moved
	public DMISubgraph			itsVisiblexSG;				//visible, can be moved

	public DMIBaseVocabulary	itsBaseVocab;

	public DMIGraph				itsGraph;
	private DMIHashTable		itsHT;

	private boolean				itsDoBolding;				//highlight feature
	private DMIElem				itsCurrentProvenanceWkb;	//Current Source Unit as Name Concept (worksheet qualified by workbook)
	private DMIElem				itsCurrentProvenanceWks;	//Current Source Unit as Name Concept (worksheet qualified by workbook)
	private int					itsCurrentProvenanceRow;	//Current row number in worksheet
	private ProvenanceInfo		itsCurrentProvenanceRec;	//A record holding the current, if there is one at present, null otherwise


	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++; //self
		mrc.ptrCount += 8; //subgraph ptrs
		mrc.ptrCount += 6; //other ptrs
		mrc.atrCount += 1;
		itsHT.memUsage ( mrc );
	}

	public ConceptManager () {
		itsHT = new DMIHashTable ();
	}

	public void bindToSubgraphConfiguration ( DMISubgraph currSG1, DMISubgraph namesSG1, DMISubgraph abstrSG1, DMISubgraph provSG1, DMISubgraph visibleSG1, DMISubgraph invisibleSG1, DMISubgraph metaSG1 ) {
		itsCurrSG = currSG1;
		itsGraph = itsCurrSG.itsGraph;
		itsBaseVocab = itsGraph.itsBaseVocab;
		itsNamesSG = namesSG1;
		itsAbstrSG = abstrSG1;
		itsProvSG = provSG1;
		itsVisiblexSG = visibleSG1;
		itsInvisiblexSG = invisibleSG1;
		itsMetaSG = metaSG1;
	}

	public DMIView trackAdditionsInView ( DMIView vw ) {
		DMIView t = itsCurrVW;
		itsCurrVW = vw;
		return t;
	}

	public DMISubgraph setDefaultSubgraph ( DMISubgraph sg ) {
		DMISubgraph t = itsCurrSG;
		itsCurrSG = sg;
		return t;
	}

	public DMIElem enterPreAllocatedConcept ( String nameStr, DMIElem ansIn ) {
		return enterNamedConceptEx ( nameStr, itsBaseVocab.gConcept, true, DMISharedNameList.kHasName, null, null, true, null, ansIn );
	}

	public DMIElem findConcept ( String nameStr, DMIElem typ ) {
		DMIElem ans;
		ans = isSelectConstant ( nameStr, typ );
		if ( ans != null ) {
			return ans;
		} else {
			return enterNamedConceptEx ( nameStr, typ, true, DMISharedNameList.kHasName, null, null, false, null, null );
		}
	}

	public DMIElem findOrAddConceptOrProperty ( String nameStr, DMIElem typ, DMIElem dom, DMIElem rng, boolean add, booleanRef added ) {
		return enterNamedConceptEx ( nameStr, typ, true, DMISharedNameList.kHasName, dom, rng, add, added, null );
	}

	public DMIElem enterUntypedConcept ( String nameStr ) {
		return enterNamedConcept ( nameStr, itsBaseVocab.gConcept, true, true, null );
	}

	public DMIElem enterCloneOfConcept ( String name, DMIElem cloneOf ) {
		DMIElem nb = enterTypedConcept ( name, cloneOf.itsDeclaredTypeList.get ( 0 ), true );

		if ( cloneOf.itsDeclaredTypeList.size () > 1 ) {
			for ( int i = 1; i < cloneOf.itsDeclaredTypeList.size (); i++ )
				enterStatementEx ( nb, itsBaseVocab.gInstanceOf, cloneOf.itsDeclaredTypeList.get ( i ), itsMetaSG );
		}

		return nb;
	}

	// Optional declType = true;
	public DMIElem enterTypedConcept ( String nameStr, DMIElem typ, boolean declType ) {
		DMIElem ans;
		ans = null;

		ans = isSelectConstant ( nameStr, typ );
		if ( ans == null ) {
			if ( typ != null && typ.subclassOf ( itsBaseVocab.gValue ) )
				ans = enterSerialization ( nameStr, typ );
			else
				ans = enterNamedConcept ( nameStr, typ, false, true, null );
			if ( declType && !ans.instanceOf ( typ ) )
				enterStatementEx ( ans, itsBaseVocab.gInstanceOf, typ, itsMetaSG );

		}

		return ans;
	}

	public DMIElem enterStatement ( DMIElem sb, DMIElem vb, DMIElem ob, boolean autohide ) {
		DMIElem ans = null;

		if ( autohide && vb.itsDiagrammingInfo != null && "0".equals ( vb.itsDiagrammingInfo.itsDefaultViz ) )
			ans = enterStatementEx ( sb, vb, ob, itsInvisiblexSG );
		else
			ans = enterStatementInternal ( sb, vb, ob );

		return ans;
	}

	public DMIElem enterProperty ( String nameStr, DMIElem dom, DMIElem rng, boolean isAbstract, booleanRef added ) {
		DMIElem ans = null;

		if ( added == null )
			added = new booleanRef ();

		DMIElem typ = itsBaseVocab.gProperty;
		if ( isAbstract )
			typ = itsBaseVocab.gAbstractProperty;

		ans = enterNamedConceptEx ( nameStr, typ, true, DMISharedNameList.kHasName, dom, rng, true, added, ans );
		//Note: add is out ByRef, and will only be true if it was added

		if ( added.bool ) {
			if ( isAbstract ) // no need to set type if not abstract, it will happen by setting domain or range
				enterStatementEx ( ans, itsBaseVocab.gInstanceOf, typ, itsAbstrSG );
			enterStatementEx ( ans, itsBaseVocab.gHasDomain, dom, itsAbstrSG );
			enterStatementEx ( ans, itsBaseVocab.gHasRange, rng, itsAbstrSG );
		}

		return ans;
	}

	public DMIElem enterAbstractPropertyByNameDomainRange ( String nameStr, DMIElem dom, DMIElem rng ) {
		DMISubgraph saveSG = setDefaultSubgraph ( itsAbstrSG );
		DMIElem t = enterProperty ( nameStr, dom, rng, true, null );
		setDefaultSubgraph ( saveSG );
		return t;
	}

	public boolean isTypeExpression ( String typeStr ) {
		return TypeExpressions.isTypeExpression ( typeStr );
	}

	public void setHighlight ( boolean x ) {
		itsDoBolding = x;
	}

	public void setProvenanceInfo ( String qualStr, String nameStr, int rowNum ) {
		clearProvenance ();

		DMISubgraph svSG = setDefaultSubgraph ( itsProvSG );

		//Could Save, + Restore default subgraph, this is one of the few places where this is needed...
		DMIElem w = enterSerialization ( qualStr, null );

		DMIElem m = enterSerialization ( nameStr, null );

		//EnterStatement m, b.gHasQualifier, w, sg:=provSG

		setDefaultSubgraph ( svSG );

		itsCurrentProvenanceWkb = w;
		itsCurrentProvenanceWks = m;
		itsCurrentProvenanceRow = rowNum;
	}

	public void incProvenanceRow () {
		itsCurrentProvenanceRow++;
		itsCurrentProvenanceRec = null;
	}

	public void addProvenance ( DMIElem m ) {
		if ( m.itsSubgraph.itsSupportsProvenance && (itsCurrentProvenanceWkb != null || itsCurrentProvenanceRec != null) ) {
			if ( itsCurrentProvenanceRec == null ) {
				itsCurrentProvenanceRec = new ProvenanceInfo ( itsCurrentProvenanceWkb, itsCurrentProvenanceWks, itsCurrentProvenanceRow );
			}
			m.setProvenance ( itsCurrentProvenanceRec, itsDoBolding );
		}
	}

	// #if ( kSingleProv ) {
	public void setProvenanceInfoEx ( DMIElem m ) {
		itsCurrentProvenanceRec = m.provInfo ();
		itsDoBolding = m.itsBoldTag;
	}

	// #} else {
	/*
	public void AddProvenanceToFrom(DMIElem m, DMIElem from) {
		AddProvenance m;
		if ( from != null ) {
			m.ProvInfo.MergeProvenance from;
		}
	}
	*/
	// #}

	public void clearProvenance () {
		itsCurrentProvenanceWkb = null;
		itsCurrentProvenanceWks = null;
		itsCurrentProvenanceRow = 0;
		itsCurrentProvenanceRec = null;
		itsDoBolding = false;
	}

	public DMIElem enterName ( String nameStr, boolean declType ) {
		return enterNamedConceptEx ( nameStr, itsBaseVocab.gName, true, DMISharedNameList.kIsName, null, null, true, null, null );
	}

	//
	//--------------------------------------------------------------------------------------------------------------
	//
	private DMIElem isSelectConstant ( String nameStr, DMIElem typ ) {
		DMIElem ans = null;

		if ( typ != null ) {
			if ( typ == null || typ == itsBaseVocab.gboolean ) {
				if ( nameStr.length () > 0 ) {
					switch ( nameStr.toLowerCase ().charAt ( 0 ) ) {
					case 't' :
						ans = itsBaseVocab.gtrue;
						break;
					case '1' :
						ans = itsBaseVocab.gtrue;
						break;
					case 'y' :
						ans = itsBaseVocab.gtrue;
						break;
					case 'x' :
						ans = itsBaseVocab.gtrue;
						break;
					case 'f' :
						ans = itsBaseVocab.gfalse;
						break;
					case '0' :
						ans = itsBaseVocab.gfalse;
						break;
					case 'n' :
						ans = itsBaseVocab.gfalse;
						break;
					}
				}
			} else if ( typ == itsBaseVocab.gNothing ) {
				ans = itsBaseVocab.gNothing; // nothing is nothing, enter no new instance node
				//Stop
			}
		}

		return ans;
	}

	private DMIElem enterStatementEx ( DMIElem sb, DMIElem vb, DMIElem ob, DMISubgraph sg ) {
		DMISubgraph defSG = setDefaultSubgraph ( itsMetaSG );
		DMIView defVW = trackAdditionsInView ( null );

		DMIElem t = enterStatementInternal ( sb, vb, ob );

		if ( defSG != null )
			setDefaultSubgraph ( defSG );
		if ( defVW != null )
			trackAdditionsInView ( defVW );

		return t;
	}

	private DMIElem enterStatementInternal ( DMIElem sb, DMIElem vb, DMIElem ob ) {
		DMIElem x = null;

		assert sb.itsIndex != 0 : "DMISubgraph.EnterStatement: sb has no Index";
		assert vb.itsIndex != 0 : "DMISubgraph.EnterStatement: vb has no Index";
		assert ob.itsIndex != 0 : "DMISubgraph.EnterStatement: ob has no Index";

		//search shorter lists, and, btw, these list will search the whole graph
		if ( ob.itsUsedAsObjectStmtSet.size () <= sb.itsUsedAsSubjectStmtSet.size () ) {
			x = ob.find1StatementFromUsedAsObjectBySubjectAndVerb ( sb, vb, null ); //$$$TBD TODO, set exact here
		} else {
			x = sb.find1StatementFromUsedAsSubjectByVerbAndObject ( vb, ob, null ); //$$$TBD TODO, set exact here
		}
		if ( x == null ) {
			x = new DMIElem ();
			x.setStatement ( sb, vb, ob );
			itsCurrSG.addConcept ( x );
			if ( itsCurrVW != null ) {
				itsCurrVW.addToView ( x );
			}
		}
		addProvenance ( x );
		return x;
	}

	private DMIElem enterNamedConcept ( String nameStr, DMIElem typ, boolean exact, boolean add, booleanRef added ) {
		DMIElem z = null;

		DMISharedNameList snl = itsHT.associate ( nameStr, add );

		if ( snl != null ) {
			if ( added == null )
				added = new booleanRef ();
			z = snl.findConcept ( typ, exact, DMISharedNameList.kHasName, add, added );
			if ( added.bool ) {
				itsCurrSG.addConcept ( z );
				if ( itsCurrVW != null ) {
					itsCurrVW.addToView ( z );
				}

				//setting instance of is now caller's job
				//if ( typ == null ) {
				//} else if ( typ == b.gConcept ) {
				//    'Everything is a Concept, no need to make a statement for that
				//} else if ( declType ) {
				//    EnterStatementEx z, b.gInstanceOf, typ, hiddenSG
				//}
			}
		}

		if ( z != null ) {
			addProvenance ( z );
		}

		return z;
	}

	//
	// Same as EnterNamedConcept, (pass null for domain + range, and ansIn)
	//   but in addition, can
	//	         (1) find properties qualified by domain + range
	//	         (2) supports pre-defined ansIn
	//   ...Since the caller always knows whether their doing domain + range, and have a predefined DMIElem to use
	//	         we could have 3 routines for this instead of one...
	//
	private DMIElem enterNamedConceptEx ( String nameStr, DMIElem typ, boolean exact, int assn, DMIElem dom, DMIElem rng,
			boolean add, booleanRef added, DMIElem ansIn ) {
		DMIElem z = null;

		DMISharedNameList snl = itsHT.associate ( nameStr, add );

		if ( snl != null ) {
			if ( added == null )
				added = new booleanRef ();
			//if ( nameStr.equals("Page To Page") ) {
			//	int a = 1;
			//}
			z = snl.findConceptEx ( typ, exact, assn, dom, rng, add, added, ansIn );
			if ( added.bool ) {
				if ( ansIn == null ) {
					itsCurrSG.addConcept ( z );
					if ( itsCurrVW != null ) {
						itsCurrVW.addToView ( z );
					}
				} else {
					//we presume that if supplied with the node then it is in the graph
				}

				//setting instance of is now caller's job
				//if ( typ == null ) {
				//} else if ( typ == b.gConcept ) {
				//    'Everything is a Concept, no need to make a statement for that
				//} else {
				//    EnterStatementEx z, b.gInstanceOf, typ, hiddenSG
				//}
			}
		}

		if ( z != null ) {
			addProvenance ( z );
		}

		return z;
	}

	public DMIElem enterSerialization ( String nameStr, DMIElem typ ) {
		DMIElem ans = null;

		DMISharedNameList snl = itsHT.associate ( nameStr, true );
		if ( snl != null ) {
			boolean add = true;
			booleanRef added = new booleanRef ();
			if ( typ == null )
				typ = itsBaseVocab.gStringSerialization;
			ans = snl.findConcept ( typ, true, DMISharedNameList.kSerialization, add, added );
			if ( added.bool ) {
				ans.setSerialization ( nameStr );
				itsCurrSG.addConcept ( ans );
				if ( itsCurrVW != null ) {
					itsCurrVW.addToView ( ans );
				}
			}
		}

		return ans;
	}

	public DMIElem findNNConcept ( String nameStr, ISet<DMIElem> avoid, DMISubgraph.SubgraphLevelEnum lv ) {
		DMISharedNameList snl = itsHT.associate ( nameStr, false );
		if ( snl != null ) {
			int cnt = snl.conceptCount ();
			for ( int i = 0; i < cnt; i++ ) {
				DMIElem m = snl.conceptAt ( i );
				if ( !m.subclassOf ( itsBaseVocab.gName ) && !m.subclassOf ( itsBaseVocab.gSerialization ) &&
						avoid.indexOf ( m ) < 0 && m.itsSubgraph.atLevel2 ( lv ) ) {
					return m;
				}
			}
		}
		return null;
	}

	// for debugging
	public DMIHashTable getHT () {
		return itsHT;
	}

	public DMIElem newConcept () {
		DMIElem x = new DMIElem ();
		itsCurrSG.addConcept ( x );
		if ( itsCurrVW != null ) {
			itsCurrVW.addToView ( x );
		}
		addProvenance ( x );
		return x;
	}

}
