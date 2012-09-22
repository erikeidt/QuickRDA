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

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;

public class DMISharedNameList {

	// public static final int		kAny			= 7;
	public static final int		kHasName		= 1;
	public static final int		kIsName			= 2;
	public static final int		kSerialization	= 3;

	public String				itsLCKey;
	public String				itsMCText;
	public String				itsPlural;

	public static final boolean	deferNames		= true;

	private DMIElem []			itsElementList;		// the list of elements that have this LCKey
	private int []				itsElementAssn;		// the associations for any given element on the list


	public DMISharedNameList ( String lcStr, String mcStr ) {
		itsElementList = new DMIElem [ 0 ];
		itsElementAssn = new int [ 0 ];
		itsLCKey = lcStr;
		itsMCText = mcStr;
	}

	public DMIElem findConcept ( DMIElem t, boolean exact, int assn, boolean add, booleanRef added ) {
		DMIElem m;

		if ( added != null )
			added.bool = false;

		for ( int i = 0; i < itsElementList.length; i++ ) {
			if ( (itsElementAssn [ i ] & assn) != 0 ) {
				m = itsElementList [ i ];
				if ( t == null )
					return m;
				if ( m.instanceOf ( t ) )
					return m;
				if ( !exact && m.canBePromotedTo ( t ) )
					return m;
			}
		}

		if ( add ) {
			m = new DMIElem ();
			addToList ( m, assn );
			if ( added != null )
				added.bool = true;
		} else {
			m = null;
		}

		return m;
	}

	public DMIElem findConceptEx ( DMIElem t, boolean exact, int assn, DMIElem d, DMIElem r, boolean add, booleanRef added, DMIElem ansIn ) {
		DMIElem m = null;

		if ( added != null )
			added.bool = false;

		if ( ansIn != null ) {
			for ( int i = 0;; ) {
				i = lang.indexOf ( ansIn, itsElementList, i );
				if ( i < 0 )
					break;
				if ( (itsElementAssn [ i ] & assn) != 0 )
					return itsElementList [ i ];
			}
		} else {
			for ( int i = 0; i < itsElementList.length; i++ ) {
				if ( (itsElementAssn [ i ] & assn) != 0 ) {
					m = itsElementList [ i ];

					if ( t == null )
						return m;

					if ( m.instanceOf ( t ) ) {
						if ( d == null && r == null )
							return m;
						if ( TypeRelations.propertyAppliesOverDomainAndRange ( m, d, r ) )
							return m;
					} else if ( !exact && m.canBePromotedTo ( t ) ) {
						if ( d == null && r == null )
							return m;
						if ( TypeRelations.propertyAppliesOverDomainAndRange ( m, d, r ) )
							return m;
					}
				}
			}
		}

		if ( add ) {
			if ( ansIn == null ) {
				m = new DMIElem ();
			} else {
				m = ansIn;
			}
			addToList ( m, assn );
			if ( added != null )
				added.bool = true;
		} else {
			m = null;
		}

		return m;
	}

	public int conceptCount () {
		return itsElementList.length;
	}

	public DMIElem conceptAt ( int i ) {
		return itsElementList [ i ];
	}

	@SuppressWarnings("unchecked")
	private void addToList ( DMIElem e, int assn ) {
		itsElementList = DMIElem.addToList ( e, itsElementList );
		itsElementAssn = lang.addToList ( assn, itsElementAssn );
		// e.itsNameSet = DMISharedNameList.addToList  (this, (DMISharedNameList []) e.itsNameSet);
		if ( e.itsNameSet == null )
			e.itsNameSet = new XSetList<DMISharedNameList> ();
		((List<DMISharedNameList>) e.itsNameSet).add ( this );
		// Just for debugging
		if ( e.aaaName == null )
			e.aaaName = itsMCText;
	}

	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++; //self
		mrc.ptrCount += +itsElementList.length;
		mrc.strCount += 3;
		mrc.strSize += itsLCKey.length () + itsMCText.length ();
		if ( itsPlural != null )
			mrc.strSize += itsPlural.length ();
	}

}
