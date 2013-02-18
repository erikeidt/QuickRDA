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

public class Strings {

	public static String [] addToList ( String s, String [] sL ) {
		int u = (sL == null ? 0 : sL.length);
		sL = expandPreserve ( sL, u + 1 );
		sL [ u ] = s;
		return sL;
	}

	public static String [] expandPreserve ( String [] v, int u ) {
		String [] ans = new String [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

	// VBA Equivalent function
	public static int InStr ( String master, String subStr ) {
		return master.indexOf ( subStr ) + 1;
	}

	// VBA Equivalent function
	public static int InStr ( int start, String master, String subStr ) {
		return master.indexOf ( subStr, start - 1 ) + 1;
	}

	// VBA Equivalent function
	public static String Mid ( String s1, int start, int slen ) {
		start--;
		int len = s1.length ();
		int xlen = start + slen;
		if ( xlen > len )
			xlen = len;
		return s1.substring ( start, xlen );
	}

	// VBA Equivalent function
	public static String Mid ( String s1, int start ) {
		return s1.substring ( start - 1 );
	}

	// VBA Equivalent function
	public static String Replace ( String s1, String s2, String s3 ) {
		String ans = null;
		int mark = 0;
		for ( ;; ) {
			int next = s1.indexOf ( s2, mark );
			if ( next < 0 )
				break;
			if ( ans == null )
				ans = s1.substring ( mark, next );
			else
				ans += s1.substring ( mark, next );
			mark = next + s2.length ();
			ans += s3;
		}
		if ( mark == 0 )
			return s1;
		return ans + s1.substring ( mark );
	}

	// VBA Equivalent function
	public static String Replace ( String s1, char c2, String s3 ) {
		String ans = null;
		int mark = 0;
		for ( ;; ) {
			int next = s1.indexOf ( c2, mark );
			if ( next < 0 )
				break;
			if ( ans == null )
				ans = s1.substring ( mark, next );
			else
				ans += s1.substring ( mark, next );
			mark = next + 1;
			ans += s3;
		}
		if ( mark == 0 )
			return s1;
		return ans + s1.substring ( mark );
	}

	// VBA Equivalent function
	public static String Replace ( String s1, char c2, char c3, String s4 ) {
		String ans = null;
		int mark = 0;
		for ( ;; ) {
			int next = s1.indexOf ( c2, mark );
			if ( next < 0 )
				break;
			if ( ans == null )
				ans = s1.substring ( mark, next );
			else
				ans += s1.substring ( mark, next );
			mark = next + 2;
			int nx = s1.charAt ( next + 1 );
			nx -= (int) c3;
			ans += s4.charAt ( nx );
		}
		if ( mark == 0 )
			return s1;
		return ans + s1.substring ( mark );
	}

	// VBA Equivalent function
	public static boolean isNumeric ( String s ) {
		boolean ans;
		try {
			// java.lang.Integer.parseInt(s);
			java.lang.Double.parseDouble ( s );
			ans = true;
		} catch ( Exception e ) {
			ans = false;
		}
		return ans;
	}

	public static String xtrim ( String s1 ) {
		s1 = s1.replace ( '\r', ' ' );
		s1 = s1.replace ( '\n', ' ' );
		return s1.trim ();
	}

	public static boolean initialMatch ( String x, StringRef xx, String k ) {
		if ( InStr ( 1, x, k ) == 1 ) {
			xx.str = Mid ( x, k.length() + 1 );
			return true;
		}
		return false;
	}

	//Updates parameter x
	public static String splitAfterComma ( String xi, StringRef xo ) {
		return splitAfter ( xi, xo, "," );
	}

	public static String splitAfterGtr ( String xi, StringRef xo ) {
		return splitAfter ( xi, xo, ">" );
	}

	public static String splitAfter ( String xi, StringRef xo, String s ) {
		int p;
		p = InStr ( 1, xi, s );
		if ( p > 0 ) {
			xo.str = Mid ( xi, p + s.length () );
			return Mid ( xi, 1, p - 1 );
		} else {
			xo.str = "";
			return xi;
		}
	}

	public static String tSplitAfter ( String xi, StringRef xo, String s ) {
		return splitAfter ( xi, xo, s ).trim ();
	}

	public static String splitafterItemSeparator ( String xi, StringRef xo ) {
		String s1;
		s1 = " && ";
		int p;
		p = InStr ( 1, xi, s1 );

		String s2;
		int q;

		s2 = "\n";
		q = InStr ( 1, xi, s2 );
		if ( q > 0 && (p == 0 || q < p) ) {
			p = q;
			s1 = s2;
		}

		s2 = "\r";
		q = InStr ( 1, xi, s2 );
		if ( q > 0 && (p == 0 || q < p) ) {
			p = q;
			s1 = s2;
		}

		if ( p > 0 ) {
			xo.str = Mid ( xi, p + s1.length () );
			return xtrim ( Mid ( xi, 1, p - 1 ) ); //Added XTrim here 12/09/2010
		} else {
			xo.str = "";
			return xtrim ( xi ); //And here...
		}
	}

	public static final boolean	gOptionCRLFAsSeparator	= false;

	public static String ztrim ( String sIn ) {
		if ( gOptionCRLFAsSeparator ) {
			String s1 = sIn;
			for ( ;; ) {
				String s2 = s1.trim ();
				s1 = atrim ( s2, "\n" );
				s1 = atrim ( s1, "\r" );
				s1 = atrim ( s1, "\t" );
				if ( s1 != s2 )
					continue;
				return s1;
			}
		}
		return xtrim ( sIn );
	}

	private static String atrim ( String sIn, String sx ) {
		int p = InStr ( 1, sIn, sx );
		if ( p == 1 ) {
			return Mid ( sIn, sx.length () );
		} else if ( p == sIn.length () - sx.length () ) {
			return Mid ( sIn, 1, p - 1 );
		} else {
			return sIn;
		}
	}

}
