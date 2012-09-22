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
import com.hp.QuickRDA.L1.Core.DMIElem;


public class NamedPatternManager {

	public IList<NamedPattern>	itsNamedPatterns	= new XSetList<NamedPattern> ();

	public static class NamedPattern {
		public String		itsName;
		public BoundFrame	itsInitialFrame;

		public NamedPattern ( String nm, BoundFrame bf ) {
			itsName = nm;
			itsInitialFrame = bf;
		}
	}

	public void add ( NamedPattern p ) {
		if ( find ( p.itsName ) != null )
			lang.errMsg ( "Pattern name already defined: " + p.itsName );

		itsNamedPatterns.add ( p );
	}

	public BoundFrame primePattern ( String name, ISet<DMIElem> xV ) {
		NamedPattern p = find ( name );
		if ( p != null ) {
			int fCnt = p.itsInitialFrame.itsPatternDefinition.itsFormalArgumentCount;
			int aCnt = xV.size ();
			if ( aCnt > fCnt )
				lang.errMsg ( "too many parameters to pattern: " + name );
			BoundFrame ans = new BoundFrame ( p.itsInitialFrame, true );
			for ( int i = 0; i < aCnt; i++ ) {
				DMIElem m = xV.get ( i );
				if ( m != null )
					ans.bindInputVariable ( i, m );
			}
			return ans;
		}
		return null;
	}

	NamedPattern find ( String name ) {
		for ( int i = 0; i < itsNamedPatterns.size (); i++ ) {
			NamedPattern np = itsNamedPatterns.get ( i );
			if ( np.itsName.toLowerCase ().equals ( name.toLowerCase () ) )
				return np;
		}
		return null;
	}

}
