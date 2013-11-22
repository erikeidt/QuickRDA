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

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L3.Inferencing.Query.NamedPatternManager;

public class NamedRuleManager extends NamedPatternManager {

	public Rule primeRule ( String name, ISet<DMIElem> xV ) {
		Rule r = (Rule) find ( name );
		if ( r != null ) {
			//int fCnt = p.itsInitialFrame.itsPatternDefinition.itsFormalArgumentCount;
			int fCnt = r.variableDefinitions.length;
			int aCnt = xV.size ();
			if ( aCnt > fCnt )
				lang.errMsg ( "too many parameters to pattern: " + name );
			Rule ans = new Rule ( r );
			for ( int i = 0; i < aCnt; i++ ) {
				DMIElem m = xV.get ( i );
				if ( m != null )
					ans.bindInputVariable ( i, m );
			}
			return ans;
		}
		return null;
	}
}
