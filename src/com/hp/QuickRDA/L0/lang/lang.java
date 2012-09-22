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

package com.hp.QuickRDA.L0.lang;


public class lang {

	public static int [] expandPreserve ( int [] v, int u ) {
		int [] ans = new int [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

	public static int [] toIntArray ( IList<Integer> il ) {
		int cnt = il.size ();
		int [] ans = new int [ cnt ];
		for ( int i = 0; i < cnt; i++ )
			ans [ i ] = il.get ( i );
		return ans;
	}

	public static int indexOf ( int v, int [] iL ) {
		if ( iL != null ) {
			for ( int i = 0; i < iL.length; i++ ) {
				if ( iL [ i ] == v )
					return i;
			}
		}
		return -1;
	}

	public static int indexOf ( int v, int [] iL, int i ) {
		for ( ; i < iL.length; i++ ) {
			if ( iL [ i ] == v ) {
				return i;
			}
		}
		return -1;
	}

	public static Object [] expandPreserve ( Object [] v, int u ) {
		Object [] ans = new Object [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

	public static Object [] expandPreserve ( Object [] v, Object [] w ) {
		int u = v.length + w.length;
		Object [] ans = expandPreserve ( v, u );
		System.arraycopy ( w, 0, ans, v.length, w.length );
		return ans;
	}

	public static int indexOf ( java.lang.Object o, java.lang.Object[] oL ) {
		if ( oL != null ) {
			for ( int i = 0; i < oL.length; i++ ) {
				if ( oL [ i ] == o )
					return i;
			}
		}
		return -1;
	}

	public static int indexOf ( java.lang.Object o, java.lang.Object[] oL, int i ) {
		for ( ; i < oL.length; i++ ) {
			if ( oL [ i ] == o ) {
				return i;
			}
		}
		return -1;
	}

	public static int [] addToList ( int v, int [] iL ) {
		int u = (iL == null ? 0 : iL.length);
		iL = lang.expandPreserve ( iL, u + 1 );
		iL [ u ] = v;
		return iL;
	}

	public static int [] addToSet ( int v, int [] iL ) {
		if ( iL != null && indexOf ( v, iL ) >= 0 )
			return iL;
		return addToList ( v, iL );
	}

	public static int minInt ( int a, int b ) {
		if ( a < b )
			return a;
		return b;
	}

	private static int	gMsgCount;

	public static void clearErrors () {
		gMsgCount = 0;
	}

	public static int getMsgCount () {
		return gMsgCount;
	}

	public static void errMsg ( String s ) {
		gMsgCount++;
		System.err.println ( s );
	}

	private static class IntegerComparator implements IComparator<Integer> {
		@Override
		public boolean follows ( Integer i, Integer j ) {
			return i.intValue () > j.intValue ();
		}
	}

	public static IntegerComparator	gIntegerComparator	= new IntegerComparator ();

	public static int rotateLeft ( int x, int cnt ) {
		int ans = (x << cnt) | ((x >> (32 - cnt)) & (cnt - 1));
		return ans;
	}

}
