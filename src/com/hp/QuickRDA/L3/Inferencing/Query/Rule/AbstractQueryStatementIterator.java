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

import java.util.Iterator;

import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition.QueryVariableExpressionEnum;

abstract class AbstractQueryStatementIterator implements Iterator<DMIElem []> {
	protected final Rule				rule;
	protected final HLQueryStatement	statement;
	protected DMIElem []				values;
	protected boolean					cloned;

	public AbstractQueryStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		this.rule = rule;
		this.statement = statement;
		this.values = values;
	}

	public abstract boolean hasNext ();

	public abstract DMIElem [] next ();

	public void remove () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}

	public void cloneValuesIfNeeded () {
		if ( !cloned ) {
			values = values.clone ();
			cloned = true;
		}
	}
}


class HLQueryStatementIterator extends AbstractQueryStatementIterator {
	public HLQueryStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );
	}

	@Override
	public boolean hasNext () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}

	@Override
	public DMIElem [] next () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}
}


class SingleStatementIterator extends AbstractQueryStatementIterator {
	public SingleStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );
	}

	@Override
	public boolean hasNext () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}

	@Override
	public DMIElem [] next () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}
}


class MatchStatementIterator extends AbstractQueryStatementIterator {
	MatchStatement		stmt;
	int					mask;
	Iterator<DMIElem>	matchIterator;
	DMIElem				match;
	int					valuesInCount;

	boolean				testMatchLabel;
	boolean				testMatchSubject;
	boolean				testMatchVerb;
	boolean				testMatchObject;

	boolean				captureLabel;
	boolean				captureSubject;
	boolean				captureVerb;
	boolean				captureObject;

	public MatchStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );
		stmt = (MatchStatement) statement;
		valuesInCount = values.length;

		testMatchLabel = testMatchVar ( stmt.itsLB );
		testMatchSubject = testMatchVar ( stmt.itsSB );
		testMatchVerb = testMatchVar ( stmt.itsVB );
		testMatchObject = testMatchVar ( stmt.itsOB );

		captureLabel = captureVar ( stmt.itsLB );
		captureSubject = captureVar ( stmt.itsSB );
		captureVerb = captureVar ( stmt.itsVB );
		captureObject = captureVar ( stmt.itsOB );

		matchIterator = getIterator ();
	}

	protected Iterator<DMIElem> getIterator () {
		Iterator<DMIElem> ans = null;
		DMIElem m = getVar ( stmt.itsLB );
		if ( m != null )
			ans = new OneShotIterator ( m );

		if ( ans == null ) {
			if ( (m = getVar ( stmt.itsSB )) != null )
				ans = m.itsUsedAsSubjectStmtSet.iterator ();
			else if ( (m = getVar ( stmt.itsOB )) != null )
				ans = m.itsUsedAsObjectStmtSet.iterator ();
			else if ( (m = getVar ( stmt.itsVB )) != null )
				ans = m.itsFullInstanceSet.iterator ();
			else if ( matchIterator == null )
				ans = new WholeGraphIterator ( rule.graph );
		}
		return ans;
	}

	@Override
	public boolean hasNext () {
		while ( matchIterator.hasNext () ) {
			match = matchIterator.next ();
			if ( qualifies ( match ) )
				return true;
		}
		return false;
	}

	@Override
	public void cloneValuesIfNeeded () {
		if ( !stmt.itsViz )
			super.cloneValuesIfNeeded ();
		else {
			if ( !cloned ) {
				DMIElem [] clone = new DMIElem [ valuesInCount + 1 ];
				System.arraycopy ( values, 0, clone, 0, valuesInCount );
				clone [ valuesInCount ] = null;
				values = clone;
				cloned = true;
			}
		}
	}

	@Override
	public DMIElem [] next () {
		if ( stmt.itsViz ) {
			cloneValuesIfNeeded ();
			values [ valuesInCount ] = match;
		}
		if ( captureLabel )
			setVarFor ( stmt.itsLB, match );
		if ( captureSubject )
			setVarFor ( stmt.itsSB, match.itsSubject );
		if ( captureVerb )
			setVarFor ( stmt.itsVB, match.itsVerb );
		if ( captureObject )
			setVarFor ( stmt.itsOB, match.itsObject );

		return values;
	}

	public void setVarFor ( NamedQueryVariable nqv, DMIElem m ) {
		if ( nqv != null )
		{
			int varNum = nqv.itsIndex;
			if ( varNum >= 0 ) {
				cloneValuesIfNeeded ();
				values [ varNum ] = m;
			}
		}
	}

	public boolean checkVarFor ( NamedQueryVariable nqv, DMIElem m ) {
		if ( nqv == null )
			return true;
		int varNum = nqv.itsIndex;
		if ( varNum < 0 )
			return true;
		DMIElem n = values [ varNum ];
		return n == null || n == m;
	}

	public boolean qualifies ( DMIElem m ) {
		return (!testMatchLabel || checkVarFor ( stmt.itsLB, match )) &&
				(!testMatchSubject || checkVarFor ( stmt.itsSB, match.itsSubject )) &&
				(!testMatchVerb || checkVarFor ( stmt.itsVB, match.itsVerb )) &&
				(!testMatchObject || checkVarFor ( stmt.itsOB, match.itsObject ));

	}

	public DMIElem getVar ( NamedQueryVariable nqv ) {
		if ( nqv == null )
			return null;
		int varNum = nqv.itsIndex;
		if ( varNum < 0 )
			return null;
		DMIElem m = values [ varNum ];
		return m;
	}

	public boolean testMatchVar ( NamedQueryVariable nqv ) {
		if ( nqv == null )
			return false;
		int varNum = nqv.itsIndex;
		if ( varNum < 0 )
			return false;
		DMIElem m = values [ varNum ];
		return m != null;
	}

	public boolean captureVar ( NamedQueryVariable nqv ) {
		if ( nqv == null )
			return false;
		int varNum = nqv.itsIndex;
		if ( varNum < 0 )
			return false;
		DMIElem m = values [ varNum ];
		return m == null;
	}
}


class FilterStatementIterator extends MatchStatementIterator {
	boolean	done;

	public FilterStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );

		if ( captureLabel )
			throw new UnsupportedOperationException ( "Filters cannot take a label variable" );

		switch ( stmt.itsQVE ) {
		case IdenticalTo :
		case NotIdenticalTo :
			if ( captureSubject || captureObject || !testMatchSubject || !testMatchObject )
				throw new UnsupportedOperationException ( "Filters = and != must have bound variables" );
			break;
		case InstanceOf :
		case SubclassOf :
			if ( testMatchSubject && testMatchObject )
				break;
			DMIElem sb = getVar ( stmt.itsSB );
			DMIElem ob = getVar ( stmt.itsOB );
			if ( sb == null && ob == null )
				throw new UnsupportedOperationException ( "Filters 'Is An Instance Of' and 'Is A Subclass Of' must use at least one bound subject or object" );
			if ( testMatchSubject && captureObject )
				matchIterator = (stmt.itsQVE == QueryVariableExpressionEnum.InstanceOf) ?
						sb.itsFullTypeSet.iterator () :
						sb.superclassSet ().iterator ();
			else if ( testMatchObject && captureSubject )
				matchIterator = (stmt.itsQVE == QueryVariableExpressionEnum.InstanceOf) ?
						ob.itsFullInstanceSet.iterator () :
						ob.subclassSet ().iterator ();
			else
				throw new UnsupportedOperationException ( "Filters 'Is An Instance Of' and 'Is A Subclass Of' used improperly" );
		default :
			break;
		}
	}

	@Override
	public boolean hasNext () {
		if ( done )
			return false;
		switch ( stmt.itsQVE ) {
		case IdenticalTo : {
			DMIElem v1 = getVar ( stmt.itsSB );
			DMIElem v2 = getVar ( stmt.itsOB );
			done = true;
			return v1 == v2;
		}
		case NotIdenticalTo : {
			DMIElem v1 = getVar ( stmt.itsSB );
			DMIElem v2 = getVar ( stmt.itsOB );
			done = true;
			return v1 != v2;
		}
		case InstanceOf :
		case SubclassOf : {
			if ( testMatchSubject && testMatchObject ) {
				DMIElem sb = getVar ( stmt.itsSB );
				DMIElem ob = getVar ( stmt.itsOB );
				done = true;
				return (stmt.itsQVE == QueryVariableExpressionEnum.InstanceOf) ?
						sb.instanceOf ( ob ) :
						sb.subclassOf ( ob );
			}
			if ( testMatchSubject && captureObject ) {
				if ( matchIterator.hasNext () )
				{
					setVarFor ( stmt.itsOB, matchIterator.next () );
					return true;
				}
				return false;
			}
			if ( testMatchObject && captureSubject ) {
				if ( matchIterator.hasNext () )
				{
					setVarFor ( stmt.itsSB, matchIterator.next () );
					return true;
				}
				return false;
			}
			throw new UnsupportedOperationException ( "Filter ouch" );
		}
		default :
			throw new UnsupportedOperationException ();
		}
	}

	@Override
	public DMIElem [] next () {
		return values;
	}
}


class UnOPStatmentIterator extends AbstractQueryStatementIterator {
	UnOPStatement			stmt;
	Iterator<DMIElem []>	childIterator;
	boolean					done;
	boolean					optionalMatched;

	public UnOPStatmentIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );
		stmt = (UnOPStatement) statement;
		childIterator = stmt.itsArg.getIterator ( rule, values );
	}

	@Override
	public boolean hasNext () {
		if ( done )
			return false;
		switch ( stmt.itsOP ) {
		case '!' :
			done = true;
			return !childIterator.hasNext ();
		case '+' :
		case '-' :
			return childIterator.hasNext ();
		case '*' :
			if ( childIterator.hasNext () )
			{
				optionalMatched = true;
				return true;
			}
			done = true;
			return !optionalMatched;
		default :
			throw new UnsupportedOperationException ();
		}
	}

	@Override
	public DMIElem [] next () {
		switch ( stmt.itsOP ) {
		case '!' :
			return values;
		case '+' :
		case '-' :
			return childIterator.next ();
		case '*' :
			return optionalMatched ? childIterator.next () : values;
		default :
			throw new UnsupportedOperationException ();
		}
	}

}


class BinOPStatementIterator extends AbstractQueryStatementIterator {
	BinOPStatement			stmt;
	boolean					firstChild;
	boolean					optionalMatched;
	Iterator<DMIElem []>	firstChildIterator;
	Iterator<DMIElem []>	secondChildIterator;

	public BinOPStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );
		stmt = (BinOPStatement) statement;
		firstChild = true;
		firstChildIterator = stmt.itsLeft.getIterator ( rule, values );
	}

	@Override
	public boolean hasNext () {
		switch ( stmt.itsOP ) {
		case '&' :
			for ( ;; ) {
				if ( firstChild ) {
					boolean first = firstChildIterator.hasNext ();
					if ( !first )
						return false;
					secondChildIterator = stmt.itsRight.getIterator ( rule, firstChildIterator.next () );
					firstChild = false;
				}
				if ( secondChildIterator.hasNext () )
					return true;
				firstChild = true;
			}
		case '%' :
			for ( ;; ) {
				if ( firstChild ) {
					boolean first = firstChildIterator.hasNext ();
					if ( !first )
						return false;
					secondChildIterator = stmt.itsRight.getIterator ( rule, firstChildIterator.next () );
					firstChild = false;
					optionalMatched = false;
				}
				if ( secondChildIterator.hasNext () ) {
					optionalMatched = true;
					return true;
				}
				firstChild = true;
				if ( !optionalMatched )
					return true;
			}
		case '|' :
			if ( firstChild ) {
				boolean ans = firstChildIterator.hasNext ();
				if ( ans )
					return true;
			}
			if ( firstChild ) {
				firstChild = false;
				secondChildIterator = stmt.itsRight.getIterator ( rule, values );
			}
			return secondChildIterator.hasNext ();
		default :
			throw new UnsupportedOperationException ();
		}
	}

	@Override
	public DMIElem [] next () {
		switch ( stmt.itsOP ) {
		case '&' :
			return secondChildIterator.next ();
		case '%' :
			return optionalMatched ? secondChildIterator.next () : firstChildIterator.next ();
		case '|' :
			return firstChild ? firstChildIterator.next () : secondChildIterator.next ();
		default :
			throw new UnsupportedOperationException ();
		}
	}
}


class InvokeStatementIterator extends AbstractQueryStatementIterator {
	public InvokeStatementIterator ( Rule rule, HLQueryStatement statement, DMIElem [] values ) {
		super ( rule, statement, values );
	}

	@Override
	public boolean hasNext () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}

	@Override
	public DMIElem [] next () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}
}


class OneShotIterator implements Iterator<DMIElem> {
	DMIElem	itsShot;
	boolean	done;

	public OneShotIterator ( DMIElem m ) {
		itsShot = m;
	}

	@Override
	public boolean hasNext () {
		if ( done || itsShot == null )
			return false;
		done = true;
		return true;
	}

	@Override
	public DMIElem next () {
		return itsShot;
	}

	@Override
	public void remove () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}
}


class WholeGraphIterator implements Iterator<DMIElem> {
	final DMIGraph				graph;
	final Iterator<DMISubgraph>	subgraphIterator;
	Iterator<DMIElem>			assertionIterator;

	public WholeGraphIterator ( DMIGraph graph ) {
		this.graph = graph;
		subgraphIterator = graph.getSubgraphList ().iterator ();
	}

	@Override
	public boolean hasNext () {
		for ( ;; ) {
			if ( assertionIterator == null ) {
				if ( !subgraphIterator.hasNext () )
					return false;
				assertionIterator = subgraphIterator.next ().toSet ().iterator ();
			}
			if ( assertionIterator.hasNext () )
				return true;
			assertionIterator = null;
		}
	}

	@Override
	public DMIElem next () {
		return assertionIterator.next ();
	}

	@Override
	public void remove () {
		throw new java.lang.UnsupportedOperationException ( "remove" );
	}
}