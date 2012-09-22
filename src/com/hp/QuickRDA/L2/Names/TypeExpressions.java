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

package com.hp.QuickRDA.L2.Names;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;

public class TypeExpressions {

	private static class DomainRangeTree {
		//It's either a (domain,range) pair, or some text, but not both
		public boolean			itsSimple;

		public String			itsText;

		public DomainRangeTree	itsDomain;
		public DomainRangeTree	itsRange;
	}

	public static boolean isTypeExpression ( String typ ) {
		return typ.contains ( "->" );
	}

	//
	// Parse and locate the node for abstract property expression
	// with a->b usage; the expression can have more than one -> as well parens as expected
	// not sure what precedence you'll get w/o parens (e.g. in a->b->c)
	// easy to sway though, either push when equal for right associativity, or pop when equal for left associativity
	//

	public static DMIElem getTypeFromNameExpression ( String tName, ConceptManager cm, DMIBaseVocabulary b ) {
		//FindConcept
		DMIElem ans = cm.findConcept ( tName, b.gClass );
		if ( ans == null ) {
			if ( cm.isTypeExpression ( tName ) ) {
				ans = enterParsedType ( tName, cm );
				if ( ans == null ) {
					// $$$ TODO make this an error msg instead of entering as type...
					//EnterUntypedConcept
					ans = cm.enterTypedConcept ( "Error type value in: " + tName, b.gConcept, true );
				}
			} else {
				//EnterUntypedConcept
				ans = cm.enterUntypedConcept ( tName );
			}
		}
		return ans;
	}

	public static DMIElem enterParsedType ( String tIn, ConceptManager cm ) {
		// Lookup as text to see if we've got it already
		// Note that it is an expression as a string, and there are many-to-one possible-to-desired forms
		//   e.g. "(x->y)", "x -> y", "(x)->  y" all of which are equivalent to "x->y"
		//   (the desired form is the normalized "x->y")
		// We do a FIND here as a short cut, but we don't ENTER because we don't want to enter the user supplied expression
		// We probably should just forgo the FIND here and parse it, but i think this is harmless enough.
		// if we don't find it as is, then we will parse it and build the desired form
		// 	search again, then if not found, build it.
		DMIElem m = cm.findConcept ( tIn, cm.itsBaseVocab.gProperty );

		if ( m == null ) {
			//if not then parse text to tree
			DomainRangeTree drt = parseStringToDRTree ( tIn );
			if ( drt != null ) {
				//and tree to type
				m = convertDRTreeToType ( drt, cm );
			}
		}

		//given tree, find type
		return m;
	}

	private static DMIElem convertDRTreeToType ( DomainRangeTree drt, ConceptManager cm ) {
		//generate normalized name and look up
		String n = convertDRTreeToName ( drt, false );

		DMIElem m = cm.findConcept ( n, cm.itsBaseVocab.gProperty );

		//if not found then
		if ( m == null ) {
			//find domain name by tree as dtype
			DMIElem dT = findConceptByDRTree ( drt.itsDomain, cm );

			//find range name by tree as rtype
			DMIElem rT = findConceptByDRTree ( drt.itsRange, cm );

			//Here if dT or rT is nothing, should report an error (instead of crashing later...)

			//enter normalized name to atype
			m = cm.enterAbstractPropertyByNameDomainRange ( n, dT, rT );
		}

		return m;
	}

	private static DMIElem findConceptByDRTree ( DomainRangeTree drt, ConceptManager cm ) {
		DMIElem m = null;

		String nameStr = convertDRTreeToName ( drt, false );

		if ( cm.isTypeExpression ( nameStr ) ) {
			m = cm.enterTypedConcept ( nameStr, cm.itsBaseVocab.gProperty, true );
		} else {
			m = cm.enterTypedConcept ( nameStr, cm.itsBaseVocab.gClass, true );
		}

		return m;
	}

	private static String convertDRTreeToName ( DomainRangeTree drt, boolean complex ) {
		String ans;

		if ( drt.itsSimple ) {
			ans = drt.itsText;
		} else {
			ans = convertDRTreeToName ( drt.itsDomain, true ) + "->" + convertDRTreeToName ( drt.itsRange, true );
			if ( complex ) {
				ans = "(" + ans + ")";
			}
		}

		return ans;
	}

	// Parsing property types of the form x->y with parens allowed anywhere as desired
	//   e.g. (x)->y, x->(y), (x->y), (x->y)->z, x->(y->z), (x->y)->(p->q), and so on
	//   do we allow x->y->z, and if so, what precedence, and if not, how to deny
	//(NOTE: this is not x--p->y, which is another thing (i.e. statement as a concept expressions))
	private static DomainRangeTree parseStringToDRTree ( String tIn ) {
		String op;

		IList<DomainRangeTree> operandStack = new XSetList<DomainRangeTree> ();
		IList<String> operatorStack = new XSetList<String> ();

		String rest = tIn.trim ();
		StringRef xx = new StringRef ();

		// Parse expression (or rest of expression), starting in State 1
		for ( ;; ) {
			//State 1 is accepting id and ( and is rejecting EOS and ) and ->
			if ( Strings.InStr ( 1, rest, "(" ) == 1 ) {
				rest = Strings.Mid ( rest, 2 ).trim ();
				operatorStack.add ( "(" );
				continue;
			}

			if ( Strings.InStr ( 1, rest, "->" ) == 1 )
				return null;
			if ( rest.length () == 0 )
				return null;

			DomainRangeTree o = new DomainRangeTree ();

			o.itsSimple = true;
			o.itsText = isolateID ( rest, xx );
			rest = xx.str;

			operandStack.add ( o );

			//State 2 is accepting -> and ) and EOS and is rejecting id
			for ( ;; ) {
				if ( Strings.InStr ( 1, rest, ")" ) == 1 ) {
					rest = Strings.Mid ( rest, 2 ).trim ();
					op = reduceTo ( "(", operatorStack, operandStack );
					if ( "(".equals ( op ) )
						continue; // stay in State 2
					return null;
				} else if ( Strings.InStr ( 1, rest, "->" ) == 1 ) {
					// this is right associativity for one operator: the new one wins, and gets pushed on the stack
					//   this means that the operands bind to this (current and later than the others) operator first.
					// for left associativity, we'd reduce the stacked operators first then push this one,
					//   this would mean that the operands bind to the earlier (stacked) operator first.
					// for a richer set of operators (not worthwhile here),
					// we'd have a notion of both an on-stack and a just-encountered precendece for each operator
					// and here we'd reduce while the on-stack precedence of the operators on the stack was greater than
					//    the just-encountered precedence of the current operator
					// then by adjusting two precedence values of the same operator you can achieve
					//    separate left vs. right associativity for each operator
					rest = Strings.Mid ( rest, 3 ).trim ();
					operatorStack.add ( "->" );
					break; // back to State 1
				} else if ( rest.length () == 0 ) {
					op = reduceTo ( null, operatorStack, operandStack );
					if ( op == null )
						return operandStack.top ();
				}
				return null;
			}
		}
	}

	//returns last operator taken (if it returns what, then success; otherwise, failure)
	//ReduceTo null means do all upto excluding the null, which is not an operator
	//ReduceTo "(" means do all upto including the "(", it should come off the stack
	private static String reduceTo ( String what, IList<String> orS, IList<DomainRangeTree> andS ) {
		String op = null;

		for ( ;; ) {
			op = orS.top ();
			if ( op != null ) {
				orS.pop ();
				if ( op != what ) { // this pointer compare works because we're only ever comparing string literals not string variables
					//combine top two items on operandStack into one item on operandStack
					//using op as the operator, but we have only one operator, so...
					DomainRangeTree rightHand = andS.top ();
					andS.pop ();
					DomainRangeTree leftHand = andS.top ();
					andS.pop ();
					DomainRangeTree newHand = new DomainRangeTree ();

					newHand.itsSimple = false;
					newHand.itsDomain = leftHand;
					newHand.itsRange = rightHand;

					andS.add ( newHand );

					continue;
				}
			}
			break;
		}

		return op;
	}

	private static String isolateID ( String s, StringRef remainder ) {
		final int Largeint = 100000;
		int x = Largeint;
		int p = Strings.InStr ( s, "(" );
		if ( p > 0 ) {
			x = lang.minInt ( p, x );
		}
		p = Strings.InStr ( s, ")" );
		if ( p > 0 ) {
			x = lang.minInt ( p, x );
		}
		p = Strings.InStr ( s, "-" );
		if ( p > 0 ) {
			x = lang.minInt ( p, x );
		}
		p = Strings.InStr ( s, ">" );
		if ( p > 0 ) {
			x = lang.minInt ( p, x );
		}

		if ( x == Largeint ) {
			remainder.str = "";
			return s;
		} else {
			remainder.str = Strings.Mid ( s, x );
			return Strings.Mid ( s, 1, x - 1 );
		}
	}

}
