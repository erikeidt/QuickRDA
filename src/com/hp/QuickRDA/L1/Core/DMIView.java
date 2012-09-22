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

public class DMIView {

	public enum ViewInitializerEnum {
		AllDomain,
		AllVisible
	}

	private static int		itsNextIndex;	// just an aid for debugging
	@SuppressWarnings("unused")
	private int				itsIndex;		// just an aid for debugging

	private ISet<DMIElem>	itsConceptSet;

	public DMIView () {
		itsConceptSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
	}

	public DMIView ( ISet<DMIElem> s ) {
		itsConceptSet = s;
	}

	public int length () {
		return itsConceptSet.size ();
	}

	public void initializeFromGraph ( DMIGraph graph, ViewInitializerEnum include ) {
		itsIndex = itsNextIndex++;
		// for ( int si = 0; si < graph.subgraphCount (); si++ ) {
		//	DMISubgraph sg = graph.Subgraph (si);
		for ( DMISubgraph sg : graph.getSubgraphList () ) {
			if ( (include == ViewInitializerEnum.AllDomain && sg.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel )) || (include == ViewInitializerEnum.AllVisible && sg.isVisible) ) {
				addSubgraph ( sg );
			}
		}
	}

	public void addSubgraph ( DMISubgraph sg ) {
		addElementsFromListToView ( sg.toSet () );
	}

	public void addElementsFromListToView ( IList<DMIElem> v ) {
		itsConceptSet.mergeListIntoSet ( v );
	}

	public void trim () {
		itsConceptSet.trim ();
	}

	public ISet<DMIElem> toSet () {
		return itsConceptSet;
	}

	public void clear () {
		itsConceptSet.clear ();
	}

	public boolean inView ( DMIElem m ) {
		return itsConceptSet.indexOf ( m ) >= 0;
	}

	public boolean addToView ( DMIElem m ) {
		return itsConceptSet.addToSet ( m );
	}

	public boolean remove ( DMIElem m ) {
		boolean ans = false;

		int i = itsConceptSet.indexOf ( m );
		if ( i >= 0 ) {
			itsConceptSet.clear ( i );
			ans = true;
		}
		return ans;
	}

	@Override
	public Object clone () {
		return copy ();
	}

	@SuppressWarnings("unchecked")
	public DMIView copy () {
		DMIView ans = new DMIView ();
		ans.itsConceptSet = (ISet<DMIElem>) itsConceptSet.clone ();
		return ans;
	}

	// $$$$ gL is ByRef, changing signature...
	// not sure why we're not using a vector here for the result
	public DMIElem [] viewToMap ( DMIGraph g ) {
		int minIndex = g.getMinIndex ();
		DMIElem [] ans = new DMIElem [ g.getMaxIndex () - minIndex ];
		for ( int i = 0; i < itsConceptSet.size (); i++ ) {
			DMIElem m = itsConceptSet.get ( i );
			if ( m != null )
				ans [ m.itsIndex - minIndex ] = m;
		}
		return ans;
	}
}
