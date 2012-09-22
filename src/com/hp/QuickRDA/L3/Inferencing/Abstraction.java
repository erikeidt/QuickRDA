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

public class Abstraction {

	// "/Abstract=Has Parent"
	public static void performAbstraction ( String cmdIn, ConceptManager cm, boolean autohide, DMIView vw, boolean allOrInView ) {
		StringRef xx = new StringRef ();
		ISet<DMIElem> pV = InferencingUtilities.makeList ( cmdIn, xx, cm.itsBaseVocab.gProperty, cm );
		for ( int i = 0; i < pV.size (); i++ )
			performaAbstraction ( pV.get ( i ), cm, autohide, vw, allOrInView );
	}

	// Abstract: t=Has Parent
	private static void performaAbstraction ( DMIElem t, ConceptManager cm, boolean autohide, DMIView vw, boolean allOrInView ) {
		/* 
		 * Get direction of abstraction: 
		 * 	Toward Subject (e.g. Plays) or
		 * 	Toward Subject (e.g. Is Assigned To, Member Of, Has Parent)
		 */
		boolean sb = false;
		if ( t.instanceOf ( cm.itsBaseVocab.gDecomposingProperty ) ) {
			sb = true;
		}
		else if ( !t.instanceOf ( cm.itsBaseVocab.gComposingProperty ) ) {
			lang.errMsg ( "Warning: Property " + NameUtilities.getNameValue ( t, cm.itsGraph, false, false ) + " assumed as composing property" );
		}

		ISet<DMIElem> usages = t.itsFullInstanceSet;
		usages.sortList ( transCmp ); // Ok, were sorting in place on the instance list here, probably should make a copy, but it doesn't hurt much...

		for ( int i = 0; i < usages.size (); i++ ) {
			DMIElem r = usages.get ( i );
			// r = subject Has Parent object
			if ( r != null && (allOrInView || vw.inView ( r ) && vw.inView ( r.itsSubject ) && vw.inView ( r.itsObject )) ) {
				/*
				 * Find all relationships to the Object (Subject) and add same to the Subject (Object).
				 * Then hide the Object (Subject) and the Subject (Object) verb Object (Subject) relationship.
				 * Only promote if the relationship allows it by type.
				 */
				if ( sb )
					replicate ( r, r.itsSubject, r.itsObject, cm, autohide, vw, allOrInView );
				else
					replicate ( r, r.itsObject, r.itsSubject, cm, autohide, vw, allOrInView );
			}
		}
	}

	private static class TransitiveComparator extends DMIElem.Comparator {
		/*
		private IList<DMIElem>  itsList;

		public TransitiveComparator(IList<DMIElem>  aList) {
			itsList = aList;
		}
		*/

		@Override
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			// e1, e2 are relations of same kind
			// we care about them relative to each other if:
			//   e1.object == e2.subject
			return e1.itsObject == e2.itsSubject;
		}
	}

	private static TransitiveComparator	transCmp	= new TransitiveComparator ();

	private static void replicate ( DMIElem skip, DMIElem to, DMIElem from, ConceptManager cm, boolean autohide, DMIView vw, boolean allOrInView ) {
		replicate ( skip.itsVerb, to, from, from.itsUsedAsObjectStmtSet, true, cm, autohide, vw, allOrInView );
		replicate ( skip.itsVerb, to, from, from.itsUsedAsSubjectStmtSet, false, cm, autohide, vw, allOrInView );
		if ( autohide ) {
			vw.remove ( skip );
			vw.remove ( from );
		}
	}

	private static void replicate ( DMIElem skip, DMIElem to, DMIElem from, ISet<DMIElem> lst, boolean swap, ConceptManager cm, boolean autohide, DMIView vw, boolean allOrInView ) {
		for ( int i = 0; i < lst.size (); i++ ) {
			DMIElem m = lst.get ( i );
			if ( m != null && (allOrInView || vw.inView ( m )) && !m.instanceOf ( skip ) ) {
				// we have m = sb vb ob
				// we want to substitute sb (ob depending on swap), which is from, to to
				cm.setProvenanceInfoEx ( m );
				if ( swap )
					enterDuplicateStatement ( m.itsSubject, m.itsVerb, to, cm, m, m.itsObject, autohide, vw );
				else
					enterDuplicateStatement ( to, m.itsVerb, m.itsObject, cm, m, m.itsSubject, autohide, vw );
			}
		}
	}

	private static void enterDuplicateStatement ( DMIElem sb, DMIElem vb, DMIElem ob, ConceptManager cm, DMIElem oldStmt, DMIElem oldTrg, boolean autohide, DMIView vw ) {
		/*
		 * Validate that vb is appropriate to sb, ob; if not: don't add stmt (but still hide)...
		 */
		// TODO validate
		if ( true )
			cm.enterStatement ( sb, vb, ob, autohide );
		if ( autohide ) {
			vw.remove ( oldStmt );
			vw.remove ( oldTrg );
		}
	}

	public static void promoteSubResponsibilities ( DMIBaseVocabulary b, ConceptManager conceptMgr, boolean autohide, DMIView vw ) {
		promotePC ( b.gResponsibility, b.gRespParent, b.gAssignedTo, b.gRespConsumes, b.gRespProvides, b.gRespConsumes, b.gRespProvides, autohide, true, conceptMgr, vw );
		// PromotePC(b.gResponsibility, b.gRespParent, b.gAssignedTo, b.RespConsumes2, b.gRespProvides2, b.gRespConsumes, b.gRespProvides, autohide, true, conceptMgr, vw);
	}

	public static void promoteResponsibiltiesToRoles ( DMIBaseVocabulary b, ConceptManager conceptMgr, boolean autohide, DMIView vw ) {
		promotePC ( b.gResponsibility, b.gAssignedTo, null, b.gRespConsumes, b.gRespProvides, b.gRoleConsumes, b.gRoleProvides, autohide, false, conceptMgr, vw );
		// PromotePC(b.gResponsibility, b.gAssignedTo, null, b.gRespConsumes2, b.gRespProvides2, b.gRoleConsumes, b.gRoleProvides, autohide, false, conceptMgr, vw);
	}

	public static void promotePCToParentRoles ( DMIBaseVocabulary b, ConceptManager conceptMgr, boolean autohide, DMIView vw ) {
		promotePC ( b.gRole, b.gMemberOf, b.gMemberOf, b.gRoleConsumes, b.gRoleProvides, b.gRoleConsumes, b.gRoleProvides, autohide, false, conceptMgr, vw );
	}

	public static void promtePCRoleToInteractsWith ( DMIBaseVocabulary b, ConceptManager conceptMgr, boolean autohide, DMIView vw ) {
		flattenPC ( b.gRole, b.gRoleConsumes, b.gRoleProvides, b.gInteractsWith, b.gInteractsWith, autohide, conceptMgr, vw );
	}

	// a provides b; c consumes b --> a interacts with c; hide provides, consumes; hide b on last use
	private static void flattenPC ( DMIElem nodeType, DMIElem propConsumes, DMIElem propProvides, DMIElem propNewConsumes, DMIElem propNewProvides, boolean suppress, ConceptManager cm, DMIView vw ) {
		ISet<DMIElem> qV = propProvides.itsFullInstanceSet;

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && vw.inView ( m ) ) {
				if ( m.itsSubject.instanceOf ( nodeType ) ) {
					cm.setProvenanceInfoEx ( m ); //xV.ElementAt(j)

					//identify all targets of
					//FindRelationSet(propConsumes, null, m.itsObject)
					ISet<DMIElem> xV = m.itsObject.findStatementsFromUsedAsObjectBySubjectAndVerb ( null, propConsumes, vw );

					for ( int j = 0; j < xV.size (); j++ ) {
						DMIElem n = xV.get ( j );
						if ( n != null && vw.inView ( n ) ) {
							DMIElem sb = n.itsSubject;
							if ( sb.instanceOf ( nodeType ) && vw.inView ( sb ) ) {
								if ( m.itsSubject == sb ) {
									//don't enter self-to-self edges
								} else {
									//cm.SetProvenanceInfoEx m 'xV.ElementAt(j)
									cm.enterStatement ( m.itsSubject, propNewConsumes, sb, suppress );
								}
								if ( suppress ) {
									//hiddenSG.MoveHere xV.ElementAt(j)
									vw.remove ( n );
								}
							}
						}
					}

					if ( suppress ) {
						vw.remove ( m );
						if ( !m.itsObject.isVisiblyReferredTo ( vw, true ) ) {
							vw.remove ( m.itsObject );
						}
					}
				}
			}
		}

		cm.clearProvenance ();
	}

	public static class IndexComparator extends DMIElem.Comparator {
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			return e1.itsIndex > e2.itsIndex;
		}
	}

	public static IComparator<DMIElem>	indexCmp	= new IndexComparator ();

	// Promote provides and consumes on nodeType up the chain of propParOrAsTo
	// for nodes up chain that match the nodeType's parents
	// a decomposes into b; b provides|consumes c --> a provides|consumes c; hide b provides|consumes; hide decomposes + b if last use; decomposes hides if b hides
	private static void promotePC ( DMIElem nodeType, DMIElem propParOrAsTo, DMIElem propAsToOrMemOf, DMIElem propConsumes, DMIElem propProvides, DMIElem propNewConsumes, DMIElem propNewProvides, boolean suppress, boolean checkProp, ConceptManager cm, DMIView vw ) {
		ISet<DMIElem> qV = nodeType.itsFullInstanceSet;

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && vw.inView ( m ) ) {

				ISet<DMIElem> parV = null;

				if ( propParOrAsTo != null ) {
					//find immediate other kind of "parents" of m; initial population of list
					//Set par = FindRelationSet(propParOrAsTo, m, null)
					//par = SetFromObject(par)
					parV = m.findObjectsFromUsedAsSubjectByVerb ( propParOrAsTo, vw );

					if ( propAsToOrMemOf != null ) {
						//then traverse list looking for nodes with parents to add to expand list
						addParSet ( propParOrAsTo, parV, vw );

						//then traverse list removing nodes with parents; leaving nodes without parents
						removeParSet ( propParOrAsTo, parV, vw );

						if ( checkProp ) {
							//Immediate "parents" of m as qualifiers
							//Set asnV = FindRelationSet(propAsToOrMemOf, m, null)
							//Set asnV = SetFromObject(asn)
							ISet<DMIElem> asnV = m.findObjectsFromUsedAsSubjectByVerb ( propAsToOrMemOf, vw );

							asnV.sortList ( indexCmp );

							//now cull leaf parents that don't match the filter
							filterParSetSourceToTarget ( propAsToOrMemOf, asnV, parV, vw );
						}
					}
				}

				if ( parV != null && parV.size () >= 1 ) {
					//Set pcartV = FindRelationSet(propConsumes, m, null)
					ISet<DMIElem> pcartV = m.findStatementsFromUsedAsSubjectByVerbAndObject ( propConsumes, null, vw );
					DMIElem prv = m;
					if ( pcartV.size () >= 1 ) {
						prv = pcartV.get ( 0 );
					}

					for ( int j = 0; j < parV.size (); j++ ) {
						DMIElem n = parV.get ( j );
						if ( n != null )
							copyRelationsBySet ( propNewConsumes, n, pcartV, prv, cm, suppress, vw ); //changed prov from m to pcart(1) 3/7/2011
					}

					//Set pcartV = FindRelationSet(propProvides, m, null)
					pcartV = m.findStatementsFromUsedAsSubjectByVerbAndObject ( propProvides, null, vw );
					for ( int j = 0; j < parV.size (); j++ ) {
						DMIElem n = parV.get ( j );
						if ( n != null )
							copyRelationsBySet ( propNewProvides, n, pcartV, prv, cm, suppress, vw ); //changed prov from m to pcart(1) 3/7/2011
					}

					if ( suppress ) {
						//SuppressDMINode m, vw
						vw.remove ( m );

						// vw.Remove(propParOrAsTo);?? we don't have the assignment
						// brute force go get it...
						ISet<DMIElem> x = m.findStatementsFromUsedAsSubjectByVerbAndObject ( propParOrAsTo, null, vw );
						for ( int j = 0; j < x.size (); j++ ) {
							DMIElem n = x.get ( j );
							if ( n != null )
								vw.remove ( n );
						}
					}
				}
			}
		}
	}

	private static void addParSet ( DMIElem p, ISet<DMIElem> parV, DMIView vw ) {
		boolean change = true;

		while ( change ) {
			parV.trim ();
			int u1 = parV.size ();

			for ( int i = 0; i < u1; i++ ) {
				//Set xl = FindRelationSet(p, par(i), null)
				//Set xl = SetFromObject(xl)
				DMIElem m = parV.get ( i );
				if ( m != null ) {
					ISet<DMIElem> xV = m.findObjectsFromUsedAsSubjectByVerb ( p, vw );
					//MergeSetsSourceToTarget xl, par
					parV.mergeListIntoSet ( xV );
				}
			}
			if ( parV.size () == u1 ) {
				change = false;
			}
		}
	}

	private static void removeParSet ( DMIElem p, ISet<DMIElem> parV, DMIView vw ) {
		for ( int i = 0; i < parV.size (); i++ ) {
			//Set xV = FindRelationSet(p, par(i), null)
			DMIElem m = parV.get ( i );
			if ( m != null ) {
				ISet<DMIElem> xV = m.findStatementsFromUsedAsSubjectByVerbAndObject ( p, null, vw );
				if ( xV.size () >= 1 ) {
					parV.clear ( i );
				}
			}
		}
		parV.trim ();
	}

	private static void filterParSetSourceToTarget ( DMIElem prop, ISet<DMIElem> asnV, ISet<DMIElem> parV, DMIView vw ) {
		for ( int i = 0; i < parV.size (); i++ ) {
			if ( !parMatch ( prop, parV.get ( i ), asnV, vw ) ) {
				parV.clear ( i );
			}
		}
		parV.trim ();
	}

	private static boolean parMatch ( DMIElem prop, DMIElem m, ISet<DMIElem> mV, DMIView vw ) {
		//xl = FindRelationSet(prop, m, null)
		//xl = SetFromObject(xl)
		ISet<DMIElem> xV = m.findObjectsFromUsedAsSubjectByVerb ( m, vw );
		xV.sortList ( indexCmp );

		//ParMatch = MatchSortedLists(mL, xl)
		return mV.sameList ( xV );
	}

	private static void copyRelationsBySet ( DMIElem prop, DMIElem m, ISet<DMIElem> sV, DMIElem prov, ConceptManager cm, boolean suppress, DMIView vw ) {
		cm.setProvenanceInfoEx ( prov );

		for ( int i = 0; i < sV.size (); i++ ) {
			DMIElem rep = sV.get ( i );
			if ( rep != null ) {
				//EnterRelation prop, m, s(i).itsObject
				cm.enterStatement ( m, prop, rep.itsObject, suppress );
				if ( suppress ) {
					//SuppressDMINode rep, hiddenSG
					vw.remove ( rep );
				}
			}
		}
		cm.clearProvenance ();
	}

}
