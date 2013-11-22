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

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L3.Inferencing.Query.NamedPatternManager.NamedPattern;
import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition.VariableDefinition;

public class Rule extends NamedPattern {
	public final DMIGraph					graph;
	public final HLQueryStatement			statement;
	public final IList<HLQueryStatement>	inferred;
	//public final NamedQueryVariable[] variables;
	public final VariableDefinition []		variableDefinitions;
	public final DMIElem []					values;

	public Rule ( String name,
			DMIGraph graph,
			HLQueryStatement statement,
			IList<HLQueryStatement> inferred,
			/* NamedQueryVariable[] variables, */
			VariableDefinition [] variableDefinitions,
			DMIElem [] values )
	{
		super ( name, null );
		this.graph = graph;
		this.statement = statement;
		this.inferred = inferred;
		// this.variables = variables;
		this.variableDefinitions = variableDefinitions;
		this.values = values;
	}

	public Rule ( Rule r ) {
		super ( r.itsName, null );
		this.graph = r.graph;
		this.statement = r.statement;
		this.inferred = r.inferred;
		// this.variables = variables;
		this.variableDefinitions = r.variableDefinitions;
		this.values = r.values.clone ();
	}

	public void bindInputVariable ( int i, DMIElem m ) {
		this.values [ i ] = m;
	}

	public Iterator<DMIElem []> getIterator () {
		return statement.getIterator ( this );
	}

}
