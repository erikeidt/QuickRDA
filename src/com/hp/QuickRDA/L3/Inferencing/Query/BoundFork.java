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

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;

/*
 * A BoundFork is a collection of BoundFrames.
 * An analogy is that a BoundFrame represents a thread, and a BoundFork represents multiple threads.
 * The multiple threads represent multiple possible interpretation of bindings over the same statements in a path.
 */
public class BoundFork {
	private IList<BoundFrame>	itsValues;

	public BoundFork () {
	}

	@SuppressWarnings("all")
	public BoundFork ( BoundFork f ) {
		itsValues = (IList<BoundFrame>) f.itsValues.clone ();
	}

	public BoundFork ( BoundFrame f ) {
		this ();
		addFrame ( f );
	}

	public IList<BoundFrame> getFrames () {
		return itsValues;
	}

	public void clear () {
		itsValues.clear ();
	}

	public boolean isEmpty () {
		return itsValues == null ? true : (itsValues.size () == 0);
	}

	/*
	 * In order to accept a candidate, just one of the fork's frames must accept it.
	 * If more than one frame accepts it, then we simply will accumulate a larger fork as a result.
	 */
	public boolean acceptCandidate ( DMIElem m, BoundFork boundOut, ISet<BoundFork []> updatedSubpatterns ) {
		boolean ans = false;
		if ( itsValues != null )
			for ( BoundFrame f : itsValues )
				ans |= f.acceptCandidate ( m, boundOut, updatedSubpatterns );
		return ans;
	}

	public void addFrame ( BoundFrame f ) {
		// itsValues = BoundFrame.addToList  (f, itsValues);
		if ( itsValues == null )
			itsValues = new XSetList<BoundFrame> ( XSetList.AsSet, true ); // $$$ big change
		/*
		int len = itsValues.size ();
		for ( int i = 0; i < len; i++ ) {
			BoundFrame g = itsValues.get (i);
			if ( f.equals (g) ) {
				if ( Tracing.gTraceFile != null )
					Tracing.gTraceFile.println ("found frame #" + f.itsIndex + " same as #" + g.itsIndex);
				return;
			}
		}
		*/
		itsValues.add ( f );
	}

	public void replaceFrame ( int i, BoundFrame f ) {
		itsValues.set ( i, f );
	}

	public boolean usesSubForkIn ( ISet<BoundFork []> updatedSubpatterns ) {
		int cnt = itsValues.size ();
		for ( int i = 0; i < cnt; i++ ) {
			if ( itsValues.get ( i ).usesSubForkIn ( updatedSubpatterns ) )
				return true;
		}
		return false;
	}

	public void sortSubpatternFrames () {
		// keep frame 0 where it is; it's the primordial match all frame
		if ( itsValues.size () > 2 )
			itsValues.sortList ( 1, BoundFrame.cmp );
	}

	public void mergeIntoFork ( BoundFork fk ) {
		mergeIntoFork ( fk.itsValues );
	}

	private void mergeIntoFork ( List<BoundFrame> fL ) {
		if ( fL != null ) {
			if ( itsValues == null )
				itsValues = new XSetList<BoundFrame> ( XSetList.AsSet, true );
			for ( BoundFrame f : fL )
				// itsValues.add (f);
				addFrame ( f );
		}
	}

	public BoundFork clone () {
		BoundFork ans = new BoundFork ();
		ans.mergeIntoFork ( itsValues );
		return ans;
	}

	public void traceOut ( String prefix ) {
		if ( itsValues == null )
			Tracing.gTraceFile.println ( prefix + " is null!" );
		else {
			for ( BoundFrame f : itsValues ) {
				Tracing.gTraceFile.print ( prefix );
				f.traceOut ();
				Tracing.gTraceFile.println ();
			}
		}
	}

	/*
	public int getHashCode() {		
	}
	
	public void Equals(Object obj) {
	}
	*/

	public boolean equals ( BoundFork fk ) {
		if ( fk == null )
			return false;

		if ( this == fk )
			return true;

		int len = itsValues.size ();
		if ( fk.itsValues.size () != len )
			return false;
		for ( int i = 0; i < len; i++ ) {
			BoundFrame t = itsValues.get ( i );
			BoundFrame f = fk.itsValues.get ( i );
			if ( f != t ) {
				if ( f == null )
					return false;
				if ( t == null )
					return false;
				if ( !f.equals ( t ) )
					return false;
			}
		}

		return true;
	}

}