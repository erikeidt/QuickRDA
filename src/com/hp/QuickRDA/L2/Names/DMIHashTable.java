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

public class DMIHashTable {
	//
	// Provides basic hash function
	// key = string; item = DMIElem() that represents all the Names with that string
	//

	private DMIHashBucketCollisionList []	itsHashCollisionBuckets;

	public DMIHashTable () {
		itsHashCollisionBuckets = new DMIHashBucketCollisionList [ 2048 ];
	}

	// Optional add = true
	public DMISharedNameList associate ( String mcStr, boolean add ) {
		DMISharedNameList snl = null;
		String lcStr = mcStr.toLowerCase ();
		int hv = hashValue ( lcStr );

		DMIHashBucketCollisionList hl = itsHashCollisionBuckets [ hv ];

		if ( hl == null ) {
			if ( add ) {
				hl = new DMIHashBucketCollisionList ();
				itsHashCollisionBuckets [ hv ] = hl;
			} else {
				return null;
			}
		}

		snl = hl.findInCollisionBucket ( lcStr, add );
		if ( snl == null && add ) {
			snl = new DMISharedNameList ( lcStr, mcStr );
			hl.addToCollisionBucket ( snl );
		}

		return snl;
	}

	private int hashValue ( String s ) {
		int a;

		int vLen = s.length ();
		int v = vLen;

		for ( int i = 0; i < vLen; i++ ) {
			a = v & 0x1FFFFFFF;
			int b = v - a;
			if ( b > 0 ) {
				v = a ^ 0x7;
			} else {
				v = a;
			}
			int c = s.charAt ( i );
			b = i & 0x3;
			b = b + b;
			b = b + b;
			v = (v + c) ^ (b + b);
		}
		a = v >> 11;
		v = (a + v) & 0x7FF;
		//a = itsHashCollisionBuckets).length
		//while ( v >== a ) {
		//    b = v Mod a
		//    c = v / a
		//    v = b + c
		//}
		return v;
	}

	// for debugging
	public DMIHashBucketCollisionList [] hashCollisionBuckets () {
		return itsHashCollisionBuckets;
	}

	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++; //self
		mrc.ptrCount += itsHashCollisionBuckets.length;
		for ( int i = 0; i < itsHashCollisionBuckets.length; i++ ) {
			if ( itsHashCollisionBuckets [ i ] != null ) {
				itsHashCollisionBuckets [ i ].memUsage ( mrc );
			}
		}
	}
}
