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

package com.hp.QuickRDA.L3.Inferencing.Query;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.ConceptManager;
import com.hp.QuickRDA.L2.Names.NameUtilities;
import com.hp.QuickRDA.L3.Inferencing.WaveTraverser.InferredRelationship;
import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition.*;

/*
 * A BoundFrame is a set of separate query variables that are related to each other in a single PatternDefinition, 
 * 	like separate variables in a stack frame are related to the same method
 * 
 * Some the variables in the BoundFrame have been bound, others not yet
 * 	like some variables in a method have been initialized as yet, others not
 * 		e.g. parameters have been initialized, and other variables get non-default values via execution steps

 * At any point in time, the variables of the BoundFrame describe the current state of the pattern match,
 *   like the variables in a stack frame describe the state of execution of a method
 *   
 * However, unlike a stack frame which is usually modified in place in transitioning from step to step,
 *  Advancement of match state generally represents forking or fanning out to many possible next states
 *  As such, we clone & modify rather than advancing state of a given BoundFrame,
 *   meaning that created BoundFrames are functional or read-only.
 */
public class BoundFrame {
	public static final int	NotFound		= 0;
	public static final int	Matched			= 1;
	public static final int	Excluded		= 2;

	private static int		itsNextIndex	= 0;		// debugging only
	public final int		itsIndex;					// debugging only
	private int				itsDerivedFromIndex;		// debugging only
	private boolean			itsAdvanceOrPropagate;		// debugging only

	private boolean			itsIsSuccessRoot;
	private BoundFrame		itsSuccessRoot;

	PatternDefinition		itsPatternDefinition;

	DMIElem []				itsVariableBindings;

	int						itsStmtMatchCount;			// not counting subframes
	int						itsStmtNegationMatchCount;
	int []					itsStatementMatching;		// one of NotFound, Matched, Excluded

	// each statement is either a MatchPattern (and bound to a DMIELem) or a Subpattern, and bound to a BoundFork
	DMIElem []				itsStatementBindings;
	public BoundFork []		itsSubForks;				// need a fork for each sub pattern; one fork holds the arbitrary length of elements of one sub pattern chain

	// awkwardness is that if you have a single BoundFork, you don't know what the subpattern is, you have to have the BoundFrame, and the statement index;

	public void check () {
		assert itsStmtMatchCount > 0 || itsSubForks != null : "BoundFrame.check: itsStmtMatchCount == 0";
	}


	// Number the BoundFrames for debugging purposes
	private BoundFrame ( int ix, boolean aOrP ) {
		itsIndex = itsNextIndex++;
		itsDerivedFromIndex = ix;
		itsAdvanceOrPropagate = aOrP;
	}

	// Create BoundFrame from (static) pattern, and set initial pattern match state
	public BoundFrame ( PatternDefinition pat ) {
		this ( -1, false );

		itsPatternDefinition = pat;
		itsVariableBindings = new DMIElem [ pat.itsVariableDefinitions.length ];

		itsStmtMatchCount = pat.itsInitialStmtCount;
		itsStmtNegationMatchCount = pat.itsInitialStmtNegationCount;

		itsStatementMatching = new int [ pat.itsQueryStatements.length ];
		itsStatementBindings = new DMIElem [ pat.itsQueryStatements.length ];

		if ( pat.itsHasSubpatterns ) {
			itsSubForks = new BoundFork [ pat.itsQueryStatements.length ];
			for ( int i = 0; i < itsSubForks.length; i++ ) {
				BasePattern b = pat.itsQueryStatements [ i ];
				if ( b instanceof SubPattern ) {
					SubPattern p = (SubPattern) b;
					// create BoundFork with initial frame (frame zero) as the empty slot, as needed for our [n:*] algorithm
					itsSubForks [ i ] = new BoundFork ( p.itsInitialFrame == null ? new BoundFrame ( p.itsPattern ) : p.itsInitialFrame );
				}
			}
		}

		check ();
	}

	// Advance pattern match state by one pattern match statement, yielding new resulting frame.
	// Start from shallow clone of advanceMe, and mark an additional statement as matching.
	private BoundFrame ( BoundFrame advanceMe, int matchingStmtIndex ) {
		this ( advanceMe.itsIndex, true );
		shallowCopy ( advanceMe, true );
		markAsMatched ( matchingStmtIndex );
	}

	// Same as above, but with a matching statement in the graph, which we bind to, and bind vars also.
	private BoundFrame ( BoundFrame advanceMe, DMIElem stmt, int matchingStmtIndex, int bindVars ) {
		this ( advanceMe, matchingStmtIndex );
		itsStatementBindings [ matchingStmtIndex ] = stmt;

		MatchPattern q = (MatchPattern) itsPatternDefinition.itsQueryStatements [ matchingStmtIndex ];

		if ( (bindVars & PatternDefinition.LabelBinding) != 0 )
			itsVariableBindings [ q.itsLabelVariable ] = stmt;
		if ( (bindVars & PatternDefinition.SubjectBinding) != 0 )
			itsVariableBindings [ q.itsSubjectVariable ] = stmt.itsSubject;
		if ( (bindVars & PatternDefinition.VerbBinding) != 0 )
			itsVariableBindings [ q.itsVerbVariable ] = stmt.itsVerb;
		if ( (bindVars & PatternDefinition.ObjectBinding) != 0 )
			itsVariableBindings [ q.itsObjectVariable ] = stmt.itsObject;
	}

	private void shallowCopy ( BoundFrame bv, boolean top ) {
		bv.check ();

		itsPatternDefinition = bv.itsPatternDefinition;
		itsStmtMatchCount = bv.itsStmtMatchCount;
		itsStmtNegationMatchCount = bv.itsStmtNegationMatchCount;

		// if ( itsPatternDefinition.itsHasNegation ) to reduce gc 
		itsSuccessRoot = bv;

		if ( top ) {
			itsVariableBindings = bv.itsVariableBindings.clone ();
			itsStatementMatching = bv.itsStatementMatching.clone ();
			itsStatementBindings = bv.itsStatementBindings.clone ();
			itsSubForks = bv.itsSubForks;
		}
		else {
			itsVariableBindings = bv.itsVariableBindings;
			itsStatementMatching = bv.itsStatementMatching;
			itsStatementBindings = bv.itsStatementBindings;
			itsSubForks = bv.itsSubForks; // $20$ .clone ();
		}

		check ();
	}

	public BoundFrame ( BoundFrame cloneMe, boolean x ) {
		this ( cloneMe.itsIndex, false );
		shallowCopy ( cloneMe, true );
	}

	private BoundFrame ( BoundFrame cloneMe ) {
		this ( cloneMe.itsIndex, false );
		shallowCopy ( cloneMe, false );
	}

	IList<BoundFrame> matchQualifiers ( int bindVars, DMIElem stmt, int matchingStmtIndex ) {
		PatternDefinition.MatchPattern q = (MatchPattern) itsPatternDefinition.itsQueryStatements [ matchingStmtIndex ];

		IList<BoundFrame> ans = null;

		if ( (bindVars & PatternDefinition.LabelBinding) != 0 )
			ans = matchQualifier ( ans, stmt, q.itsLabelVariable, stmt, matchingStmtIndex, bindVars );
		if ( (bindVars & PatternDefinition.SubjectBinding) != 0 )
			ans = matchQualifier ( ans, stmt.itsSubject, q.itsSubjectVariable, stmt, matchingStmtIndex, bindVars );
		if ( (bindVars & PatternDefinition.VerbBinding) != 0 )
			ans = matchQualifier ( ans, stmt.itsVerb, q.itsVerbVariable, stmt, matchingStmtIndex, bindVars );
		if ( (bindVars & PatternDefinition.ObjectBinding) != 0 )
			ans = matchQualifier ( ans, stmt.itsObject, q.itsObjectVariable, stmt, matchingStmtIndex, bindVars );

		return ans;
	}

	private void neuterQualifierList ( int [] qa ) {
		if ( qa != null ) {
			for ( int i = 0; i < qa.length; i++ ) {
				int qs = qa [ i ];
				if ( itsStatementMatching [ qs ] == NotFound )
					markAsMatched ( qs );
			}
		}
	}

	private IList<BoundFrame> matchQualifier ( IList<BoundFrame> ans, DMIElem var, int varIndex, DMIElem stmt, int matchingStmtIndex, int bindVars ) {
		int [] qa = itsPatternDefinition.itsVariableDefinitions [ varIndex ].itsSubjectQualifyingStatements;
		if ( qa != null ) {
			for ( int i = 0; i < qa.length; i++ ) {
				int qs = qa [ i ];
				if ( itsStatementMatching [ qs ] == NotFound ) {
					PatternDefinition.VariableQualifierPattern vqp = (PatternDefinition.VariableQualifierPattern) itsPatternDefinition.itsQueryStatements [ qs ];
					DMIElem ob = itsVariableBindings [ vqp.itsObjectVariable ];
					if ( ob != null )
						ans = addBinding ( matches ( vqp.itsQVE, var, ob ), ans, bindVars, qs, stmt, matchingStmtIndex );
				}
			}
		}

		qa = itsPatternDefinition.itsVariableDefinitions [ varIndex ].itsObjectQualifyingStatements;
		if ( qa != null ) {
			for ( int i = 0; i < qa.length; i++ ) {
				int qs = qa [ i ];
				if ( itsStatementMatching [ qs ] == NotFound ) {
					PatternDefinition.VariableQualifierPattern vqp = (PatternDefinition.VariableQualifierPattern) itsPatternDefinition.itsQueryStatements [ qs ];
					DMIElem sb = itsVariableBindings [ vqp.itsSubjectVariable ];
					if ( sb != null )
						ans = addBinding ( matches ( vqp.itsQVE, sb, var ), ans, bindVars, qs, stmt, matchingStmtIndex );
				}
			}
		}

		return ans;
	}

	private IList<BoundFrame> addBinding ( boolean match, IList<BoundFrame> ans, int bindVars, int qs, DMIElem stmt, int matchingStmtIndex ) {
		if ( !match ) {
			// no match, don't add it; and make sure there's a new list so it gets ignored.
			if ( ans == null )
				ans = new XSetList<BoundFrame> (); // didn't match, so exclude it
		}
		else {
			// match:
			if ( ans == null && !itsPatternDefinition.itsQueryStatements [ qs ].itsIsExcluded ) {
				// short cut; modify in place and proceed.
				// if this doesn't materially change the frame i.e. could not be excluded, then we're going to
				//  change the frame in place and add it instead of making a new frame
				markAsMatched ( matchingStmtIndex );
			}
			else {
				if ( ans == null )
					ans = new XSetList<BoundFrame> (); // different match, so include it
				BoundFrame nbf = new BoundFrame ( this, qs );
				IList<BoundFrame> nbfList = nbf.matchQualifiers ( bindVars, stmt, matchingStmtIndex );
				if ( nbfList != null )
					for ( int k = 0; k < nbfList.size (); k++ )
						ans.add ( nbfList.get ( k ) );
				else
					ans.add ( nbf );
			}
		}

		return ans;
	}

	//
	// Given a root pattern, and a new (sub) pattern (with (advanced by one) match state) 
	// 	splice in the new (sub) pattern where appropriate, and return a new root pattern as needed.
	// 
	// We want to recurse to the bottom and clone/substitute on the way back up as substitutions necessitate.
	// 
	// If we are the substitution target, we'll just return that.
	// If underneath us hasn't changed, we'll do nothing and return self.
	// If something underneath us has changed, we'll shallow copy, splice and return the new.
	//
	private BoundFrame substituteSubPatternFrame ( BoundFrame replaceMe, BoundFrame withThis, ISet<BoundFork []> updatedSubpatterns ) {
		if ( this == replaceMe )
			return withThis;

		for ( int i = 0; i < itsSubForks.length; i++ ) {
			BoundFork subFork = itsSubForks [ i ];
			if ( subFork != null ) {
				IList<BoundFrame> ff = subFork.getFrames ();
				for ( int j = 0; j < ff.size (); j++ ) {
					BoundFrame trg = ff.get ( j );
					BoundFrame chg = trg.substituteSubPatternFrame ( replaceMe, withThis, updatedSubpatterns );
					if ( chg != trg ) {

						BoundFrame copy = new BoundFrame ( this );

						// $20$: BoundFork newSB = new BoundFork (copy.itsSubForks [ i ]);
						BoundFork newSB = copy.itsSubForks [ i ];

						// We always keep BoundFrame 0 of any SubFork unmatched, so as to be ready to match any new input.
						// So, if we are matching to BoundFrame 0, then we simply add the replacement instead of substituting for it.

						if ( j == 0 )
							newSB.addFrame ( chg );
						else
							newSB.replaceFrame ( j, chg );

						updatedSubpatterns.add ( copy.itsSubForks );

						// $20$: newSB.sortSubpatternFrames ();
						// $20$: copy.itsSubForks [ i ] = newSB;

						// A BoundFrame is a tree that branches out, and none of the branches are shared (amongst that BoundFrame)
						// (of course, branches can be shared by other BoundFrame trees in a read-only mode)
						// Since we've found the slot that need substitution, we can stop looking further in this tree's (BoundFrame's) branches 
						// (also note that this is desirable, because we may have enlarged SubFork at i anyway, and don't need to revisit the addition)

						return copy;
					}
				}
			}
		}

		// return copy == null ? this : copy;
		return this;
	}

	private void decrementMatchingStatementCount ( int stmtIndex ) {
		BasePattern bpa[] = itsPatternDefinition.itsQueryStatements;
		BasePattern bp = bpa [ stmtIndex ];
		if ( bp.itsNegation )
			itsStmtNegationMatchCount--;
		else
			itsStmtMatchCount--;
	}

	private void markAsMatched ( int matchingStmtIndex ) {
		check ();
		decrementMatchingStatementCount ( matchingStmtIndex );
		itsStatementMatching [ matchingStmtIndex ] = Matched;

		int [] exclusionsList = itsPatternDefinition.itsQueryStatements [ matchingStmtIndex ].itsExclusions;
		if ( exclusionsList != null ) {
			for ( int i = 0; i < exclusionsList.length; i++ ) {
				int exclStmt = exclusionsList [ i ];
				assert itsStatementMatching [ exclStmt ] != Matched : "BoundFrame.BoundFrame: excluded statement should not have been Matched";
				if ( itsStatementMatching [ exclStmt ] != Excluded ) {
					itsStatementMatching [ exclStmt ] = Excluded;
					check ();
					decrementMatchingStatementCount ( exclStmt );
				}
			}
		}
	}

	private void bindVariable ( int ix, DMIElem m ) {
		assert itsVariableBindings [ ix ] == null : "BoundFrame.BindVariable: var at " + ix + " is already bound.";
		itsVariableBindings [ ix ] = m;
		int [] ql = itsPatternDefinition.itsVariableDefinitions [ ix ].itsSubjectQualifyingStatements;
		neuterQualifierList ( ql );
		ql = itsPatternDefinition.itsVariableDefinitions [ ix ].itsObjectQualifyingStatements;
		neuterQualifierList ( ql );
	}

	public void bindInputVariable ( int ix, DMIElem m ) {
		assert itsPatternDefinition.itsVariableDefinitions [ ix ].itsKind == QueryVariableKindEnum.InputVariable : "BoundFrame.BindInputVariable: var at " + ix + " is not a prebound variable.";
		bindVariable ( ix, m );
	}

	public void bindGivenValueVariable ( int ix, DMIElem m ) {
		assert itsPatternDefinition.itsVariableDefinitions [ ix ].itsKind == QueryVariableKindEnum.GivenValueVariable : "BoundFrame.BindGivenValueVariable: var at " + ix + " is not a prebound variable.";
		bindVariable ( ix, m );
	}

	// Returns false if candidate is rejected
	// Returns true and accumulates to array of new bound variables if accepted
	public boolean acceptCandidate ( DMIElem m, BoundFork boundForkOut, ISet<BoundFork []> updatedSubpatterns ) {
		return acceptCandidate ( m, this, null, 0, boundForkOut, 0, updatedSubpatterns, null );
	}

	private boolean acceptCandidate ( DMIElem m, BoundFrame root, BoundFrame parent, int parStmtIndex, BoundFork outBoundFork, int lv, ISet<BoundFork []> updatedSubpatterns, BoundFrame ze ) {
		boolean ans = false;

		assert m.isStatement () : "BoundFrame.acceptCandidate: m should be a statement/relationship";

		for ( int i = 0; i < itsPatternDefinition.itsQueryStatements.length; i++ ) {
			if ( itsStatementMatching [ i ] == NotFound ) {
				BasePattern b = itsPatternDefinition.itsQueryStatements [ i ];
				if ( b instanceof MatchPattern ) {
					intRef bindRef = new intRef ();
					if ( acceptAtPos ( m, i, bindRef ) ) { // potential binding match

						int bindVars = bindRef.val;

						/* if ( parent == null || !precludeInSubChain ( parent, parStmtIndex, m, i, bindRef.val ) ) */{ // compatible with other chains

							if ( (Tracing.gTraceSet & Tracing.TraceFrameZ) != 0 )
								Tracing.gTraceFile.println ( "+++Adding binding for " + i + " on " + m.itsIndex + " (lv=" + lv + ")" );

							BoundFrame nbf = new BoundFrame ( this, m, i, bindVars );
							IList<BoundFrame> nbfList = nbf.matchQualifiers ( bindVars, m, i );

							//
							// Variable Qualifiers are statements that must prove true, but don't equate to graphical statements (and never bind to variables).
							// Since they don't equate to statement matches in the graph, we validate them as soon as we match variables that they qualify.
							// Thus, if, as a result of accepting m, there aren't any additional variable qualifiers, then nbf is good to go.
							// However, if there were variable qualifiers, we get back a list of new bindings with the validations and we don't use nbf.
							// When we get back a non-null list we must honor as it has the matched qualifiers in it (whereas nbf doesn't).
							// When there are no variable qualifiers, we get back null from matchQualifiers.
							// Note that when the list is non-null, yet empty (zero length), 
							//	it means that there were variable qualifiers and they all failed, so m was actually a bad match, hence nbf is bad.
							//	Thus, we add no bindings for m.
							// 

							if ( nbfList != null )
								for ( int k = 0; k < nbfList.size (); k++ )
									ans |= addBinding ( m, bindVars, nbfList.get ( k ), root, parent, parStmtIndex, outBoundFork, lv, updatedSubpatterns, ze );
							else
								ans = addBinding ( m, bindVars, nbf, root, parent, parStmtIndex, outBoundFork, lv, updatedSubpatterns, ze );
						}
					}
				}
				else if ( b instanceof SubPattern ) {
					IList<BoundFrame> ff = itsSubForks [ i ].getFrames ();
					BoundFrame newZE = ff.get ( 0 );
					boolean tryRest = true;

					if ( ze != newZE ) {
						boolean zans = newZE.acceptCandidate ( m, root, this, i, outBoundFork, lv, updatedSubpatterns, newZE );
						if ( zans ) {
							ans = true;
							tryRest = true;
							// zero is always the empty frame, so if zero doesn't offer a match, don't bother checking the other frames
						}
					}

					if ( tryRest ) {
						for ( int j = 1; j < ff.size (); j++ )
							ans |= ff.get ( j ).acceptCandidate ( m, root, this, i, outBoundFork, lv, updatedSubpatterns, null );
					}
				}
			}
		}

		if ( (Tracing.gTraceSet & Tracing.TraceFrameZ) != 0 ) {
			if ( !ans && lv == 0 )
				Tracing.gTraceFile.println ( "$$$Rejecting as no match " + java.lang.Integer.toString ( m.itsIndex ) );
			if ( ans && lv > 0 )
				Tracing.gTraceFile.println ( "$$$Found additional match (" + lv + ") " + java.lang.Integer.toString ( m.itsIndex ) );
		}

		return ans;
	}

	private boolean addBinding ( DMIElem m, int bindVars, BoundFrame nbf, BoundFrame root, BoundFrame parent, int parStmtIndex, BoundFork outBoundFork, int lv, ISet<BoundFork []> updatedSubpatterns, BoundFrame ze ) {
		BoundFrame binding = root.substituteSubPatternFrame ( this, nbf, updatedSubpatterns );
		outBoundFork.addFrame ( binding );

		if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
			binding.traceOut ();
			Tracing.gTraceFile.println ();
		}

		//
		// This addresses the possibility of one actual statement matching the more than one pattern statement
		//		meaning, we don't want to miss matches in esoteric cases, such as:
		//			?a ? ?c
		//			?a ?b ?
		//			...
		//		where the proper match has a single statement involving a b c
		//		instead of two statements matching a ? c and a b ? separately.
		//  Without the following, we'll get a frame for each query statement line,
		//  	but no frame that represents both at the same time.
		//	To fix this, we recursively invoke Accept on the result (but only if bindings have been generated):
		//		you'd expect the same boolean true result, of course, so the return value (ans) isn't updated
		//		and if more bindings came out, they are added to the set
		//  We don't do this recursion, of course, if no new bindings were discovered.
		//

		if ( bindVars != 0 )
			binding.acceptCandidate ( m, binding, null, 0, outBoundFork, lv + 1, updatedSubpatterns, ze );

		return true;
	}

	public boolean rejected () {
		if ( !itsPatternDefinition.itsHasNegation || itsStmtNegationMatchCount > 0 )
			return false;

		if ( itsSubForks == null )
			return true;

		boolean ans = true;
		for ( int i = 0; i < itsSubForks.length; i++ ) {
			if ( itsSubForks [ i ] != null && itsPatternDefinition.itsQueryStatements [ i ].itsNegation && !completed ( i, null, null ) )
				ans = false;
		}

		return ans;
	}

	public boolean succcessPathComplete () {
		return itsStmtMatchCount == 0;
	}

	public boolean couldBeRejected () {
		return itsPatternDefinition.itsHasNegation;
	}

	public boolean markAsSuccessRoot () {
		if ( getSuccessRoot () == null )
			itsIsSuccessRoot = true;
		return itsIsSuccessRoot;
	}

	public BoundFrame getSuccessRoot () {
		BoundFrame ans = this;
		while ( !ans.itsIsSuccessRoot ) {
			ans = ans.itsSuccessRoot;
			if ( ans == null )
				break;
		}
		return ans;
	}

	// Have all statements have been matched in some way (matched or excluded)?
	public boolean completed ( ISet<DMIElem> ansSet, IList<InferredRelationship> inferSet, ISet<DMIElem> reportSet ) {
		boolean ans = (itsStmtMatchCount == 0);

		/*
		 * queries involving instanceOf and subclassOf won't work right, since the statements aren't all in the graph
		 * the following somewhat expensive code handles the cases such as :
		 * 		 ?a instanceOf ?c  -and-   ?a subclassOf ?c
		 * However, if the instanceOf or subclassOf are constants in the query, 
		 *   then the query should have been transformed to use query variable qualifiers instead, 
		 *     much more efficient.
		 * Otherwise, these must be some kind of ?a ?b ?c statement where ?b happens to be instanceOf or subclassOf
		 * However, general cases of ?a ?b ?c where ?b is some instanceOf won't work even with this special case code, because
		 *   not all the instanceOf and subclassOf statements are in the graph.
		 * To fix this, we'd have to add all of the inferred instancing and subclassing to the graph, not just the cache.
		 * Ultimately, I think that's the only solution, and if we did that, we wouldn't need this special casing.
		 * -- OK, we're not doing that, but we special case instance of and subclass of in another way: as qualifiers.
		boolean ans = true;
		if ( itsStmtMatchCount == 0 ) {}
		else {
			for ( int i = 0; i < itsStatementMatching.length; i++ ) {
				if ( itsStatementMatching [ i ] == NotFound && !isSpecialCaseBuiltIn (i, b) ) {
					ans = false;
					break;
				}
			}
		}
		*/

		if ( ans ) {
			if ( itsSubForks == null || itsPatternDefinition.itsPositiveSubpatternCount == 0 )
				accumulateSuccess ( ansSet, inferSet );
			else {
				ans = true;

				if ( itsPatternDefinition.itsPositiveSubpatternCount == 1 ) {
					for ( int i = 0; i < itsSubForks.length; i++ ) {
						if ( itsSubForks [ i ] != null && !itsPatternDefinition.itsQueryStatements [ i ].itsNegation && !completed ( i, ansSet, inferSet ) )
							ans = false;
					}
				}
				else {
					for ( int i = 0; i < itsSubForks.length; i++ ) {
						if ( itsSubForks [ i ] != null && !itsPatternDefinition.itsQueryStatements [ i ].itsNegation && !completed ( i, null, null ) )
							ans = false;
					}
					if ( ans ) {
						for ( int i = 0; i < itsSubForks.length; i++ ) {
							if ( itsSubForks [ i ] != null && !itsPatternDefinition.itsQueryStatements [ i ].itsNegation && completed ( i, ansSet, inferSet ) )
								;
						}
					}
				}
			}
		}

		/*
		if ( ans && Tracing.gTraceFile != null ) {
			if ( this.itsVariableBindings [ 4 ] != this.itsVariableBindings [ 11 ] ) {
				int st = Tracing.gTraceSet;
				Tracing.gTraceFile.print ("***NZA: ");
				Tracing.gTraceSet |= Tracing.TraceFrame | Tracing.TraceFrameX | Tracing.TraceFrameY;
				traceOut ();
				Tracing.gTraceFile.println ();
				Tracing.gTraceSet = st;
			}
		}
		*/

		return ans;
	}

	public boolean usesSubForkIn ( ISet<BoundFork []> updatedSubpatterns ) {
		if ( itsSubForks != null ) {
			if ( updatedSubpatterns.contains ( itsSubForks ) )
				return true;
			int cnt = itsSubForks.length;
			for ( int i = 0; i < cnt; i++ ) {
				BoundFork sbf = itsSubForks [ i ];
				if ( sbf != null && sbf.usesSubForkIn ( updatedSubpatterns ) )
					return true;
			}
		}
		return false;
	}

	private boolean completed ( int stmtIndex, ISet<DMIElem> ansSet, IList<InferredRelationship> inferSet ) {
		boolean ans = false;

		SubPattern sbp = (SubPattern) itsPatternDefinition.itsQueryStatements [ stmtIndex ];

		int minChainLen = sbp.itsMinCount;

		int parLeftVarNum = sbp.itsParentLeftVariable;
		int parRightVarNum = sbp.itsParentRightVariable;

		int subLeftVarNum = sbp.itsChildLeftVariable;
		int subRightVarNum = sbp.itsChildRightVariable;

		DMIElem [] parVars = itsVariableBindings;

		DMIElem parLeft = parVars [ parLeftVarNum ];
		DMIElem parRight = parVars [ parRightVarNum ];

		//
		// Need to accommodate the case where parLeft or parRight == null
		//	That is, the variable remains unbound in the parent even when all statements are matched
		// 	(because it is only referenced in the sub query and not bound to anything in the parent).
		//

		// nulls as wild card...
		if ( parLeft == null || parRight == null || parLeft == parRight ) {
			// because parLeft == parRight, we don't want to do any chain matches, we'll only take the chain length of zero: no matches
			// this prevents cases where the same statement matching forward in one sub frame, 
			// then reversed in another sub frame
			// which would result in a chain where chain start left == chain end right,
			// being a chain of the same statement..
			// it could potentially match the subchain to the parent when parLeft == parRight
			// but we're not interested, so leave early.

			// Case of chain length == 0
			if ( minChainLen == 0 ) {
				// In this case, leftParent must == rightParent
				accumulateSuccess ( ansSet, inferSet );
				if ( parLeft != null && parRight != null )
					return true;
			}

			if ( parLeft != null && parRight != null ) {
				// if we can't take chain length of zero, then by definition, we cannot match with this frame
				return false;
			}
		}

		IList<BoundFrame> sbff = itsSubForks [ stmtIndex ].getFrames ();

		// Collect all the subpatterns that are completed, which are elements of the chain we're going to form.
		IList<BoundFrame> chainElements = new XSetList<BoundFrame> ();
		for ( BoundFrame bf : sbff ) {
			if ( bf.completed ( null, /*inferSet*/null, null ) )
				chainElements.add ( bf );
		}

		int maxChainLen = sbp.itsMaxCount;
		if ( maxChainLen <= 0 )
			maxChainLen = chainElements.size ();

		/*
		// Case of chain length of 1
		if ( maxChainLen >= 1 ) {
			for ( int i = 0; i < chainElements.size (); i++ ) {
				BoundFrame bf = chainElements.get ( i );
				if ( bf != null ) {
					DMIElem [] childVars = bf.itsVariableBindings;
					DMIElem childLeft = childVars [ subLeftVarNum ];
					DMIElem childRight = childVars [ subRightVarNum ];
					if ( (parLeft == null || parLeft == childLeft) && (parRight == null || childRight == parRight) ) {
						ans = true;
						accumulateSuccess ( ansSet, inferSet );
						bf.accumulateSuccess ( ansSet, inferSet );

						// no need to prevent longer matches as they're perfectly valid
						// // remove this chain element from further consideration in longer chains here.
						// chainElements.clear ( i );
						// // $20$: sbff.remove (bf); // remove this chain element from the subfork, too, no need to match with it anymore.
					}
				}
			}
		}

		//
		// NOTE: Having removed preclude-sub-chain-cycle-elimination (since it filtered good matches in some examples), 
		//	we need to eliminate cycles here in the match instead.
		//

		if ( maxChainLen >= 2 ) {
			for ( int i = 0; i < chainElements.size (); i++ ) {
				BoundFrame bf = chainElements.get ( i );
				if ( bf != null ) {
					DMIElem [] childVars = bf.itsVariableBindings;
					DMIElem childLeft = childVars [ subLeftVarNum ];
					if ( parLeft == null || parLeft == childLeft ) {
						DMIElem childRight = childVars [ subRightVarNum ];
						for ( int j = 0; j < chainElements.size (); j++ ) {
							if ( i == j )
								continue;
							BoundFrame bfj = chainElements.get ( j );
							if ( bfj != null ) {
								DMIElem [] childVarsj = bfj.itsVariableBindings;
								DMIElem childLeftj = childVarsj [ subLeftVarNum ];
								if ( childRight == childLeftj ) {
									DMIElem childRightj = childVarsj [ subRightVarNum ];
									if ( parRight == null || childRightj == parRight ) {
										ans = true;
										accumulateSuccess ( ansSet, inferSet );
										bf.accumulateSuccess ( ansSet, inferSet );
										bfj.accumulateSuccess ( ansSet, inferSet );

										// no need to prevent longer matches as they're perfectly valid
										// // in trying to prevent longer matches, this also erroneously prevents same sized alternate matches... :(
										// chainElements.clear ( i ); //                    remove this chain element from further consideration in longer chains here.
										// chainElements.clear ( j );
										// // $20$: sbff.remove (bf); // remove this chain element from the subfork, too, no need to match with it anymore.
										// // $20$: sbff.remove (bfj); // remove this chain element from the subfork, too, no need to match with it anymore.
									}
								}
							}
						}
					}
				}
			}
		}
		*/

		if ( minChainLen == 0 )
			minChainLen = 1;

		if ( minChainLen <= maxChainLen ) {
			ISet<BoundFrame> bfSet = new XSetList<BoundFrame> ();
			ans |= matchToDepth ( 0, minChainLen, maxChainLen, parLeft, subLeftVarNum, parRight, subRightVarNum, chainElements, bfSet, ansSet, inferSet );
		}

		// $20$: chainElements.trim ();
		// $20$: sbff.trim ();

		return ans;
	}

	private boolean matchToDepth ( int currDepth, int minDepth, int maxDepth, DMIElem parLeft, int subLeftVarNum, DMIElem parRight, int subRightVarNum, IList<BoundFrame> chainElements, IList<BoundFrame> bfSet, ISet<DMIElem> ansSet, IList<InferredRelationship> inferSet ) {
		boolean ans = false;

		if ( currDepth >= minDepth ) {
			if ( parRight == null || parRight == bfSet.get ( currDepth - 1 ).itsVariableBindings [ subRightVarNum ] ) {
				accumulateSuccess ( ansSet, inferSet );
				for ( int i = 0; i < bfSet.size (); i++ )
					bfSet.get ( i ).accumulateSuccess ( ansSet, inferSet );
				ans = true;
			}
		}

		if ( currDepth < maxDepth ) {
			// find another matching element in chain
			for ( int i = 0; i < chainElements.size (); i++ ) {
				BoundFrame bf = chainElements.get ( i );

				// chain must match parLeft (parLeft allowed to be wild card initially)
				if ( (currDepth == 0 && parLeft == null) || bf.itsVariableBindings [ subLeftVarNum ] == parLeft ) {

					// chain must not already be in the set
					if ( bfSet.contains ( bf ) )
						continue;

					// chain must not cycle on parLeft
					if ( currDepth > 0 && hasCycle ( bfSet, bf.itsVariableBindings [ subRightVarNum ], subLeftVarNum ) )
						continue;

					// extend chain
					@SuppressWarnings("unchecked")
					IList<BoundFrame> nbfSet = (IList<BoundFrame>) bfSet.clone ();
					nbfSet.add ( bf );

					ans |= matchToDepth ( currDepth + 1, minDepth, maxDepth, bf.itsVariableBindings [ subRightVarNum ], subLeftVarNum, parRight, subRightVarNum, chainElements, nbfSet, ansSet, inferSet );
				}
			}
		}

		return ans;
	}

	private boolean hasCycle ( IList<BoundFrame> bfSet, DMIElem parRight, int subLeftVarNum ) {
		boolean ans = false;
		for ( int i = 0; i < bfSet.size (); i++ ) {
			if ( bfSet.get ( i ).itsVariableBindings [ subLeftVarNum ] == parRight )
				return true;
		}
		return ans;
	}

	private void accumulateSuccess ( ISet<DMIElem> ansSet, IList<InferredRelationship> inferSet ) {
		if ( ansSet != null ) {
			DMIElem [] vd = itsVariableBindings;
			for ( int i = 0; i < vd.length; i++ ) {
				if ( vd [ i ] != null && itsPatternDefinition.itsVariableDefinitions [ i ].itsTaggedForOutput )
					ansSet.addToSet ( vd [ i ] );
			}
			vd = itsStatementBindings;
			for ( int i = 0; i < vd.length; i++ ) {
				if ( vd [ i ] != null && itsPatternDefinition.itsQueryStatements [ i ].itsTaggedForOutput )
					ansSet.addToSet ( vd [ i ] );
			}
			if ( inferSet != null ) {
				MatchPattern [] ifs = this.itsPatternDefinition.itsInferredStatements;
				if ( ifs != null ) {
					for ( int i = 0; i < ifs.length; i++ ) {
						MatchPattern n = ifs [ i ];
						int sbix = n.itsSubjectVariable;
						DMIElem sb = itsVariableBindings [ sbix ];
						int vbix = n.itsVerbVariable;
						DMIElem vb = itsVariableBindings [ vbix ];
						int obix = n.itsObjectVariable;
						DMIElem ob = itsVariableBindings [ obix ];
						inferSet.add ( new InferredRelationship ( sb, vb, ob ) );
					}
				}
			}
		}
	}

	// NOTE: this method should not be used to see if it is currently completed!!!
	// returns true if the frame has no (variable length) sub patterns
	public boolean whenCompletedisFinished () {
		return itsSubForks == null;
	}

	public boolean acceptAtPos ( DMIElem m, int si, intRef bindRef ) {
		// use this statement to try match up something 
		PatternDefinition.MatchPattern q = (MatchPattern) itsPatternDefinition.itsQueryStatements [ si ];
		// label, subject, verb, object refer to variables
		// see if any of these 4 variables are newly found

		return isCompatible ( m, q.itsLabelVariable, PatternDefinition.LabelBinding, bindRef ) &&
				isCompatible ( m.itsSubject, q.itsSubjectVariable, PatternDefinition.SubjectBinding, bindRef ) &&
				isCompatible ( m.itsVerb, q.itsVerbVariable, PatternDefinition.VerbBinding, bindRef ) &&
				isCompatible ( m.itsObject, q.itsObjectVariable, PatternDefinition.ObjectBinding, bindRef );
	}

	private boolean isCompatible ( DMIElem m, int varIndex, int pos, intRef bindRef ) {
		// Compatible if there is no var, meaning wild card
		if ( varIndex < 0 )
			return true;

		// Compatible if there is a var but it is currently unbound
		DMIElem x = itsVariableBindings [ varIndex ];
		if ( x == null ) {
			/* if ( !qualifies (m, varIndex) )
				return false; // can't bind, not qualified
			else */{
				if ( bindRef != null )
					bindRef.val |= pos; // let caller know we can bind to this pos
				return true;
			}
		}

		// And lastly, compatible if bound to the right var
		return m == x;
	}

	private static boolean matches ( QueryVariableExpressionEnum qve, DMIElem sb, DMIElem ob ) {
		boolean ans = false;
		switch ( qve ) {
		case InstanceOf :
			ans = sb.instanceOf ( ob );
			break;
		case SubclassOf :
			ans = sb.subclassOf ( ob );
			break;
		case IdenticalTo :
			ans = (sb == ob);
			break;
		case NotIdenticalTo :
			ans = (sb != ob);
			break;
		}
		return ans;
	}

	/*
	* Subpatterns support repetitive matching.  
	* Once fully matched, the repetitive matches chain together (certainly not necessarily in order we found them)
	* 	by linking the left of one match to the right of another match.
	*  There should be a remaining left and right unmatched by neighbors.
	*  These should match the left and right, respectively in the parent.
	*  
	* To eliminate unwanted cycles in the match, we need to guard against matching the same left var or same right var in another neighbor.
	* 
		 * Need to look in enclosing sub pattern: subFork
		 * 	then, identify subLeft (subRight)
		 * 	then, see if this statement causes subLeft (subRight) to be bound, if so find value for variable
		 *  if so, then see if any other subpattern chain link has the same bound subLeft (subRight)
		 *  and return whether so.

	private boolean precludeInSubChain ( BoundFrame parent, int parStmtIndex, DMIElem stmt, int si, int bindVars ) {
		SubPattern sb = (SubPattern) parent.itsPatternDefinition.itsQueryStatements [ parStmtIndex ];

		int lv = sb.itsChildLeftVariable;
		int rv = sb.itsChildRightVariable;

		PatternDefinition.MatchPattern q = (MatchPattern) itsPatternDefinition.itsQueryStatements [ si ];

		BoundFork subFork = parent.itsSubForks [ parStmtIndex ];

		if ( (bindVars & PatternDefinition.LabelBinding) != 0 )
			if ( findCycle ( q.itsLabelVariable, lv, rv, stmt, subFork ) )
				return true;
		if ( (bindVars & PatternDefinition.SubjectBinding) != 0 )
			if ( findCycle ( q.itsSubjectVariable, lv, rv, stmt.itsSubject, subFork ) )
				return true;
		if ( (bindVars & PatternDefinition.VerbBinding) != 0 )
			if ( findCycle ( q.itsVerbVariable, lv, rv, stmt.itsVerb, subFork ) )
				return true;
		if ( (bindVars & PatternDefinition.ObjectBinding) != 0 )
			if ( findCycle ( q.itsObjectVariable, lv, rv, stmt.itsObject, subFork ) )
				return true;

		return false;
	}

	private static boolean findCycle ( int vPos, int leftVar, int rightVar, DMIElem m, BoundFork subFork ) {
		if ( vPos == leftVar )
			return findUse ( leftVar, m, subFork );
		if ( vPos == rightVar )
			return findUse ( rightVar, m, subFork );
		return false;
	}

	// left or right var position being searched in each frame of the fork/chain for m
	private static boolean findUse ( int vPos, DMIElem m, BoundFork sf ) {
		IList<BoundFrame> bfl = sf.getFrames ();
		for ( int i = 0; i < bfl.size (); i++ ) {
			BoundFrame bf = bfl.get ( i );
			if ( bf.itsVariableBindings [ vPos ] == m )
				return true;
		}
		return false;
	}
	*/

	public void traceOut () {
		if ( (Tracing.gTraceSet & Tracing.TraceFrame) != 0 ) {
			if ( (Tracing.gTraceSet & Tracing.TraceFrameX) != 0 )
				Tracing.gTraceFile.print ( "#" + itsIndex );
			if ( (Tracing.gTraceSet & Tracing.TraceFrameX) != 0 )
				Tracing.gTraceFile.print ( ":" + itsDerivedFromIndex + "," + (itsAdvanceOrPropagate ? "T" : "F") );
			if ( (Tracing.gTraceSet & Tracing.TraceFrameY) != 0 )
				Tracing.gTraceFile.println ();
			for ( int i = 0; i < itsStatementMatching.length; i++ ) {
				if ( itsStatementMatching [ i ] == Matched ) {
					Tracing.gTraceFile.print ( " " + i );
					if ( (Tracing.gTraceSet & Tracing.TraceFrameY) != 0 ) {
						PatternDefinition.BasePattern b = itsPatternDefinition.itsQueryStatements [ i ];
						// b.dump(gTracing.gTraceFile);
						if ( b instanceof MatchPattern ) {
							MatchPattern p = (MatchPattern) b;
							Tracing.gTraceFile.println ( ":  (" + varName ( p.itsLabelVariable ) + ")  " +
									varName ( p.itsSubjectVariable ) + "  --  " +
									varName ( p.itsVerbVariable ) + "  --  " +
									varName ( p.itsObjectVariable ) );
						}
						else if ( b instanceof SubPattern ) {
							SubPattern s = (SubPattern) b;
							Tracing.gTraceFile.println ( ":  [" + s.itsMinCount + ":" + s.itsMaxCount + "] <" + s.itsParentLeftVariable + "," + s.itsParentRightVariable + ">" );
						}
					}
				}
			}
			if ( itsSubForks != null ) {
				for ( int i = 0; i < itsSubForks.length; i++ ) {
					BoundFork sf = itsSubForks [ i ];
					if ( sf != null ) {
						Tracing.gTraceFile.print ( " {" + i + " " );
						IList<BoundFrame> ff = sf.getFrames ();
						for ( int j = 1; j < ff.size (); j++ ) { // frame zero is always empty, so to save log file generation, we skip it.
							BoundFrame bf = ff.get ( j );
							if ( (Tracing.gTraceSet & Tracing.TraceFrameX) != 0 )
								Tracing.gTraceFile.print ( " [" + j );
							else
								Tracing.gTraceFile.print ( " [" );
							bf.traceOut ();
							Tracing.gTraceFile.print ( " ]" );
						}
						Tracing.gTraceFile.print ( "} " );
					}
				}
			}
		}
	}

	public String varName ( int i ) {
		String ans = "*";
		if ( i >= 0 ) {
			DMIElem m = itsVariableBindings [ i ];
			if ( m == null )
				ans = "??";
			else {
				String name = NameUtilities.hasMCText ( NameUtilities.getFirstName ( m ) );
				if ( name != null ) {
					ans = m.itsIndex + " {" + name + "}";
				} else {
					ans = m.itsIndex + " [" + NameUtilities.getMCText ( m.itsVerb ) + "]";
				}
			}
		}
		return ans;
	}

	public void suggestSearchSources ( ISet<DMIElem> ans, ConceptManager cm ) {
		suggestSearchSources ( ans, false, cm );
		if ( ans.size () == 0 )
			suggestSearchSources ( ans, true, cm );
		if ( ans.size () == 0 )
			ans.mergeListIntoSet ( cm.itsBaseVocab.gConcept.itsFullInstanceSet );
	}

	private void suggestSearchSources ( ISet<DMIElem> ans, boolean sbob, ConceptManager cm ) {
		for ( int i = 0; i < itsPatternDefinition.itsQueryStatements.length; i++ ) {
			BasePattern b = itsPatternDefinition.itsQueryStatements [ i ];
			if ( b instanceof MatchPattern ) {
				MatchPattern p = (MatchPattern) b;
				if ( p.itsVerbVariable >= 0 ) {
					DMIElem vb = itsVariableBindings [ p.itsVerbVariable ];
					if ( vb != null )
						ans.add ( vb );
				}
				else if ( sbob ) {
					if ( p.itsSubjectVariable >= 0 ) {
						DMIElem sb = itsVariableBindings [ p.itsSubjectVariable ];
						if ( sb != null )
							ans.add ( sb );
					}
					if ( p.itsObjectVariable >= 0 ) {
						DMIElem ob = itsVariableBindings [ p.itsObjectVariable ];
						if ( ob != null )
							ans.add ( ob );
					}
				}
			}
			else if ( b instanceof SubPattern ) {
				itsSubForks [ i ].getFrames ().get ( 0 ).suggestSearchSources ( ans, cm );
			}
		}
	}

	@Override
	public boolean equals ( Object o ) {
		if ( o == null )
			return false;
		if ( !(o instanceof BoundFrame) )
			return false;
		return equals ( (BoundFrame) o );
	}

	@Override
	public int hashCode () {
		int ans = 0;
		int len = itsStatementMatching.length;
		for ( int i = 0; i < len; i++ ) {
			if ( itsStatementMatching [ i ] == Matched ) {
				ans ^= 1 << (i & 31);
				DMIElem m = itsStatementBindings [ i ];
				if ( m != null )
					ans += lang.rotateLeft ( m.itsIndex, i );
			}
		}

		/* $20$:
		if ( itsSubForks != null ) {
			ans = lang.rotateLeft (ans, 5);
			len = itsSubForks.length;
			for ( int i = 0; i < len; i++ ) {
				BoundFork sb = itsSubForks [ i ];
				if ( sb != null )
					ans += sb.getFrames ().size ();
			}
		}
		*/

		return ans;
	}

	public boolean equals ( BoundFrame f ) {
		if ( f == null )
			return false;

		if ( this == f )
			return true;

		if ( f.itsPatternDefinition != itsPatternDefinition )
			return false;

		if ( f.itsStmtMatchCount != itsStmtMatchCount )
			return false;

		int len = itsStatementBindings.length;
		for ( int i = 0; i < len; i++ ) {
			if ( f.itsStatementMatching [ i ] != itsStatementMatching [ i ] )
				return false;
			if ( f.itsStatementBindings [ i ] != itsStatementBindings [ i ] )
				return false;
		}

		len = itsVariableBindings.length;
		for ( int i = 0; i < len; i++ ) {
			if ( f.itsVariableBindings [ i ] != itsVariableBindings [ i ] )
				return false;
		}

		if ( itsSubForks != null ) {
			if ( f.itsSubForks == itsSubForks )
				return true;

			// $20$:
			return false;
			/*
			len = itsSubForks.length;
			for ( int i = 0; i < len; i++ ) {
				BoundFork tk = itsSubForks [ i ];
				BoundFork fk = f.itsSubForks [ i ];
				if ( fk != tk ) {
					if ( fk == null )
						return false;
					if ( tk == null )
						return false;
					if ( !fk.equals (tk) )
						return false;
				}
			}
			*/
		}

		return true;
	}

	private static class BoundFrameComparator implements IComparator<BoundFrame> {
		@Override
		public boolean follows ( BoundFrame f1, BoundFrame f2 ) {
			assert f1.itsPatternDefinition == f2.itsPatternDefinition : "Cannot compare unlike patterns.";
			int len = f1.itsStatementBindings.length;
			for ( int i = 0; i < len; i++ ) {
				DMIElem s1 = f1.itsStatementBindings [ i ];
				DMIElem s2 = f2.itsStatementBindings [ i ];
				if ( s1 != s2 ) {
					if ( s1 == null )
						return true;
					if ( s2 == null )
						return false;
					return s1.itsIndex > s2.itsIndex;
				}
			}
			return false;
		}
	}

	public static final IComparator<BoundFrame>	cmp	= new BoundFrameComparator ();
}
