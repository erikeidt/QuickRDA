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

public class ScanReader {
	public BufferedReader	itsReader;
	public String			itsName;
	public String			itsOperation;
	public int				itsLineNumber;
	public int				itsLineOffset;
	public int				itsCurrChar;
	public int				itsNextChar;

	public ScanReader ( BufferedReader rdr, String op, String fName ) {
		itsReader = rdr;
		itsOperation = op;
		itsName = fName;
		itsLineNumber = 1;
		itsLineOffset = -2;
		advanceChar ();
		advanceChar ();
	}

	private String scanErrMsg ( String msg ) {
		return (itsOperation + ": " + msg + "  at line " + itsLineNumber + " (" + itsLineOffset + ")");
	}

	public void scanError ( String msg ) {
		String err = scanErrMsg ( msg );
		// lang.errMsg (err);
		throw new RuntimeException ( err );
	}

	public void close () {
		if ( itsReader != null ) {
			try {
				itsReader.close ();
			} catch ( IOException e ) {}
			itsReader = null;
		}
	}

	private int readChar () {
		try {
			return itsReader.read ();
		} catch ( Exception e ) {
			lang.errMsg ( scanErrMsg ( e.getMessage () ) );
		}
		return -1;
	}

	public int advanceChar () {
		itsLineOffset++;
		itsCurrChar = itsNextChar;

		if ( itsNextChar != -1 )
			itsNextChar = readChar ();

		if ( itsCurrChar == '\r' ) {
			itsCurrChar = '\n';
			itsLineNumber++;
			itsLineOffset = 0;
			if ( itsNextChar == '\n' )
				itsNextChar = readChar ();
		}
		else if ( itsCurrChar == '\n' ) {
			itsLineNumber++;
			itsLineOffset = 0;
		}

		return itsCurrChar;
	}

	public int currChar () {
		return itsCurrChar;
	}

	public boolean isEOF () {
		trim ();
		return itsCurrChar == -1;
	}

	public void trim () {
		for ( ;; ) {
			// Trim all comments and whitespace (in any order)
			if ( itsCurrChar == '/' ) {
				if ( itsNextChar == '/' ) {
					advanceChar ();
					advanceChar ();
					while ( itsCurrChar != -1 && itsCurrChar != '\r' && itsCurrChar != '\n' )
						advanceChar ();
					advanceChar ();
					continue;
				}
				if ( itsNextChar == '*' ) {
					advanceChar ();
					advanceChar ();
					while ( itsCurrChar != -1 && itsCurrChar != '*' && itsNextChar != '/' )
						advanceChar ();
					advanceChar ();
					advanceChar ();
					continue;
				}
			}

			if ( itsCurrChar == -1 )
				break;

			if ( java.lang.Character.isWhitespace ( (char) itsCurrChar ) ) {
				advanceChar ();
				continue;
			}

			break;
		}
	}

	public int currToken () {
		trim ();

		if ( itsCurrChar == -1 )
			return -1;

		if ( java.lang.Character.isDigit ( (char) itsCurrChar ) )
			return '0';

		if ( java.lang.Character.isUnicodeIdentifierPart ( (char) itsCurrChar ) )
			return 'A';

		return itsCurrChar;
	}

	public String getMoreIdentifier () {
		String ans = "";
		if ( java.lang.Character.isUnicodeIdentifierPart ( (char) itsCurrChar ) ) {
			while ( java.lang.Character.isUnicodeIdentifierPart ( (char) itsNextChar ) ) {
				ans = ans + (char) itsCurrChar;
				advanceChar ();
			}
			ans = ans + (char) itsCurrChar;
			advanceChar ();
		}
		return ans;
	}

	public String expectIdentifier () {
		trim ();
		if ( !java.lang.Character.isUnicodeIdentifierPart ( (char) itsCurrChar ) )
			scanError ( "Expected Identifier instead of: '" + (char) itsCurrChar + "\'" );
		return getMoreIdentifier ();
	}

	public int getSimpleInteger () {
		trim ();
		if ( !java.lang.Character.isDigit ( itsCurrChar ) )
			scanError ( "Expected Number instead of: '" + (char) itsCurrChar + "\'" );
		int ans = 0;
		while ( java.lang.Character.isDigit ( itsNextChar ) ) {
			ans = ans * 10 + itsCurrChar - '0';
			advanceChar ();
		}
		ans = ans * 10 + itsCurrChar - '0';
		return ans;
	}

	public int getNegPosInteger () {
		trim ();
		boolean neg = false;
		if ( itsCurrChar == '-' ) {
			neg = true;
			advanceChar ();
		}
		int ans = getSimpleInteger ();
		return neg ? -ans : ans;
	}

	public String getRestOfString () {
		if ( itsCurrChar != '\'' && itsCurrChar != '\"' )
			scanError ( "Expected String instead of: '" + (char) itsCurrChar + "\'" );
		int quote = itsCurrChar;
		String ans = "";
		boolean first = true;
		while ( itsNextChar != -1 && itsNextChar != quote ) {
			if ( first )
				first = false;
			else
				ans = ans + (char) itsCurrChar;
			advanceChar ();
		}
		if ( first )
			first = false;
		else
			ans = ans + (char) itsCurrChar;
		if ( itsNextChar == -1 )
			scanError ( "Expected String ending quote: '\\" + (char) quote + "' not EOF " );
		advanceChar ();
		advanceChar ();
		return ans;
	}

	public void expectToken ( char expectedChar ) {
		trim ();
		if ( itsCurrChar != expectedChar )
			scanError ( "Expected character: '" + expectedChar + "' instead found: '" + (char) itsCurrChar + "\'" );
		advanceChar ();
	}

	public void expectToken ( String expectedIdentifier ) {
		if ( currToken () != 'A' )
			scanError ( "Identifier expected: " + expectedIdentifier + " instead of character '" + (char) itsCurrChar + "\'" );
		String id = expectIdentifier ();
		if ( !expectedIdentifier.equals ( id ) )
			scanError ( "Expected Identifier: " + expectedIdentifier + " instead of identifier '" + id + "\'" );
	}

	public boolean ifToken ( char testChar ) {
		trim ();
		if ( itsCurrChar != testChar )
			return false;
		advanceChar ();
		return true;
	}

}
