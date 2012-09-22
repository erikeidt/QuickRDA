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

import com.hp.JEB.*;
import com.hp.QuickRDA.Excel.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;

public class Dropdowns {

	private static class DropDownSource {
		String		name;
		String []	values;
	}

	private static class DropDownTarget {
		Range	targetRange;
		int		targetColNum;
		int		sourceIndex;
	}

	private List<DropDownSource>	ddValidationSources;
	private List<DropDownTarget>	ddValidationTargets;

	public void generateDropdowns ( Builder bldr, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems ) {

		for ( ;; ) {
			ddValidationSources = null;
			ddValidationTargets = null;
			Workbook wkb = null;

			// for ( int i = 0; i < bldr.itsHeaders.length; i++ ) {
			//	TableReader tb = bldr.itsHeaders [ i ];
			// for ( XIterator<TableReader> it = bldr.itsHeaders.setListIterator (); it.hasNext (); ) {
			//	TableReader tb = it.next ();
			for ( int i = 0; i < bldr.itsHeaders.size (); i++ ) {
				TableReader tb = bldr.itsHeaders.get ( i );
				if ( tb != null ) {
					if ( wkb == null )
						wkb = tb.itsWkb;
					else if ( tb.itsWkb != wkb && !tb.itsWkb.Name ().equals ( wkb.Name () ) )
						continue;
					tb.SetRow ( 1 );
					Range sutR = tb.itsRange;

					try {
						// clear all validations on sheet, whether or not there are validation sources on this sheet, or targets in this workbook. 
						// to work around excel inter workbook copy & paste issues, that give:
						// 	Security Warning   Automatic update of links has been disabled   Enable Content
						tb.itsWks.Cells ().Validation ().Delete ();
					} catch ( Exception e ) {}

					generateDropdownsForTable ( tb.itsWks, sutR, inclDomainModelItems, inclUnderlyingMetaModelItems, bldr );
					bldr.itsHeaders.clear ( i ); /* why bother?? */
				}
			}

			if ( wkb == null )
				break;

			writeSources ( wkb );
			writeTargets ( wkb );

		}

		// bldr.Destroy();
	}

	private void generateDropdownsForTable ( Worksheet wks, Range hdrR, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems, Builder bldr ) {
		TableReader headerTab = new TableReader ( hdrR, SourceUnitReader.kTableToFirstRowOffset, TableReader.FirstAllRestVisibleByFirst | TableReader.TrackRows );
		int cxc = hdrR.Columns ().Count ();
		for ( int cx = 1; cx <= cxc; cx++ )
			generateDropdownsForColumn ( wks, headerTab, cx, inclDomainModelItems, inclUnderlyingMetaModelItems, bldr );
	}

	private void generateDropdownsForColumn ( Worksheet wks, TableReader headerTab, int c, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems, Builder bldr ) {
		String t = SourceUnitReader.columnTypeValue ( headerTab, c );

		//don't handle typeless columns in this format; skip whole column
		if ( "".equals ( t ) )
			return;

		//Set te = bldr.conceptMgr.FindConcept(t, bldr.baseVocab.gConcept)
		DMIElem te = TypeExpressions.getTypeFromNameExpression ( t, bldr.itsConceptMgr, bldr.itsBaseVocab );

		if ( te != null ) {
			int ddSourceIndex = -1;
			if ( ddValidationSources != null )
				ddSourceIndex = findInList ( NameUtilities.getMCText ( te ), ddValidationSources );
			if ( ddSourceIndex < 0 ) {
				DropDownSource ddSrc = setupDDSource ( te, inclDomainModelItems, inclUnderlyingMetaModelItems, bldr );
				if ( ddSrc != null ) {
					if ( ddValidationSources == null )
						ddValidationSources = new XSetList<DropDownSource> ();
					ddSourceIndex = ddValidationSources.size ();
					ddValidationSources.add ( ddSrc );
					// dds.col = u;
				}
			}

			if ( ddSourceIndex >= 0 && ddValidationSources.get ( ddSourceIndex ).values.length > 0 ) {
				// no need to capture targets whose sources are zero length
				if ( ddValidationTargets == null )
					ddValidationTargets = new XSetList<DropDownTarget> ();

				DropDownTarget ddTrg = new DropDownTarget ();
				ddTrg.targetRange = headerTab.itsRange;
				ddTrg.targetColNum = c;
				ddTrg.sourceIndex = ddSourceIndex;

				ddValidationTargets.add ( ddTrg );
			}
		}
	}

	private static DropDownSource setupDDSource ( DMIElem tm, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems, Builder bldr ) {
		ISet<DMIElem> mV = null;

		if ( tm.instanceOf ( bldr.itsBaseVocab.gProperty ) ) {
			if ( tm.isAbstractProperty () ) {
				mV = findPropertiesMatchingAbstractType ( tm, inclDomainModelItems, inclUnderlyingMetaModelItems, bldr );
			} else {
				mV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
				mV.addToSet ( tm );
			}
		} else if ( tm.instanceOf ( bldr.itsBaseVocab.gClass ) ) {
			DMIElementDiagrammingInfo di = tm.itsDiagrammingInfo;
			if ( di == null || !"0".equals ( di.itsDDViz ) ) {
				mV = getAllInstances ( tm, inclDomainModelItems, inclUnderlyingMetaModelItems, bldr );
				if ( tm != bldr.itsBaseVocab.gboolean ) {
					NameUtilities.sortList ( mV, NameUtilities.SortKindEnum.SortByLCName );
				}
			}
		}

		//leave sorted by index not name, this allows custom sorting via metamodel placement
		//so more popular ones can be sorted to the top...
		//SortListByName mL

		DropDownSource dds = null;
		if ( mV != null ) {
			dds = new DropDownSource ();
			dds.name = NameUtilities.getMCText ( tm );
			dds.values = new String [ mV.size () ];
			for ( int i = 0; i < mV.size (); i++ ) {
				DMIElem m = mV.get ( i );
				if ( m != null )
					dds.values [ i ] = NameUtilities.getMCText ( m );
			}
		}
		return dds;
	}

	private static ISet<DMIElem> getAllInstances ( DMIElem tm, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems, Builder bldr ) {
		@SuppressWarnings("unchecked")
		ISet<DMIElem> ans = (ISet<DMIElem>) tm.itsFullInstanceSet.clone ();

		for ( int i = 0; i < ans.size (); i++ ) {
			DMIElem m = ans.get ( i );
			if ( m != null ) {
				DMIElementDiagrammingInfo di = m.itsDiagrammingInfo;
				if ( di == null ) {
					if ( m.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) && !inclDomainModelItems ) {
						ans.clear ( i );
					}
				} else {
					if ( "1".equals ( di.itsDDViz ) )
						;
					else if ( "0".equals ( di.itsDDViz ) || (m.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) && !inclDomainModelItems) ) {
						ans.clear ( i );
					}
				}
			}
		}

		ans.trim ();

		return ans;
	}

	// Any matches of domain + range pair, exact or subclasses
	// dV, rV belong to an Property, we know that the caller has supplied us with an Abstract Property
	// and therefore, there is unlikely to be more than one domain & range in each set.
	//
	private static ISet<DMIElem> findPropertiesMatchingAbstractType ( DMIElem tm, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems, Builder bldr ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		/*
		ISet<DMIElem> abstractPropertyDomainSet = tm.DomainSet();
		ISet<DMIElem> abstractPropertyRangeSet = tm.RangeSet();
		*/

		ISet<DMIElem> qV = bldr.itsBaseVocab.gProperty.itsFullInstanceSet;

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {

				boolean x = m.isAbstractProperty ();
				DMIElementDiagrammingInfo di = m.itsDiagrammingInfo;
				if ( !x ) {
					if ( di != null ) {
						DMIElem [] dvl = di.getDDVizSet ();
						if ( dvl.length > 0 ) { // if dvl is empty then there are no explicit restrictions
							x = (lang.indexOf ( tm, dvl ) < 0); // dvl is a restricting list of allowable appearances
						}
					}
				}

				if ( !x && di != null ) {
					x = "0".equals ( di.itsDDViz ); // force exclusion if so specified in the metamodel
				}

				boolean y = false;
				if ( !x ) {
					if ( di != null )
						y = "1".equals ( di.itsDDViz ); // force inclusion if so specified in the metamodel
				}

				if ( y || !x && ((m.itsSubgraph.isAccessible ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) || inclUnderlyingMetaModelItems) && (!m.itsSubgraph.isVisible || inclDomainModelItems)) ) {
					boolean t = TypeRelations.propertyIsApplicableToTargetType ( m, tm );
					/*
					boolean t = Types.ConcreteSetAppliesToAbstractSet(m.DomainSet(), abstractPropertyDomainSet);
					if ( x ) {
						t = Types.ConcreteSetAppliesToAbstractSet(m.RangeSet(), abstractPropertyRangeSet);
					}
					*/
					if ( t )
						ans.addToSet ( m );
				}
			}
		}

		ans.sortList ( cmp ); // reverse sort by metamodel depth: show underlying metamodel items after domain model items.

		return ans;
	}

	private static class NameComparator extends DMIElem.Comparator {
		// reverse sort by metamodel depth: show underlying metamodel items after domain model items.
		// otherwise (when equal) leave order as is, so the dropdowns appear in the order declared in the metamodel.
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			return e1.itsSubgraph.levelAsNumber () < e2.itsSubgraph.levelAsNumber ();
		}
	}

	private static DMIElem.Comparator	cmp	= new NameComparator ();

	public void writeSources ( Workbook wkb ) {
		if ( ddValidationSources != null ) {
			String ddTab = "";

			//for ( int i = 0; i < ddValidationSources.length; i++ ) {
			//	DropDownSource dds = ddValidationSources [ i ];
			for ( DropDownSource dds : ddValidationSources ) {
				ddTab = ddTab + dds.name + '\t';
				for ( int j = 0; j < dds.values.length; j++ ) {
					ddTab = ddTab + dds.values [ j ] + '\t';
				}
				ddTab = ddTab + '\r';
			}
			wkb.SetDropDownSources ( ddTab );
		}
	}

	public void writeTargets ( Workbook wkb ) {
		if ( ddValidationTargets != null ) {
			//for ( int i = 0; i < ddValidationTargets.length; i++ ) {
			//	DropDownTarget ddt = ddValidationTargets [ i ];
			for ( DropDownTarget ddt : ddValidationTargets )
				ddt.targetRange.SetValidation ( wkb, ddt.targetColNum, ddt.sourceIndex + 1, ddValidationSources.get ( ddt.sourceIndex ).values.length );
		}
	}

	public static int findInList ( String name, List<DropDownSource> ddss ) {
		if ( ddss != null ) {
			//for ( int i = 0; i < ddss.length; i++ ) {
			//	if ( name.equals (ddss [ i ].name) )
			int i = 0;
			for ( DropDownSource dds : ddss ) {
				if ( name.equals ( dds.name ) )
					return i;
				i++;
			}

		}
		return -1;
	}

}
