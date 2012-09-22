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

import com.hp.QuickRDA.Excel.StatusMessage;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.NameUtilities;

public class Filtration_ORReach {

	//
	// Find all Paths going from any elements of source to any elements of target, avoiding some as specified by avs
	//	    Returns this set
	//	    NOTE: does not include sources and targets in the returned set
	//
	public static ISet<DMIElem> findAllPathSetBF ( DMIGraph g, ISet<DMIElem> srcV, Filtration.AVSet avs, ISet<DMIElem> trgV ) {
		DMIElem m;
		ISet<DMIElem> pathV;

		//path tail, rest of list
		ISet<DMIElem> [] pL0 = seedPathFromSet ( srcV );
		// List<ISet<DMIElem>> pL0 = SeedPathFromSet (srcV);
		@SuppressWarnings("unchecked")
		ISet<DMIElem> ansV = (ISet<DMIElem>) trgV.clone (); //.Copy -- yes: ansV is modified, and tV later referenced herein (caller doesn't care)

		int icnt;
		icnt = 1;

		while ( pL0.length > 0 ) {
			//if ( icnt == 30 ) {
			//    Stop
			//}

			//Application.ScreenUpdating = true doesn't help...
			StatusMessage.StatusUpdate ( "ReachNode iteration " + icnt + "..." );

			if ( Tracing.gTraceFile != null ) {
				Tracing.gTraceFile.println ();
				Tracing.gTraceFile.println ( "next iteration " + icnt );
				Tracing.gTraceFile.println ();
				logExtendedPaths ( "Starting Candidate Paths", pL0 );
				Tracing.flushLog ();
			}

			//Remove Paths that reach any target node
			for ( int i = 0; i < pL0.length; i++ ) {
				boolean ichg = false;

				pathV = pL0 [ i ];
				m = pathV.get ( pathV.size () );

				//Must stop and cull if we reach a source, to be consistent with behavior of stopping at target
				//Thus, we're not including paths from source to source (or target to target below)
				//but not on the first iteration, we must prime the pump...
				if ( icnt > 1 ) { //or if pathV.size () > 1
					if ( srcV.indexOf ( m ) >= 0 ) {
						if ( Tracing.gTraceFile != null ) {
							Tracing.gTraceFile.println ( "**Removing Candidate that Reached Source** " + logCandidate ( i, pL0 [ i ], true ) );
						}
						ichg = true;
					}
				}

				//Stop at target, we'll capture this path, but no longer consider further
				//Thus, we're not including paths from target to target (or source to source above)
				if ( trgV.indexOf ( m ) >= 0 ) {
					if ( Tracing.gTraceFile != null ) {
						Tracing.gTraceFile.println ( "**Taking Result** " + logCandidate ( i, pL0 [ i ], true ) );
					}
					ansV.mergeListIntoSet ( pathV );
					ichg = true;
				}

				if ( ichg ) {
					pL0 [ i ] = null;
				}
			}

			if ( Tracing.gTraceFile != null ) {
				logExtendedPaths ( "new Candidate() Paths", pL0 );
				Tracing.flushLog ();
			}

			// ISet<DMIElem> [] pL1 = new ISet<DMIElem> [ 0 ];
			IList<ISet<DMIElem>> pL1 = new XSetList<ISet<DMIElem>> ();

			//
			// given set of paths, extend them with possible new paths.
			//   which amounts to extending each path in cv0 by one
			//       new possible path must pass 3 avs tests.
			//       new possible path must not already be in current path, right?
			//
			for ( int i = 0; i < pL0.length; i++ ) {
				pathV = pL0 [ i ];
				if ( pathV != null ) {
					m = pathV.get ( pathV.size () );

					//if ( i Mod 1000 == 0 ) {
					//    Stop
					//}

					ISet<DMIElem> extV = null;

					//$2 can subst. ansV for trgV; maybe, but not much help though speed wise on my test
					//need to accept path when it reaches, so maybe return boolean of reach from Add's instead of adding
					//need to check for reaching source set here instead of adding
					//need to cull lists (in particular the first parts of the lists) on match given new additions

					extV = addToCandidatesFromSet ( extV, m.itsUsedAsObjectStmtSet, trgV, pathV, avs, Filtration.StatementPositionEnum.ObjectPosition );
					extV = addToCandidatesFromSet ( extV, m.itsUsedAsSubjectStmtSet, trgV, pathV, avs, Filtration.StatementPositionEnum.SubjectPosition );
					extV = addToCandidatesFromItem ( extV, m.itsSubject, trgV, pathV, avs, Filtration.StatementPositionEnum.SubjectPosition );
					extV = addToCandidatesFromItem ( extV, m.itsObject, trgV, pathV, avs, Filtration.StatementPositionEnum.ObjectPosition );

					if ( extV != null ) {
						for ( int j = 0; j < extV.size (); j++ ) {
							@SuppressWarnings("unchecked")
							ISet<DMIElem> nV = (ISet<DMIElem>) pathV.clone ();
							nV.addToList ( extV.get ( j ) );
							pL1.add ( nV );
						}
					}

				}
			}

			// pL0 = pL1;
			// pL0 = pL1.toElementArray ();
			@SuppressWarnings("all")
			ISet<DMIElem> [] _pL0 = (ISet<DMIElem> []) new XSetList [ pL1.size () ];
			pL1.populateElementArray ( _pL0 );
			pL0 = _pL0;
			icnt = icnt + 1;
		}

		return ansV;
	}

	//Check multiple avoids in Inferencing_Filtration.AVSet
	private static boolean instanceTypeOK ( DMIElem m, ISet<DMIElem> tV, Filtration.AVSet avs ) {
		boolean ans = (tV.indexOf ( m ) >= 0);
		//We keep the node if it's a target, regardless of being in the Inferencing_Filtration.AVSet
		//so if ans is true then that's that
		//if ans is false, then we leave it up to the lists

		if ( !ans ) {
			//OK, so we're not (keeping it because it's a target)
			//if we find it in any of these lists then it's not ok

			//Check to see if we should reject due to being on the avoid node list
			//=0 means not in list: ans is true if not in list; false if in list
			ans = (avs.anV.indexOf ( m ) < 0);
			if ( ans ) {
				//Check to see if we should reject due to being on the avoid type list
				ans = !isInstanceOfAny ( m, avs.atV );

				//Caller qualifies verb to subject/object relations
			}
		}

		if ( !ans && Tracing.gTraceFile != null ) {
			Tracing.gTraceFile.println ( "$$Rejecting " + m.itsIndex + " {" + NameUtilities.getMCText ( m ) + "}" );
		}

		return ans;
	}

	private static boolean isInstanceOfAny ( DMIElem m, ISet<DMIElem> tV ) {
		for ( int i = 0; i < tV.size (); i++ ) {
			if ( m.instanceOf ( tV.get ( i ) ) ) {
				return true;
			}
		}

		return false;
	}

	private static ISet<DMIElem> addToCandidatesFromSet ( ISet<DMIElem> resV, ISet<DMIElem> candV, ISet<DMIElem> trgV, ISet<DMIElem> pathV, Filtration.AVSet avs, Filtration.StatementPositionEnum ave ) {
		int i;
		for ( i = 1 - 1; i < candV.size (); i++ ) {
			resV = addToCandidatesFromItem ( resV, candV.get ( i ), trgV, pathV, avs, ave );
		}
		return resV;
	}

	private static ISet<DMIElem> addToCandidatesFromItem ( ISet<DMIElem> resV, DMIElem cand, ISet<DMIElem> trgV, ISet<DMIElem> pathV, Filtration.AVSet avs, Filtration.StatementPositionEnum ave ) {
		if ( cand != null ) {
			boolean add = false;

			if ( cand.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
				if ( pathV.indexOf ( cand ) < 0 ) {
					DMIElem vb = cand.itsVerb;
					if ( vb == null ) {
						add = true;
					} else {
						if ( avs.apL.pAV.indexOf ( vb ) < 0 ) {
							if ( ave == Filtration.StatementPositionEnum.SubjectPosition ) {
								add = (avs.apL.pSV.indexOf ( vb ) < 0);
							} else if ( ave == Filtration.StatementPositionEnum.ObjectPosition ) {
								add = (avs.apL.pOV.indexOf ( vb ) < 0);
							} else {
								add = true;
							}
						}
					}
				}
			}

			if ( add && instanceTypeOK ( cand, trgV, avs ) ) {
				resV = DMIElem.allocateSetIfNull ( resV );
				resV.addToSet ( cand );
			}
		}
		return resV;
	}

	private static ISet<DMIElem> [] seedPathFromSet ( ISet<DMIElem> sV ) {
		@SuppressWarnings("unchecked")
		ISet<DMIElem> [] ans = new XSetList [ sV.size () ];
		for ( int i = 0; i < sV.size (); i++ ) {
			ISet<DMIElem> v = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
			v.addToList ( sV.get ( i ) );
			ans [ i ] = v;
		}
		return ans;
	}

	private static void logExtendedPaths ( String txt, ISet<DMIElem> [] pL ) {
		Tracing.gTraceFile.println ();
		Tracing.gTraceFile.println ( txt );
		for ( int i = 0; i < pL.length; i++ ) {
			Tracing.gTraceFile.println ( logCandidate ( i, pL [ i ], false ) );
		}
	}

	private static String logCandidate ( int i, ISet<DMIElem> paths, boolean crlf ) {
		String ans;
		if ( paths == null ) {
			ans = i + ": nil";
		} else {
			ans = i + ": " + listToStr ( paths, crlf );
		}
		return ans;
	}

	private static String listToStr ( ISet<DMIElem> mV, boolean crlf ) {
		String ans;
		ans = "";

		String sep;
		if ( crlf ) {
			sep = "\r\n";
		} else {
			sep = " ";
		}

		for ( int i = 0; i < mV.size (); i++ ) {
			DMIElem m;
			m = mV.get ( i );
			String name = NameUtilities.hasMCText ( NameUtilities.getFirstName ( m ) );
			if ( name != null ) {
				ans += sep + m.itsIndex + " {" + name + "}";
			} else {
				ans += sep + m.itsIndex + " [" + NameUtilities.getMCText ( m.itsVerb ) + "]";
			}
		}

		return ans;
	}

}
