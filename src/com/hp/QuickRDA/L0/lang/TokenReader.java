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

import java.io.BufferedReader;
import java.io.IOException;

import com.hp.QuickRDA.L5.ExcelTool.Start;

public class TokenReader {
	public BufferedReader	itsReader;
	public String			itsName;
	public String			itsOperation;
	public int				itsLineNum;
	public int				itsOffset;

	public void close () {
		if ( itsReader != null ) {
			try {
				itsReader.close ();
			} catch ( IOException e ) {}
			itsReader = null;
		}
	}

	public TokenReader ( BufferedReader rdr, String op, String fName ) {
		itsReader = rdr;
		itsOperation = op;
		itsName = fName;
		itsLineNum = 1;
		itsOffset = 0;
	}

	public void error ( String s ) {
		lang.errMsg ( itsOperation + ": " + s + "  at line " + itsLineNum + " (" + itsOffset + ")" );
		throw new RuntimeException ( "Error in input file." );
	}

	public boolean isEOF () throws Exception {
		itsReader.mark ( 1 );
		int ch = nextNonBlankChar ();
		if ( ch < 0 )
			return true;
		itsReader.reset ();
		return false;
	}

	public boolean ifChar ( char mx ) throws Exception {
		itsReader.mark ( 1 );
		int ch = nextNonBlankChar ();
		if ( ch == mx )
			return true;
		itsReader.reset ();
		return false;
	}

	public void expectChar ( int c, char ch ) {
		if ( c != ch )
			error ( itsOperation + ": Expected Character '" + ch + "' instead of '" + (char) c + "'" );
	}

	public void expectChar ( char ch ) {
		try {
			for ( ;; ) {
				int c = itsReader.read ();
				if ( c < 0 )
					error ( "Expected Character '" + ch + "' instead of EOF" );
				else if ( c == (int) ch )
					return;
				else if ( !java.lang.Character.isWhitespace ( c ) )
					error ( itsOperation + ": Expected Character '" + ch + "' instead of '" + (char) c + "'" );
				if ( c == '\r' ) {
					itsLineNum++;
					itsOffset = 0;
				} else
					itsOffset++;
			}
		} catch ( IOException e ) {
			e.printStackTrace (Start.gErrLogFile);
		}
	}

	public void expectIdentifier ( String str ) {
		try {
			int pos = 0;
			char ch = str.charAt ( pos );
			for ( ;; ) {
				int c = itsReader.read ();
				if ( c < 0 )
					error ( "Expected Character '" + ch + "' instead of EOF" );
				else if ( c == (int) ch ) {
					pos++;
					if ( pos == str.length () )
						return;
					ch = str.charAt ( pos );
				}
				else if ( pos > 0 || !java.lang.Character.isWhitespace ( c ) )
					error ( itsOperation + ": Expected Word/Token '" + str + "' instead of '" + (char) c + "'" );
				if ( c == '\r' ) {
					itsLineNum++;
					itsOffset = 0;
				} else
					itsOffset++;
			}
		} catch ( IOException e ) {
			e.printStackTrace (Start.gErrLogFile);
		}
	}

	/*
	public int expectOneOfStrings (String [] strs) {
		try {
			int pos = 0;
			char ch = str.charAt (pos);
			for ( ;; ) {
				int c = itsReader.read ();
				if ( c < 0 )
					error ("Expected Character '" + ch + "' instead of EOF");
				else if ( c == (int) ch ) {
					pos++;
					if ( pos == str.length () )
						return;
					ch = str.charAt (pos);
				}
				else if ( pos > 0 || !java.lang.Character.isWhitespace (c) )
					error (itsOperation + ": Expected Word/Token '" + str + "' instead of '" + (char) c + "'");
				if ( c == '\r' ) {
					itsLineNum++;
					itsOffset = 0;
				} else
					itsOffset++;
			}
		} catch ( IOException e ) {
			e.printStackTrace ();
		}

	}
	*/

	public int nextChar () {
		int c = -1;
		try {
			c = itsReader.read ();
			if ( c >= 0 ) {
				if ( c == '\r' ) {
					itsLineNum++;
					itsOffset = 0;
				} else
					itsOffset++;
			}
		} catch ( IOException e ) {
			e.printStackTrace (Start.gErrLogFile);
		}
		return c;

	}

	public int nextNonBlankChar () {
		int c = -1;
		try {
			for ( ;; ) {
				itsReader.mark ( 2 );
				c = itsReader.read ();
				if ( c < 0 )
					break;
				else if ( c == '/' ) {
					if ( nextChar () == '*' ) {
						for ( ;; ) {
							c = skipCharsUntil ( '*' );
							if ( c < 0 )
								break;
							if ( nextChar () == '/' )
								break;
						}
					} else {
						itsReader.reset (); // back to the '/'
						itsReader.read (); // and eat it
						break;
					}
				} else if ( !java.lang.Character.isWhitespace ( c ) )
					break;
				if ( c == '\r' ) {
					itsLineNum++;
					itsOffset = 0;
				} else {
					itsOffset++;
				}
				if ( c < 0 )
					break;
			}
		} catch ( IOException e ) {
			e.printStackTrace (Start.gErrLogFile);
		}
		return c;
	}

	/*
	public void pushback (int c) throws Exception {
		itsReader.reset ();
	}
	*/

	public int skipCharsUntil ( char ch ) {
		int c = -1;
		for ( ;; ) {
			c = nextChar ();
			if ( c < 0 )
				break;
			if ( c == ch )
				break;
		}
		return c;
	}

	public String getRestOfString () {
		String ans = "";
		for ( ;; ) {
			int c = nextChar ();
			switch ( c ) {
			case '\\' :
				c = nextChar ();
				switch ( c ) {
				case -1 :
					error ( "Unterminated String Literal" );
				case '\\' :
					ans += '\\';
					break;
				case 'b' :
					ans += '\b';
					break;
				case 'f' :
					ans += '\f';
					break;
				case 'n' :
					ans += '\n';
					break;
				case 'r' :
					ans += '\r';
					break;
				case 't' :
					ans += '\t';
					break;
				case 'u' : // $$$ TODO fix me
				}
				if ( c >= 0 )
					continue;
			case '"' :
				break;
			default :
				ans += (char) c;
				continue;
			}
			break;
		}
		return ans;
	}

	public int getInteger () throws Exception {
		boolean neg = false;
		int ans = 0;
		int c = nextNonBlankChar ();
		if ( c == '-' ) {
			neg = true;
			c = nextNonBlankChar ();
		}
		else if ( c == '*' )
			return -1;
		while ( c >= '0' && c <= '9' ) {
			ans = ans * 10 + (c - '0');
			itsReader.mark ( 1 );
			c = nextChar ();
		}
		itsReader.reset ();
		return neg ? -ans : ans;
	}

	public String getIdentifier () throws Exception {
		String ans = "";
		int c = nextNonBlankChar ();
		for ( ;; ) {
			ans = ans + (char) c;
			itsReader.mark ( 1 );
			c = nextChar ();
			if ( c < 0 || java.lang.Character.isWhitespace ( c ) || !java.lang.Character.isJavaIdentifierPart ( c ) ) {
				if ( c >= 0 )
					itsReader.reset ();
				break;
			}
		}
		return ans;
	}

	public String getRestOfIdentifier () throws Exception {
		String ans = "";
		for ( ;; ) {
			itsReader.mark ( 1 );
			int c = nextChar ();
			if ( c < 0 || java.lang.Character.isWhitespace ( c ) || !java.lang.Character.isJavaIdentifierPart ( c ) ) {
				if ( c >= 0 )
					itsReader.reset ();
				break;
			}
			ans = ans + (char) c;
		}
		return ans;
	}
}
