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

package com.hp.QuickRDA.L4.Build.DOT;

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.DMIBaseVocabulary;
import com.hp.QuickRDA.L1.Core.DMIElem;

public class DiagrammingAdornment {
	public DMIElem					itsConcept;
	public String					itsProvenance;
	public DMIElem					itsDiagParent;

	private DiagrammingPrimary		itsPrimaryAlias;
	private List<DiagrammingAlias>	itsSecondaryAliases;

	public boolean					itsIsRoot;
	public boolean					itsIsRelationTarget;
	public boolean					itsOffView;
	public boolean					itsIsValue;

	public DiagrammingAdornment ( DMIElem m, DMIBaseVocabulary b ) {
		itsConcept = m;
		itsIsValue = m.instanceOf ( b.gValue );
	}

	public DiagrammingPrimary getPrimaryAlias () {
		if ( itsPrimaryAlias == null )
			itsPrimaryAlias = new DiagrammingPrimary ( itsConcept );
		return itsPrimaryAlias;
	}

	public DMIElem getEnvironment () {
		return getPrimaryAlias ().itsEnvironment;
	}

	private void addSecondary ( DiagrammingSecondaryAlias dsa )
	{
		if ( itsSecondaryAliases == null )
			itsSecondaryAliases = new XSetList<DiagrammingAlias> ();
		itsSecondaryAliases.add ( dsa );
	}

	public DiagrammingSecondaryAlias getSecondaryAlias ( DMIElem env, boolean isAttached, DiagrammingAdornment toWhat ) {
		DiagrammingSecondaryAlias ans = null;
		if ( itsSecondaryAliases != null && !itsIsValue ) {
			//for ( int i = 0; i < itsSecondaryAliases.length; i++ ) {
			//	ans = itsSecondaryAliases [ i ];
			for ( DiagrammingAlias x : itsSecondaryAliases ) {
				ans = (DiagrammingSecondaryAlias) x;
				if ( ans.itsEnvironment == env && (ans.itsToWhat == toWhat && ans.itsIsAttached == isAttached) )
					return ans;
			}
		}
		ans = new DiagrammingSecondaryAlias ( itsConcept, env, isAttached, toWhat );
		//itsSecondaryAliases = DiagrammingSecondaryAlias.addToSet (ans, itsSecondaryAliases);
		addSecondary ( ans );
		return ans;
	}

	public List<DiagrammingAlias> getSecondaries () {
		return (List<DiagrammingAlias>) itsSecondaryAliases;
	}

	public boolean canReferencePrimary () {
		return !itsConcept.isStatement () && itsPrimaryAlias.getGroupCount () == 0;
	}

	public DiagrammingAlias makeValueRefTarget ( DMIElem env ) {
		DiagrammingSecondaryAlias ans = null;
		ans = new DiagrammingSecondaryAlias ( itsConcept, env, false, null );
		addSecondary ( ans );
		return ans;
	}

	public DiagrammingAlias getReferenceTargetAlias ( DMIElem env, DiagrammingAdornment a, boolean offView ) {
		itsIsRelationTarget = true;
		itsOffView = offView;

		if ( this.itsIsValue && !offView ) {
			// for values, every time the question is asked, we return a new alias
			getPrimaryAlias ().itsSuppress = true;
			DiagrammingAlias da = getSecondaryAlias ( env, false, null );
			if ( env != null )
				a.getPrimaryAlias ().addAsGroupment ( da );
			return da;
		}

		// find best fit secondary if present
		if ( itsSecondaryAliases != null ) {
			//for ( int i = 0; i < itsSecondaryAliases.length; i++ ) {
			//	if ( itsSecondaryAliases [ i ].itsEnvironment == env )
			//		return itsSecondaryAliases [ i ];
			//}
			for ( DiagrammingAlias x : itsSecondaryAliases )
				if ( x.itsEnvironment == env )
					return x;
		}

		// otherwise use primary if possible
		if ( offView || itsPrimaryAlias != null && canReferencePrimary () /* && itsPrimaryAlias.itsEnvironment == env */)
			return getPrimaryAlias ();

		// else create mininode
		return forceReferenceTargetAlias ();
	}

	public DiagrammingAlias forceReferenceTargetAlias () {
		assert itsConcept.isGrouping () || itsConcept.isStatement ();
		if ( itsPrimaryAlias.itsReferenceTarget == null ) {
			itsPrimaryAlias.itsReferenceTarget = new DiagrammingAlias ( itsConcept, itsPrimaryAlias.itsEnvironment );
			if ( itsPrimaryAlias.getGroupCount () > 0 )
				itsPrimaryAlias.itsMiniNodeInvisible = true;

			/*
			if ( itsConcept.IsStatement() ) {
				itsPrimaryAlias.itsMiniNodeInvisible = false;
			} else if ( itsConcept.IsGrouping() ) {
				itsPrimaryAlias.itsReferenceTarget = new DOTDiagrammingAlias(itsConcept);
				itsPrimaryAlias.itsMiniNodeInvisible = true;
			} else {
				itsPrimaryAlias.itsReferenceTarget = itsPrimaryAlias;
			}
			*/
		}

		return itsPrimaryAlias.itsReferenceTarget;
	}

	public void issueAliases () {
		if ( itsConcept.isAttachedToOnce () ) {
			DMIElem at = itsConcept.attachedToSet ().get ( 0 );
			while ( at.isAttachedToOnce () ) {
				at = at.attachedToSet ().get ( 0 );
			}
			itsPrimaryAlias.itsGraphID = (at.itsIndex + ":" + itsConcept.itsIndex);
			itsPrimaryAlias.itsPortID = java.lang.Integer.toString ( itsConcept.itsIndex );
		} else if ( itsPrimaryAlias.getGroupCount () > 0 ) {
			itsPrimaryAlias.itsGraphID = Integer.toString ( itsConcept.itsIndex ); // "cluster_" + m.itsIndex;
		} else {
			itsPrimaryAlias.itsGraphID = Integer.toString ( itsConcept.itsIndex );
		}

		List<DiagrammingAlias> aL = itsSecondaryAliases;
		if ( aL != null ) {
			int cnt = -1;
			//for ( int j = 0; j < aL.length; j++ ) {
			//	DiagrammingSecondaryAlias sa = aL [ j ];
			for ( DiagrammingAlias x : aL ) {
				cnt++;
				DiagrammingSecondaryAlias sa = (DiagrammingSecondaryAlias) x;
				if ( sa.itsIsAttached ) {
					DiagrammingAdornment dda = sa.itsToWhat;
					DMIElem at = dda.itsConcept;
					while ( at.isAttachedToOnce () ) {
						at = at.attachedToSet ().get ( 0 );
					}
					sa.itsGraphID = (at.itsIndex + ":" + itsConcept.itsIndex);
					sa.itsPortID = java.lang.Integer.toString ( itsConcept.itsIndex );
				} else {
					sa.itsGraphID = "S_" + itsConcept.itsIndex + "_" + cnt;
				}
			}
		}

		if ( itsPrimaryAlias.itsReferenceTarget != null && itsPrimaryAlias.itsReferenceTarget != itsPrimaryAlias ) {
			itsPrimaryAlias.itsReferenceTarget.itsGraphID = "_" + itsConcept.itsIndex;
		}
	}
}
