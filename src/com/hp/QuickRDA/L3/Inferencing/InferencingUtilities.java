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

package com.hp.QuickRDA.L3.Inferencing;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;

public class InferencingUtilities {

	public static ISet<DMIElem> makeList ( String cmd, StringRef xx, DMIElem t, ConceptManager cm ) {
		return makeList ( cmd, xx, t, cm, false );
	}

	public static ISet<DMIElem> makeList ( String cmd, StringRef xx, DMIElem t, ConceptManager cm, boolean asList ) {
		ISet<DMIElem> tV = null;
		if ( asList )
			tV = new XSetList<DMIElem> ();
		else
			tV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		String itm = Strings.tSplitAfter ( cmd, xx, "," );
		cmd = xx.str;

		intRef mode = new intRef ();

		while ( !"".equals ( itm ) || !"".equals ( cmd ) ) {
			DMIElem m = null;
			if ( !"".equals ( itm ) ) {
				m = findTarget ( itm, xx, mode, t, cm );
				if ( m == null )
					lang.errMsg ( "Supplied parameter (for filter/apply) was not found: '" + itm + "'; the filter will run with this parameter unbound." );
			}
			itm = xx.str;

			if ( asList )
				tV.addToList ( m );
			else if ( m != null )
				tV.addToSet ( m );

			itm = Strings.tSplitAfter ( cmd, xx, "," );
			cmd = xx.str;
		}

		return tV;
	}

	public static DMIElem findTarget ( String itm, StringRef xx, intRef mode, DMIElem tIn, ConceptManager cm ) {
		DMIElem m = null;

		DMIElem t;

		String typ = Strings.tSplitAfter ( itm, xx, ":" );
		itm = xx.str;

		if ( !"".equals ( typ ) ) {
			mode.val = 0;
			if ( "".equals ( itm ) ) {
				itm = typ;
				t = tIn;
			} else {
				t = cm.findConcept ( typ, cm.itsBaseVocab.gClass ); // b.gClass
			}
			if ( t != null ) {
				if ( Strings.InStr ( 1, itm, "-" ) == 1 ) {
					mode.val = -1;
					itm = Strings.Mid ( itm, 2 );
				} else {
					int l = itm.length ();
					if ( l > 1 ) {
						if ( Strings.InStr ( l - 1, itm, "-" ) > 0 ) {
							mode.val = 1;
							itm = Strings.Mid ( itm, l - 1 );
						}
					}
				}
				//FindConceptByNameAndExactType(null, itm, t)
				m = cm.findConcept ( itm, t );
			}
		}

		return m;
	}
}
