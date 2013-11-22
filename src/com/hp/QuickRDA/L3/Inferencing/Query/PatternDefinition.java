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


/*
 * The compiled pre-execution form of a query pattern.
 */
public class PatternDefinition {

	public static final int			NoBinding		= 0;
	public static final int			LabelBinding	= 1;
	public static final int			SubjectBinding	= 2;
	public static final int			VerbBinding		= 4;
	public static final int			ObjectBinding	= 8;

	public boolean					itsHasSubpatterns;
	public boolean					itsHasNegation;
	public int						itsPositiveSubpatternCount;

	public VariableDefinition []	itsVariableDefinitions;		// The set of variables defined by this query
	public int						itsFormalArgumentCount;		// Formals need to be the first ones in the array.
	public BasePattern []			itsQueryStatements;			// The set of matching statements defined by this query
	public int						itsInitialStmtCount;
	public int						itsInitialStmtNegationCount;

	public MatchPattern []			itsInferredStatements;

	public PatternDefinition ( VariableDefinition [] variableDefinitions, BasePattern [] queryStatements ) {
		this ( variableDefinitions, queryStatements, null );
	}

	public PatternDefinition ( VariableDefinition [] variableDefinitions, BasePattern [] queryStatements, MatchPattern [] inferredStatements ) {
		itsVariableDefinitions = variableDefinitions;

		for ( int i = 0; i < itsVariableDefinitions.length; i++ ) {
			VariableDefinition v = itsVariableDefinitions [ i ];
			if ( v.itsKind != QueryVariableKindEnum.InputVariable )
				break;
			itsFormalArgumentCount++;
		}

		itsQueryStatements = queryStatements;
		itsInitialStmtCount = CountMatches ( queryStatements, true );
		itsInitialStmtNegationCount = CountMatches ( queryStatements, false );
		itsHasNegation = (itsInitialStmtNegationCount > 0);

		for ( int i = 0; i < queryStatements.length; i++ ) {
			BasePattern b = queryStatements [ i ];
			if ( b instanceof SubPattern ) {
				SubPattern p = (SubPattern) b;
				if ( p.itsMinCount == 0 ) {
					if ( p.itsNegation )
						itsInitialStmtNegationCount--;
					else {
						itsInitialStmtCount--;
						itsPositiveSubpatternCount++;
					}
				}
				itsHasSubpatterns = true;
			}
		}

		itsInferredStatements = inferredStatements;

		for ( int i = 0; i < itsQueryStatements.length; i++ ) {
			int [] ex = itsQueryStatements [ i ].itsExclusions;
			if ( ex != null ) {
				for ( int j = 0; j < ex.length; j++ )
					itsQueryStatements [ ex [ j ] ].itsIsExcluded = true;
			}
		}
	}

	private int CountMatches ( BasePattern [] qs, boolean posOrNeg ) {
		int ans = 0;
		for ( int i = 0; i < qs.length; i++ ) {
			if ( qs [ i ].itsNegation ) {
				if ( !posOrNeg )
					ans++;
			}
			else if ( posOrNeg )
				ans++;
		}
		return ans;
	}

	public abstract static class BasePattern {
		boolean	itsTaggedForOutput;
		boolean	itsIsExcluded;
		boolean	itsNegation;
		int		itsExclusions[];

		BasePattern ( boolean tag, boolean neg, int [] ex ) {
			itsTaggedForOutput = tag;
			itsNegation = neg;
			itsExclusions = ex;
		}
	}

	//
	// The statements from the query referencing variables (-1 == don't care)
	//
	public static class MatchPattern extends BasePattern {
		int	itsLabelVariable;
		int	itsSubjectVariable;
		int	itsVerbVariable;
		int	itsObjectVariable;

		public MatchPattern ( int lb, int sb, int vb, int ob, boolean tag, boolean neg, int [] ex ) {
			super ( tag, neg, ex );
			itsLabelVariable = lb;
			itsSubjectVariable = sb;
			itsVerbVariable = vb;
			itsObjectVariable = ob;
		}
	}

	public static class VariableQualifierPattern extends BasePattern {
		int							itsSubjectVariable;
		QueryVariableExpressionEnum	itsQVE;
		int							itsObjectVariable;

		public VariableQualifierPattern ( int sb, QueryVariableExpressionEnum qve, int ob, boolean tag, boolean neg, int [] ex ) {
			super ( tag, neg, ex );
			itsSubjectVariable = sb;
			itsQVE = qve;
			itsObjectVariable = ob;
		}
	}

	public enum QueryVariableKindEnum {
		None,
		InputVariable,
		OutputVariable,
		MatchVariable,
		GivenValueVariable,
		BlankVariable
	}

	public int varBindings ( int si ) {
		int ans = 0;
		BasePattern b = itsQueryStatements [ si ];
		assert b instanceof MatchPattern : "PatternDefinition.varBindings: expecting MatchPattern at " + si;
		MatchPattern q = (MatchPattern) b;
		if ( q.itsLabelVariable >= 0 )
			ans |= LabelBinding;
		if ( q.itsSubjectVariable >= 0 )
			ans |= SubjectBinding;
		if ( q.itsVerbVariable >= 0 )
			ans |= VerbBinding;
		if ( q.itsObjectVariable >= 0 )
			ans |= ObjectBinding;
		return ans;
	}


	public enum QueryVariableExpressionEnum {
		InstanceOf, SubclassOf, IdenticalTo, NotIdenticalTo
	};

	// 
	// All the statements where the variable is used in the query
	// 
	public static class VariableDefinition {
		QueryVariableKindEnum	itsKind;
		public boolean			itsTaggedForOutput;
		// QueryVariableQualifier []	itsQualifiers;
		int []					itsSubjectQualifyingStatements;
		int []					itsObjectQualifyingStatements;

		public VariableDefinition ( QueryVariableKindEnum k, boolean tag ) {
			itsKind = k;
			itsTaggedForOutput = tag;
		}

		public VariableDefinition ( QueryVariableKindEnum k, boolean tag, int [] subQuals, int [] objQuals ) {
			this ( k, tag );
			itsSubjectQualifyingStatements = subQuals;
			itsObjectQualifyingStatements = objQuals;
		}
	}

	/*
	 * Sub patterns form an arbitrary chain of connections according to pattern, allowing wild card match counts in a chain.
	 *   In allowing any number of subpattern matches to occur, they must ultimately form some kind of complete chain (yet without loops).
	 *   This chaining with multiple copies of the sub pattern means that the "right" designated concept of one subpattern
	 *     must match up with the "left" designated concept of a neighboring sub pattern.  
	 *   (These neighbors can appear anywhere in the sub match array, they don't have to be consecutive in array position.)
	 * In addition, the whole chain needs to connect in two places to the parent pattern, in particular,
	 *   one sub pattern's left must connect to the parent left, and one pattern's right must connect to the parent right.
	 * Thus, whenever a subpattern is attached to a parent pattern, a left and a right concept are identified.
	 * Sub patterns connect one concept in the parent (the left) to another concept in the parent (the right).
	 * 
	 * min = minimum match count in order to consider sub pattern a valid match
	 * max = maximum match count allowed (zero = no max specified)
	 * parLeft = this variable in the parent is the left match variable
	 * parRight = this variable in the parent is the right match variable
	 * 
	 * (NOTE: we need this to find all patterns that connect parLeft to parRight, including, zero when applicable, and
	 *  also including multiple different kinds of matches, when applicable, e.g. there is more than one path/chain,
	 *  however, there's no value in finding loops that connect parLeft to parLeft then subsequently to parRight,
	 *  so we need to be able to truncate searches when either a subLeft or subRight is either a parLeft or parRight)
	 */
	public static class SubPattern extends BasePattern {
		public int					itsMinCount;
		public int					itsMaxCount;

		public int					itsParentLeftVariable;
		public int					itsParentRightVariable;

		public PatternDefinition	itsPattern;
		public BoundFrame			itsInitialFrame;		// a base bound: don't write into this frame!

		public int					itsChildLeftVariable;
		public int					itsChildRightVariable;

		public SubPattern ( int min, int max, int parLeft, int parRight, PatternDefinition subPat, BoundFrame init, int subLeft, int subRight, boolean tag, boolean neg, int [] ex ) {
			super ( tag, neg, ex );
			itsMinCount = min;
			itsMaxCount = max;
			itsParentLeftVariable = parLeft;
			itsParentRightVariable = parRight;
			itsPattern = subPat;
			itsInitialFrame = init;
			itsChildLeftVariable = subLeft;
			itsChildRightVariable = subRight;
		}
	}

}
