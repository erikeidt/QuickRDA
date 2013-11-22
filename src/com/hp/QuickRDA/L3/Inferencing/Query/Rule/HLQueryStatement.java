/* 
Copyright (c) 2011-2013 Erik Eidt
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

package com.hp.QuickRDA.L3.Inferencing.Query.Rule;

import java.io.PrintStream;
import java.util.Iterator;

import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L3.Inferencing.Query.NamedPatternManager;
import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition;
import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition.QueryVariableExpressionEnum;

class HLQueryStatement {
	int	itsStatementIndex;

	HLQueryStatement () {
		itsStatementIndex = -1;
	}

	/*	int assignStatementIndecies ( int ix ) {
			return ix;
		}

		void getStmtList ( IList<Integer> ret ) {
		}

		void setSuppressList ( IList<Integer> ex ) {
		}
	*/
	void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
	}

	void dump ( int t, PrintStream out ) {
		TextToRule.indent ( t, out );
		out.println ( "The Null Statement;" );
	}

	public Iterator<DMIElem []> getIterator ( Rule r ) {
		return getIterator ( r, r.values );
	}

	public Iterator<DMIElem []> getIterator ( Rule r, DMIElem [] values ) {
		return new HLQueryStatementIterator ( r, this, values );
	}
}


class SingleStatement extends HLQueryStatement {
	int []	itsSuppressList;

	/*	@Override
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
	*/
	@Override
	public Iterator<DMIElem []> getIterator ( Rule r, DMIElem [] values ) {
		return new SingleStatementIterator ( r, this, values );
	}
}


class MatchStatement extends SingleStatement {
	NamedQueryVariable			itsLB;
	NamedQueryVariable			itsSB;
	NamedQueryVariable			itsVB;
	QueryVariableExpressionEnum	itsQVE;
	NamedQueryVariable			itsOB;
	boolean						itsViz;

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
			/*			qs [ itsStatementIndex ] =
								new PatternDefinition.VariableQualifierPattern (
										itsSB == null ? -1 : itsSB.itsIndex,
										itsQVE,
										itsOB == null ? -1 : itsOB.itsIndex,
										false, neg, itsSuppressList );
			*/
			//if ( viz && itsSB != null )
			//	itsSB.itsViz = true;				
		}
		else {
			itsViz = viz;

			/*			qs [ itsStatementIndex ] =
								new PatternDefinition.MatchPattern ( itsLB == null ? -1 : itsLB.itsIndex,
										itsSB == null ? -1 : itsSB.itsIndex,
										itsVB == null ? -1 : itsVB.itsIndex,
										itsOB == null ? -1 : itsOB.itsIndex,
										viz, neg, itsSuppressList );
			*/
			if ( viz ) {
				if ( itsSB != null )
					itsSB.itsViz = true;
				if ( itsOB != null )
					itsOB.itsViz = true;
			}
		}
	}

	@Override
	public Iterator<DMIElem []> getIterator ( Rule r, DMIElem [] values ) {
		if ( itsQVE != null )
			return new FilterStatementIterator ( r, this, values );
		return new MatchStatementIterator ( r, this, values );
	}

	@Override
	void dump ( int t, PrintStream out ) {
		TextToRule.indent ( t, out );
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


class UnOPStatement extends HLQueryStatement {
	char				itsOP;
	HLQueryStatement	itsArg;

	UnOPStatement ( char op, HLQueryStatement arg ) {
		itsOP = op;
		itsArg = arg;
	}

	/*	@Override
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
	*/
	@Override
	void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
		if ( itsOP == '+' )
			viz = true;
		if ( itsOP == '-' )
			viz = false;
		itsArg.genStatements ( qs, viz, itsOP == '!' ? !neg : neg );
	}

	@Override
	public Iterator<DMIElem []> getIterator ( Rule r, DMIElem [] values ) {
		return new UnOPStatmentIterator ( r, this, values );
	}

	@Override
	void dump ( int t, PrintStream out ) {
		TextToRule.indent ( t, out );
		if ( itsStatementIndex >= 0 )
			out.print ( "#" + itsStatementIndex + " " );
		out.print ( "OP " + itsOP );
		out.println ();
		itsArg.dump ( t + 1, out );
	}
}


class BinOPStatement extends HLQueryStatement {
	char				itsOP;
	HLQueryStatement	itsLeft;
	HLQueryStatement	itsRight;

	BinOPStatement ( char op, HLQueryStatement left, HLQueryStatement right ) {
		itsOP = op;
		itsLeft = left;
		itsRight = right;
	}

	/*	@Override
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
	*/
	@Override
	public Iterator<DMIElem []> getIterator ( Rule r, DMIElem [] values ) {
		return new BinOPStatementIterator ( r, this, values );
	}

	@Override
	void genStatements ( PatternDefinition.BasePattern[] qs, boolean viz, boolean neg ) {
		itsLeft.genStatements ( qs, viz, neg );
		itsRight.genStatements ( qs, viz, neg );
	}

	@Override
	void dump ( int t, PrintStream out ) {
		TextToRule.indent ( t, out );
		if ( itsStatementIndex >= 0 )
			out.print ( "#" + itsStatementIndex + " " );
		out.print ( "OP " + itsOP );
		out.println ();
		itsLeft.dump ( t + 1, out );
		itsRight.dump ( t + 1, out );
	}
}


class InvokeStatement extends SingleStatement {
	int					itsMin;
	int					itsMax;
	String				itsName;
	NamedQueryVariable	itsLeft;
	NamedQueryVariable	itsRight;
	TextToRule			itsTTP;

	InvokeStatement ( int min, int max, String name, NamedQueryVariable left, NamedQueryVariable right, TextToRule ttp ) {
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

		/*		qs [ itsStatementIndex ] =
						new PatternDefinition.SubPattern ( itsMin, itsMax,
								itsLeft.itsIndex, itsRight.itsIndex,
								np.itsInitialFrame.itsPatternDefinition, np.itsInitialFrame,
								0, 1,
								viz, neg, itsSuppressList );
		*/
	}

	@Override
	public Iterator<DMIElem []> getIterator ( Rule r, DMIElem [] values ) {
		return new InvokeStatementIterator ( r, this, values );
	}

	@Override
	void dump ( int t, PrintStream out ) {
		TextToRule.indent ( t, out );
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
