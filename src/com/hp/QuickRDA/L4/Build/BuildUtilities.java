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

package com.hp.QuickRDA.L4.Build;

import com.hp.QuickRDA.L0.lang.*;

public class BuildUtilities {

	private static long	gt0;	// used for performance timing

	public static long startStopwatch () {
		gt0 = System.currentTimeMillis ();
		return gt0;
	}

	public static void stopStopwatch ( long t0 ) {
		double td = System.currentTimeMillis ();
		if ( t0 > 0 ) {
			td = td - t0;
		} else {
			td = td - gt0;
		}
		td = td * 1000;
	}

	public static String makeJSONEscapeText ( String sIn ) {
		String s;
		s = sIn;
		//s = Replace(s, "\", "/")
		s = Strings.Replace ( s, '\"', "\\\"" );
		s = s.replace ( '’', '\'' );
		return s;
	}

	public static String escapeTextForFile ( String s ) {
		String res = ""; 
		char[] buf = s.toCharArray(); 
		int charval;
		for(int i=0;i<buf.length;i++) { 
			switch (buf[i]) {
			case '<':  
			case '>': 
			case ':': 
			case '"':
			case '/':
			case '\\':
			case '|': 
			case '?': 
			case '*': 
			case '&': 
			case ' ':     res += "_";		break;
			default: {
				charval = buf[i];
				if (charval < 32) ; // drop control chars
				//	else if (charval > 127) res += "_";
				else res += Character.toString (buf[i]);	
			}
			}
		}
//		System.out.println ("Input:  " + s );
//		System.out.println ("Result: " + res );
		return res;
	}
}