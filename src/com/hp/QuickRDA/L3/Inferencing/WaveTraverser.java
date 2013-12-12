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
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L3.Inferencing.Query.*;

public class WaveTraverser {
	private ConceptManager		itsConceptMgr;
	private int					itsNodeIndexOffset;
	private boolean []			itsVisited;
	private DMIBaseVocabulary	itsBaseVocab;

	public static class InferredRelationship {
		DMIElem	sb;
		DMIElem	vb;
		DMIElem	ob;

		public InferredRelationship ( DMIElem sb1, DMIElem vb1, DMIElem ob1 ) {
			sb = sb1;
			vb = vb1;
			ob = ob1;
		}
	}

	public WaveTraverser ( ConceptManager cm ) { // TODO change From ConceptManager cm To DMIGraph g
		itsConceptMgr = cm;
		itsBaseVocab = cm.itsBaseVocab;
		DMIGraph g = cm.itsGraph;
		itsNodeIndexOffset = g.getMinIndex ();
		itsVisited = new boolean [ g.getMaxIndex () - itsNodeIndexOffset ];
	}

	public ISet<DMIElem> traverse ( ISet<DMIElem> inNodes, BoundFork inPatterns, boolean infer, boolean loop, ISet<DMIElem> suggested ) {
		if ( infer ) {
			ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet );
			ISet<DMIElem> xV = new XSetList<DMIElem> ( XSetList.AsSet );
			for ( ;; ) {
				IList<InferredRelationship> inferSet = new XSetList<InferredRelationship> ();
				ISet<DMIElem> res = traverse ( inNodes, inPatterns, inferSet, suggested );
				ans.merge ( res );
				for ( int i = 0; i < inferSet.size (); i++ ) {
					InferredRelationship ifr = inferSet.get ( i );
					int cnt = itsConceptMgr.itsGraph.getMaxIndex ();
					DMIElem m = itsConceptMgr.enterStatement ( ifr.sb, ifr.vb, ifr.ob, false );
					ans.add ( m );
					ans.add ( ifr.sb );
					ans.add ( ifr.ob );
					if ( itsConceptMgr.itsGraph.getMaxIndex () > cnt )
						xV.add ( m );
				}
				if ( loop && xV.size () > 0 )
					continue;
				break;
			}
			return ans; // xV; only the inferred nodes
		}
		return traverse ( inNodes, inPatterns, null, suggested );
	}

	private ISet<DMIElem> traverse ( ISet<DMIElem> inNodes, BoundFork inPatterns, IList<InferredRelationship> inferSet, ISet<DMIElem> suggested ) {
		BoundFork currPatterns = inPatterns;

		ISet<DMIElem> currNodes = new XSetList<DMIElem> ( XSetList.AsSet );
		ISet<DMIElem> currLinks = new XSetList<DMIElem> ( XSetList.AsSet );

		for ( DMIElem src : inNodes )
			if ( src != null )
				setVisited ( src );

		for ( DMIElem src : inNodes )
			if ( src != null )
				stepFromConcept ( src, currNodes, currLinks );

		if ( suggested != null ) {
			for ( DMIElem src : suggested )
				classifyConcept ( src, currNodes, currLinks );
		}

		ISet<DMIElem> ansSet = new XSetList<DMIElem> ( XSetList.AsSet );
		ISet<BoundFrame> nonRejectedCandidates = new XSetList<BoundFrame> ( XSetList.AsSet );

		int iterCnt = 0;

		for ( ;; ) {
			++iterCnt;
			if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 )
				Tracing.gTraceFile.println ( "---- Iteration " + iterCnt + " ----" );

			// pattern match exhausted?  if so, there's nothing left to match.
			if ( currPatterns.isEmpty () )
				break;

			IList<BoundFrame> lbf = currPatterns.getFrames ();
			int cnt = lbf.size ();

			if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
				Tracing.gTraceFile.println ( "frames for iteration:" );
				for ( int i = 0; i < cnt; i++ ) {
					BoundFrame fr = lbf.get ( i );
					fr.traceOut ();
				}
				Tracing.gTraceFile.println ( "==========" );
			}

			// Pass 1 search new frames for successes or success roots
			for ( int i = 0; i < cnt; i++ ) {
				BoundFrame fr = lbf.get ( i );
				if ( fr.succcessPathComplete () ) {
					if ( fr.couldBeRejected () ) {
						if ( fr.markAsSuccessRoot () ) {
							if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
								Tracing.gTraceFile.println ( "$$$ Adding frame root: " );
								fr.traceOut ();
							}

							boolean b = nonRejectedCandidates.add ( fr );

							if ( !b && (Tracing.gTraceSet & Tracing.TraceWave) != 0 )
								Tracing.gTraceFile.println ( " ****** was already on root list" );

						}
					} else if ( fr.completed ( ansSet, inferSet, null ) ) {

						if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
							Tracing.gTraceFile.print ( "$$$Taking result frame: " );
							int st = Tracing.gTraceSet;
							Tracing.gTraceSet |= Tracing.TraceFrameX;
							fr.traceOut ();
							Tracing.gTraceFile.println ();
							Tracing.gTraceSet = st;
						}

						if ( fr.whenCompletedisFinished () )
							lbf.clear ( i );
					}
				}
			}

			// Pass 2 search for rejections
			for ( int i = 0; i < cnt; i++ ) {
				BoundFrame fr = lbf.get ( i );
				if ( fr != null ) {
					if ( fr.rejected () ) {
						lbf.clear ( i );
						BoundFrame successRoot = fr.getSuccessRoot ();
						if ( successRoot != null )
							nonRejectedCandidates.remove ( successRoot );

						if ( (Tracing.gTraceSet & Tracing.TraceWave) != 0 ) {
							Tracing.gTraceFile.println ( "$$$ Rejecting frame: " );
							fr.traceOut ();
							if ( successRoot != null ) {
								Tracing.gTraceFile.println ( " Which takes out root frame: " );
								successRoot.traceOut ();
							}
						}

					}
				}
			}
			lbf.trim ();

			if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
				Tracing.gTraceFile.println ( "frames after checking success:" );
				for ( int i = 0; i < lbf.size (); i++ ) {
					BoundFrame fr = lbf.get ( i );
					fr.traceOut ();
				}
				Tracing.gTraceFile.println ( "==========" );
			}

			cnt = currNodes.size ();
			for ( int i = 0; i < cnt; i++ ) {
				DMIElem m = currNodes.get ( i );
				stepFromNode ( m, currLinks );
			}

			// input set exhausted?  if so, there's nothing left to traverse.
			if ( currLinks.isEmpty () )
				break;

			BoundFork nextPatterns = new BoundFork ();
			ISet<DMIElem> nextNodes = new XSetList<DMIElem> ( XSetList.AsSet );
			ISet<DMIElem> nextLinks = new XSetList<DMIElem> ( XSetList.AsSet );
			ISet<BoundFork []> updatedSubpatterns = new XSetList<BoundFork []> ( XSetList.AsSet );

			cnt = currLinks.size ();
			for ( int i = 0; i < cnt; i++ ) {
				DMIElem m = currLinks.get ( i );
				if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
					Tracing.gTraceFile.println ( "visiting: " + m.itsIndex );
				}
				// all patterns have to see all statements, 
				//  even newly generated patterns from current generation
				BoundFork morePatterns = new BoundFork ();
				accept ( m, nextPatterns, morePatterns, nextNodes, nextLinks, updatedSubpatterns );
				nextPatterns.mergeIntoFork ( morePatterns );

				// existing patterns have to see all statements,
				// and if they cannot advance, they'll be retired this generation
				accept ( m, currPatterns, nextPatterns, nextNodes, nextLinks, updatedSubpatterns );
			}

			// $21$: carry frames over whose subforks are modified
			lbf = currPatterns.getFrames ();
			cnt = lbf.size ();
			for ( int i = 0; i < cnt; i++ ) {
				BoundFrame fr = lbf.get ( i );
				if ( fr.usesSubForkIn ( updatedSubpatterns ) ) {
					if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 )
						Tracing.gTraceFile.println ( "carrying over frame #" + fr.itsIndex );
					nextPatterns.addFrame ( fr );
				}
			}

			currNodes = nextNodes; // switch over to new set of nodes
			currLinks = nextLinks; // switch over to new set of links
			currPatterns = nextPatterns; // retire all patterns, having seen a full step
		}

		// process nonRejectedCandidates list
		for ( int i = 0; i < nonRejectedCandidates.size (); i++ ) {
			BoundFrame fr = nonRejectedCandidates.get ( i );
			boolean y = fr.completed ( ansSet, inferSet, null );
			if ( y && (Tracing.gTraceSet & Tracing.TraceFrame) != 0 )
				Tracing.gTraceFile.println ( "took: " + fr.itsIndex );
		}

		return ansSet;
	}

	private void accept ( DMIElem m, BoundFork inPatterns, BoundFork outPatterns, ISet<DMIElem> outNodes, ISet<DMIElem> outLinks, ISet<BoundFork []> updatedSubpatterns ) {
		assert m.isStatement () : "WaveTraversal.accept: not a statement";
		if ( inPatterns.acceptCandidate ( m, outPatterns, updatedSubpatterns ) )
			stepFromLink ( m, outNodes, outLinks );
	}

	private void classifyConcept ( DMIElem m, ISet<DMIElem> outNodes, ISet<DMIElem> outLinks ) {
		if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 )
			Tracing.gTraceFile.println ( "adding: " + m.itsIndex );
		setVisited ( m );
		if ( m.isStatement () )
			outLinks.add ( m );
		else
			outNodes.add ( m );
	}

	private void stepFromConcept ( DMIElem m, ISet<DMIElem> outNodes, ISet<DMIElem> outLinks ) {
		if ( m.isStatement () )
			stepFromLink ( m, outNodes, outLinks );
		else
			stepFromNode ( m, outLinks );
	}

	private void stepFromNode ( DMIElem m, ISet<DMIElem> outLinks ) {
		step ( m.itsUsedAsObjectStmtSet, outLinks );
		if ( m.instanceOf ( itsBaseVocab.gProperty ) )
			step ( m.itsFullInstanceSet, outLinks );
		step ( m.itsUsedAsSubjectStmtSet, outLinks );
	}

	private void step ( ISet<DMIElem> mSet, ISet<DMIElem> outLinks ) {
		int cnt = mSet.size ();
		for ( int i = 0; i < cnt; i++ ) {
			DMIElem m = mSet.get ( i );
			step ( m, null, outLinks );
		}
	}

	private void stepFromLink ( DMIElem m, ISet<DMIElem> outNodes, ISet<DMIElem> outLinks ) {
		step ( m.itsSubject, outNodes, outLinks );
		step ( m.itsObject, outNodes, outLinks );
		step ( m.itsVerb, outNodes, outLinks );
		stepFromNode ( m, outLinks );
	}

	private void step ( DMIElem m, ISet<DMIElem> outNodes, ISet<DMIElem> outLinks ) {
		if ( wasVisited ( m ) ) {
			if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 )
				Tracing.gTraceFile.println ( "skipping: " + m.itsIndex );
		}
		else {
			classifyConcept ( m, outNodes, outLinks );
		}
	}

	private void setVisited ( DMIElem m ) {
		itsVisited [ m.itsIndex - itsNodeIndexOffset ] = true;
	}

	private boolean wasVisited ( DMIElem m ) {
		return itsVisited [ m.itsIndex - itsNodeIndexOffset ];
	}

}
