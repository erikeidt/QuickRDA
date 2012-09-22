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

public class DMIElem {

	private enum ValEnum {
		kNoValue,
		kSingleValue,
		kVectorValue
	}

	// public static final boolean			kSuperNeuter	= true;
	// public static final boolean			kSingleProv		= true;

	public String						aaaName;					// just to aid debugging; written never read except during debugging

	//Declared Things
	public int							itsIndex;
	public DMISubgraph					itsSubgraph;				// revisit 1:1 in subgraph? is ok, have views now...

	public ISet<DMIElem>				itsQualifiers;				// 070811 qualifiers $$$ TODO Finish


	private ValEnum						itsHasValue;				// Applies to Serializations
	private String						itsSerialization;			// Applies to Serializations

	public DMIElem						itsSubject;				// Applies to Statements
	public DMIElem						itsVerb;					// Applies to Statements
	public DMIElem						itsObject;					// Applies to Statements

	//Cached state
	public Object						itsNameSet;				// Applied to Concepts
	// public DMISharedNameList			itsFirstName;				// Applied to Concepts; should be a set
	public String						itsDescription;			// Should just not cache this; lookup as needed instead

	public ISet<DMIElem>				itsDeclaredTypeList;		// Applies to Concepts & Serializations
	public ISet<DMIElem>				itsFullTypeSet;			// Applies to Concepts & Serializations

	public ISet<DMIElem>				itsSameAsSet;				// Applies to Concepts

	public ISet<DMIElem>				itsUsedAsSubjectStmtSet;	// Applies to Concepts & Serializations
	public ISet<DMIElem>				itsUsedAsObjectStmtSet;	// Applies to Concepts & Serializations

	private ISet<DMIElem>				itsAttachedToSet;			// Applies to Concepts
	private ISet<DMIElem>				itsAttachedSet;			// Applies to Concepts

	private ISet<DMIElem>				itsGroupedToSet;			// Applies to Concepts
	private ISet<DMIElem>				itsGroupedSet;				// Applies to Concepts

	private ProvenanceInfo				itsProvInfo;				//
	public boolean						itsBoldTag;				//

	public ISet<DMIElem>				itsFullInstanceSet;		// Applies to Classes
	private ISet<DMIElem>				itsFullSubclassSet;		// Applies to Classes; these are private in order to put the ISet<DMIElem>.VZ() check around them
	private ISet<DMIElem>				itsFullSuperclassSet;		// Applies to Classes

	private ISet<DMIElem>				itsDomainSet;				// Applies to Properties
	private ISet<DMIElem>				itsRangeSet;				// Applies to Properties
	private ISet<DMIElem>				itsPreferredSet;			// Applies to Properties
	private ISet<DMIElem>				itsInverseSet;				// Applies to Properties

	public DMIElementDiagrammingInfo	itsDiagrammingInfo;		// Slated to become part of the metamodel instead of as attributes

	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++;
		//ptrCount = ptrCount
		mrc.atrCount += 25;
		mrc.strCount += 2;
		if ( itsSerialization != null )
			mrc.strSize += itsSerialization.length ();

		itsDeclaredTypeList.memUsage ( mrc );
		itsFullTypeSet.memUsage ( mrc );
		DMIElem.readAccessToSet ( itsSameAsSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsSameAsSet ).memUsage ( mrc );

		itsUsedAsSubjectStmtSet.memUsage ( mrc );
		itsUsedAsObjectStmtSet.memUsage ( mrc );
		itsFullInstanceSet.memUsage ( mrc );
		itsUsedAsSubjectStmtSet.memUsage ( mrc );

		DMIElem.readAccessToSet ( itsAttachedToSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsAttachedSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsGroupedToSet ).memUsage ( mrc );

		DMIElem.readAccessToSet ( itsFullInstanceSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsFullSubclassSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsFullSuperclassSet ).memUsage ( mrc );

		DMIElem.readAccessToSet ( itsDomainSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsRangeSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsPreferredSet ).memUsage ( mrc );
		DMIElem.readAccessToSet ( itsInverseSet ).memUsage ( mrc );
	}

	public DMIElem () {
		itsDeclaredTypeList = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsFullTypeSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsUsedAsSubjectStmtSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsUsedAsObjectStmtSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsFullInstanceSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsHasValue = ValEnum.kNoValue;
	}

	public void resetCache () {
		itsDeclaredTypeList = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsFullTypeSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsUsedAsSubjectStmtSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsUsedAsObjectStmtSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsFullInstanceSet = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		itsHasValue = ValEnum.kNoValue;

		itsFullSubclassSet = null;
		itsFullSuperclassSet = null;
	}

	public boolean instanceOf ( DMIElem c ) {
		if ( c == null ) {
			return true;
			/*
				// #if ( kSuperNeuter ) {
					} else if ( c == ggConcept ) {
						return true;
					} else if ( itsVerb == null ) {
						return (itsFullTypeSet.indexOf(c) >= 0);
					} else if ( c == ggRelation ) {
						return true;
				// #}
			*/
		} else {
			return itsFullTypeSet.indexOf ( c ) >= 0;
		}
	}

	public boolean instanceOfEx ( ISet<DMIElem> mV ) {
		for ( int i = 0; i < mV.size (); i++ ) {
			DMIElem m = mV.get ( i );
			if ( m != null && instanceOf ( m ) )
				return true;
		}

		return false;
	}

	public boolean matches ( DMIElem e ) {
		if ( e == null ) {
			return true;
		} else {
			return (e == this);
		}
	}

	public boolean subclassOf ( DMIElem c ) {
		if ( c == null ) {
			return true;
		} else if ( c == this ) { //itsIndex == c.itsIndex Then
			return true;
			/*
				// #if ( kSuperNeuter ) {
					} else if ( c == ggConcept ) {
						return true;
					} else if ( itsVerb == null ) {
						return (ISet<DMIElem>.ReadAccessTo(itsFullSuperclassSet).indexOf(c) >= 0);
					} else if ( c == ggRelation ) {
						return true;
				// #} 
			*/
		} else {
			return DMIElem.readAccessToSet ( itsFullSuperclassSet ).indexOf ( c ) >= 0;
		}
	}

	public boolean isSimpleConcept () {
		return (itsVerb == null) && (itsHasValue == ValEnum.kNoValue);
	}

	public boolean isStatement () {
		return (itsVerb != null);
	}

	public void setStatement ( DMIElem s, DMIElem v, DMIElem o ) {
		itsSubject = s;
		itsVerb = v;
		itsObject = o;
	}

	public ISet<DMIElem> subclassSet () {
		return DMIElem.readAccessToSet ( itsFullSubclassSet );
	}

	public ISet<DMIElem> superclassSet () {
		return DMIElem.readAccessToSet ( itsFullSuperclassSet );
	}

	public ISet<DMIElem> inverseSet () {
		return DMIElem.readAccessToSet ( itsInverseSet );
	}

	public void addInverseProperty ( DMIElem p ) {
		itsInverseSet = DMIElem.allocateSetIfNull ( itsInverseSet );
		itsInverseSet.addToList ( p );
	}

	public ISet<DMIElem> oreferredSet () {
		return DMIElem.readAccessToSet ( itsPreferredSet );
	}

	public void addPreferredProperty ( DMIElem p ) {
		itsPreferredSet = DMIElem.allocateSetIfNull ( itsPreferredSet );
		itsPreferredSet.addToList ( p );
	}

	//Note: Caller expected infer Me hasType Class
	public void addAsInstance ( DMIElem e, boolean explicitlyDeclared ) {
		//e Is-An-Instance-Of Me, or, e has-Type Me
		if ( explicitlyDeclared ) {
			if ( itsFullInstanceSet.addToSet ( e ) ) {
				e.protected_AddAsDeclaredType ( this );
				addAsInferredInstance ( e );
			}
		} else {
			//neuter; but don't have access to g.b here...
			//if ( itsIndex == b.gRelation.itsIndex || itsIndex == b.gConcept.itsIndex ) {
			//} else {
			addAsInferredInstance ( e );
			//}
		}
	}

	public boolean addAsInferredSameAs ( DMIElem e ) {
		if ( itsSameAsSet.addToSet ( e ) ) {
			// $$$ TODO should sort entries
			// should share/propagate among all having only one vector for SameAs for all of them
			// but that would eliminate propagation state.
			e.itsSameAsSet.addToSet ( this );
			return true;
		}
		return false;
	}

	private void addAsInferredInstance ( DMIElem e ) {
		e.orotected_AddAsInferredType ( this );
		protected_AddAsInferredInstance ( e );

		ISet<DMIElem> cV = DMIElem.readAccessToSet ( itsFullSuperclassSet );

		for ( int i = 0; i < cV.size (); i++ ) {
			DMIElem c = cV.get ( i );
			if ( c != null ) {
				e.orotected_AddAsInferredType ( c );
				c.protected_AddAsInferredInstance ( e );
			}
		}
	}

	public void protected_AddAsDeclaredType ( DMIElem c ) {
		if ( itsDeclaredTypeList.addToSet ( c ) ) {
			orotected_AddAsInferredType ( c );
			//itsFullTypeSet.addToSet c
		}
	}

	protected void orotected_AddAsInferredType ( DMIElem c ) {
		/*
			// #if ( kSuperNeuter ) {
				if ( c != ggConcept ) {
					if ( itsVerb == null ) {
						// added = itsFullTypeSet.AddToSet(c);
						itsFullTypeSet.AddToSet(c);
					} else if ( c != ggRelation ) {
						// added = itsFullTypeSet.AddToSet(c);
						itsFullTypeSet.AddToSet(c);
					}
				}
			// #} else {
		 */
		// added = itsFullTypeSet.AddToSet(c);
		itsFullTypeSet.addToSet ( c );
	}

	protected void protected_AddAsInferredInstance ( DMIElem e ) {
		itsFullInstanceSet.addToSet ( e );
	}

	//Note: Caller expected to infer Me,c hasType Class
	public boolean addAsSuperclass ( DMIElem c, boolean declared ) {
		//Me Is-A-Subclass-Of c
		itsFullSuperclassSet = DMIElem.allocateSetIfNull ( itsFullSuperclassSet );
		boolean ans = itsFullSuperclassSet.addToSet ( c );
		c.protected_AddAsSubclass ( this );
		return ans;
	}

	protected void protected_AddAsSubclass ( DMIElem c ) {
		itsFullSubclassSet = DMIElem.allocateSetIfNull ( itsFullSubclassSet );
		itsFullSubclassSet.addToSet ( c );
	}

	public ISet<DMIElem> domainSet () {
		return DMIElem.readAccessToSet ( itsDomainSet );
	}

	public ISet<DMIElem> rangeSet () {
		return DMIElem.readAccessToSet ( itsRangeSet );
	}

	public boolean addDomain ( DMIElem d ) {
		itsDomainSet = DMIElem.allocateSetIfNull ( itsDomainSet );
		return itsDomainSet.addToSet ( d );
	}

	public boolean addRange ( DMIElem r ) {
		itsRangeSet = DMIElem.allocateSetIfNull ( itsRangeSet );
		return itsRangeSet.addToSet ( r );
	}

	public boolean isGrouping () {
		return (DMIElem.readAccessToSet ( itsGroupedSet ).size () > 0);
	}

	public boolean isGroupedToOnce () {
		return (DMIElem.readAccessToSet ( itsGroupedToSet ).size () == 1);
	}

	public ISet<DMIElem> groupedToSet () {
		return DMIElem.readAccessToSet ( itsGroupedToSet );
	}

	public ISet<DMIElem> groupedSet () { // Friends only
		return DMIElem.readAccessToSet ( itsGroupedSet );
	}

	protected void protected_AddGroupFrom ( DMIElem subj ) {
		itsGroupedSet = DMIElem.allocateSetIfNull ( itsGroupedSet );
		itsGroupedSet.addToSet ( subj );
	}

	public void addGroupTo ( DMIElem obj ) {
		//obj.AddToGroupSet subj
		//subj.AddToGroupedToSet obj
		obj.protected_AddGroupFrom ( this );
		itsGroupedToSet = DMIElem.allocateSetIfNull ( itsGroupedToSet );
		itsGroupedToSet.addToSet ( obj );
	}

	public boolean isAttaching () {
		return DMIElem.readAccessToSet ( itsAttachedSet ).size () > 0;
	}

	public boolean isAttachedToOnce () {
		return DMIElem.readAccessToSet ( itsAttachedToSet ).size () == 1;
	}

	public ISet<DMIElem> attachedToSet () { // Friends only
		return DMIElem.readAccessToSet ( itsAttachedToSet );
	}

	public ISet<DMIElem> attachedSet () {
		return DMIElem.readAccessToSet ( itsAttachedSet );
	}

	protected void protected_AddAttachFrom ( DMIElem subj ) {
		itsAttachedSet = DMIElem.allocateSetIfNull ( itsAttachedSet );
		itsAttachedSet.addToSet ( subj );
	}

	public void addAttachTo ( DMIElem obj ) {
		//obj.AddToAttachmentSet subj
		//subj.AddToAttachedToSet obj
		obj.protected_AddAttachFrom ( this );
		itsAttachedToSet = DMIElem.allocateSetIfNull ( itsAttachedToSet );
		itsAttachedToSet.addToSet ( obj );
	}

	// #if ( kSingleProv ) {
	public void setProvenance ( ProvenanceInfo p, boolean highlight ) {
		if ( itsProvInfo == null ) {
			itsProvInfo = p;
		}
		itsBoldTag |= highlight; // accumulate bolding
	}

	// #}

	public ProvenanceInfo provInfo () {
		// #if ( Not kSingleProv ) {
		/*
		if ( itsProvInfo == null ) {
			itsProvInfo = New SSUProvenanceInfo;
		}
		*/
		// #}
		return itsProvInfo;
	}

	public void setSerialization ( String s ) {
		itsHasValue = ValEnum.kSingleValue;
		itsSerialization = s;
	}

	public boolean isSerialization () {
		return (itsHasValue == ValEnum.kSingleValue);
	}

	public String value () {
		return itsSerialization;
	}

	public void addAsSubjectOf ( DMIElem stmt ) {
		itsUsedAsSubjectStmtSet.addToSet ( stmt );
	}

	public void addAsObjectOf ( DMIElem stmt ) {
		itsUsedAsObjectStmtSet.addToSet ( stmt );
	}

	public ISet<DMIElem> findStatementsFromUsedAsObjectBySubjectAndVerb ( DMIElem sb, DMIElem vb, DMIView vw ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		for ( int i = 0; i < itsUsedAsObjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsObjectStmtSet.get ( i );
			if ( m != null && (vw == null || vw.inView ( m )) && m.itsVerb.subclassOf ( vb ) && m.itsSubject.matches ( sb ) )
				ans.addToList ( m ); //they come from a set so no need to keep them unique
		}
		return ans;
	}

	public ISet<DMIElem> findStatementsFromUsedAsSubjectByVerbAndObject ( DMIElem vb, DMIElem ob, DMIView vw ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		for ( int i = 0; i < itsUsedAsSubjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsSubjectStmtSet.get ( i );
			if ( m != null && (vw == null || vw.inView ( m )) && m.itsVerb.subclassOf ( vb ) && m.itsObject.matches ( ob ) )
				ans.addToList ( m ); //they come from a set so no need to keep them unique
		}
		return ans;
	}

	public ISet<DMIElem> findSubjectsFromUsedAsObjectByVerb ( DMIElem vb, DMIView vw ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		for ( int i = 0; i < itsUsedAsObjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsObjectStmtSet.get ( i );
			if ( m != null && (vw == null || vw.inView ( m )) && m.itsVerb.subclassOf ( vb ) )
				ans.addToSet ( m.itsSubject );
		}
		return ans;
	}

	public ISet<DMIElem> findObjectsFromUsedAsSubjectByVerb ( DMIElem vb, DMIView vw ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		for ( int i = 0; i < itsUsedAsSubjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsSubjectStmtSet.get ( i );
			if ( m != null && (vw == null || vw.inView ( m )) && m.itsVerb.subclassOf ( vb ) )
				ans.addToSet ( m.itsObject );
		}
		return ans;
	}

	public DMIElem find1StatementFromUsedAsObjectBySubjectAndVerb ( DMIElem sb, DMIElem vb, DMIView vw ) {
		for ( int i = 0; i < itsUsedAsObjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsObjectStmtSet.get ( i );
			if ( m != null && (vw == null || vw.inView ( m )) ) {
				if ( m.itsVerb.subclassOf ( vb ) && m.itsSubject.matches ( sb ) )
					return m;
			}
		}

		return null;
	}

	public DMIElem find1StatementFromUsedAsSubjectByVerbAndObject ( DMIElem vb, DMIElem ob, DMIView vw ) {
		for ( int i = 0; i < itsUsedAsSubjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsSubjectStmtSet.get ( i );
			if ( m != null && (vw == null || vw.inView ( m )) ) {
				if ( m.itsVerb.subclassOf ( vb ) && m.itsObject.matches ( ob ) )
					return m;
			}
		}

		return null;
	}

	public boolean isVisiblyReferredTo ( DMIView vw, boolean D2D ) {
		for ( int i = 0; i < itsUsedAsSubjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsSubjectStmtSet.get ( i );
			if ( m != null && vw.inView ( m ) ) {
				if ( D2D ) {
					// object must also be in view
					if ( vw.inView ( m.itsObject ) ) {
						return true;
					}
				} else {
					return true;
				}
			}
		}

		for ( int i = 0; i < itsUsedAsObjectStmtSet.size (); i++ ) {
			DMIElem m = itsUsedAsObjectStmtSet.get ( i );
			if ( m != null && vw.inView ( m ) ) {
				if ( D2D ) {
					// subject must also be in view
					if ( vw.inView ( m.itsSubject ) ) {
						return true;
					}
				} else {
					return true;
				}
			}
		}

		//explicitly don't take into account attach/group
		//as those should not prevent removal of nodes from graph

		return false;
	}

	public boolean specializes ( DMIElem c ) {
		return subclassOf ( c );
	}

	public boolean canBePromotedTo ( DMIElem c ) {
		boolean ans;
		ans = true;

		// Some algorithmic choices here between:
		//   checking declared type set, meaning that promotion is a domain concept
		//   full type set, meaning that promotion is an underlying concept
		//   either way, can only be promoted if all match
		//   not if just one match as we've done before
		// NOTE: it has to be the domain concept because otherwise extra types (e.g. caused by provides) will prevent promotion
		// #if ( true ) {
		if ( !itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
			return false;
		} else {
			for ( int i = 0; i < itsDeclaredTypeList.size (); i++ ) {
				DMIElem decl = itsDeclaredTypeList.get ( i );
				if ( decl != null ) {
					if ( !c.subclassOf ( decl ) ) {
						if ( c.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ^ decl.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {} else {
							ans = false;
							break;
						}
					}
				}
			}
		}
		// #} else {
		/*
		for ( int i = 0; i < itsFullTypeSet.size (); i++ ) {
			DMIElem decl = itsFullTypeSet.ElementUt(i);
			if ( decl != null ) {
				if ( ! c.SubclassOf(decl) ) {
					ans = false;
					break;
				}
			}
		}
		*/
		// #}

		return ans;
	}

	public boolean isAbstractProperty () {
		return instanceOf ( this.itsSubgraph.itsGraph.itsBaseVocab.gAbstractProperty );
		/*
		//Could also see if we're in the abstract property graph
		//and, we could also have a boolean state that records if we're an abstract property
		return Strings.InStr(itsFirstName.itsMCText, "->") > 0;
		*/
	}

	public static DMIElem [] addToList ( DMIElem m, DMIElem [] mL ) {
		int u = (mL == null ? 0 : mL.length);
		mL = expandPreserve ( mL, u + 1 );
		mL [ u ] = m;
		return mL;
	}

	public static DMIElem [] expandPreserve ( DMIElem [] v, int u ) {
		DMIElem [] ans = new DMIElem [ u ];
		int len = (v == null ? 0 : v.length);
		if ( u < len )
			len = u;
		if ( len > 0 )
			System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

	@Override
	public int hashCode () {
		return itsIndex;
	}


	private static final XSetList<DMIElem>	mtX	= new XSetList<DMIElem> ();

	public static ISet<DMIElem> readAccessToSet ( ISet<DMIElem> x ) {
		if ( x == null )
			return mtX;
		return x;
	}

	public static IList<DMIElem> readAccessToList ( IList<DMIElem> x ) {
		if ( x == null )
			return mtX;
		return x;
	}

	public static ISet<DMIElem> allocateSetIfNull ( ISet<DMIElem> x ) {
		if ( x == null )
			return new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		return x;
	}

	public static IList<DMIElem> allocateListIfNull ( IList<DMIElem> x ) {
		if ( x == null )
			return new XSetList<DMIElem> ( XSetList.HashOnDemand );
		return x;
	}

	public static class Comparator implements IComparator<DMIElem> {
		@Override
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			return false;
		}
	}

	public static IList<DMIElem> principlesFromStatements ( IList<DMIElem> inEvocations, boolean sb, DMIView vw ) {
		IList<DMIElem> ans = new XSetList<DMIElem> ();
		for ( DMIElem m : inEvocations ) {
			if ( m != null && (vw == null || vw.inView ( m )) ) {
				assert m.isStatement () : "DMList.SetFromSubjects: element on list is not a statement.";
				if ( sb )
					ans.addToList ( m.itsSubject );
				else
					ans.addToList ( m.itsObject );
			}
		}
		return ans;
	}

}
