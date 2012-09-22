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

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;


public class WVSearchTree {

	public enum SearchStatusEnum {
		Source, // In initial search set, not an official element of the path
		Target, // Our targets, also not an official element of the path
		Searching, // In flight
		ReachedTarget, // Success!!
		Exhausted; // Retired
	}

	public DMIElem				itsElem;										// The graph node this tree/list element refers to

	public int					itsRefCount;									// The number of uses of this tree/list element by other tree/list elements
																				//   via next or chain, see below

	public WVSearchTree			itsNext;										// The next tree/list element in this path chain (hold a ref count)

	private List<WVSearchTree>	itsChain	= new XSetList<WVSearchTree> ();	// A list of other tree/list elements representing paths that reached here cyclically
	//   (holds a ref count)

	public SearchStatusEnum		itsStatus;
	public int					itsGeneration;

	// public BoundFork			itsBoundVarsSet;
	// public boolean				itsQueryMatchChecked;

	public WVSearchTree ( DMIElem m, int gen, SearchStatusEnum k /*, BoundFork boundVarsSet */) {
		itsElem = m;
		itsStatus = k;
		itsGeneration = gen;
		// itsBoundVarsSet = boundVarsSet;
		// itsQueryMatchChecked = true;
	}

	public WVSearchTree ( DMIElem m, int gen /*, BoundFork boundVarsSet, boolean checked*/) {
		itsElem = m;
		itsStatus = SearchStatusEnum.Searching;
		itsGeneration = gen;
		// itsBoundVarsSet = boundVarsSet;
		// itsQueryMatchChecked = checked;
	}

	public WVSearchTree ( DMIElem m, WVSearchTree lst, int gen /* BoundFork boundVarsSet, boolean checked*/) {
		itsElem = m;
		itsNext = lst;
		lst.itsRefCount++;
		itsStatus = SearchStatusEnum.Searching;
		itsGeneration = gen;
		// itsBoundVarsSet = boundVarsSet;
		// itsQueryMatchChecked = checked;
	}

	public void addPathToChain ( WVSearchTree path ) {
		// merge path into chain
		// itsChain = AddToList (path, itsChain);
		itsChain.add ( path );
		// and merge bound fork of other to this one

		//		BoundFork boundInFork = path.itsBoundVarsSet;
		//		if ( boundInFork != null ) {
		//			BoundFork boundOutFork = new BoundFork ();
		//			boolean add = boundInFork.acceptCandidate (itsElem, boundOutFork);
		//			if ( add ) {
		//				if ( boundOutFork.isBlank () )
		//					boundOutFork = boundInFork;
		//				itsBoundVarsSet.mergeIntoFork (boundOutFork);
		//			}
		//		}
	}

	public boolean accumulateSuccessPath ( ISet<DMIElem> v, boolean matched ) {
		itsStatus = SearchStatusEnum.ReachedTarget;

		boolean ans = false;
		if ( matched )
			ans = v.addToSet ( itsElem );

		if ( itsNext != null ) {
			ans |= itsNext.accumulateSuccessPath ( v, matched );
		}

		/*
		for ( int i = 0; i < itsChain.length; i++ ) {
			ans |= itsChain [ i ].AccumulateSuccessPath (v, matched);
		}
		*/
		for ( WVSearchTree t : itsChain )
			ans |= t.accumulateSuccessPath ( v, matched );

		return ans;
	}

	public void accumulateZCntFailPath ( ISet<DMIElem> v ) {
		//
		// if ( its free (no spurs),
		//   and it was not itself reaching success,
		//   and it had no chains then we can accumulate to the non reach set
		//   otherwise we leave it undetermined
		//
		if ( itsRefCount == 0 && itsStatus == SearchStatusEnum.Searching ) {
			if ( itsChain.size () == 0 ) {
				itsStatus = SearchStatusEnum.Exhausted;

				v.addToSet ( itsElem );

				if ( itsNext != null ) {
					itsNext.itsRefCount--;
					if ( itsNext.itsRefCount == 0 ) {
						itsNext.accumulateZCntFailPath ( v );
					}
				}

				// #if ( false ) {
				/*
							//note this will never happen now that we leave loop chains open 'till the end
							for ( int i= 0; i < itsChain.length; i++ ) {
								DMITree t = itsChain(i);
								t.itsRefCount--;
								if ( t.itsRefCount == 0 ) {
									t.AccumulateZCntFailPath v;
								}
							}
				*/
				// #}
			}
		}
	}

	public boolean reachesSuccess () {
		if ( itsStatus == SearchStatusEnum.ReachedTarget
				||
				itsStatus == SearchStatusEnum.Target ) { // Target test added 06/09/2011; for SourceSet==TargetSet
			return true;
		} else if ( itsStatus == SearchStatusEnum.Exhausted ) {
			return false;
		} else {
			boolean ans = false;

			if ( itsNext != null ) {
				ans = itsNext.reachesSuccess ();
			}

			if ( !ans ) {
				/*
				for ( int i = 0; i < itsChain.length; i++ ) {
					ans = itsChain [ i ].ReachesSuccess ();
					if ( ans )
						break;
				}
				*/
				for ( WVSearchTree t : itsChain ) {
					ans = t.reachesSuccess ();
					if ( ans )
						break;

				}
			}
			return ans;
		}
	}

	public ISet<DMIElem> successPaths ( ISet<DMIElem> v ) {
		switch ( itsStatus ) {
		case ReachedTarget :
			v = DMIElem.allocateSetIfNull ( v );
			v.addToList ( itsElem );
			break;
		case Searching :
			v = itsNext.successPaths ( v );
			/*
			for ( int i = 0; i < itsChain.length; i++ )
				v = itsChain [ i ].SuccessPaths (v);
			*/
			for ( WVSearchTree t : itsChain )
				v = t.successPaths ( v );

		default : // added 06/09/2011; for SourceSet==TargetSet: do nothing in Exhausted AND Target cases
			break;
		}
		/*
		if ( itsStatus == SearchStatusEnum.ReachedTarget ) {
			v = ISet<DMIElem>.AllocateIfNull(v);
			v.addToList (itsElem);
		} else if ( itsStatus == SearchStatusEnum.Exhausted ) {
		} else {
			v = itsNext.SuccessPaths(v);
			for ( int i = 0; i < itsChain.length; i++ ) {
				v = itsChain[i].SuccessPaths(v);
			}
		}
		*/
		return v;
	}

	public String listToStr () {
		String ans = "";

		if ( itsElem == null ) {
			ans += " <nil>";
		} else {
			WVSearchTree head = this;
			for ( ;; ) {
				DMIElem m = head.itsElem;
				String name = NameUtilities.hasMCText ( NameUtilities.getFirstName ( m ) );
				if ( name != null ) {
					ans += " " + m.itsIndex + " {" + name + "}";
				} else {
					ans += " " + m.itsIndex + " [" + NameUtilities.getMCText ( m.itsVerb ) + "]";
				}
				ans += head.listChain ();
				head = head.itsNext;
				if ( head == null )
					break;
			}
		}

		return ans;
	}

	public String listChain () {
		String ans = "";
		if ( itsStatus != SearchStatusEnum.Source && itsStatus != SearchStatusEnum.Target ) {
			// for ( int i = 0; i < itsChain.length; i++ )
			//	ans += " <<" + i + ">> [[ " + itsChain [ i ].ListToStr () + " ]]";
			int i = 0;
			for ( WVSearchTree t : itsChain ) {
				ans += " <<" + i + ">> [[ " + t.listToStr () + " ]]";
				i++;
			}
		}
		return ans;
	}

	public List<WVSearchTree> getChain () {
		return itsChain;
	}

}
