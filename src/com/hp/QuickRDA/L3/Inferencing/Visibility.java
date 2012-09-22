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

public class Visibility {

	// Hide statements about hidden concepts
	// Cannot display edges whose subject or object are invisible, so hide the edge.
	// This in turn may propogate to other edges, so we have an outer while ! chg loop
	// For DMI, we process isMeta graphs as well as visible; here we are laxer, we only care if hidden in domain model.
	@SuppressWarnings("all")
	public static void diagramVisibilityReasoning ( DMIView vw, DMIView vwGray ) {
		boolean evict;

		ISet<DMIElem> qV = vw.toSet ();

		boolean chg = true;

		while ( chg ) {
			chg = false;
			for ( int i = 0; i < qV.size (); i++ ) {
				DMIElem m = qV.get ( i );
				if ( m != null ) {
					evict = false;

					if ( !m.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
						evict = true;
					} else if ( m.isStatement () ) {
						////if ( ! vw.InView(m.itsSubject) || Not m.itsSubject.itsSubgraph.isDomain ) {
						//if ( !m.itsSubject.itsSubgraph.AtLevel(DMISubgraph.SubgraphLevelEnum.kDomainModel) ) {
						//	evict = true;
						//	//} else if ( ! vw.InView(m.itsObject) || Not m.itsObject.itsSubgraph.isDomain ) {
						//} else if ( !m.itsObject.itsSubgraph.AtLevel(DMISubgraph.SubgraphLevelEnum.kDomainModel) ) {
						//	evict = true;
						//}
						DMIElem sb = m.itsSubject;
						DMIElem ob = m.itsObject;

						if ( !vw.inView ( sb ) || !vw.inView ( ob ) ) {
							// if ( vwGray == null || !vwGray.InView(m) )
							evict = true;
						}

						///if ( !vw.InView(sb) && (vwGray == null || !vwGray.InView(sb)) )
						///		evict = true;
						///if ( !vw.InView(ob) && (vwGray == null || !vwGray.InView(ob)) )
						///	evict = true;
					}

					if ( evict ) {
						chg = true;
						qV.clear ( i );
					}
				}
			}
		}

		vw.trim ();
	}

	public static void xDMIExportVisibilityReasoning ( DMIView vw ) {
		ISet<DMIElem> qV = vw.toSet ();

		boolean chg = true;

		while ( chg ) {
			chg = false;
			for ( int i = 0; i < qV.size (); i++ ) {
				DMIElem m = qV.get ( i );
				if ( m != null ) { // When you're moving things out of views you have to expect some nulls
					if ( m.isStatement () ) {
						boolean evict = false;

						if ( m.itsSubject.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
							if ( !vw.inView ( m.itsSubject ) ) {
								evict = true;
							}
						}
						if ( !evict && m.itsObject.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
							if ( !vw.inView ( m.itsObject ) ) {
								evict = true;
							}
						}
						if ( evict ) {
							chg = true;
							qV.clear ( i );
						}
					}
				}
			}
		}

		vw.trim ();
	}

	public static void revealRelationshipsForNodesInView ( DMIView vw ) {
		ISet<DMIElem> qV = vw.toSet ();

		boolean chg = true;
		while ( chg ) {
			chg = false;
			for ( int i = 0; i < qV.size (); i++ ) {
				DMIElem m = qV.get ( i );
				if ( m != null ) {
					/*
					 * We're checking to see if subject and object are both in the view, if so, then add the relationship to the view
					 * 	In theory, we don't have to traverse both subjects and objects; 
					 *  	since they both have to be in the view, we can pick up objects from being subjects of the other node.
					 */
					chg |= connectNodes ( m.itsUsedAsSubjectStmtSet, vw );
					// chg |= ConnectNodes(m.itsUsedAsObjectStmtSet, vw);
				}
			}
		}

		vw.trim ();
	}

	private static boolean connectNodes ( ISet<DMIElem> s, DMIView vw ) {
		boolean chg = false;
		for ( int i = 0; i < s.size (); i++ ) {
			DMIElem m = s.get ( i );
			if ( m != null ) {
				if ( !vw.inView ( m ) && vw.inView ( m.itsSubject ) && vw.inView ( m.itsObject ) ) {
					vw.addToView ( m );
					chg = true;
				}
			}
		}
		return chg;
	}

	public static void revealReferencedNodesInView ( DMIView vw ) {
		ISet<DMIElem> qV = vw.toSet ();

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && m.isStatement () ) {
				vw.addToView ( m.itsSubject );
				vw.addToView ( m.itsObject );
			}

		}
	}

}
