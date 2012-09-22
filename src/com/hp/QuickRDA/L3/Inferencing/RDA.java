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

package com.hp.QuickRDA.L3.Inferencing;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;

public class RDA {

	public static void suppressAssignedTos ( DMIGraph graph, DMIBaseVocabulary baseVocab, DMIView vw ) {
		ISet<DMIElem> qV = baseVocab.gAssignedTo.itsFullInstanceSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && suppressAssignedTo ( m, baseVocab, vw ) ) {
				//hiddenSG.MoveHere m
				vw.remove ( m );
			}
		}
		vw.trim ();
	}

	private static boolean suppressAssignedTo ( DMIElem m, DMIBaseVocabulary baseVocab, DMIView vw ) {
		boolean ans = false;

		//Is this an Assigned To edge?
		//   a - assigned to - b
		if ( m.itsVerb.subclassOf ( baseVocab.gAssignedTo ) ) {
			//if ( this assigned to is a target then don't suppress
			if ( vw.inView ( m ) && !m.isVisiblyReferredTo ( vw, true ) ) {
				//Check if assigned to more than one role; if so then don't suppress.
				DMIElem lst = null;
				DMIElem nxt = m;
				for ( ;; ) {
					nxt = nextAstoParent ( nxt, baseVocab, vw );
					if ( nxt != null )
						lst = nxt;
					//now, pop up one level while the same holds; then back down one level and do the suppression test.
					if ( nxt == null )
						break;
				}

				if ( lst != null ) {
					//if ( this link exists then subject + its parent have same assigned to;
					//so, let's just make sure its will appear in the graph, which is tricky because the graph keeps getting modified
					//for example, right here we're assignment suppressing nodes
					//ans = lst.itsSubgraph.IsVisible
					ans = vw.inView ( lst );
				}
			}
		}

		return ans;
	}

	private static DMIElem nextAstoParent ( DMIElem m, DMIBaseVocabulary baseVocab, DMIView vw ) {
		DMIElem ans = null;

		ISet<DMIElem> atV = m.itsSubject.findStatementsFromUsedAsSubjectByVerbAndObject ( baseVocab.gAssignedTo, null, vw );
		if ( atV.size () == 1 ) {
			//Check the subject's Assigned To object is the same as the Parent's Assigned To target
			//Find link for subject,RespParent,object
			ISet<DMIElem> pV = m.itsSubject.findStatementsFromUsedAsSubjectByVerbAndObject ( baseVocab.gRespParent, null, vw );
			//Is the assigned to subject also the subject of a parent relation?
			//   a - RespParent - ?
			if ( pV.size () == 1 ) {
				//Is this testing to see if it is its own parent?  if ( so, why?
				if ( pV.get ( 0 ).itsObject != m.itsSubject ) {
					//Set astoV = FindRelation(gAssignedTo, pL(1).itsObject, m.itsObject)
					ISet<DMIElem> astoV = m.itsObject.findStatementsFromUsedAsObjectBySubjectAndVerb ( pV.get ( 0 ).itsObject, baseVocab.gAssignedTo, vw );
					if ( astoV.size () >= 1 ) {
						ans = astoV.get ( 0 );
					}
				}
			}
		}
		return ans;
	}

}