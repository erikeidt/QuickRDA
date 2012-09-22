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

import java.util.List;
import java.util.ArrayList;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L1.Core.DMISubgraph.SubgraphLevelEnum;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L5.ExcelTool.Debug;


public class DMIImport {
	private ISet<DMIElem>		itsConceptSet	= new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
	private UnitOfInterchange	itsUOI;
	private DMIGraph			itsGraph;
	private CacheBuilder		itsCacheBuilder;
	// private DMISubgraph []		itsSGsToTrim; JCF upgrade
	private List<DMISubgraph>	itsSGsToTrim;

	public void subgraphFrom ( UnitOfInterchange uoi, ConceptManager cm ) {
		itsUOI = uoi;
		itsGraph = cm.itsGraph;
		itsCacheBuilder = itsGraph.itsCacheBuilder;

		for ( int i = 0; i < itsUOI.itsSerializations.length; i++ ) {
			UnitOfInterchange.Serialization ser = itsUOI.itsSerializations [ i ];
			DMIElem m = cm.enterSerialization ( ser.itsValue, null );
			itsConceptSet.addToList ( m );
		}

		int simpleConceptOffset = itsConceptSet.size ();

		for ( int i = 0; i < itsUOI.itsSimpleConceptCount; i++ ) {
			DMIElem m = cm.newConcept ();
			itsConceptSet.addToList ( m );
		}

		int stmtOffset = itsConceptSet.size ();

		for ( int i = 0; i < itsUOI.itsAssertedStatements.length; i++ ) {
			DMIElem m = cm.newConcept ();
			itsConceptSet.addToList ( m );
		}

		Debug.exportDump ( cm );
		Debug.exportDump ( "DMIImport", itsConceptSet, cm );

		for ( int i = 0; i < itsUOI.itsAssertedStatements.length; i++ ) {
			UnitOfInterchange.Statement stmt = itsUOI.itsAssertedStatements [ i ];
			DMIElem m = itsConceptSet.get ( i + stmtOffset );
			DMIElem sb = itsConceptSet.get ( stmt.itsSubject );
			DMIElem vb = itsConceptSet.get ( stmt.itsVerb );
			DMIElem ob = itsConceptSet.get ( stmt.itsObject );
			m.setStatement ( sb, vb, ob );
			cm.itsGraph.itsCacheBuilder.DynamicCachingAndInferencing ( m );
		}

		Debug.exportDump ( cm );
		Debug.exportDump ( "DMIImport", itsConceptSet, cm );

		// itsSGsToTrim = new DMISubgraph[0]; JCF upgrade
		itsSGsToTrim = new ArrayList<DMISubgraph> ();

		for ( int i = 0; i < itsUOI.itsInitialReferences.length; i++ ) {
			UnitOfInterchange.InitialReference ir = itsUOI.itsInitialReferences [ i ];
			DMIElem sb = itsConceptSet.get ( ir.itsClass );
			DMIElem ob = cm.findConcept ( itsConceptSet.get ( ir.itsSerialization ).value (), null );
			substitute ( sb, ob );
		}

		Debug.exportDump ( cm );
		Debug.exportDump ( "DMIImport", itsConceptSet, cm );

		for ( int i = simpleConceptOffset; i < itsConceptSet.size (); i++ ) {
			matchNameUM ( itsConceptSet.get ( i ), cm );
		}

		Debug.exportDump ( cm );
		Debug.exportDump ( "DMIImport", itsConceptSet, cm );

		for ( int i = simpleConceptOffset; i < itsConceptSet.size (); i++ ) {
			matchNameDL ( itsConceptSet.get ( i ), cm );
		}

		for ( int i = simpleConceptOffset; i < itsConceptSet.size (); i++ ) {
			matchNameDM ( itsConceptSet.get ( i ), cm );
		}

		Debug.exportDump ( cm );
		Debug.exportDump ( "DMIImport", itsConceptSet, cm );

		/*
		for ( int i = 0; i < itsUOI.itsAssertedStatements.length; i++ ) {
			UnitOfInterchange.Statement stmt = itsUOI.itsAssertedStatements[i];
			DMIElem sb = itsVector.ElementAt(stmt.itsSubject);
			DMIElem vb = itsVector.ElementAt(stmt.itsVerb);
			DMIElem ob = itsVector.ElementAt(stmt.itsObject);
			
			 * would have gladly used name matching, but we're not building names for core!!
			if ( vb.SubclassOf(cm.itsBaseVocab.gHasName) ) {
				// statement is: sb Has Name ob;
				// find others that have the same name as ob
				ISet<DMIElem> namedObjects = ob.FindSubjectsFromUsedAsObjectByVerb(cm.itsBaseVocab.gHasName);
				for ( int j = 0; j < namedObjects.size (); j++ ) {
					// these are the same as sb...
					DMIElem nb = namedObjects.ElementAt(j);
					if ( sb != nb )
						Substitute(sb, nb);
				}
			}
		}
		*/

		/* JCF upgrade
		for ( int i = 0; i < itsSGsToTrim.length; i++ ) {
			itsSGsToTrim[i].trim();
		}
		*/

		for ( DMISubgraph sg : itsSGsToTrim ) {
			sg.trim ();
		}

		itsSGsToTrim = null;

		for ( int i = 0; i < itsUOI.itsAssertedStatements.length; i++ ) {
			DMIElem m = itsConceptSet.get ( i + stmtOffset );
			cm.itsGraph.itsCacheBuilder.DynamicCachingAndInferencing ( m );
		}
	}

	private void matchNameUM ( DMIElem m, ConceptManager cm ) {
		String nameStr = NameUtilities.firstName ( m, itsGraph, false, false );
		if ( nameStr != null && !"".equals ( nameStr ) ) {
			// DMIElem c = cm.FindConcept(nameStr, null);
			DMIElem c = cm.findNNConcept ( nameStr, itsConceptSet, SubgraphLevelEnum.kUnderlyingMetamodel );
			if ( c != null ) {
				if ( m != c )
					substitute ( m, c );
			}
		}
	}

	private void matchNameDL ( DMIElem m, ConceptManager cm ) {
		String nameStr = NameUtilities.firstName ( m, itsGraph, false, false );
		if ( nameStr != null && !"".equals ( nameStr ) ) {
			// DMIElem c = cm.FindConcept(nameStr, null);
			DMIElem c = cm.findNNConcept ( nameStr, itsConceptSet, SubgraphLevelEnum.kDomainLanguageMetamodel );
			if ( c != null ) {
				if ( m != c )
					substitute ( m, c );
			}
		}
	}

	private void matchNameDM ( DMIElem m, ConceptManager cm ) {
		/*
		String nameStr = NameUtilities.FirstName(m, itsGraph, false, false);
		if ( nameStr != null && !"".equals(nameStr) ) {
			// DMIElem c = cm.FindConcept(nameStr, null);
			DMIElem c = cm.FindNNConcept(nameStr, itsVector, SubgraphLevelEnum.kDomainModel.kDomainModel);
			if ( c != null && c.itsSubgraph.AtLevel(SubgraphLevelEnum.kDomainModel) ) {
				if ( m != c )
					Substitute(m, c);
			}
		}
		*/
	}

	private void substitute ( DMIElem sb, DMIElem ob ) {
		// lang.ErrMsg("Substituting " + sb + " for " + ob);
		itsCacheBuilder.Substitute ( sb, ob );
		itsConceptSet.substitute ( sb, ob );
		DMISubgraph sg = sb.itsSubgraph;
		sg.remove ( sb );
		/* JCF upgrade
		if ( lang.indexOf(sg, itsSGsToTrim) < 0 ) {
			itsSGsToTrim = DMISubgraph.addToList (sb.itsSubgraph, itsSGsToTrim);
		}
		*/
		if ( itsSGsToTrim.indexOf ( sg ) < 0 ) {
			itsSGsToTrim.add ( sg );
		}
	}

}
