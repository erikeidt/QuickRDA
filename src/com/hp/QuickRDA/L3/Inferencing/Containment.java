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

package com.hp.QuickRDA.L3.Inferencing;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L4.Build.BuildOptions; // TODO: bad layering: we just need 4 of the options

public class Containment {

	public static void containmentInferencing ( DMIBaseVocabulary baseVocab, BuildOptions options, DMIView vw ) {
		// LinkAndSortTimeline(baseVocab.gTimeline, baseVocab.gTimelineDate, vw);
		//find all attachments by the attached to arcs
		//add the attachments to their parents so they can be graphed there
		//mark their index as different, for DOT, e.g. parent:attachment
		//suppress themselves - just the arc, so they don't graph the "attach to" arc
		if ( options.gOptionAttach ) {
			crossLinkingAndEdgeSuppression ( baseVocab.gIsAttachedTo, true, true, options.gOptionNestedGroups, baseVocab, options.gOptionAutoHide, vw );
			crossLinkingAndEdgeSuppression ( baseVocab.gAttaches, false, true, options.gOptionNestedGroups, baseVocab, options.gOptionAutoHide, vw );
		}
		if ( options.gOptionGroup ) {
			crossLinkingAndEdgeSuppression ( baseVocab.gIsInGroup, true, false, options.gOptionNestedGroups, baseVocab, options.gOptionAutoHide, vw );
			crossLinkingAndEdgeSuppression ( baseVocab.gGroups, false, false, options.gOptionNestedGroups, baseVocab, options.gOptionAutoHide, vw );
		}
	}

	private static class ValueComparator extends DMIElem.Comparator {
		// reverse sort by metamodel depth: show underlying metamodel items after domain model items.
		// otherwise (when equal) leave order as is, so the dropdowns appear in the order declared in the metamodel.
		@SuppressWarnings("deprecation")
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			java.util.Date d1 = new java.util.Date ( e1.value () );
			java.util.Date d2 = new java.util.Date ( e2.value () );
			return d1.before ( d2 );
		}
	}

	private static DMIElem.Comparator	cmp	= new ValueComparator ();

	@SuppressWarnings("unused")
	private static void linkAndSortTimeline ( DMIElem aTimeLine, DMIElem timeLineDateClass, DMIView vw ) {
		ISet<DMIElem> tldi = timeLineDateClass.itsFullInstanceSet;

		if ( tldi.size () > 0 ) {
			tldi.sortList ( cmp );
			// timeLineDateClass.itsFullInstanceSet = tldi;
			for ( int i = 0; i < tldi.size (); i++ ) {
				DMIElem m = tldi.get ( i );
				// m.AddAttachTo(aTimeLine);
				m.addGroupTo ( aTimeLine );
			}
			vw.addToView ( aTimeLine );
		}
	}

	private static void crossLinkingAndEdgeSuppression ( DMIElem nodeType, boolean forwardOrBackward, boolean atOrgp, boolean nest, DMIBaseVocabulary baseVocab, boolean suppress, DMIView vw ) {
		ISet<DMIElem> qV = nodeType.itsFullInstanceSet;

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && vw.inView ( m ) ) { // InView 072011
				DMIElem sb = forwardOrBackward ? m.itsSubject : m.itsObject;
				DMIElem ob = forwardOrBackward ? m.itsObject : m.itsSubject;

				if ( vw.inView ( sb ) && vw.inView ( ob ) ) {
					boolean doit = true;
					if ( !nest ) {
						// $$$ TODO rework hard coded FirstElement's...
						doit = !(sb.itsDeclaredTypeList.get ( 0 ) == ob.itsDeclaredTypeList.get ( 0 ));
					}

					if ( doit ) {
						if ( atOrgp ) {
							sb.addAttachTo ( ob );
							if ( suppress ) {
								//SuppressDMINode m, hiddenSG
								vw.remove ( m );
							}
							crossLinkFollowers ( sb, ob, true, baseVocab, vw );
						} else {
							sb.addGroupTo ( ob );
							if ( suppress ) {
								//SuppressDMINode m, hiddenSG
								vw.remove ( m );
							}
							crossLinkFollowers ( sb, ob, false, baseVocab, vw );
						}
					}
				}
			}
		}
	}

	//
	// NOTE: $$$TBD this algorithm needs a guard against circularity or it will inf. loop in the presence of circularity,e.g. if x is owned by x
	//
	private static void crossLinkFollowers ( DMIElem cur, DMIElem container, boolean atOrgp, DMIBaseVocabulary baseVocab, DMIView vw ) {
		ISet<DMIElem> cV = cur.findSubjectsFromUsedAsObjectByVerb ( baseVocab.gFollowsContainer, vw );
		crossLink ( cV, container, atOrgp, baseVocab, vw );
		cV = cur.findObjectsFromUsedAsSubjectByVerb ( baseVocab.gFollowedByContainer, vw );
		crossLink ( cV, container, atOrgp, baseVocab, vw );
	}

	private static void crossLink ( ISet<DMIElem> cV, DMIElem container, boolean atOrgp, DMIBaseVocabulary baseVocab, DMIView vw ) {
		for ( int i = 0; i < cV.size (); i++ ) {
			DMIElem m = cV.get ( i );
			if ( m != null && vw.inView ( m ) ) {
				if ( atOrgp ) {
					m.addAttachTo ( container );
					//if ( suppress ) {
					//    SuppressDMINode m, hiddenSG
					//}
					crossLinkFollowers ( m, container, true, baseVocab, vw );
				} else {
					m.addGroupTo ( container );
					//if ( suppress ) {
					//    SuppressDMINode m, hiddenSG
					//}
					crossLinkFollowers ( m, container, false, baseVocab, vw );
				}
			}
		}
	}

}
