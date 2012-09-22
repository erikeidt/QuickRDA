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

public class TypeRelations {

	// A property applies over a given domain and given range, if
	//	The given domain type is assignable to the property's domain type set and
	//	The given range type is assignable to the property's range type set.
	//	A given type is assignable to some type set if it is assignable to any element of the type set.
	//	A given type is assignable to some type if it is the same as or narrower than some type.
	//	A given property type is narrower than some property type if
	//		the given property's domain is narrower than some property's domain and
	//		the given property's range is narrower than some property's range.
	public static boolean propertyAppliesOverDomainAndRange ( DMIElem theProperty, DMIElem aDomain, DMIElem aRange ) {
		return TypeRelations.sourceSameOrNarrowerThanTarget ( aDomain, theProperty.domainSet () ) &&
				TypeRelations.sourceSameOrNarrowerThanTarget ( aRange, theProperty.rangeSet () );
	}

	// A property is applicable to a given abstract type if
	//	The property's domain is ... the abstract property's domain and
	//	The property's range is ... the abstract property's range
	public static boolean propertyIsApplicableToTargetType ( DMIElem aProperty, DMIElem aType ) {
		return sourceSameOrNarrowerThanTarget ( aType, aProperty );
	}

	private static boolean sourceSameOrNarrowerThanTarget ( DMIElem sourceType, ISet<DMIElem> targetTypeSet ) {
		boolean ans = false;

		for ( int i = 0; i < targetTypeSet.size (); i++ ) {
			DMIElem m = targetTypeSet.get ( i );
			if ( m != null ) {
				ans = sourceSameOrNarrowerThanTarget ( sourceType, m );
				if ( ans )
					break;
			}
		}
		return ans;
	}

	private static boolean sourceSameOrNarrowerThanTarget ( DMIElem sourceType, DMIElem targetType ) {
		boolean ans = false;

		if ( targetType.isAbstractProperty () ) {
			if ( sourceType.instanceOf ( sourceType.itsSubgraph.itsGraph.itsBaseVocab.gProperty ) ) {
				ans = sourceSameOrNarrowerThanTarget ( sourceType.domainSet (), targetType.domainSet () ) &&
						sourceSameOrNarrowerThanTarget ( sourceType.rangeSet (), targetType.rangeSet () );
			}
		} else if ( sourceType.isAbstractProperty () ) {
			if ( targetType.instanceOf ( targetType.itsSubgraph.itsGraph.itsBaseVocab.gProperty ) ) {
				ans = sourceSameOrNarrowerThanTarget ( sourceType.domainSet (), targetType.domainSet () ) &&
						sourceSameOrNarrowerThanTarget ( sourceType.rangeSet (), targetType.rangeSet () );
			}
		} else {
			ans = sourceType.subclassOf ( targetType );
		}

		return ans;
	}

	private static boolean sourceSameOrNarrowerThanTarget ( ISet<DMIElem> sourceTypeSet, ISet<DMIElem> targetTypeSet ) {
		boolean ans = false;

		for ( int i = 0; i < sourceTypeSet.size (); i++ ) {
			DMIElem m = sourceTypeSet.get ( i );
			if ( m != null ) {
				ans = sourceSameOrNarrowerThanTarget ( m, targetTypeSet );
				if ( ans )
					break;
			}
		}

		return ans;
	}

}
