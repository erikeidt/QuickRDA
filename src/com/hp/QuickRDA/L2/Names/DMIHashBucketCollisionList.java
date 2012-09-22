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
import java.util.ArrayList;

import com.hp.QuickRDA.L0.lang.MemRefCount;

public class DMIHashBucketCollisionList {

	//
	// A list of all of the things having the same hash bucket index, which is the hash value modulo the hash table size
	//

	// private DMISharedNameList []	itsSharedNameList = new DMISharedNameList [ 0 ];
	private List<DMISharedNameList>	itsSharedNameList	= new ArrayList<DMISharedNameList> ();

	public DMISharedNameList findInCollisionBucket ( String lcStr, boolean add ) {
		// for ( int i = 0; i < itsSharedNameList.length; i++ ) {
		//	DMISharedNameList nl = itsSharedNameList [ i ];
		for ( DMISharedNameList nl : itsSharedNameList ) {
			if ( lcStr.equals ( nl.itsLCKey ) ) {
				return nl;
			}
		}
		return null;
	}

	public void addToCollisionBucket ( DMISharedNameList snl ) {
		// itsSharedNameList = DMISharedNameList.addToList  (snl, itsSharedNameList);
		itsSharedNameList.add ( snl );
	}


	// for debugging	
	public List<DMISharedNameList> getSNL () {
		return itsSharedNameList;
	}

	/*
	// for debugging	
	public DMISharedNameList [] SharedNameList () {
		return itsSharedNameList;
	}

	// for debugging
	public int Count () {
		return itsSharedNameList.length;
	}
	*/

	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++; //self
		/*
		mrc.ptrCount += itsSharedNameList.length;
		for ( int i = 0; i < itsSharedNameList.length; i++ ) {
			if ( itsSharedNameList [ i ] != null ) {
				itsSharedNameList [ i ].memUsage (mrc);
			}
		}
		*/
		mrc.ptrCount += itsSharedNameList.size ();
		for ( int i = 0; i < itsSharedNameList.size (); i++ ) {
			if ( itsSharedNameList.get ( i ) != null ) {
				itsSharedNameList.get ( i ).memUsage ( mrc );
			}
		}

	}

}
