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

import java.io.BufferedReader;

import com.hp.QuickRDA.L0.lang.*;

public class UnitOfInterchange {

	public static class Serialization {
		public int	itsType;
		String		itsValue;

		public Serialization ( int n, String v ) {
			itsType = n;
			itsValue = v;
		}
	}

	public static class Statement {
		public int	itsSubject;
		public int	itsVerb;
		public int	itsObject;

		public Statement ( int s, int v, int o ) {
			itsSubject = s;
			itsVerb = v;
			itsObject = o;
		}
	}

	public static class InitialReference {
		public int	itsClass;
		public int	itsSerialization;

		public InitialReference ( int cls, int ser ) {
			itsClass = cls;
			itsSerialization = ser;
		}
	}

	public String				itsDMIVersion;
	public String				itsSourceFile;
	public String				itsLabel;

	public Serialization []		itsSerializations		= new Serialization [ 0 ];
	public int					itsSimpleConceptCount;
	public Statement []			itsUnassertedStatements	= new Statement [ 0 ];
	public Statement []			itsAssertedStatements	= new Statement [ 0 ];
	public InitialReference []	itsInitialReferences	= new InitialReference [ 0 ];

	private boolean				itsDidVersion;
	private boolean				itsDidLabel;
	private boolean				itsDidSerializations;
	private boolean				itsDidUnassertedStatements;
	private boolean				itsDidAssertedStatements;

	private TokenReader			itsTkn;

	public void importJSONDMI ( String filePath, String filePrefix, String fileSuffix, String label ) throws Exception {
		StringRef xx = new StringRef ();
		BufferedReader in = TextFile.openTheFileForRead ( filePath, filePrefix, fileSuffix, xx );
		itsSourceFile = xx.str;
		itsTkn = new TokenReader ( in, "DMI Import", xx.str );
		itsTkn.expectChar ( '{' );
		for ( ;; ) {
			int c = itsTkn.nextNonBlankChar ();
			switch ( c ) {
			case -1 :
				itsTkn.error ( "Expecting , or } ... found EOF" );
				break;
			case '\"' :
				String s = itsTkn.getRestOfString ();
				if ( "Serializations".equals ( s ) )
					readSerializations ();
				else if ( "Simple Concept Count".equals ( s ) )
					readConceptCount ();
				else if ( "Unasserted Statements".equals ( s ) )
					readUnassertedStatements ();
				else if ( "Asserted Statements".equals ( s ) )
					readAssertedStatements ();
				else if ( "DMI Version".equals ( s ) )
					readVersion ();
				else if ( "Label".equals ( s ) )
					readLabel ();
				else if ( "Initial Reference".equals ( s ) )
					readInitialReference ();
				continue;
			case ',' :
				continue;
			case '}' :
				break;
			default :
				itsTkn.error ( "Expecting , or } ... found " + (char) c );
				break;
			}
			break;
		}
		// itsTkn.ExpectChar('}');
	}

	public void readVersion () throws Exception {
		if ( itsDidVersion ) {
			itsTkn.error ( "DMI Version already seen." );
		}

		itsTkn.expectChar ( ':' );
		parseValue ();
		itsDidVersion = true;
	}

	public void readLabel () throws Exception {
		if ( itsDidLabel ) {
			itsTkn.error ( "Label already seen." );
		}
		itsTkn.expectChar ( ':' );
		parseValue ();
		itsDidLabel = true;
	}

	private void readSerializations () throws Exception {
		if ( itsDidSerializations ) {
			itsTkn.error ( "Serializations already seen." );
		}
		itsTkn.expectChar ( ':' );
		itsTkn.expectChar ( '[' );
		for ( ;; ) {
			int c = itsTkn.nextNonBlankChar ();
			switch ( c ) {
			case -1 :
				itsTkn.error ( "Unexpected EOF" );
				break;
			case '[' :
				readRestOfSerialization ();
				continue;
			case ',' :
				continue;
			case ']' :
				break;
			default :
				itsTkn.error ( "Unexpected Character '" + (char) c + "'" );
			}
			break;
		}
		itsDidSerializations = true;
	}

	private void readRestOfSerialization () throws Exception {
		int n = itsTkn.getInteger ();
		itsTkn.expectChar ( ',' );
		itsTkn.expectChar ( '\"' );
		String v = itsTkn.getRestOfString ();
		itsSerializations = addToList ( new Serialization ( n, v ), itsSerializations );
		itsTkn.expectChar ( ']' );
	}

	private void readConceptCount () throws Exception {
		itsTkn.expectChar ( ':' );
		this.itsSimpleConceptCount = itsTkn.getInteger ();
	}

	private Statement [] readStatements ( Statement [] stmts ) throws Exception {
		itsTkn.expectChar ( ':' );
		itsTkn.expectChar ( '[' );
		for ( ;; ) {
			int c = itsTkn.nextNonBlankChar ();
			switch ( c ) {
			case -1 :
				itsTkn.error ( "Unexpected EOF" );
				break;
			case '[' :
				stmts = readRestOfStatement ( stmts );
				continue;
			case ',' :
				continue;
			case ']' :
				break;
			default :
				itsTkn.error ( "Unexpected Character '" + (char) c + "'" );
				break;
			}
			break;
		}
		return stmts;
	}

	public Statement [] readRestOfStatement ( Statement [] stmts ) throws Exception {
		int sb = itsTkn.getInteger ();
		itsTkn.expectChar ( ',' );
		int vb = itsTkn.getInteger ();
		itsTkn.expectChar ( ',' );
		int ob = itsTkn.getInteger ();
		int u = stmts.length;
		stmts = expandPreserve ( stmts, u + 1 );
		stmts [ u ] = new Statement ( sb, vb, ob );
		itsTkn.expectChar ( ']' );
		return stmts;
	}

	private void readUnassertedStatements () throws Exception {
		if ( itsDidUnassertedStatements ) {
			itsTkn.error ( "Unasserted Statements already seen." );
		}
		itsUnassertedStatements = readStatements ( itsUnassertedStatements );
		itsDidUnassertedStatements = true;
	}

	private void readAssertedStatements () throws Exception {
		if ( itsDidAssertedStatements ) {
			itsTkn.error ( "Asserted Statements already seen." );
		}
		itsAssertedStatements = readStatements ( itsAssertedStatements );
		itsDidAssertedStatements = true;
	}

	private void readInitialReference () throws Exception {
		itsTkn.expectChar ( ':' );
		itsTkn.expectChar ( '{' );
		for ( ;; ) {
			int c = itsTkn.nextNonBlankChar ();
			switch ( c ) {
			case -1 :
				itsTkn.error ( "Expecting , or } ... found EOF" );
				break;
			case '\"' :
				String s = itsTkn.getRestOfString ();
				if ( "Scheme".equals ( s ) )
					readScheme ();
				else if ( "Concepts".equals ( s ) )
					readInitialConcepts ();
				continue;
			case ',' :
				continue;
			case '}' :
				break;
			default :
				itsTkn.error ( "Expecting , or } ... found " + (char) c );
				break;
			}
			break;
		}
		// itsTkn.ExpectChar('}');
	}

	private void readScheme () throws Exception {
		itsTkn.expectChar ( ':' );
		parseValue ();
	}

	private void readInitialConcepts () throws Exception {
		itsTkn.expectChar ( ':' );
		itsTkn.expectChar ( '[' );
		for ( ;; ) {
			int c = itsTkn.nextNonBlankChar ();
			switch ( c ) {
			case -1 :
				itsTkn.error ( "Expecting , or } ... found EOF" );
				break;
			case '[' :
				int x = itsTkn.getInteger ();
				itsTkn.expectChar ( ',' );
				int y = itsTkn.getInteger ();
				InitialReference ir = new InitialReference ( x, y );
				itsInitialReferences = addToList ( ir, itsInitialReferences );
				itsTkn.expectChar ( ']' );
				continue;
			case ',' :
				continue;
			case ']' :
				break;
			default :
				itsTkn.error ( "Expecting , or } ... found " + (char) c );
				break;
			}
			break;
		}
	}

	private void parseValue () throws Exception {
		int c = itsTkn.nextNonBlankChar ();
		switch ( c ) {
		case -1 :
			// TODO error...
			break;
		case '\"' :
			/* String s = */
			itsTkn.getRestOfString ();
			break;
		case '{' : // TODO
		case '[' : // TODO
		default :
			if ( c >= '0' && c <= '9' )
				itsTkn.getInteger ();
			// TODO
		}
		// itsTkn.SkipCharsUntil(',');
	}

	public static Serialization [] addToList ( Serialization s, Serialization [] sL ) {
		int u = (sL == null ? 0 : sL.length);
		sL = expandPreserve ( sL, u + 1 );
		sL [ u ] = s;
		return sL;
	}

	public static Serialization [] expandPreserve ( Serialization [] v, int u ) {
		Serialization [] ans = new Serialization [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

	public static Statement [] addToList ( Statement s, Statement [] sL ) {
		int u = (sL == null ? 0 : sL.length);
		sL = expandPreserve ( sL, u + 1 );
		sL [ u ] = s;
		return sL;
	}

	public static Statement [] expandPreserve ( Statement [] v, int u ) {
		Statement [] ans = new Statement [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

	public static InitialReference [] addToList ( InitialReference s, InitialReference [] sL ) {
		int u = (sL == null ? 0 : sL.length);
		sL = expandPreserve ( sL, u + 1 );
		sL [ u ] = s;
		return sL;
	}

	public static InitialReference [] expandPreserve ( InitialReference [] v, int u ) {
		InitialReference [] ans = new InitialReference [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

}
