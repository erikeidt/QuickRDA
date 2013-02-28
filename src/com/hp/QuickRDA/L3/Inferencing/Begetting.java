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
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L5.ExcelTool.Builder; // TODO: bad layering: we only need subset of builder (i.e. concept manager at L2...)
import com.hp.QuickRDA.L5.ExcelTool.Start;

public class Begetting {

	public static void generative_Inferencing ( DMIView vw, Builder bldr ) {
		template_Inferencing ( vw, bldr );
		begets_Inferencing ( bldr );
		qualifies_Inferencing ( vw, bldr );
	}

	private static void template_Inferencing ( DMIView vw, Builder bldr ) {
		// Find template Instantiations
		ISet<DMIElem> qV = bldr.itsBaseVocab.gEvokeAs.itsFullInstanceSet;

		// TODO need to order evocations due to the complex interactions of substitution of evocations!
		// so evocations that have other evocations need to go first, especially if those evocations
		// involve substitution of evocation!

		for ( int i = 0; i < qV.size (); i++ ) {
			Start.checkForShutDown();
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				// this delays evocation of templates that are within templates;
				// they will be evoked after their enclosing template is evoked;
				// 	this by being added to the collection of instances as we're iterating over it...
				DMIElem x = m.find1StatementFromUsedAsSubjectByVerbAndObject ( bldr.itsConceptMgr.itsBaseVocab.gInTemplate, null, null );
				if ( x == null )
					copyTemplate ( m, vw, bldr.itsConceptMgr, bldr.itsOptions.gOptionAutoHide );
			}
		}
	}

	private static void copyTemplate ( DMIElem asStmt, DMIView vw, ConceptManager cm, boolean autohide ) {
		// m: Template evoked-as nameString
		DMIElem sb = asStmt.itsSubject;
		DMIElem vb = asStmt.itsVerb;
		DMIElem ob = asStmt.itsObject;

		// Copy Template
		cm.setProvenanceInfoEx ( asStmt );

		// IList<DMIElem>  templateInclusions = sb.FindSubjectsFromUsedAsObjectByVerb(cm.itsBaseVocab.gInTemplate);
		IList<DMIElem> templateInclusions = sb.findStatementsFromUsedAsObjectBySubjectAndVerb ( null, cm.itsBaseVocab.gInTemplate, null );
		// IList<DMIElem>  substitutions = ob.FindSubjectsFromUsedAsObjectByVerb(cm.itsBaseVocab.gInEvocation);
		IList<DMIElem> inEvocations = ob.findStatementsFromUsedAsObjectBySubjectAndVerb ( null, cm.itsBaseVocab.gInEvocation, null );

		// IList<DMIElem>  templateMembers = 
		replicateList ( templateInclusions, inEvocations, cm, sb, vb, NameUtilities.getMCText ( ob ) /* should be ob.Value() */, vw, autohide );
		// vw.AddElementsFromListToView(templateMembers); not needed, the view has been as tracking

		// Hide original
		if ( autohide ) {
			vw.remove ( asStmt );
			vw.remove ( ob );
		}
	}

	private static void replicateList ( IList<DMIElem> templateInclusions, IList<DMIElem> inEvocations, ConceptManager cm, DMIElem diffSubj, DMIElem diffVerb, String diffObj, DMIView vw, boolean autohide ) {
		// The idea is to populate the map
		ISet<DMIElem> templateMembers = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		for ( int i = 0; i < templateInclusions.size (); i++ ) {
			DMIElem m = templateInclusions.get ( i );
			if ( !m.instanceOf ( cm.itsBaseVocab.gOptionallyIncludedDefNo ) ) {
				templateMembers.addToSet ( m.itsSubject );
			}
		}

		DMIElem [] map = new DMIElem [ templateMembers.size () ];
		int count = 0;

		IList<DMIElem> substitionTargets = DMIElem.principlesFromStatements ( DMIElem.principlesFromStatements ( inEvocations, true, null ), false, null );

		while ( count != templateMembers.size () ) {
			int oldCount = count;
			for ( int i = 0; i < templateMembers.size (); i++ ) {
				if ( map [ i ] == null ) {
					DMIElem m = templateMembers.get ( i );
					if ( m != null ) {
						if ( !m.isStatement () ) {
							// Replicate using diff
							/*
							String name = NameUtilities.StructuredDifferentiator(NameUtilities.GetMCText(m), NameUtilities.GetMCText(diffSubj), NameUtilities.GetMCText(diffVerb), diffObj);
							*/
							DMIElem x = substitutionTarget ( substitionTargets, inEvocations, m, true, vw, autohide );
							if ( x == null ) {
								// No substitution: we replicate with differentiator
								// String name = NameUtilities.BasicDifferentiator(NameUtilities.GetMCText(m), diffObj);
								String name = NameUtilities.flattenedBasicDifferentiator ( NameUtilities.getMCText ( m ), diffObj );
								x = cm.enterCloneOfConcept ( name, m );
							}
							map [ i ] = x;
							count++;
						} else {
							// Replicate relationship (w/o diff) using mapped subject and object
							int si = templateMembers.indexOf ( m.itsSubject );
							DMIElem sb = null;
							if ( si < 0 ) {
								// The relationship subject is not in the template, so doesn't get replicated with differentiator
								// So, we take the unreplicated item from the subject of the original relationship.
								// Still, we can honor substitution declarations!
								sb = substitutionTarget ( substitionTargets, inEvocations, m.itsSubject, false, vw, autohide );
							}
							else {
								sb = map [ si ];
							}
							if ( sb != null ) {
								int oi = templateMembers.indexOf ( m.itsObject );
								DMIElem ob = null;
								if ( oi < 0 ) {
									// The relationship object is not in the template, so doesn't get replicated with differentiator
									// So, we take the unreplicated item from the  of the original relationship.
									// Still, we can honor substitution declarations!
									ob = substitutionTarget ( substitionTargets, inEvocations, m.itsObject, false, vw, autohide );
								}
								else {
									ob = map [ oi ];
								}
								if ( ob != null ) {
									// We have the matches for the subject and object, so can now replicate the relationship
									// No need for differentiator because subject and object being different from original takes care of that
									map [ i ] = cm.enterStatement ( sb, m.itsVerb, ob, autohide );
									count++;
								}
							}
						}
					}
				}
			}
			assert count > oldCount : "Begets.Replicate: Count not advancing.";
		}

		// return new IList<DMIElem> (map); not needed, the view is tracking
	}

	private static DMIElem substitutionTarget ( IList<DMIElem> substitionTargets, IList<DMIElem> inEvocations, DMIElem m, boolean nullIfNotFound, DMIView vw, boolean autohide ) {
		DMIElem ans = null;

		int sp = substitionTargets.indexOf ( m );
		if ( sp >= 0 ) {
			DMIElem inEv = inEvocations.get ( sp );
			DMIElem subst = inEv.itsSubject;
			ans = subst.itsSubject;
			if ( autohide ) {
				vw.remove ( inEv );
				vw.remove ( subst );
			}
		}
		else if ( !nullIfNotFound ) {
			ans = m;
		}

		return ans;
	}

	private static void begets_Inferencing ( Builder bldr ) {
		ISet<DMIElem> qV = bldr.itsBaseVocab.gBegets.itsFullInstanceSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null )
				begets ( m, bldr, bldr.itsOptions.gOptionAutoHide );
		}
	}

	private static void begets ( DMIElem bgStmt, Builder bldr, boolean autohide ) {
		DMIElem sbClass = bgStmt.itsSubject;

		/*
		 * We have a statement that class-A begets prototype-B
		 * We look for that statement Begets As, which tells us what differentiator type to use
		 * We look for that statement Begets Using, which tells us what property to use for the relation
		 * 	of the begotten item to the begetting instance.
		 */
		DMIElem diffType = sbClass.find1StatementFromUsedAsSubjectByVerbAndObject ( bldr.itsBaseVocab.gBegetsAs, null, null );
		if ( diffType != null )
			diffType = diffType.itsObject;

		ISet<DMIElem> ugProp = bgStmt.findObjectsFromUsedAsSubjectByVerb ( bldr.itsBaseVocab.gBegetsUsing, null );
		begetsAs ( sbClass, diffType, bgStmt.itsObject, ugProp, bldr.itsConceptMgr, autohide );
	}

	private static void begetsAs ( DMIElem sbClass, DMIElem diffClass, DMIElem ob, ISet<DMIElem> ugProp, ConceptManager cm, boolean autohide ) {
		ISet<DMIElem> cV = sbClass.itsFullInstanceSet;
		DMISubgraph defSG = null;
		DMIView defVW = null;

		for ( int i = 0; i < cV.size (); i++ ) {
			DMIElem sb = cV.get ( i );
			if ( sb != null ) {

				if ( sb.itsSubgraph.isVisible ) {
					if ( defSG != null )
						cm.setDefaultSubgraph ( defSG );
					if ( defVW != null )
						cm.trackAdditionsInView ( defVW );
					defSG = null;
					defVW = null;
				} else {
					if ( defSG == null ) {
						defSG = cm.setDefaultSubgraph ( cm.itsInvisiblexSG );
						defVW = cm.trackAdditionsInView ( null );
					}
				}

				// backup prov info is the item itself
				//   but would rather have prov info of where sb was known as sbClass
				//   cm.Find1StatementFromUsedAsSubjectByVerbAndObject(bldr.baseVocab.gInstanceOf, bldr.baseVocab.gCRUDService)
				cm.setProvenanceInfoEx ( sb );
				if ( diffClass != null ) {
					// older style semi-formal differentiator (will be used in the presence of a Begets As statement)
					String c = NameUtilities.constructDifferentiator ( NameUtilities.getMCText ( ob ), NameUtilities.getMCText ( diffClass ), NameUtilities.getMCText ( sb ) );
					DMIElem bgItm = cm.enterCloneOfConcept ( c, ob );
					for ( int j = 0; j < ugProp.size (); j++ ) {
						DMIElem n = ugProp.get ( j );
						if ( n != null )
							cm.enterStatement ( bgItm, n, sb, autohide );
					}
				} else {
					// new style structured differentiator (will be used in the absence of Begets As statement)
					for ( int j = 0; j < ugProp.size (); j++ ) {
						DMIElem n = ugProp.get ( j );
						if ( n != null ) {
							String c = NameUtilities.structuredDifferentiator ( NameUtilities.getMCText ( ob ), null, n, sb );
							DMIElem bgItm = cm.enterCloneOfConcept ( c, ob );
							cm.enterStatement ( bgItm, n, sb, autohide );
						}
					}
				}
			}
		}

		if ( defSG != null )
			cm.setDefaultSubgraph ( defSG );
		if ( defVW != null )
			cm.trackAdditionsInView ( defVW );
	}

	private static void qualifies_Inferencing ( DMIView vw, Builder bldr ) {
		ISet<DMIElem> qV = bldr.itsBaseVocab.gQualifiesSO.itsFullInstanceSet;
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null )
				qualifies ( m, vw, bldr.itsConceptMgr, bldr.itsOptions.gOptionAutoHide );
		}
	}

	private static void qualifies ( DMIElem m, DMIView vw, ConceptManager cm, boolean autohide ) {
		/*
		 * We have: A subproperty-of-qualifies B
		 *	So, we are tointroduce a new B: B'
		 * 		and a new statement relating A and B'
		 * 		and possibly hide the original B and/or original statement relating A and B
		 * 
		 * 	Regarding the property to use for the new relationship (i.e. between A and B'):
		 *		See if subproperty-of-qualifies is Qualified To some other property, 
		 * 			if not use the same property as the property type
		 * 				i.e. A subproperty-of-qualifies B'
		 * 			if so, use that other property
		 * 				i.e. A qualified-to-property B'
		 */
		DMIElem lk = m.itsVerb.find1StatementFromUsedAsSubjectByVerbAndObject ( cm.itsBaseVocab.gQualifiedTo, null, null );
		if ( lk == null ) {
			lk = m.itsVerb;
		} else {
			lk = lk.itsObject;
		}
		generateQualifiedAndHideOriginal ( m, lk, vw, cm, autohide );
	}

	private static void generateQualifiedAndHideOriginal ( DMIElem m, DMIElem lk, DMIView vw, ConceptManager cm, boolean autohide ) {
		DMIElem stmt = m.itsSubject;
		DMIElem tg = m.itsObject;
		DMIElem sb = stmt.itsSubject;
		DMIElem vb = stmt.itsVerb;
		DMIElem ob = stmt.itsObject;

		cm.setProvenanceInfoEx ( m );

		String name;
		/*
		name = BuildUtilities.ConstructDifferentiator(ob.itsFirstName.itsMCText, vb.itsFirstName.itsMCText, sb.itsFirstName.itsMCText);
		name = BuildUtilities.ConstructDifferentiator(name, lk.itsFirstName.itsMCText, tg.itsFirstName.itsMCText);
		*/
		name = NameUtilities.structuredDifferentiator ( NameUtilities.getMCText ( ob ), sb, vb, null );
		name = NameUtilities.structuredDifferentiator ( name, null, lk, tg );

		DMIElem nb = cm.enterCloneOfConcept ( name, ob );

		DMIElem s1 = cm.enterStatement ( sb, vb, nb, autohide );
		DMIElem s2 = cm.enterStatement ( nb, lk, tg, autohide );

		if ( autohide ) {
			if ( m != s1 && m != s2 )
				vw.remove ( m );
			if ( stmt != s1 && stmt != s2 )
				vw.remove ( stmt );
			if ( !ob.isVisiblyReferredTo ( vw, true ) ) {
				vw.remove ( ob );
			}
		}
	}

}
