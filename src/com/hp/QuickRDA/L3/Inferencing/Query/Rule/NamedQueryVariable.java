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

import com.hp.QuickRDA.L3.Inferencing.Query.PatternDefinition;

class NamedQueryVariable {
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
