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

import java.util.List;
import com.hp.QuickRDA.L0.lang.*;

public class DMIGraph {

	private static final int	kStartIndex	= 10000;

	//
	// ===Manages Whole Graph===
	// ---Constructor---
	// Initialize
	//
	// ---Provides Base Vocabulary---
	// Member b
	//
	// ---Manages Index Assignments for DMIElems---
	// Method Get}Index
	//
	// ---Manages Serializations for Whole Graph---
	// Method AddConcept(e)
	// Expects only Serialization Concepts
	// Gives e an index
	// Hashes Serializations for lookup
	// NOTE: does not expect duplicates, will throw error on duplicates
	// Method LookupSerializations(typ,ser)
	// Uses hash
	//

	public DMIBaseVocabulary	itsBaseVocab;
	public CacheBuilder			itsCacheBuilder;
	private int					itsNextIndex;
	// private DMISubgraph []		itsSubgraphs;  JCF upgrade
	private List<DMISubgraph>	itsSubgraphs;

	public DMIGraph () {
		itsBaseVocab = new DMIBaseVocabulary ();
		// itsSubgraphs = new DMISubgraph[0];  JCF upgrade
		itsSubgraphs = new XSetList<DMISubgraph> ();
		itsNextIndex = kStartIndex;
	}

	public void initialize ( CacheBuilder cacheBuilder ) {
		this.itsCacheBuilder = cacheBuilder;
	}

	public void setIndex ( DMIElem e ) {
		assert e.itsIndex == 0 : "DMIGraph.SetIndex: input already has an Index";
		e.itsIndex = itsNextIndex;
		//if ( itsNextIndex == 11002 ) {
		//	int a = 1;
		//}
		itsNextIndex = itsNextIndex + 1;
	}

	public int getMinIndex () {
		return kStartIndex;
	}

	public int getMaxIndex () {
		return itsNextIndex;
	}

	public void addSubgraph ( DMISubgraph s ) {
		// itsSubgraphs = DMISubgraph.addToList (s, itsSubgraphs); JCF upgrade
		itsSubgraphs.add ( s );
	}

	public List<DMISubgraph> getSubgraphList () {
		return itsSubgraphs;
	}

	public void trim () {
		/* JCF upgrade
		for ( int i = 0; i < itsSubgraphs.length; i++ ) {
			itsSubgraphs[i].trim();
		}
		*/
		for ( DMISubgraph sg : itsSubgraphs ) {
			sg.trim ();
		}
	}

}
