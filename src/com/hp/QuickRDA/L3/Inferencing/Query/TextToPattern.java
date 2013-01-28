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

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition.*;

import com.hp.QuickRDA.L4.Build.BuildOptions; // just for debugging
import com.hp.QuickRDA.L5.ExcelTool.Debug;


// 
// $$$ TODO:
//	Handle (or Error) statements in a query that are not interconnected by variables (should be only one graph of statements)
//	Handle nested named patterns of match value 1
//		1. by syntax: no [], or detect [1:1]
//		2. by unrolling nested pattern
//

public class TextToPattern {

	public NamedPatternManager	itsPM;
	public ConceptManager		itsCM;
	public ScanReader			itsTkn;

	private boolean				itsDefaultViz	= true;

	// BufferedReader in = new BufferedReader (new StringReader (text));
	public TextToPattern ( BufferedReader br, NamedPatternManager pm, ConceptManager cm ) {
		itsPM = pm;
		itsCM = cm;
		itsTkn = new ScanReader ( br, "Query Pattern Parser", "text parameter" );
	}

	private static class NamedQueryVariable {
		int										itsIndex;
		String									itsName;
		PatternDefinition.QueryVariableKindEnum	itsKind;
		boolean									itsViz;
		int										itsStatementPositionBindings;

		// IList<PatternDefinition.QueryVariableQualifier>	itsQualifiers;

		NamedQueryVariable ( String nm, PatternDefinition.QueryVariableKindEnum k, boolean viz ) {
			itsIndex = -1;
			itsName = nm;
			itsKind = k;
			itsViz = viz;
		}

		void setIndex ( int i ) {
			itsIndex = i;
		}

		void dump ( PrintStream out ) {
			if ( itsIndex >= 0 )
				out.print ( "#" + itsIndex + "-" );
			if ( itsKind == PatternDefinition.QueryVariableKindEnum.GivenValueVariable )
				out.print ( "\"" + itsName + "\" " );
			else
				out.print ( "?" + itsName + " " );
		}
	}

	private static class HLQueryStatement {
		int	itsStatementIndex;

		HLQueryStatement () {
			itsStatementIndex = -1;
		}

		int assignStatementIndecies ( int ix ) {
			return ix;
		}

		void getStmtList ( IList<Integer> ret ) {
		}

		void setSuppressList ( IList<Integer> ex ) {
		}

		void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
		}

		void dump ( int t, PrintStream out ) {
			indent ( t, out );
			out.println ( "The Null Statement;" );
		}
	}

	private static class SingleStatement extends HLQueryStatement {
		int []	itsSuppressList;

		@Override
		int assignStatementIndecies ( int ix ) {
			itsStatementIndex = ix;
			return ix + 1;
		}

		@Override
		void getStmtList ( IList<Integer> ret ) {
			ret.add ( itsStatementIndex );
		}

		@Override
		void setSuppressList ( IList<Integer> ex ) {
			if ( ex != null && ex.size () > 0 ) {
				ex.sortList ( lang.gIntegerComparator );
				itsSuppressList = lang.toIntArray ( ex );
			}
		}
	}

	private static class MatchStatement extends SingleStatement {
		NamedQueryVariable			itsLB;
		NamedQueryVariable			itsSB;
		NamedQueryVariable			itsVB;
		QueryVariableExpressionEnum	itsQVE;
		NamedQueryVariable			itsOB;

		MatchStatement ( NamedQueryVariable lb, NamedQueryVariable sb, NamedQueryVariable vb, NamedQueryVariable ob ) {
			itsLB = lb;
			itsSB = sb;
			itsVB = vb;
			itsOB = ob;
		}

		MatchStatement ( NamedQueryVariable sb, QueryVariableExpressionEnum qve, NamedQueryVariable ob ) {
			itsSB = sb;
			itsQVE = qve;
			itsOB = ob;
		}

		@Override
		void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
			if ( itsQVE != null ) {
				qs [ itsStatementIndex ] =
						new PatternDefinition.VariableQualifierPattern (
								itsSB == null ? -1 : itsSB.itsIndex,
								itsQVE,
								itsOB == null ? -1 : itsOB.itsIndex,
								false, neg, itsSuppressList );
				//if ( viz && itsSB != null )
				//	itsSB.itsViz = true;				
			}
			else {
				qs [ itsStatementIndex ] =
						new PatternDefinition.MatchPattern ( itsLB == null ? -1 : itsLB.itsIndex,
								itsSB == null ? -1 : itsSB.itsIndex,
								itsVB == null ? -1 : itsVB.itsIndex,
								itsOB == null ? -1 : itsOB.itsIndex,
								viz, neg, itsSuppressList );
				if ( viz ) {
					if ( itsSB != null )
						itsSB.itsViz = true;
					if ( itsOB != null )
						itsOB.itsViz = true;
				}
			}
		}

		@Override
		void dump ( int t, PrintStream out ) {
			indent ( t, out );
			if ( itsStatementIndex >= 0 )
				out.print ( "#" + itsStatementIndex + " " );
			if ( itsQVE != null ) {
				out.print ( "Qualify  " );
				dump ( itsSB, out );
				out.print ( itsQVE.name () + " " );
				dump ( itsOB, out );
			}
			else {
				out.print ( "Match  " );
				dump ( itsLB, out );
				dump ( itsSB, out );
				dump ( itsVB, out );
				dump ( itsOB, out );
			}
			if ( itsSuppressList != null ) {
				out.print ( " [" );
				for ( Integer I : itsSuppressList )
					out.print ( " " + I );
				out.print ( " ]" );
			}
			out.println ();
		}

		private static void dump ( NamedQueryVariable n, PrintStream out ) {
			if ( n == null )
				out.print ( "? " );
			else
				n.dump ( out );
		}
	}

	private static class UnOPStatement extends HLQueryStatement {
		char				itsOP;
		HLQueryStatement	itsArg;

		UnOPStatement ( char op, HLQueryStatement arg ) {
			itsOP = op;
			itsArg = arg;
		}

		@Override
		int assignStatementIndecies ( int ix ) {
			ix = itsArg.assignStatementIndecies ( ix );
			return ix;
		}

		@Override
		void getStmtList ( IList<Integer> ret ) {
			itsArg.getStmtList ( ret );
		}

		@Override
		void setSuppressList ( IList<Integer> ex ) {
			itsArg.setSuppressList ( ex );
		}

		@Override
		void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
			itsArg.genStatements ( qs, itsOP == '+' ? true : false, itsOP == '!' ? !neg : neg );
		}

		@Override
		void dump ( int t, PrintStream out ) {
			indent ( t, out );
			if ( itsStatementIndex >= 0 )
				out.print ( "#" + itsStatementIndex + " " );
			out.print ( "OP " + itsOP );
			out.println ();
			itsArg.dump ( t + 1, out );
		}
	}

	private static class BinOPStatement extends HLQueryStatement {
		char				itsOP;
		HLQueryStatement	itsLeft;
		HLQueryStatement	itsRight;

		BinOPStatement ( char op, HLQueryStatement left, HLQueryStatement right ) {
			itsOP = op;
			itsLeft = left;
			itsRight = right;
		}

		@Override
		int assignStatementIndecies ( int ix ) {
			ix = itsLeft.assignStatementIndecies ( ix );
			ix = itsRight.assignStatementIndecies ( ix );
			return ix;
		}

		@Override
		void getStmtList ( IList<Integer> ret ) {
			itsLeft.getStmtList ( ret );
			itsRight.getStmtList ( ret );
		}

		@Override
		void setSuppressList ( IList<Integer> ex ) {
			switch ( itsOP ) {
			case '|' :
				IList<Integer> ex2 = new XSetList<Integer> ();
				itsLeft.getStmtList ( ex2 );
				ex2.merge ( ex );
				itsRight.setSuppressList ( ex2 );

				ex2 = new XSetList<Integer> ();
				itsRight.getStmtList ( ex2 );
				ex2.merge ( ex );
				itsLeft.setSuppressList ( ex2 );
				break;
			default :
				itsLeft.setSuppressList ( ex );
				itsRight.setSuppressList ( ex );
				break;
			}
		}

		@Override
		void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
			itsLeft.genStatements ( qs, viz, neg );
			itsRight.genStatements ( qs, viz, neg );
		}

		@Override
		void dump ( int t, PrintStream out ) {
			indent ( t, out );
			if ( itsStatementIndex >= 0 )
				out.print ( "#" + itsStatementIndex + " " );
			out.print ( "OP " + itsOP );
			out.println ();
			itsLeft.dump ( t + 1, out );
			itsRight.dump ( t + 1, out );
		}
	}

	private static class InvokeStatement extends SingleStatement {
		int					itsMin;
		int					itsMax;
		String				itsName;
		NamedQueryVariable	itsLeft;
		NamedQueryVariable	itsRight;
		TextToPattern		itsTTP;

		InvokeStatement ( int min, int max, String name, NamedQueryVariable left, NamedQueryVariable right, TextToPattern ttp ) {
			itsMin = min;
			itsMax = max;
			itsName = name;
			itsLeft = left;
			itsRight = right;
			itsTTP = ttp;
		}

		@Override
		void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
			// Need to lookup the subpattern definition, and then, find the two input args
			NamedPatternManager.NamedPattern np = itsTTP.itsPM.find ( itsName );
			if ( np == null )
				itsTTP.itsTkn.scanError ( "Named Pattern not found: " + itsName );

			qs [ itsStatementIndex ] =
					new PatternDefinition.SubPattern ( itsMin, itsMax,
							itsLeft.itsIndex, itsRight.itsIndex,
							np.itsInitialFrame.itsPatternDefinition, np.itsInitialFrame,
							0, 1,
							viz, neg, itsSuppressList );
		}

		@Override
		void dump ( int t, PrintStream out ) {
			indent ( t, out );
			if ( itsStatementIndex >= 0 )
				out.print ( "#" + itsStatementIndex + " " );
			out.print ( "Invoke Statement: [" + itsMin + ":" + (itsMax < 0 ? "*" : itsMax) + "] '" + itsName + "' < " );
			itsLeft.dump ( out );
			out.print ( ", " );
			itsRight.dump ( out );
			out.print ( " >" );
			if ( itsSuppressList != null ) {
				out.print ( " [" );
				for ( int i = 0; i < itsSuppressList.length; i++ )
					out.print ( " " + itsSuppressList [ i ] );
				out.print ( " ]" );
			}
			out.println ();
		}
	}

	private static void indent ( int t, PrintStream out ) {
		for ( int i = 0; i < t; i++ )
			out.print ( "\t" );
	}

	public void parseAndAddPatterns (List<String> activePatterns) {
		for ( ;; ) {
			String patternName = null;
			IList<NamedQueryVariable> nqv = new XSetList<NamedQueryVariable> ();
			HLQueryStatement hlqs = null;
			// try {
			if ( itsTkn.isEOF () )
				break;

			// define name
			itsTkn.expectToken ( "define" );
			patternName = itsTkn.expectIdentifier ();
			itsTkn.expectToken ( '<' );

			// Formal Arguments
			if ( !itsTkn.ifToken ( '>' ) ) {
				for ( ;; ) {
					int token = itsTkn.currToken ();
					if ( token != '?' )
						itsTkn.scanError ( "? Variable expected, found: '" + (char) token + "\'" );
					StatementQueryVariable sqv = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.InputVariable );
					queryVar ( sqv.itsName, sqv.itsKind, true, nqv, PatternDefinition.NoBinding );
					if ( !itsTkn.ifToken ( ',' ) )
						break;
				}

				itsTkn.expectToken ( '>' );
			}

			itsTkn.expectToken ( '=' );

			IList<HLQueryStatement> inferredStatements = null;

			String id = itsTkn.expectIdentifier ();
			if ( "reveal".equals ( id ) ) {
				itsDefaultViz = true;
			} else if ( "infer".equals ( id ) ) {
				itsDefaultViz = false;
				itsTkn.expectToken ( '(' );
				// the infer clause allows only & operators
				for ( ;; ) {
					if ( inferredStatements == null )
						inferredStatements = new XSetList<HLQueryStatement> ();
					HLQueryStatement inferred = parseQueryStatement ( nqv );
					inferredStatements.add ( inferred );
					int token = itsTkn.currToken ();
					if ( token == '&' ) {
						itsTkn.advanceChar ();
						continue;
					}
					break;
				}
				itsTkn.expectToken ( ')' );
			}
			itsTkn.expectToken ( "where" );

			hlqs = parseBody ( nqv );
			if (activePatterns.contains(patternName)) {
				// System.out.println("Adding pattern " + patternName);
				NamedPatternManager.NamedPattern npf = assemblePatternFrame ( patternName, inferredStatements, nqv, hlqs );
				itsPM.add ( npf );
			} // else System.out.println("Rejecting pattern " + patternName);
		}

		itsTkn.close ();
	}

	private HLQueryStatement parseBody ( IList<NamedQueryVariable> nqv ) {
		IList<HLQueryStatement> operandStack = new XSetList<HLQueryStatement> ();
		IList<Character> operatorStack = new XSetList<Character> ();

		boolean hasUnless = false;

		for ( ;; ) {
			// State 1 is accepting match-stmt and invocation start and ( and is rejecting EOS, and ) and |
			String ident = null;
			int token = itsTkn.currToken ();
			if ( token == 'A' ) {
				ident = itsTkn.getMoreIdentifier ();
				if ( "hide".equals ( ident ) ) {
					operatorStack.add ( '-' ); // no reduction, unary operators have highest precedence
					continue; // back to state 1
				}
				if ( "reveal".equals ( ident ) ) {
					operatorStack.add ( '+' ); // no reduction, unary operators have highest precedence
					continue; // back to state 1
				}
				// fall thru
			} else if ( token == '(' ) {
				operatorStack.add ( '(' );
				itsTkn.advanceChar ();
				continue; // back to state 1
			}
			else if ( token == '!' ) {
				operatorStack.add ( '!' );
				itsTkn.advanceChar ();
				continue; // back to state 1
			}
			else if ( token == ')' || token == '|' )
				itsTkn.scanError ( "expecting match statement or '(', not ')' or '|'" );

			HLQueryStatement operand = parseStatement ( nqv, ident );

			if ( (Tracing.gTraceSet & Tracing.TraceParse) != 0 ) {
				Tracing.gTraceFile.println ( "Parsed:" );
				operand.dump ( 0, Tracing.gTraceFile );
				Tracing.gTraceFile.println ();
			}

			operandStack.add ( operand );

			// State 2 is accepting | and ) and EOS and is rejecting match stmt and invocation start
			for ( ;; ) {
				token = itsTkn.currToken ();
				if ( token == ')' ) {
					itsTkn.advanceChar ();
					char op = reduceTo ( '(', operatorStack, operandStack );
					if ( op == '(' )
						continue; // stay in state 2
					itsTkn.scanError ( "parenthesis mismatch, unexpected operator on stack (\'" + op + "\')" );
				}
				else if ( token == '|' || token == '&' ) {
					itsTkn.advanceChar ();
					reduceWhileHigherPrecedence ( (char) token, operatorStack, operandStack );
					operatorStack.add ( (char) token );
					break; // back to State 1
				}
				else if ( token == '.' ) {
					itsTkn.advanceChar ();
					if ( hasUnless ) {
						char op = reduceTo ( '(', operatorStack, operandStack );
						if ( op == '(' )
							hasUnless = false;
					}
					if ( !hasUnless ) {
						char op = reduceTo ( (char) 0, operatorStack, operandStack );
						if ( op == 0 )
							return operandStack.top ();
						// fall thru to error
					}
					// fall thru to error
				}
				else if ( token == 'A' ) {
					ident = itsTkn.getMoreIdentifier ();
					if ( "unless".equals ( ident ) ) {
						if ( hasUnless )
							itsTkn.scanError ( "only one unless clause allowed" );
						char op = reduceTo ( (char) 0, operatorStack, operandStack );
						if ( op == 0 ) {
							hasUnless = true;
							operatorStack.add ( '&' );
							operatorStack.add ( '!' );
							operatorStack.add ( '(' );
							break; // back to state 1
						}
						// fall thru to error
					}
					// fall thru to error
				}
				itsTkn.scanError ( "unexpected character: '" + (char) token + "'" );
			}
		}

	}

	private static boolean isUnop ( char c ) {
		return c == '+' || c == '-' || c == '!';
	}

	private static char reduceTo ( char toWhat, IList<Character> operatorStack, IList<HLQueryStatement> operandStack ) {
		char op = (char) 0;

		for ( ;; ) {
			Character OP = operatorStack.top ();
			op = (char) (OP == null ? 0 : OP);
			if ( op != 0 ) {
				operatorStack.pop ();
				if ( op != toWhat ) {
					if ( isUnop ( op ) )
						reduceUnop ( op, operatorStack, operandStack );
					else
						reduceBinOp ( op, operatorStack, operandStack );
					continue;
				}
			}
			break;
		}

		return op;
	}

	private static void reduceWhileHigherPrecedence ( char onRight, IList<Character> operatorStack, IList<HLQueryStatement> operandStack ) {
		for ( ;; ) {
			Character OP = operatorStack.top ();
			char op = (char) (OP == null ? 0 : OP);
			// tweak to adjust operator precedence
			if ( op == 0 || op == '(' )
				break;
			if ( isUnop ( op ) ) {
				operatorStack.pop ();
				reduceUnop ( op, operatorStack, operandStack );
				continue;
			}
			if ( op == '&' && onRight == '|' )
				break;
			operatorStack.pop ();
			reduceBinOp ( op, operatorStack, operandStack );
		}
	}

	private static void reduceUnop ( char op, IList<Character> operatorStack, IList<HLQueryStatement> operandStack ) {
		HLQueryStatement arg = operandStack.top ();
		operandStack.pop ();
		HLQueryStatement hlqs = new UnOPStatement ( op, arg );

		if ( (Tracing.gTraceSet & Tracing.TraceParse) != 0 ) {
			Tracing.gTraceFile.println ( "Pushing: " );
			hlqs.dump ( 0, Tracing.gTraceFile );
			Tracing.gTraceFile.println ();
		}

		operandStack.add ( hlqs );
	}

	private static void reduceBinOp ( char op, IList<Character> operatorStack, IList<HLQueryStatement> operandStack ) {
		HLQueryStatement right = operandStack.top ();
		operandStack.pop ();
		HLQueryStatement left = operandStack.top ();
		operandStack.pop ();
		HLQueryStatement hlqs = new BinOPStatement ( op, left, right );

		if ( (Tracing.gTraceSet & Tracing.TraceParse) != 0 ) {
			Tracing.gTraceFile.println ( "Pushing: " );
			hlqs.dump ( 0, Tracing.gTraceFile );
			Tracing.gTraceFile.println ();
		}

		operandStack.add ( hlqs );
	}

	private HLQueryStatement parseStatement ( IList<NamedQueryVariable> nqv, String ident ) {
		HLQueryStatement operand;
		int token = itsTkn.currToken ();
		if ( token == '[' || token == 'A' )
			operand = parsePatternStatement ( nqv, ident );
		else
			operand = parseQueryStatement ( nqv );
		return operand;
	}

	private HLQueryStatement parsePatternStatement ( IList<NamedQueryVariable> nqv, String ident ) {
		// [ min : max ] NamedPatternManager.NamedPattern < ?var , ?var > 
		// [] is also taken, meaning [0:*]

		int min = 0;
		int max = -1;

		int token = itsTkn.currToken ();

		if ( token != '[' ) {
			// implicit [1:1] by way of not having the []'s
			min = 1;
			max = 1;
		}
		else {
			itsTkn.advanceChar ();
			if ( itsTkn.ifToken ( ']' ) ) {
				;
			}
			else { // == [	
				min = itsTkn.getSimpleInteger ();
				itsTkn.expectToken ( ':' );
				if ( itsTkn.ifToken ( '*' ) )
					max = -1;
				else
					max = itsTkn.getSimpleInteger ();
				itsTkn.expectToken ( ']' );
			}
		}

		/*
		if ( min != 0 || max != -1 ) {
			if ( token != '[' )
				itsTkn.scanError ( "Currently only wild card (zero or more) matches are supported for nested patterns" );
			else
				itsTkn.scanError ( "Zero to infinity are the supported min to max in pattern invocation, e.g. [] or [0:*]" );
		}
		*/

		String name = ident;
		if ( name == null )
			name = itsTkn.expectIdentifier ();

		itsTkn.expectToken ( '<' );

		StatementQueryVariable lqv = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.MatchVariable );
		NamedQueryVariable left = queryVar ( lqv.itsName, lqv.itsKind, true, nqv, PatternDefinition.NoBinding );

		itsTkn.expectToken ( ',' );

		StatementQueryVariable rqv = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.MatchVariable );
		NamedQueryVariable right = queryVar ( rqv.itsName, rqv.itsKind, true, nqv, PatternDefinition.NoBinding );

		itsTkn.expectToken ( '>' );

		return new InvokeStatement ( min, max, name, left, right, this );
	}

	private static class StatementQueryVariable {
		PatternDefinition.QueryVariableKindEnum	itsKind;
		String									itsName;

		StatementQueryVariable ( PatternDefinition.QueryVariableKindEnum k, String n ) {
			itsKind = k;
			itsName = n;
		}
	}

	private HLQueryStatement parseQueryStatement ( IList<NamedQueryVariable> nqv ) {
		// either var var var var 
		//     or var var var
		// var can be "string" or ?id or just ? (no id)

		NamedQueryVariable lb = null;
		NamedQueryVariable sb = null;
		NamedQueryVariable vb = null;
		NamedQueryVariable ob = null;

		StatementQueryVariable v1 = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.MatchVariable );
		StatementQueryVariable v2 = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.MatchVariable );
		StatementQueryVariable v3 = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.MatchVariable );
		if ( itsTkn.ifToken ( '?' ) || itsTkn.ifToken ( '\"' ) ) {
			// 4 variables to match
			StatementQueryVariable v4 = parseQueryVar ( PatternDefinition.QueryVariableKindEnum.MatchVariable );

			if ( v1 == null && v2 == null && v3 == null && v4 == null )
				return new HLQueryStatement ();

			if ( v1 != null )
				lb = queryVar ( v1.itsName, v1.itsKind, false, nqv, PatternDefinition.SubjectBinding );
			if ( v2 != null )
				sb = queryVar ( v2.itsName, v2.itsKind, false, nqv, PatternDefinition.SubjectBinding );
			if ( v3 != null )
				vb = queryVar ( v3.itsName, v3.itsKind, false, nqv, PatternDefinition.VerbBinding );
			if ( v4 != null )
				ob = queryVar ( v4.itsName, v4.itsKind, false, nqv, PatternDefinition.ObjectBinding );
			return new MatchStatement ( lb, sb, vb, ob );
		}
		else {
			// 3 variables to match
			if ( v1 == null && v2 == null && v3 == null )
				return new HLQueryStatement ();

			if ( v1 != null )
				sb = queryVar ( v1.itsName, v1.itsKind, false, nqv, PatternDefinition.SubjectBinding );

			QueryVariableExpressionEnum qve = null;
			if ( v2 != null ) {
				if ( v2.itsKind == QueryVariableKindEnum.GivenValueVariable ) {
					if ( "Is An Instance Of".equals ( v2.itsName ) )
						qve = QueryVariableExpressionEnum.InstanceOf;
					else if ( "Is An Instance Of".equals ( v2.itsName ) )
						qve = QueryVariableExpressionEnum.SubclassOf;
					else if ( "=".equals ( v2.itsName ) )
						qve = QueryVariableExpressionEnum.IdenticalTo;
					else if ( "!=".equals ( v2.itsName ) )
						qve = QueryVariableExpressionEnum.NotIdenticalTo;
				}
				if ( qve == null )
					vb = queryVar ( v2.itsName, v2.itsKind, false, nqv, PatternDefinition.VerbBinding );
			}

			if ( v3 != null )
				ob = queryVar ( v3.itsName, v3.itsKind, false, nqv, PatternDefinition.ObjectBinding );

			if ( qve != null && (v1 == null || v3 == null) )
				itsTkn.scanError ( "Cannot have empty variable (?) for subject or object of qualifying statement" );

			return qve == null ? new MatchStatement ( null, sb, vb, ob ) : new MatchStatement ( sb, qve, ob );
		}
	}

	private StatementQueryVariable parseQueryVar ( PatternDefinition.QueryVariableKindEnum k ) {
		int token = itsTkn.currToken ();

		String name = null;
		if ( token == '?' ) {
			itsTkn.advanceChar ();
			name = itsTkn.getMoreIdentifier ();
		}
		else if ( token == '"' ) {
			name = itsTkn.getRestOfString ();
			k = PatternDefinition.QueryVariableKindEnum.GivenValueVariable;
			if ( "".equals ( name ) )
				itsTkn.scanError ( "Blank Concept (\"\") was not expected" );
		}
		else
			itsTkn.scanError ( "?var or \"concept name\" expected, found: " + (char) token );

		if ( "".equals ( name ) ) {
			if ( k == QueryVariableKindEnum.InputVariable )
				itsTkn.scanError ( "An anonymous query variable (?) is not allowed as formal argument." );
			return null;
		}
		return new StatementQueryVariable ( k, name );
	}

	private NamedQueryVariable queryVar ( String name, PatternDefinition.QueryVariableKindEnum k, boolean viz, IList<NamedQueryVariable> nqv, int binding ) {
		for ( int i = 0; i < nqv.size (); i++ ) {
			NamedQueryVariable n = nqv.get ( i );
			// either both kinds are GivenValue or neither are, and, names match
			if ( ((k == PatternDefinition.QueryVariableKindEnum.GivenValueVariable) ==
					(n.itsKind == PatternDefinition.QueryVariableKindEnum.GivenValueVariable)) &&
					n.itsName.equals ( name ) ) {
				if ( k == PatternDefinition.QueryVariableKindEnum.InputVariable )
					itsTkn.scanError ( "formal variable name is used more than once: " + name );
				n.itsStatementPositionBindings |= binding;
				n.itsViz |= viz;
				return n;
			}
		}
		NamedQueryVariable ans = new NamedQueryVariable ( name, k, viz );
		nqv.add ( ans );
		return ans;
	}

	private NamedPatternManager.NamedPattern assemblePatternFrame ( String patternName, IList<HLQueryStatement> inferredStatements, IList<NamedQueryVariable> nqv, HLQueryStatement hlqs ) {

		int stmtCnt = hlqs.assignStatementIndecies ( 0 );

		// Create PatternDefinition ...

		int varCnt = nqv.size ();
		for ( int i = 0; i < varCnt; i++ )
			nqv.get ( i ).setIndex ( i );

		// Create QueryStatement [] from HLQueryStatement (a tree)
		hlqs.setSuppressList ( null );
		PatternDefinition.BasePattern qs[] = new PatternDefinition.BasePattern [ stmtCnt ];
		hlqs.genStatements ( qs, itsDefaultViz, false );

		// Create VariableDefinition[] from NamedQueryVariables []
		PatternDefinition.VariableDefinition[] vd = new PatternDefinition.VariableDefinition [ varCnt ];
		for ( int i = 0; i < varCnt; i++ ) {
			IList<Integer> subQualList = null;
			IList<Integer> objQualList = null;
			NamedQueryVariable v = nqv.get ( i );
			if ( v.itsKind != PatternDefinition.QueryVariableKindEnum.GivenValueVariable ) {
				for ( int j = 0; j < stmtCnt; j++ ) {
					BasePattern p = qs [ j ];
					if ( p instanceof VariableQualifierPattern ) {
						VariableQualifierPattern vqp = (VariableQualifierPattern) p;
						if ( vqp.itsSubjectVariable == i ) { // i==v.itsIndex
							if ( subQualList == null )
								subQualList = new XSetList<Integer> ();
							subQualList.add ( j );
						}
						if ( vqp.itsObjectVariable == i ) {
							if ( objQualList == null )
								objQualList = new XSetList<Integer> ();
							objQualList.add ( j );
						}
					}
				}
			}

			int [] sq = null;
			if ( subQualList != null ) {
				subQualList.sortList ( lang.gIntegerComparator );
				sq = lang.toIntArray ( subQualList );
			}

			int [] oq = null;
			if ( objQualList != null ) {
				objQualList.sortList ( lang.gIntegerComparator );
				oq = lang.toIntArray ( objQualList );
			}

			vd [ i ] = new PatternDefinition.VariableDefinition ( v.itsKind, v.itsViz, sq, oq );
		}

		MatchPattern [] ifs = null;
		if ( inferredStatements != null ) {
			int ifCnt = inferredStatements.size ();
			ifs = new MatchPattern [ ifCnt ];
			for ( int i = 0; i < ifCnt; i++ )
				ifs [ i ] = createMatchPattern ( inferredStatements.get ( i ) );
		}

		if ( BuildOptions.gDebug ) {
			PrintStream f = Tracing.gTraceFile;
			if ( f == null )
				f = Debug.OpenDebugFile ();
			dump ( f, patternName, inferredStatements, hlqs, nqv, vd );
			TextFile.closeTheFile ( f );
		}

		// ... Finish PatternDefinition
		PatternDefinition pd = new PatternDefinition ( vd, qs, ifs );

		// Create Initial Frame with pre-bound variables
		BoundFrame bf = new BoundFrame ( pd );

		for ( int i = 0; i < nqv.size (); i++ ) {
			NamedQueryVariable v = nqv.get ( i );
			if ( v.itsKind == QueryVariableKindEnum.GivenValueVariable ) {
				DMIElem m = findConstant ( v.itsName, v.itsStatementPositionBindings );
				bf.bindGivenValueVariable ( i, m );
			}
		}

		NamedPatternManager.NamedPattern npf = new NamedPatternManager.NamedPattern ( patternName, bf );
		return npf;
	}

	private DMIElem findConstant ( String theName, int bindings ) {
		DMIElem t = itsCM.itsBaseVocab.gConcept;
		if ( (bindings & PatternDefinition.VerbBinding) != 0 )
			t = itsCM.itsBaseVocab.gProperty;
		DMIElem m = itsCM.findConcept ( theName, t );
		if ( m == null )
			itsTkn.scanError ( "Could not find quoted concept in graph: '" + theName + "'" );
		return m;
	}

	private MatchPattern createMatchPattern ( HLQueryStatement hlqs ) {
		MatchPattern ans = null;
		if ( hlqs instanceof MatchStatement ) {
			MatchStatement ms = (MatchStatement) hlqs;
			ans = new MatchPattern ( ms.itsLB == null ? -1 : ms.itsLB.itsIndex,
					ms.itsSB == null ? -1 : ms.itsSB.itsIndex,
					ms.itsVB == null ? -1 : ms.itsVB.itsIndex,
					ms.itsOB == null ? -1 : ms.itsOB.itsIndex,
					true, false, null );
		}
		return ans;
	}

	private static void dump ( PrintStream out, String patternName, IList<HLQueryStatement> inferredStatements, HLQueryStatement hlqs, IList<NamedQueryVariable> nqv, VariableDefinition [] vd ) {
		out.println ( "Assembling pattern named '" + patternName + "'" );
		for ( int i = 0; i < nqv.size (); i++ ) {
			out.print ( "\tvar #" + i + " " );
			nqv.get ( i ).dump ( out );
			if ( vd [ i ].itsSubjectQualifyingStatements != null ) {
				out.print ( " [ " );
				for ( int k = 0; k < vd [ i ].itsSubjectQualifyingStatements.length; k++ )
					out.print ( vd [ i ].itsSubjectQualifyingStatements [ k ] + " " );
				out.print ( "] " );
			}
			if ( vd [ i ].itsObjectQualifyingStatements != null ) {
				out.print ( " { " );
				for ( int k = 0; k < vd [ i ].itsObjectQualifyingStatements.length; k++ )
					out.print ( vd [ i ].itsObjectQualifyingStatements [ k ] + " " );
				out.print ( "} " );
			}
			out.println ();
		}

		if ( inferredStatements != null ) {
			out.println ( "inferred statements:" );
			for ( int i = 0; i < inferredStatements.size (); i++ )
				inferredStatements.get ( i ).dump ( 1, out );
		}

		out.println ();
		hlqs.dump ( 0, out );
	}


}
