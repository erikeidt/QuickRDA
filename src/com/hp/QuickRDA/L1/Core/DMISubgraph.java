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

public class DMISubgraph {

	public enum SubgraphLevelEnum {
		kUnderlyingMetamodel,
		kCrossDomainLanguage,
		kDomainLanguageMetamodel,
		kCrossDomainModel,
		kDomainModel;

		private static int toInt ( SubgraphLevelEnum lv ) {
			switch ( lv ) {
			case kUnderlyingMetamodel :
				return 0;
			case kCrossDomainLanguage :
				return 1;
			case kDomainLanguageMetamodel :
				return 2;
			case kCrossDomainModel :
				return 3;
			case kDomainModel :
				return 4;
			}
			return -1;
		}
	}

	//
	// ===Manages Subgraph===
	//
	// ---Constructor---
	// Initialize(g,s)
	//
	// NOTES: Defers to Graph for index assignment
	//
	// Method AddConcept(e)
	//  Adds concept to graph, whether serialization, concept, or statement/relation
	//  Won't move/enter already added known concepts
	//  Defers to Graph for Serialization entries via g.AddSerialization(e)
	//
	// Method AllConcepts
	//  Returns list of all concepts in the subgraph
	//
	// ?Optional: Method AllRelations
	//
	// Method MoveIntoSubgrah(e)
	//  Moves concept into subgraph, from the one its in already
	//

	public String				itsName;				//Just for debugging
	public boolean				itsSupportsProvenance;	//false only for the provenance subgraph itself; so we won't try to apply prov to itself
														//(it would be fine to have prov about prov but this prevents prov asserting itself)
	public boolean				isMeta;
	public boolean				isVisible;				// elements in this graph are intended to be visible externally
	public SubgraphLevelEnum	itsLevel;				//
	public DMIGraph				itsGraph;
	private ISet<DMIElem>		itsConceptSet;

	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++;
		mrc.atrCount += 5;
		mrc.strCount += 1;
		mrc.strSize += itsName.length ();
		itsConceptSet.memUsage ( mrc );
	}

	// boolean supportsProvenance = true, Optional Boolean IsVisible = false, Optional Boolean isMeta = False
	public DMISubgraph ( DMIGraph g, String subgraphName, SubgraphLevelEnum slv,
			boolean supportsProvenance, boolean isVisible, boolean isMeta ) {

		itsName = subgraphName;
		this.itsGraph = g;
		g.addSubgraph ( this );
		itsSupportsProvenance = supportsProvenance;
		itsLevel = slv;
		this.isMeta = isMeta;
		this.isVisible = isVisible;

		itsConceptSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
	}

	public ISet<DMIElem> toSet () {
		return itsConceptSet;
	}

	public int levelAsNumber () {
		return SubgraphLevelEnum.toInt ( itsLevel );
	}

	public boolean isAccessible ( SubgraphLevelEnum lv ) {
		return SubgraphLevelEnum.toInt ( itsLevel ) >= SubgraphLevelEnum.toInt ( lv ) - 2;
	}

	public boolean atLevel ( SubgraphLevelEnum lv ) {
		return SubgraphLevelEnum.toInt ( itsLevel ) >= SubgraphLevelEnum.toInt ( lv );
	}

	// TODO Fix this ...
	public boolean atLevel2 ( SubgraphLevelEnum lv ) {
		return SubgraphLevelEnum.toInt ( lv ) >= SubgraphLevelEnum.toInt ( itsLevel );
	}

	public void addConcept ( DMIElem e ) {
		itsGraph.setIndex ( e ); // this will error if the item is already in some other subgraph
		assert e.itsSubgraph == null : "DMISubgraph.AddConcept: subgraph not null";
		e.itsSubgraph = this;
		itsConceptSet.addToList ( e );
		// #if ( gOptDynInline ) {
		if ( e.isStatement () ) {
			itsGraph.itsCacheBuilder.DynamicCachingAndInferencing ( e );
		}
		// #}
	}

	// Caller must trim after; but note of trim needed is recorded in the vector
	public void remove ( DMIElem e ) {
		int i = itsConceptSet.indexOf ( e );
		if ( i >= 0 ) {
			itsGraph.itsCacheBuilder.Remove ( e );
			itsConceptSet.clear ( i );
		}
	}

	public void trim () {
		itsConceptSet.trim ();
	}

}
