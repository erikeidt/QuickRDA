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

import java.util.List;

import com.hp.QuickRDA.Excel.StatusMessage;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L3.Inferencing.Query.*;

public class WVSearch {

	private int					itsTBase;
	private WVSearchTree []		itsTreeMap;
	//private ConceptManager		itsCM;
	private DMIBaseVocabulary	itsBaseVocab;

	public WVSearch ( ConceptManager cm ) { // TODO change From ConceptManager cm To DMIGraph g
		// itsCM = cm;
		itsBaseVocab = cm.itsBaseVocab;
		DMIGraph g = cm.itsGraph;
		itsTBase = g.getMinIndex ();
		itsTreeMap = new WVSearchTree [ g.getMaxIndex () - itsTBase ];
	}

	//
	// Find all Paths going from any elements of source to any elements of target, avoiding some as specified by avs
	//	    Returns this set
	//	    NOTE: does not include sources and targets in the returned set
	//
	/*
	public ISet<DMIElem> findAllPathSetWV (ISet<DMIElem> srcV, Filtration.AVSet avs, ISet<DMIElem> trgV, String pat) {
		BoundFrame bv = null;

		if ( "role2role".equals (pat) && srcV.size () > 0 && trgV.size () > 0 )
			bv = BuildTestPattern.patternRole2Role (srcV.get (0), trgV.get (0), itsCM, itsBaseVocab);

		//if ( "artifact2artifact".equals (pat) && srcV.size () > 0 && trgV.size () > 0 )
		//	bv = BuildTestPattern.patternArtifact2Artifact (srcV.get (0), trgV.get (0), cm, cm.itsBaseVocab);

		return findAllPathSetWV (srcV, avs, trgV, bv);
	}
	*/

	public ISet<DMIElem> findAllPathSetWV ( ISet<DMIElem> srcV, Filtration.AVSet avs, ISet<DMIElem> trgV, BoundFrame boundVars ) {
		ISet<DMIElem> ans = findIntermediatesBetween ( srcV, avs, trgV, boundVars );
		ans.mergeListIntoSet ( trgV );
		return ans;
	}

	//
	// This is algorithm that methodically searches using notion of a wave front
	//   to prevent searching things we've already searched.
	//
	public ISet<DMIElem> findIntermediatesBetween ( ISet<DMIElem> srcV, Filtration.AVSet avs, ISet<DMIElem> trgV, BoundFrame boundVars ) {
		List<WVSearchTree> candidatePaths = new XSetList<WVSearchTree> (); // DMITree list heads for current candidate path

		/*
		// For the side effect of creating DMITree's for the targets, we create and abandon their candidate paths!
		candidatePaths = SeedPathFromSet(trgV, WVSearchTree2.SearchStatusEnum.Target, itsTreeMap, itsTBase, null);
		// Create DMITree's for the sources, and use that as the seed
		candidatePaths = SeedPathFromSet(srcV, WVSearchTree2.SearchStatusEnum.Source, itsTreeMap, itsTBase, boundVars);
		*/

		// modified 06/09/2011; for SourceSet==TargetSet
		// this means that for targets that are also sources, will have search state of target instead of source like before
		// Create DMITree's for the sources, and use that as the seed
		candidatePaths = seedPathFromSet ( srcV, WVSearchTree.SearchStatusEnum.Source );
		// For the side effect of creating DMITree's for the targets, we create and abandon their candidate paths!
		if ( trgV != null )
			seedPathFromSet ( trgV, WVSearchTree.SearchStatusEnum.Target );

		ISet<DMIElem> ansSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		ISet<DMIElem> failSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		ISet<DMIElem> loopSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		IList<DMIElem> acceptCandidateSet = new XSetList<DMIElem> ( XSetList.HashOnDemand );
		IList<DMIElem> rejectCandidateSet = new XSetList<DMIElem> ( XSetList.HashOnDemand );

		BoundFork candidateFork = (boundVars == null) ? null : new BoundFork ( boundVars );

		int icnt = 1;

		while ( candidatePaths.size () > 0 ) {
			StatusMessage.StatusUpdate ( "ReachNode shortest path search: iteration = " + icnt + " ..." );

			if ( Tracing.gTraceFile != null ) {
				Tracing.gTraceFile.println ();
				Tracing.gTraceFile.println ();
				Tracing.gTraceFile.println ( "------- Iteration " + icnt + " -------" );
				if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
					logPathSet ( "Starting Candidate Paths", candidatePaths );
					Tracing.gTraceFile.println ();
					Tracing.flushLog ();
				}
			}

			List<WVSearchTree> nextCandidatePaths = new XSetList<WVSearchTree> ();
			BoundFork nextCandidateFork = null;

			if ( boundVars != null ) {
				nextCandidateFork = new BoundFork ();

				IList<BoundFrame> lbf = candidateFork.getFrames ();
				for ( int i = 0; i < lbf.size (); i++ ) {
					BoundFrame bf = lbf.get ( i );
					// check patterns for finishing, and when so, accumulate their results to success
					if ( bf.completed ( ansSet, null, null ) ) {
						if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
							Tracing.gTraceFile.print ( "$$$Taking result frame: " );
							int st = Tracing.gTraceSet;
							Tracing.gTraceSet |= Tracing.TraceFrameX;
							bf.traceOut ();
							Tracing.gTraceFile.println ();
							Tracing.gTraceSet = st;
						}
						if ( bf.whenCompletedisFinished () )
							lbf.clear ( i );
					}
				}
				lbf.trim ();

				if ( lbf.size () == 0 )
					break;
			}

			//
			// given set of paths, extend them with possible new paths.
			//   which amounts to extending each path in cL0 by one
			//       new possible path must pass 3 avs tests.
			//       new possible path must not already be in current path
			//
			int i = -1;
			//for ( int i = 0; i < candidatePaths.length; i++ ) {
			//	WVSearchTree2 aPath = candidatePaths [ i ];
			for ( WVSearchTree aPath : candidatePaths ) {
				if ( aPath == null )
					continue;

				++i;

				if ( (Tracing.gTraceSet & Tracing.TraceFrameY) != 0 ) {
					Tracing.gTraceFile.print ( "*From: " );
					logCandidate ( i, aPath );
				}

				DMIElem m = aPath.itsElem;
				// BoundFork boundVarsSet = aPath.itsBoundVarsSet;

				acceptCandidateSet.clear ();
				rejectCandidateSet.clear ();

				//
				// Look for possible extensions
				//

				DMIElem prev = null;
				if ( aPath.itsNext != null )
					prev = aPath.itsNext.itsElem;

				//
				// The possible sources of path extension are consulted here
				//
				if ( m.isStatement () ) {
					addToCandidateFromItem ( acceptCandidateSet, rejectCandidateSet, m.itsSubject, prev, avs, Filtration.StatementPositionEnum.SubjectPosition, candidateFork, nextCandidateFork );
					addToCandidateFromItem ( acceptCandidateSet, rejectCandidateSet, m.itsVerb, prev, avs, Filtration.StatementPositionEnum.VerbPosition, candidateFork, nextCandidateFork );
					addToCandidateFromItem ( acceptCandidateSet, rejectCandidateSet, m.itsObject, prev, avs, Filtration.StatementPositionEnum.ObjectPosition, candidateFork, nextCandidateFork );
				}

				if ( m.itsUsedAsObjectStmtSet.size () > 0 )
					addToCandidatesFromSet ( acceptCandidateSet, rejectCandidateSet, m.itsUsedAsObjectStmtSet, prev, avs, Filtration.StatementPositionEnum.ObjectPosition, candidateFork, nextCandidateFork );
				if ( m.subclassOf ( itsBaseVocab.gProperty ) && m.itsFullInstanceSet.size () > 0 )
					addToCandidatesFromSet ( acceptCandidateSet, rejectCandidateSet, m.itsFullInstanceSet, prev, avs, Filtration.StatementPositionEnum.VerbPosition, candidateFork, nextCandidateFork );
				if ( m.itsUsedAsSubjectStmtSet.size () > 0 )
					addToCandidatesFromSet ( acceptCandidateSet, rejectCandidateSet, m.itsUsedAsSubjectStmtSet, prev, avs, Filtration.StatementPositionEnum.SubjectPosition, candidateFork, nextCandidateFork );

				if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
					Tracing.gTraceFile.println ();
					int cnt = 0;
					if ( nextCandidateFork != null ) {
						IList<BoundFrame> bfl = nextCandidateFork.getFrames ();
						if ( bfl != null )
							cnt = bfl.size ();
					}
					Tracing.gTraceFile.println ( "ForkCount=" + cnt );
					Tracing.gTraceFile.println ();
				}

				// Take the extending candidates and decide how to use them
				for ( int j = 0; j < acceptCandidateSet.size (); j++ ) {
					DMIElem n = acceptCandidateSet.get ( j );
					WVSearchTree wvst = itsTreeMap [ n.itsIndex - itsTBase ];

					if ( wvst != null ) {
						if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
							Tracing.gTraceFile.print ( "**Reached Previous (G)" );
							logCandidate ( wvst.itsGeneration, wvst );
							Tracing.gTraceFile.print ( "  From Candidate** " );
							logCandidate ( i, aPath );
							Tracing.flushLog ();
						}

						// Ok, this is hard to follow, but here goes
						//   We're doing a methodical search, and,
						//   We ran into an area already involved in the methodical search
						switch ( wvst.itsStatus ) {
						case Source :
							// simply do nothing: don't add this to the search path
							break;
						case Target :
							if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
								Tracing.gTraceFile.print ( "**Taking Result** " );
								logCandidate ( i, aPath, true );
							}
							aPath.accumulateSuccessPath ( ansSet, true );
							break;
						case Exhausted :
							// Turns out this area we hit is a known dead end.
							// So, so are any zero ref count (non-forked) paths of this path.
							if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
								Tracing.gTraceFile.println ( "**Cutting off Path Shortcut** " );
								Tracing.flushLog ();
							}
							aPath.accumulateZCntFailPath ( failSet );
							break;
						case ReachedTarget :
							// We hit an area that is known success
							// Meaning that the whole path becomes part of the known success
							// But trust that any paths beyond the success meet up are still included
							//   in the methodical search as per the path that got there first.
							if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
								Tracing.gTraceFile.println ( "**Taking Result Shortcut** " );
								Tracing.flushLog ();
							}
							aPath.accumulateSuccessPath ( ansSet, true );
							break;
						default :
							if ( wvst.itsGeneration < icnt ) {
								// if we run into something that is in an older generation,
								// then it is a cycle that we can safely ignore since all
								// the search paths for the cycle are surely being covered elsewise
								// due to the wave of the methodical search that got there first.
								if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
									Tracing.gTraceFile.println ( "**Backtracking**" );
									Tracing.flushLog ();
								}
							} else if ( newSuccessPath ( aPath, wvst ) ) {
								// We reached a path that itself found (different) success
								// Therefore our join to it makes us a success
								if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
									Tracing.gTraceFile.println ( "**Linking Success Path**" );
									Tracing.flushLog ();
								}
								aPath.accumulateSuccessPath ( ansSet, true );
							} else {
								// Otherwise, we ran into something in the current generation,
								// thus, it is not cycle but rather a join, and we must track it
								if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
									Tracing.gTraceFile.println ( "**Joining Paths**" );
									Tracing.flushLog ();
								}
								wvst.addPathToChain ( aPath );
								loopSet.addToSet ( n );
								if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
									Tracing.gTraceFile.print ( "**Joined Paths** (G)" );
									logCandidate ( wvst.itsGeneration, wvst );
									Tracing.flushLog ();
								}
							}
						}
					} else {
						// This path element is new to the search
						// We'll create a new path by adding one to the current path
						// essentially supporting forking of the path from a common root
						WVSearchTree newPathTree;
						if ( aPath.itsStatus == WVSearchTree.SearchStatusEnum.Source ) {
							// (must be first iteration)
							// start path from scratch sans seed of source
							newPathTree = new WVSearchTree ( n, icnt );
						} else {
							// iteration N, add to path
							newPathTree = new WVSearchTree ( n, aPath, icnt );
						}
						itsTreeMap [ n.itsIndex - itsTBase ] = newPathTree;
						nextCandidatePaths.add ( newPathTree );
					}
				}
			}

			for ( WVSearchTree t : candidatePaths )
				t.accumulateZCntFailPath ( failSet );

			candidatePaths = nextCandidatePaths;
			candidateFork = nextCandidateFork;
			icnt++;
		}

		boolean chg;
		int lcnt = 1;
		for ( ;; ) {
			if ( (Tracing.gTraceSet & (Tracing.TraceWave | Tracing.TraceFrame | Tracing.TraceFrameX | Tracing.TraceFrameY)) != 0 ) {
				Tracing.gTraceFile.println ();
				Tracing.gTraceFile.println ();
				Tracing.gTraceFile.println ( "========= loopSet " + java.lang.Integer.toString ( lcnt ) + " ========  len = " + java.lang.Integer.toString ( loopSet.size () ) );
				if ( (Tracing.gTraceSet & (Tracing.TraceFrame | Tracing.TraceFrameX | Tracing.TraceFrameY)) != 0 ) {
					Tracing.gTraceFile.println ( "Loop Candidates" );
					for ( int i = 0; i < loopSet.size (); i++ ) {
						DMIElem m = loopSet.get ( i );
						WVSearchTree wvst = itsTreeMap [ m.itsIndex - itsTBase ];
						logCandidate ( i, wvst );
					}
					Tracing.gTraceFile.println ();
				}
			}

			chg = false;
			for ( int i = 0; i < loopSet.size (); i++ ) {
				DMIElem m = loopSet.get ( i );
				if ( m != null ) {
					WVSearchTree wvst = itsTreeMap [ m.itsIndex - itsTBase ];
					if ( loopsReachedSuccess ( wvst ) ) {
						if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
							Tracing.gTraceFile.print ( "Success for " );
							logCandidate ( i, wvst );
						}
						chg |= wvst.accumulateSuccessPath ( ansSet, true );
						loopSet.clear ( i );
					}
				}
			}

			if ( !chg )
				break;

			lcnt++;
		}

		return ansSet;
	}

	//
	// if any spurs of joined chains reached new success then they all reached success
	//
	private static boolean loopsReachedSuccess ( WVSearchTree xpv ) {
		if ( xpv.itsStatus != WVSearchTree.SearchStatusEnum.ReachedTarget ) {
			/*
			for ( int i = 0; i < xpv.ChainCount (); i++ ) {
				if ( NewSuccessPath (xpv, xpv.ChainAt (i)) ) {
					return true;
				}
			}
			*/
			for ( WVSearchTree t : xpv.getChain () ) {
				if ( newSuccessPath ( xpv, t ) )
					return true;
			}

			/*
			for ( int i = 0; i < xpv.ChainCount () - 1; i++ ) {
				WVSearchTree2 t1 = xpv.ChainAt (i);
				for ( int j = i + 1; j < xpv.ChainCount (); j++ ) {
					if ( NewSuccessPath (t1, xpv.ChainAt (j)) ) {
						return true;
					}
				}
			}
			*/
			for ( WVSearchTree t1 : xpv.getChain () ) {
				for ( WVSearchTree t2 : xpv.getChain () ) {
					if ( t1 != t2 ) {
						if ( newSuccessPath ( t1, t2 ) )
							return true;
					}
				}
			}
		}

		return false;
	}

	//
	// If we're a searching branch off of some success path, we don't want to consider finding another route to that
	// same success path as being success ourselves.  So, we look to see if there are different success paths involved
	// and if so, then it is a new success path, and we should honor the linkage of this meeting up with that.
	// but otherwise, it's just more searching off the existing success path, so handle it as per normal.
	//
	private static boolean newSuccessPath ( WVSearchTree aPath, WVSearchTree xpv ) {
		if ( xpv.reachesSuccess () ) {
			if ( aPath.reachesSuccess () ) {
				ISet<DMIElem> xp = null;
				xp = xpv.successPaths ( xp );
				ISet<DMIElem> ap = null;
				ap = aPath.successPaths ( ap );
				if ( xp != null ) { // null test added 06/09/2011; for case of SourceSet==TargetSet
					for ( int i = 0; i < xp.size (); i++ ) {
						DMIElem sc = xp.get ( i );
						if ( ap.indexOf ( sc ) < 0 ) {
							return true;
						}
					}
				}
			} else {
				return true;
			}
		} else if ( aPath.reachesSuccess () ) {
			return true;
		}

		return false;
	}

	// if ( false ) {
	/*
	private static void ResetResTemp(ISet<DMIElem> v, DMITree[] itsTreeMap) {
		for ( int i = 0; i < v.size (); i++ ) {
			DMIElem m = v.ElementUt(i);
			if ( m != null ) {
				DMITree t = itsTreeMap(m.itsIndex - itsTBase);
				if ( t != null ) {
					t.Destroy;
					itsTreeMap(m.itsIndex - itsTBase) = null;
				}
			}
		}
	}
	*/
	// #}

	//Check multiple avoids in AVSet
	private static boolean instanceTypeOK ( DMIElem m, Filtration.AVSet avs ) {
		boolean ans;
		//Check to see if we should reject due to being on the avoid node list
		//=0 means not in list: ans is true if not in list; false if in list
		ans = (avs.anV.indexOf ( m ) < 0);
		if ( ans ) {
			//Check to see if we should reject due to being on the avoid type list
			ans = !isInstanceOfAny ( m, avs.atV );
			//Caller qualifies verb to subject/object relations
		}

		if ( !ans && (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
			Tracing.gTraceFile.println ( "$$Rejecting by type " + java.lang.Integer.toString ( m.itsIndex ) + " {" + NameUtilities.getMCText ( m ) + "}" );
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

	private void addToCandidatesFromSet ( IList<DMIElem> acceptCandidateList, IList<DMIElem> rejectCandidateList,
			ISet<DMIElem> candV, DMIElem prev, Filtration.AVSet avs, Filtration.StatementPositionEnum ave,
			BoundFork inBoundFork, BoundFork outBoundFork ) {
		for ( int i = 0; i < candV.size (); i++ )
			addToCandidateFromItem ( acceptCandidateList, rejectCandidateList, candV.get ( i ), prev, avs, ave, inBoundFork, outBoundFork );
	}

	private void addToCandidateFromItem ( IList<DMIElem> acceptCandidateList, IList<DMIElem> rejectCandidateList,
			DMIElem cand, DMIElem prev, Filtration.AVSet avs, Filtration.StatementPositionEnum ave,
			BoundFork inBoundFork, BoundFork outBoundFork ) {
		boolean add = false;

		if ( acceptCandidateList.indexOf ( cand ) < 0 && rejectCandidateList.indexOf ( cand ) < 0 ) {
			if ( itsTreeMap [ cand.itsIndex - itsTBase ] != null ) {
				// add = true
				// Optimization to prevent excess backtracking
				add = (cand != prev);
			} else {
				if ( cand.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
					DMIElem vb = cand.itsVerb;
					if ( vb == null || avs == null ) {
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

				if ( add ) {
					if ( avs != null && !instanceTypeOK ( cand, avs ) ) {
						add = false;
					}
				}
			}


			if ( add && inBoundFork != null ) {
				if ( cand.isStatement () )
					add = inBoundFork.acceptCandidate ( cand, outBoundFork, null );
				else
					outBoundFork.mergeIntoFork ( inBoundFork );
			}

			if ( add )
				acceptCandidateList.addToList ( cand );
			else
				rejectCandidateList.addToList ( cand );
		}
	}

	private List<WVSearchTree> seedPathFromSet ( ISet<DMIElem> sV, WVSearchTree.SearchStatusEnum k ) {
		List<WVSearchTree> ans = new XSetList<WVSearchTree> ();
		for ( int i = 0; i < sV.size (); i++ ) {
			DMIElem m = sV.get ( i );
			WVSearchTree v = new WVSearchTree ( m, 0, k );
			itsTreeMap [ m.itsIndex - itsTBase ] = v;
			ans.add ( v );
		}
		return ans;
	}

	private static void logPathSet ( String txt, List<WVSearchTree> pL ) {
		Tracing.gTraceFile.println ();
		Tracing.gTraceFile.println ( txt );
		//for ( int i = 0; i < pL.length; i++ )
		//	LogCandidate (i, pL [ i ]);
		int i = 0;
		for ( WVSearchTree t : pL ) {
			logCandidate ( i, t );
			i++;
		}
	}

	private static void logCandidate ( int i, WVSearchTree pathT ) {
		if ( pathT == null ) {
			Tracing.gTraceFile.println ( java.lang.Integer.toString ( i ) + ": nil" );
		} else {
			Tracing.gTraceFile.println ( java.lang.Integer.toString ( i ) + ": " + pathT.listToStr () );
		}
	}

	private static void logCandidate ( int i, WVSearchTree pathT, boolean matched ) {
		Tracing.gTraceFile.println ( matched ? "*matched* " : "*nomatch* " );
		logCandidate ( i, pathT );
	}

}
