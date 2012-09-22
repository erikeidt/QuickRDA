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

import java.util.List;

import com.hp.QuickRDA.Excel.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;

public class MetamodelReader {

	private static final int	kFlagID				= 2;
	private static final int	kFirstRealColumn	= 3;	//Skip Level(@1) + Flag#ID(@2)

	//
	// Metamodel reader for reading metamodel tables.
	// Makes the presumption that serializations are unique
	// and that names are not arbitrarily replicated.
	// Follows these assumptions.
	// This reader is not necessarily intended to read into
	//   an empty not a pre-populated subgraph.
	//
	//

	private static class MMBoot {
		int		r;
		@SuppressWarnings("unused")
		String	qualStr;
		@SuppressWarnings("unused")
		String	nameStr;
		DMIElem	m;
		//DMIElem nm
	}

	private DMIGraph			itsGraph;
	private DMIBaseVocabulary	itsBaseVocab;
	private DMISubgraph			itsSubgraph;
	//private DMINameCache nc
	private ConceptManager		itsConceptMgr;
	private boolean				itsUnderlyingMetamodel;

	public void bindToConceptManagerConfiguration ( ConceptManager cm ) {
		itsConceptMgr = cm;
		itsSubgraph = cm.itsCurrSG;
		itsGraph = itsSubgraph.itsGraph;
		itsBaseVocab = itsGraph.itsBaseVocab;
	}

	public void buildFromMMTable ( TableReader mTab, int lvMin, int lvMax ) {
		/*
		Range hdrR = tbl.Range();

		Range tblR = tbl.Range();
		tblR = tblR.Offset(1);
		tblR = tblR.Resize(tblR.Rows().Count() - 1);
		*/

		itsUnderlyingMetamodel = (lvMax < 3);

		List<MMBoot> mLR = new XSetList<MMBoot> ();

		int rxc = mTab.RowLast (); // tblR.Rows().Count();
		for ( int r = 1; r <= rxc; r++ ) {
			String ss = mTab.GetValue ( r, 1 ); // tblR.Cells(r,1).Value();
			if ( "".equals ( ss ) )
				continue;
			int lv = java.lang.Integer.parseInt ( ss );
			if ( lv >= lvMin && lv <= lvMax ) {
				MMBoot mmb = new MMBoot ();
				mmb.r = r;
				mLR.add ( mmb );
				DMIElem m = new DMIElem ();
				itsSubgraph.addConcept ( m );

				// #if ( namesFirst ) {
				/*           
							if ( "Name".equals(tblR.Cells(r, kInstanceOfColumn))  ) {
								mLR(u).nm = m;
							} else {
								mLR(u).m = m;
							}
				*/
				//#} else {
				mmb.m = m;
				//#}
				String x = mTab.GetValue ( r, kFlagID ); // tblR.Cells(r, kFlagID).Value();
				if ( !"".equals ( x ) ) {
					itsBaseVocab.setBuiltIn ( m, java.lang.Integer.parseInt ( x ) );
				}
			}
		}

		int jx0;
		jx0 = kFirstRealColumn;

		int jx1 = 0;
		int jx2 = 0;
		int jx3 = 0;
		int jx4 = 0;
		// int jx5 = 0;

		//		for ( int ix = 0; ix < mLR.length; ix++ )
		//			jx1 = AssembleNames (mTab, mLR [ ix ], jx0);
		//
		//		for ( int ix = 0; ix < mLR.length; ix++ )
		//			jx2 = AssembleQualities0 (mTab, mLR [ ix ], jx1);
		//
		//		for ( int ix = 0; ix < mLR.length; ix++ )
		//			jx3 = AssembleQualities1 (mTab, mLR [ ix ], jx2);
		//
		//		for ( int ix = 0; ix < mLR.length; ix++ )
		//			jx4 = AssembleQualities2 (mTab, mLR [ ix ], jx3);
		//
		//		for ( int ix = 0; ix < mLR.length; ix++ )
		//			/* jx5 = */AssembleQualities3 (mTab, mLR [ ix ], jx4);

		for ( MMBoot mmb : mLR )
			jx1 = assembleNames ( mTab, mmb, jx0 );

		for ( MMBoot mmb : mLR )
			jx2 = assembleQualities0 ( mTab, mmb, jx1 );

		for ( MMBoot mmb : mLR )
			jx3 = assembleQualities1 ( mTab, mmb, jx2 );

		for ( MMBoot mmb : mLR )
			jx4 = assembleQualities2 ( mTab, mmb, jx3 );

		for ( MMBoot mmb : mLR )
			/* jx5 = */assembleQualities3 ( mTab, mmb, jx4 );

	}

	private int assembleNames ( TableReader mTab, MMBoot mr, int ic ) {
		int r = mr.r;

		// String xx = tblR.Cells(r, 1).Address();

		String qualStr = mTab.GetValue ( r, ic );
		ic = ic + 1;

		String nameStr = mTab.GetValue ( r, ic );
		ic = ic + 1;

		mr.qualStr = qualStr;
		mr.nameStr = nameStr;

		// #if ( namesFirst ) {
		/*
			if ( "".equals(mr.nameStr)  ) {
				m = cm.NewConcept();
			} else {
				if ( "Name".equals(tblR.Cells(r, kInstanceOfColumn).Value())  ) {
					m = cm.EnterTypedConcept(nameStr, b.gName, mr.nm);
				} else {
					m = cm.EnterConcept(nameStr, b.gConcept, mr.m);
				}
			}
			mr.m = m;
		*/
		// #} else {
		if ( "".equals ( nameStr ) ) {
			//Set m = mr.m  'cm.NewConcept()
		} else {
			DMIElem m = itsConceptMgr.enterPreAllocatedConcept ( nameStr, mr.m );
			assert m == mr.m : "m (" + m.itsIndex + ") != mr.m (" + mr.m.itsIndex + ")";
		}
		// #}

		return ic;
	}

	private int assembleQualities0 ( TableReader mTab, MMBoot mr, int ic ) {
		int r = mr.r;

		DMIElem m = mr.m;

		String str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			m.itsDescription = str;
			//Set x = cm.EnterSerialization(str)
			//cm.EnterStatement m, b.gHasDescription, x
		}

		DMIElem [] xL = mmRefList ( mTab.GetValue ( r, ic ), itsBaseVocab.gClass );
		ic = ic + 1;
		for ( int i = 0; i < xL.length; i++ ) {
			itsConceptMgr.enterStatement ( m, itsBaseVocab.gInstanceOf, xL [ i ], false );
		}
		return ic;
	}

	private int assembleQualities1 ( TableReader mTab, MMBoot mr, int ic ) {
		int r = mr.r;
		DMIElem m = mr.m;

		String str;

		//As Class
		DMIElem [] xL = mmRefList ( mTab.GetValue ( r, ic ), itsBaseVocab.gClass );
		ic = ic + 1;
		for ( int i = 0; i < xL.length; i++ ) {
			itsConceptMgr.enterStatement ( m, itsBaseVocab.gSubclassOf, xL [ i ], false );
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( !"".equals ( str ) ) {
			DMISharedNameList snl = NameUtilities.getFirstName ( m );
			if ( snl != null )
				snl.itsPlural = str;
			//Dim DMIElem y
			//Set y = cm.EnterConcept(mr.nameStr, kName)
			//Set x = cm.EnterConcept(str, kName)
			//cm.EnterStatement y, b.gHasPlural, x
		}

		//As Formula
		//m.itsFormula = tblR.Cells(r, ic)
		ic = ic + 1;

		return ic;
	}

	private int assembleQualities2 ( TableReader mTab, MMBoot mr, int ic ) {
		int r = mr.r;
		DMIElem m = mr.m;

		//As Property
		DMIElem [] xL = mmRefList ( mTab.GetValue ( r, ic ), itsBaseVocab.gClass );
		ic = ic + 1;
		for ( int i = 0; i < xL.length; i++ ) {
			itsConceptMgr.enterStatement ( m, itsBaseVocab.gHasDomain, xL [ i ], false );
		}

		xL = mmRefList ( mTab.GetValue ( r, ic ), itsBaseVocab.gClass );
		ic = ic + 1;
		for ( int i = 0; i < xL.length; i++ ) {
			itsConceptMgr.enterStatement ( m, itsBaseVocab.gHasRange, xL [ i ], false );
		}

		return ic;
	}

	private int assembleQualities3 ( TableReader mTab, MMBoot mr, int ic ) {
		int r = mr.r;
		DMIElem m = mr.m;

		String str;
		DMIElem x;
		//m.itsMinCard = tblR.Cells(r, ic)
		ic = ic + 1;
		//m.itsMaxCard = tblR.Cells(r, ic)
		ic = ic + 1;

		String prefStr;
		prefStr = mTab.GetValue ( r, ic );
		ic = ic + 1;

		String invStr;
		invStr = mTab.GetValue ( r, ic );
		ic = ic + 1;

		if ( !"".equals ( prefStr ) || !"".equals ( invStr ) ) {
			ISet<DMIElem> xV = m.domainSet ();
			DMIElem dom = xV.get ( 0 );

			xV = m.rangeSet ();
			DMIElem rng = xV.get ( 0 );

			if ( prefStr != "" ) {
				//if ( "Is In Tower".equals(prefStr)  ) {
				//    Stop
				//}
				x = mmRefPX ( prefStr, dom, rng );
				if ( x != null ) {
					itsConceptMgr.enterStatement ( m, itsBaseVocab.gHasPreferred, x, false );
				}
			}

			if ( invStr != "" ) {
				x = mmRefPX ( invStr, rng, dom );
				if ( x != null ) {
					itsConceptMgr.enterStatement ( m, itsBaseVocab.gHasInverse, x, false );
				}
			}
		}

		//As Relation
		m.itsSubject = mmRef ( mTab.GetValue ( r, ic ), null );
		ic = ic + 1;
		m.itsVerb = mmRef ( mTab.GetValue ( r, ic ), null );
		ic = ic + 1;
		m.itsObject = mmRef ( mTab.GetValue ( r, ic ), null );
		ic = ic + 1;

		if ( m.itsVerb != null ) {
			itsGraph.itsCacheBuilder.DynamicCachingAndInferencing ( m );
		}

		//In Diagram
		DMIElementDiagrammingInfo d;
		d = null;

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsPriority = java.lang.Integer.parseInt ( str );
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsDDViz = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.setDDVizSet ( mmRefList ( str, itsBaseVocab.gClass ) );
		}

		// #if ( false ) {
		/*
			str = tblR.Cells(r, ic);
			ic = ic + 1;
			if ( str != "" ) {
				m.itsDiagParent = MMRef(str, b.gClass);
			}
		*/
		// #}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsDefaultViz = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsDisplay = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsContainment = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsDirection = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsBiDirect = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsArrowHead = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsArrowTail = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsWeight = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsRanking = str;
		}

		str = mTab.GetValue ( r, ic );
		int objClr = mTab.Cell ( ic ).InteriorColor ();
		int txtClr = mTab.Cell ( ic ).FontColor ();
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsShape = str;
			d.itsColor = objClr;
			d.itsTextColor = txtClr;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsReportEnumerate = ("Yes".equals ( str ));
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsReportAsSubject = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsReportAsObject = str;
		}

		str = mTab.GetValue ( r, ic );
		ic = ic + 1;
		if ( str != "" ) {
			d = DMIElementDiagrammingInfo.allocateIfNull ( d );
			d.itsReportOnFormula = str;
		}

		m.itsDiagrammingInfo = d;

		return ic;
	}

	private DMIElem mmRef ( String nameStr, DMIElem typ ) {
		//
		//$$$ TBD look for ::'s to find qualifiers
		//    as namespace
		//
		if ( itsUnderlyingMetamodel ) {
			// during boot we won't find the right concepts by type
			// so the underlying metamodel must have more unique names than other metamodels
			return checkMM ( nameStr, null, null, null );
		} else {
			return checkMM ( nameStr, typ, null, null );
		}
	}

	private DMIElem mmRefPX ( String nameStr, DMIElem d, DMIElem r ) {
		if ( itsUnderlyingMetamodel ) {
			// during boot we won't find the right concepts by type
			// so the underlying metamodel must have more unique names than other metamodels
			return checkMM ( nameStr, null, d, r );
		} else {
			return checkMM ( nameStr, itsBaseVocab.gProperty, d, r );
		}
	}

	private DMIElem [] mmRefList ( String n, DMIElem typ ) {
		String f = n;
		int u = 0;
		DMIElem [] mL = new DMIElem [ 0 ];

		StringRef xx = new StringRef ();
		for ( ;; ) {
			String MM = Strings.splitAfterComma ( f, xx ).trim ();
			f = xx.str;

			if ( MM != "" ) {
				DMIElem t = mmRef ( MM, typ );
				if ( t != null ) {
					mL = DMIElem.expandPreserve ( mL, u + 1 );
					mL [ u ] = t;
					u = u + 1;
				}
				if ( !"".equals ( f ) )
					continue;
			}
			break;
		}
		return mL;
	}

	private DMIElem checkMM ( String nameStr, DMIElem typ, DMIElem d, DMIElem r ) {
		DMIElem ans = null;

		if ( "".equals ( nameStr ) ) {} else {
			boolean add = false;

			String n = Strings.Replace ( nameStr, "::", "*" );

			int p = Strings.InStr ( n, ":" );

			if ( p == 1 || p == n.length () ) {
				//we're allowed to autoadd implicit concepts from the metamodel
				add = true;
			} else {
				add = false;
			}

			booleanRef added = new booleanRef ();

			ans = itsConceptMgr.findOrAddConceptOrProperty ( nameStr, typ, d, r, add, added );

			if ( added.bool ) {
				//note if add comes back true then it was added
				if ( p == 1 ) {
					//add description domain of the rest of the string
				} else {
					//add description range of the rest of the string
				}
			}

			if ( ans == null ) {
				if ( itsConceptMgr.isTypeExpression ( nameStr ) ) {
					ans = TypeExpressions.enterParsedType ( nameStr, itsConceptMgr );
				} else {
					assert false : "Couldn't find: '" + nameStr + "'";
					/*
					String a2 = MsgBox("Couldn't find: '" + nameStr + "'", vbOKCancel, "MetaModel (forward reference?) Error")
					if ( a2 == vbCancel ) {
						Stop;
					}
					*/
				}
			}
		}
		return ans;
	}

	public static MMBoot [] ExpandPreserve ( MMBoot [] v, int u ) {
		MMBoot [] ans = new MMBoot [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}


}
