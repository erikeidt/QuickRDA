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

public class NameUtilities {

	public enum SortKindEnum {
		SortByIndex,
		SortByLCName,
		SortByMCName
	};

	@SuppressWarnings("unchecked")
	public static DMISharedNameList getFirstName ( DMIElem e ) {
		DMISharedNameList nl = null;
		Object nameSet = e.itsNameSet;
		if ( nameSet != null ) {
			// nl = ((DMISharedNameList []) nameSet) [ 0 ];
			nl = ((List<DMISharedNameList>) nameSet).get ( 0 );
		}
		return nl;
	}

	public static String getMCText ( DMIElem e ) {
		if ( e == null )
			return null;
		DMISharedNameList snl = getFirstName ( e );
		if ( snl == null )
			return "";
		return snl.itsMCText;
	}

	public static String getLCKey ( DMIElem e ) {
		DMISharedNameList snl = getFirstName ( e );
		if ( snl == null )
			return "";
		return snl.itsLCKey;
	}

	/*
	private static String HasLCText(DMISharedNameList snl) {
		if ( snl == null )
			return null;
		return snl.itsLCKey;
	}
	*/

	public static String hasMCText ( DMISharedNameList snl ) {
		if ( snl == null )
			return null;
		return snl.itsMCText;
	}

	public static String getDescriptionFor ( DMIElem m, DMIGraph g ) {
		return "";
	}

	public static String getURLFor ( DMIElem m, DMIGraph g ) {
		return "";
	}

	public static class NameComparator extends DMIElem.Comparator {
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			DMISharedNameList n1 = getFirstName ( e1 );
			DMISharedNameList n2 = getFirstName ( e2 );
			if ( n1 != null && n2 != null )
				return n1.itsLCKey.compareTo ( n2.itsLCKey ) > 0;
			if ( n2 != null )
				return true;
			return false;
		}
	}

	public static IComparator<DMIElem>	cmp	= new NameComparator ();

	public static void sortList ( IList<DMIElem> lst, SortKindEnum sk ) {
		lst.sortList ( cmp );
	}

	public static String firstName ( DMIElem concept, DMIGraph g, boolean doQual, boolean dbg ) {
		String ans = hasMCText ( getFirstName ( concept ) );
		if ( ans != null )
			return ans;

		ISet<DMIElem> stmtV;
		stmtV = concept.findStatementsFromUsedAsSubjectByVerbAndObject ( g.itsBaseVocab.gHasName, null, null );

		for ( int i = 0; i < stmtV.size (); i++ ) {
			DMIElem m = stmtV.get ( i );
			if ( m != null ) {
				if ( "".equals ( ans ) ) {
					ans = getNameValue ( m.itsObject, g, doQual, false );
				} else {
					ans = ans + "," + getNameValue ( m.itsObject, g, doQual, false );
				}
			}
		}

		if ( "".equals ( ans ) ) {
			if ( concept.instanceOf ( g.itsBaseVocab.gName ) ) {
				if ( dbg ) {
					ans = "(" + getNameValue ( concept, g, doQual, false ) + ")";
				} else {
					ans = getNameValue ( concept, g, doQual, false );
				}
			}
			if ( concept.isSerialization () ) {
				if ( dbg ) {
					ans = "['" + concept.value () + "']";
				} else {
					ans = "['" + concept.value () + "']";
				}
			}
		}
		return ans;
	}

	public static String getNameValue ( DMIElem ob, DMIGraph g, boolean doQual, boolean dbg ) {
		ISet<DMIElem> sV = ob.findStatementsFromUsedAsSubjectByVerbAndObject ( g.itsBaseVocab.gHasNameString, null, null );

		String str = "";
		if ( sV.size () > 0 ) {
			// $$$ TODO fix this...
			str = sV.get ( 0 ).itsObject.value ();
		}

		if ( doQual ) {
			sV = ob.findStatementsFromUsedAsSubjectByVerbAndObject ( g.itsBaseVocab.gHasQualifier, null, null );
			if ( sV.size () >= 1 ) {
				if ( sV.get ( 0 ).itsObject == g.itsBaseVocab.gDMI ) {
					str = "~" + str; //substitute com.hp.DMI. namespace with simple ~ prefix
				} else {
					String qstr = firstName ( sV.get ( 0 ).itsObject, g, false, false ); //False=show full namespace path; true=show one level of namespace path
					str = qstr + "." + str;
				}
			}
		}
		return str;
	}

	// Trim << ... >>
	// Additional:
	//  1: trim nested: << << ... >> >>
	//  2: trim subsequent << >> << >>
	public static String trimDifferentiator ( String s ) {
		String ans;
		for ( ;; ) {
			int q;
			ans = s;
			int p0 = Strings.InStr ( 1, s, "<<" );
			if ( p0 > 0 ) {
				int n = 0;
				int p1 = p0 + 2;
				for ( ;; ) {
					q = Strings.InStr ( p1, s, ">>" );
					int r = Strings.InStr ( p1, s, "<<" );

					if ( r > 0 && r < q ) {
						n = n + 1;
						p1 = r + 2;
						continue;
					} else if ( q > 0 && n > 0 ) {
						n = n - 1;
						p1 = q + 2;
						continue;
					}
					break;
				}
				if ( q > 0 ) {
					// TrimDifferentiator = XTrim(TrimDifferentiator(XTrim(Mid(s, 1, p0 - 1) + Mid(s, q + 2))))
					s = Strings.xtrim ( Strings.Mid ( s, 1, p0 - 1 ) + Strings.Mid ( s, q + 2 ) );
					continue;
				}
			}
			break;
		}
		return ans;
	}

	private static String getDifferentiator ( String s ) {
		String ans = null;
		int p = Strings.InStr ( 1, s, "<<" );
		if ( p > 0 ) {
			p = p + 2; // len of <<
			int q = Strings.InStr ( p, s, ">>" );
			if ( q > 0 ) {
				ans = Strings.xtrim ( Strings.Mid ( s, p, q - p ) );
			}
		}
		return ans;
	}

	public static String flattenedBasicDifferentiator ( String name, String diffVal ) {
		String existingDiff = getDifferentiator ( diffVal );
		if ( existingDiff == null ) {
			return name + " <<" + diffVal + ">>";
		}
		else {
			return name + " <<" + trimDifferentiator ( diffVal ) + ">> <<" + existingDiff + ">>";
		}
	}

	public static String basicDifferentiator ( String name, String diffVal ) {
		/*
		if ( name.contains("<<") )
			return name;
			*/
		return name + " <<" + diffVal + ">>";
	}

	public static String constructDifferentiator ( String name, String diffTyp, String diffVal ) {
		String diffStart = " <<" + diffTyp + ":";
		// don't override existing diff
		if ( name.indexOf ( diffStart ) >= 0 )
			return name;
		return name + diffStart + diffVal + ">>";
	}

	public static String structuredDifferentiator ( String name, DMIElem sb, DMIElem vb, DMIElem ob ) {
		/*
		 * New Differentiator form 
		 * 	<<sb...vb...ob>>
		 */
		return structuredDifferentiator ( name, getMCText ( sb ), getMCText ( vb ), getMCText ( ob ) );
	}

	public static String structuredDifferentiator ( String name, String sb, String vb, String ob ) {
		// $$$ TODO escape text for << >> and ... 
		// $$$ TODO also for ( and ), and, to use ( and ) as needed.
		if ( sb == null ) {
			String diffVB = " <<:" + vb + ":";
			if ( name.indexOf ( diffVB ) >= 0 )
				return name;
			return name + diffVB + ob + ">>";
		} else if ( ob == null ) {
			String diffVB = ":" + vb + ":>>";
			if ( name.indexOf ( diffVB ) >= 0 )
				return name;
			return name + " <<" + sb + diffVB;
		}
		// assert false : "BuildUtilities.StructuredDifferentiator: Unexpected input (neither sb nor ob is null)";
		String diffVB = ":" + vb + ":";
		if ( name.indexOf ( diffVB ) >= 0 )
			return name;
		return name + " <<" + sb + diffVB + ob + ">>";
	}

}
