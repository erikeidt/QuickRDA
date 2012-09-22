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

package com.hp.QuickRDA.L1.Core;

import com.hp.QuickRDA.L0.lang.*;

public class CacheBuilder {

	/*
	private static class SameAsPair {
		DMIElem	sb;
		DMIElem	ob;
	}
	private SameAsPair[]		itsSAQueue	= new SameAsPair[0];
	*/

	private DMIBaseVocabulary	baseVocab;

	public CacheBuilder ( DMIGraph g ) {
		baseVocab = g.itsBaseVocab;
	}

	public void DynamicCachingAndInferencing ( DMIElem m ) {
		DMIElem s;
		s = m.itsSubject;

		DMIElem v;
		v = m.itsVerb;

		DMIElem o;
		o = m.itsObject;

		inferredAsProperty ( v, false );
		inferredInstanceOf ( m, v, true );
		s.addAsSubjectOf ( m );
		o.addAsObjectOf ( m );

		ISet<DMIElem> cV = v.domainSet ();
		for ( int i = 0; i < cV.size (); i++ ) {
			DMIElem d = cV.get ( i );
			if ( d != null )
				inferredInstanceOf ( s, d, false );
		}

		cV = v.rangeSet ();
		for ( int i = 0; i < cV.size (); i++ ) {
			DMIElem r = cV.get ( i );
			if ( r != null )
				inferredInstanceOf ( o, r, false );
		}

		//Declared: Subject InstanceOf Object?
		//Declared: Subject SubclassOf Object?
		//Declared: Subject Domain Object?
		//Declared: Subject Range Object?

		if ( v.subclassOf ( baseVocab.gInstanceOf ) ) {
			inferredInstanceOf ( s, o, true );
		}
		if ( v.subclassOf ( baseVocab.gSubclassOf ) ) {
			InferredSubclassOf ( s, o, true );
		}
		if ( v.subclassOf ( baseVocab.gHasDomain ) ) {
			InferredPropertyWithDR ( s, o, true, true );
		}
		if ( v.subclassOf ( baseVocab.gHasRange ) ) {
			InferredPropertyWithDR ( s, o, false, true );
		}
		if ( v.subclassOf ( baseVocab.gHasPreferred ) ) {
			inferredAsProperty ( s, false );
			inferredAsProperty ( o, false );
			s.addPreferredProperty ( o );
		}
		if ( v.subclassOf ( baseVocab.gHasInverse ) ) {
			inferredAsProperty ( s, false );
			inferredAsProperty ( o, false );
			s.addInverseProperty ( o );
		}
		/*
		if ( v.SubclassOf(baseVocab.SameAs) ) {
			InferredSameAs(s, o);
		}
		*/
	}

	private void inferredAsClass ( DMIElem c, boolean explicitlyDeclared ) {
		//InferredInstanceOf c, gClass, explicitlyDeclared
		baseVocab.gClass.addAsInstance ( c, explicitlyDeclared );
	}

	private void inferredAsProperty ( DMIElem c, boolean explicitlyDeclared ) {
		//InferredInstanceOf c, gProperty, explicitlyDeclared
		baseVocab.gProperty.addAsInstance ( c, explicitlyDeclared );
		baseVocab.gClass.addAsInstance ( c, explicitlyDeclared );
	}

	private void inferredInstanceOf ( DMIElem e, DMIElem c, boolean explicitlyDeclared ) {
		inferredAsClass ( c, false );

		// if ( explicitlyDeclared || (c != baseVocab.gRelation && c != baseVocab.gConcept) )
		{
			c.addAsInstance ( e, explicitlyDeclared ); // And cache the declaration
		}
	}

	//
	// Update subc with potentially new information about superclasses from the set xV
	//   Given an input change set:
	//	         Find the full change set (could be smaller or larger than the input change set)
	//	          And  And  apply it to subc and its instances
	//	          And  And  recursively to its subclasses
	//
	private void PullSuperclass ( DMIElem subc, ISet<DMIElem> xV, boolean explicitlyDeclared ) {
		//xV is the input change set
		//   xV may have been obtained directly from a subclass of statement involving subc
		//   or xV may have been obtained by pushing changes to subclasses
		//       e.g. A is a subclass of B
		//           and we get the (new) statement that B is a subclass of C
		//           A would have been up-to-date about B and all B's previously existing superclasses
		//           so A only needs to change with respect to C and all C's superclasses that were not known in B
		//           which could be the whole change set or fewer.
		//uV is the actual change set in subc
		ISet<DMIElem> uV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		for ( int x = 0; x < xV.size (); x++ ) {
			DMIElem sprc = xV.get ( x );
			if ( sprc != null ) {
				//And cache the declaration
				if ( subc.addAsSuperclass ( sprc, explicitlyDeclared ) ) {
					uV.addToSet ( sprc );
				}

				//Pull indirect super classes down
				ISet<DMIElem> cV = sprc.superclassSet ();
				for ( int i = 0; i < cV.size (); i++ ) {
					DMIElem ss = cV.get ( i );
					if ( ss != null && subc.addAsSuperclass ( ss, false ) )
						uV.addToSet ( ss );
				}
			}
		}

		//
		if ( uV.size () > 0 ) {
			//fix existing instances
			ISet<DMIElem> cV = subc.itsFullInstanceSet;
			for ( int i = 0; i < cV.size (); i++ ) {
				DMIElem m = cV.get ( i );
				if ( m != null ) {
					for ( int j = 0; j < uV.size (); j++ ) {
						DMIElem n = uV.get ( j );
						if ( n != null )
							n.addAsInstance ( m, false );
					}
				}
			}

			//And apply the potential change set to all subclasses
			cV = subc.subclassSet ();
			for ( int i = 0; i < cV.size (); i++ ) {
				DMIElem m = cV.get ( i );
				if ( m != null )
					PullSuperclass ( m, uV, false );
			}
		}
	}

	private void InferredSubclassOf ( DMIElem subc, DMIElem sprc, boolean explicitlyDeclared ) {
		boolean sbioBefore = false;
		boolean sbsbBefore = false;

		boolean isProp = subc.instanceOf ( baseVocab.gProperty );

		if ( isProp ) {
			sbioBefore = subc.subclassOf ( baseVocab.gInstanceOf );
			sbsbBefore = subc.subclassOf ( baseVocab.gSubclassOf );
			//$$$TBD what if it was a subclass of the Domain or Range properties??
		}

		inferredAsClass ( subc, false );
		inferredAsClass ( sprc, false );

		//Given one change in me:
		//Find the full change set (could be smaller or larger than the one change)
		//And apply it to me and my instances
		//And apply that change set to my subclasses:
		ISet<DMIElem> uV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		uV.addToSet ( sprc );
		PullSuperclass ( subc, uV, explicitlyDeclared );
		//uV is the set of classes that have changed in me as a result of this new subclass statement

		if ( isProp ) {
			//m is a complex statement relating core concepts
			//subc is being related to these core concepts
			//since subc is a property, it is also a class whose instances are statements
			//its statements, if any exist, can be taken in a new light
			//if we're now learning something new about subc via the statement m
			if ( !sbioBefore ) {
				if ( subc.subclassOf ( baseVocab.gInstanceOf ) ) {
					//We learned that a property is a subclass of Instance Of
					//so it'subc statements are instance of relations
					ISet<DMIElem> cV = subc.itsFullInstanceSet;
					for ( int i = 0; i < cV.size (); i++ ) {
						DMIElem x = cV.get ( i );
						if ( x != null )
							inferredInstanceOf ( x.itsSubject, x.itsObject, false );
					}
				}
			}

			if ( !sbsbBefore ) {
				if ( subc.subclassOf ( baseVocab.gSubclassOf ) ) {
					//We learned that a property is a subclass of Subclass Of
					//so it subject statements are subclass of relations
					ISet<DMIElem> cV = subc.itsFullInstanceSet;
					for ( int i = 0; i < cV.size (); i++ ) {
						DMIElem x = cV.get ( i );
						if ( x != null )
							InferredSubclassOf ( x.itsSubject, x.itsObject, false );
					}
				}
			}
		}
	}

	private void InferredPropertyWithDR ( DMIElem p, DMIElem x, boolean dr, boolean explicitlyDeclared ) {
		inferredAsProperty ( p, false );
		inferredAsClass ( x, false );

		boolean up = false;

		//must update domain or range

		if ( dr ) {
			up = p.addDomain ( x );
		} else {
			up = p.addRange ( x );
		}

		//must update instances to incorporate the new type information
		if ( up ) {
			ISet<DMIElem> xV;
			xV = p.itsFullInstanceSet;

			for ( int i = 0; i < xV.size (); i++ ) {
				DMIElem stmt = xV.get ( i );
				if ( stmt != null ) {
					DMIElem so;
					if ( dr ) {
						so = stmt.itsSubject;
					} else {
						so = stmt.itsObject;
					}
					inferredInstanceOf ( so, x, false );
				}
			}
		}
	}

	public void Remove ( DMIElem e ) {
		Substitute ( e, null );
	}

	public void Substitute ( DMIElem sb, DMIElem ob ) {
		ISet<DMIElem> qV = sb.itsFullTypeSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null )
				m.itsFullInstanceSet.substituteDupTrim ( sb, ob );
		}

		qV = sb.itsFullInstanceSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				m.itsDeclaredTypeList.substituteDupTrim ( sb, ob );
				m.itsFullTypeSet.substituteDupTrim ( sb, ob );
				if ( m.itsVerb == sb )
					m.itsVerb = ob;
			}
		}

		qV = sb.superclassSet ();
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null )
				m.subclassSet ().substituteDupTrim ( sb, ob );
		}

		qV = sb.superclassSet ();
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null )
				m.superclassSet ().substituteDupTrim ( sb, ob );
		}

		qV = sb.itsUsedAsSubjectStmtSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				m.itsSubject = ob;
				if ( ob != null )
					ob.addAsSubjectOf ( m );
			}
		}

		qV = sb.itsUsedAsObjectStmtSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				m.itsObject = ob;
				if ( ob != null )
					ob.addAsObjectOf ( m );
			}
		}

		if ( ob != null )
			sb.resetCache ();
	}

}
