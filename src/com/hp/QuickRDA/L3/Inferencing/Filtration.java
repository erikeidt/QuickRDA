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
import com.hp.QuickRDA.L0.lang.XSetList.AsListOrSet;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L3.Inferencing.Query.*;

public class Filtration {

	public static class FilterOptions {
		boolean	gOptionReachNodeBruteForceAlog;

		public FilterOptions ( boolean gOptionReachNodeBruteForceAlog ) {
			this.gOptionReachNodeBruteForceAlog = gOptionReachNodeBruteForceAlog;
		}
	}

	// package private
	static class PropSet {
		ISet<DMIElem>	pAV;	//avoid properties symmetric (sub + obj)
		ISet<DMIElem>	pSV;	//avoid properties by subject
		ISet<DMIElem>	pOV;	//avoid properties by object
	}

	// package private
	static class AVSet {
		ISet<DMIElem>	anV;	//avoid nodes
		ISet<DMIElem>	atV;	//avoid type
		PropSet			apL;
	}

	// package private
	public enum StatementPositionEnum {
		None,
		LabelPosition,
		SubjectPosition,
		VerbPosition,
		ObjectPosition
	}

	//
	//Discussion
	//
	//The QuickRDA tooling will not graph edges unless both subject and object target are visible.
	//
	//We use i vs. x include mode to include stuff hidden; this hides both nodes and edges
	//This means we'll have to reveal both nodes and edges to make things visible.
	//
	//if ( we made edges visible but not nodes, then when nodes are revealed, any edges connecting them
	//would automatically appear
	//
	//Of course, we have edge-nodes / association classes do deal with; so sometimes edges are nodes
	//

	@SuppressWarnings("unchecked")
	public static DMIView runFilter ( String fltrIn, DMIGraph g, ConceptManager cm, NamedPatternManager npm, DMIView vw, DMIView vwGray, FilterOptions options ) {
		boolean first;
		first = true;

		ISet<DMIElem> mLIn = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		ISet<DMIElem> mLOut = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		AVSet avs = new AVSet ();
		// String pattern = "";
		clearAVSet ( avs );

		String f = Strings.Mid ( fltrIn, 2 );

		StringRef xx = new StringRef ();

		String cmd = Strings.tSplitAfter ( f, xx, ";" );
		f = xx.str;

		while ( !"".equals ( cmd ) ) {
			String op = Strings.tSplitAfter ( cmd, xx, "=" ).toLowerCase ();
			cmd = xx.str;

			//Case "selecthighlight"

			if ( "".equals ( op ) ) {
				// do nothing
			}
			else if ( "selectnode".equals ( op ) ) {
				ISet<DMIElem> xV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gConcept, cm );
				revealDMIList ( xV, mLOut, false, vw, null );
			} else if ( "selecttype".equals ( op ) ) {
				ISet<DMIElem> tV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gClass, cm );
				ISet<DMIElem> xV = findInstancesEx ( tV );
				revealDMIList ( xV, mLOut, false, vw, null );
			} else if ( "selectvis".equals ( op ) ) {
				mLOut = (ISet<DMIElem>) vw.toSet ().clone ();
				/*
				} else if ( "pattern".equals (op) ) {
				pattern = cmd.toLowerCase ();
				mLOut = mLIn;
				*/
			} else if ( "filter".equals ( op ) || "apply1".equals ( op ) ||  "apply*".equals ( op )) {
				boolean infer = false;
				boolean loop  = false;
				if      ("filter".equals ( op )) { infer = false; loop = false; }
				else if ("apply1".equals ( op )) { infer = true;  loop = false; }
				else if ("apply*".equals ( op )) { infer = true;  loop = true;  }
				// Tracing.startTracing ( Tracing.TraceWave | Tracing.TraceFrame | Tracing.TraceFrameX | Tracing.TraceFrameY /*| Tracing.TraceFrameZ */);
				// int a = 1;

				String pat = Strings.tSplitAfter ( cmd, xx, "," );
				cmd = xx.str;
				ISet<DMIElem> xV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gConcept, cm, true );
				WaveTraverser wvt = new WaveTraverser ( cm );
				BoundFrame bf = npm.primePattern ( pat, xV );
				if ( bf == null )
					lang.errMsg ( "Couldn't find pattern to apply: " + pat );
				else {
					ISet<DMIElem> suggested = null;
					if ( xV.size () == 0 ) {
						suggested = new XSetList<DMIElem> ( AsListOrSet.AsSet );
						bf.suggestSearchSources ( suggested, cm );
					}
					xV = wvt.traverse ( xV, new BoundFork ( bf ), infer, loop, suggested );
				}
				revealDMIList ( xV, mLOut, false, vw, null );
			} else if ( "hideall".equals ( op ) ) {
				vw.clear ();
				vwGray.clear ();
			}

			/*
			else if ( "selectlink".equals(op) ) {
				ISet<DMIElem> tV = MakeList(cmd, xx, cm.b.gRelation, cm);
				ISet<DMIElem> xV = FindRelationsEx(tV, false);
				RevealDMIList (xV, mLOut, true, vw, null);
			}
			*/
			else if ( "unionnode".equals ( op ) ) {
				ISet<DMIElem> xV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gConcept, cm );
				revealDMIList ( xV, mLOut, false, vw, null );
				mLOut.mergeListIntoSet ( mLIn );
			} else if ( "uniontype".equals ( op ) ) {
				ISet<DMIElem> tV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gClass, cm );
				ISet<DMIElem> xV = findInstancesEx ( tV );
				revealDMIList ( xV, mLOut, false, vw, null );
				mLOut.mergeListIntoSet ( mLIn );
			}
			/*
			else if ( "unionlink".equals(op) ) {
				ISet<DMIElem> tV = MakeList(cmd, xx, cm.b.gRelation, cm);
				ISet<DMIElem> xV = FindRelationsEx(tV, false);
				RevealDMIList (xV, mLOut, true, vw, null);
				mLOut.MergeSets(mLIn);
			}
			*/
			else if ( "avoidnode".equals ( op ) ) {
				avs.anV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gConcept, cm );
				mLOut = mLIn;
			} else if ( "avoidtype".equals ( op ) ) {
				avs.atV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gClass, cm );
				mLOut = mLIn;
			} else if ( "avoidlink".equals ( op ) ) {
				avs.apL = makePropertyList ( cmd, xx, cm );
				mLOut = mLIn;
			} else if ( "reachnode".equals ( op ) ) {
				// Tracing.openLog (); // Comment in/out to turn on/off logging
				if ( first )
					mLIn = vw.toSet (); // .Copy()
				ISet<DMIElem> tV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gConcept, cm );
				ISet<DMIElem> xV;
				if ( options.gOptionReachNodeBruteForceAlog )
					xV = Filtration_ORReach.findAllPathSetBF ( g, mLIn, avs, tV );
				else {
					WVSearch wvs = new WVSearch ( cm );
					xV = wvs.findAllPathSetWV ( mLIn, avs, tV, null );
					// xV = Filtration_WVReach.findAllPathSetWV (cm, mLIn, avs, tV, pattern);
				}
				revealDMIList ( xV, mLOut, false, vw, null );
				Tracing.closeLog ();
				clearAVSet ( avs );
				//Set mLOut = mLIn
				//pattern = "";
			}
			/*
			else if ( "apply".equals (op) ) {
				WVSearch wvs = new WVSearch (cm);
				xV = wvs.findAllPathSetWV (mLIn, avs, tV, "");
			}
			*/
			/*
			else if ( "reachtype".equals(op) ) {	        	
			}
			else if ( "reachlink".equals(op) ) {
			}
			*/
			else if ( "reach1node".equals ( op ) ) {
				//no parameters
				if ( first ) {
					mLIn = vw.toSet (); // VisibleSet(g)
				}
				ISet<DMIElem> xV = findRelationSetByTypeExEx ( g, null, mLIn, mLIn );
				ISet<DMIElem> newV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
				revealDMIList ( xV, mLOut, true, vw, newV );
				mLOut = newV;
			} else if ( "reach1type".equals ( op ) ) {
				ISet<DMIElem> tV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gClass, cm );
				if ( first ) {
					mLIn = vw.toSet (); // .Copy
				}
				ISet<DMIElem> xV = findRelationSetByTypeExEx ( g, tV, mLIn, mLIn );
				ISet<DMIElem> newV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
				revealDMIList ( xV, mLOut, true, vw, newV );
				mLOut = newV;
			} else if ( "reach1link".equals ( op ) ) {
				// $$$ TODO fix this; this is temp just to make simple examples work
				if ( true ) {
					//no parameters
					if ( first ) {
						mLIn = vw.toSet (); // VisibleSet(g)
					}
					ISet<DMIElem> xV = findRelationSetByTypeExEx ( g, null, mLIn, mLIn );
					ISet<DMIElem> newV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
					revealDMIList ( xV, mLOut, false, vw, newV );
					mLOut = newV;
				} /*
					else {
					PropSet ps = MakePropertyList(cmd, xx, cm);
					if ( first ) {
						mLIn = vw.ToSet(); //.Copy
					}
					ISet<DMIElem> xV;
					if ( ps.pAV.size () > 0 ) {
						xV = FindRelations(ps.pAV, mLIn, 0);
					} else {
						xV = new ISet<DMIElem>();
					}
					if ( ps.pSV.size () > 0 ) {
						ISet<DMIElem> tV = FindRelations(ps.pSV, mLIn, -1);
						xV.MergeListIntoSet(tV);
					}
					if ( ps.pOV.size () > 0 ) {
						ISet<DMIElem> tV = FindRelations(ps.pOV, mLIn, 1);
						xV.MergeListIntoSet(tV);
					}
					ISet<DMIElem> newV = new ISet<DMIElem>();
					RevealDMIList (xV, mLOut, true, vw, newV);
					mLOut = newV;
					}
					*/
			} else if ( "reach1forward".equals ( op ) ) {
				if ( first ) {
					mLIn = vw.toSet (); //.Copy
				}
				ISet<DMIElem> xV = findRelationSetByTypeExEx ( g, null, mLIn, null );
				ISet<DMIElem> newV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
				revealDMIList ( xV, mLOut, true, vw, newV );
				mLOut = newV;
			} else if ( "reach1back".equals ( op ) ) {
				if ( first ) {
					mLIn = vw.toSet (); //.Copy
				}
				ISet<DMIElem> xV = findRelationSetByTypeExEx ( g, null, null, mLIn );
				ISet<DMIElem> newV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
				revealDMIList ( xV, mLOut, true, vw, newV );
				mLOut = newV;
			} else if ( "cullnode".equals ( op ) ) {
				ISet<DMIElem> tV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gConcept, cm );
				for ( int i = 0; i < mLIn.size (); i++ ) {
					if ( tV.indexOf ( mLIn.get ( i ) ) >= 0 ) {
						mLIn.clear ( i );
					}
				}
				mLIn.trim ();
				mLOut = mLIn;
			} else if ( "culltype".equals ( op ) ) {
				ISet<DMIElem> tV = InferencingUtilities.makeList ( cmd, xx, cm.itsBaseVocab.gClass, cm );
				for ( int i = 0; i < mLIn.size (); i++ ) {
					if ( mLIn.get ( i ).instanceOfEx ( tV ) ) {
						mLIn.clear ( i );
					}
				}
				mLIn.trim ();
				mLOut = mLIn;
			} else if ( "culllink".equals ( op ) ) {
				PropSet ps = makePropertyList ( cmd, xx, cm );
				for ( int i = 0; i < mLIn.size (); i++ ) {
					DMIElem m = mLIn.get ( i );
					if ( m.itsVerb != null ) {
						if ( ps.pAV.indexOf ( m.itsVerb ) < 0 ) {
							mLIn.clear ( i );
						}
					}
				}
				mLIn.trim ();
				mLOut = mLIn;
			} else if ( "grey".equals ( op ) || "gray".equals ( op ) ) {
				if ( first ) {
					mLIn = vw.toSet ();
				}
				if ( vwGray == null ) {
					vwGray = new DMIView ();
				}
				vwGray.toSet ().mergeListIntoSet ( mLIn );
				mLOut = mLIn;
			} else if ( "hide".equals ( op ) ) {
				if ( first ) {
					mLIn = vw.toSet ();
					vw.clear();
				}
				else {
					int cnt = mLIn.size();
					for (int i = 0; i < cnt; i++ ) {
						DMIElem m = mLIn.get(i);
						vw.remove(m);
						if (vwGray != null)
						vwGray.remove(m);
					}
				}
				mLOut = mLIn;
			}

			// #if ( false ) {
			/*
					else if ( "restrict".equals() ) {
						tV = MakeList(cmd, gName);
						for ( int i = 0; i < tV.size (); i++ ) {
							m = tV.ElementUt(i);
							ISet<DMIElem> qV = FindQualifiedNodesEx(m);
							xV = FindInstancesEx(qV, true, true);
							for ( int j = 0; j < xV; j++ ) {
								SuppressNode(xV.ElementUt(j));
							}
						}
			*/
			// #}
			first = false;

			mLIn = mLOut;
			mLOut = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

			cmd = Strings.tSplitAfter ( f, xx, ";" );
			f = xx.str;
		}

		return vwGray;
	}

	private static PropSet makePropertyList ( String cmd, StringRef xx, ConceptManager cm ) {
		PropSet ps = new PropSet ();
		clearPropSet ( ps );

		intRef mode = new intRef ();

		String itm = Strings.tSplitAfter ( cmd, xx, "," );
		cmd = xx.str;

		while ( itm != "" ) {
			DMIElem m = InferencingUtilities.findTarget ( itm, xx, mode, cm.itsBaseVocab.gProperty, cm );
			itm = xx.str;

			if ( m != null ) {
				switch ( mode.val ) {
				case 0 :
					ps.pAV.addToSet ( m );
					break;
				case -1 :
					ps.pSV.addToSet ( m );
					break;
				case 1 :
					ps.pOV.addToSet ( m );
					break;
				}
			}
			itm = Strings.tSplitAfter ( cmd, xx, "," );
			cmd = xx.str;
		}
		return ps;
	}


	private static void clearPropSet ( PropSet ps ) {
		ps.pAV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		ps.pOV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		ps.pSV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
	}

	private static void clearAVSet ( AVSet avs ) {
		avs.anV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		avs.atV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		avs.apL = new PropSet ();
		clearPropSet ( avs.apL );
	}

	private static ISet<DMIElem> findInstancesEx ( ISet<DMIElem> tV ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		for ( int i = 0; i < tV.size (); i++ ) {
			DMIElem m = tV.get ( i );
			if ( m != null )
				ans.mergeListIntoSet ( m.itsFullInstanceSet );
		}

		return ans;
	}

	private static ISet<DMIElem> findRelationSetByTypeExEx ( DMIGraph g, ISet<DMIElem> tV, ISet<DMIElem> sbV, ISet<DMIElem> obV ) {
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		if ( sbV != null ) {
			for ( int i = 0; i < sbV.size (); i++ ) {
				DMIElem m = sbV.get ( i );
				if ( m != null ) {
					ISet<DMIElem> xV = m.itsUsedAsSubjectStmtSet;
					for ( int j = 0; j < xV.size (); j++ ) {
						DMIElem n = xV.get ( j );
						if ( n != null ) {
							if ( n.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) { // 081111
								if ( tV == null || tV.size () == 0 ) {
									ans.addToSet ( n );
								} else if ( n.instanceOfEx ( tV ) ) {
									ans.addToSet ( n );
								}
							}
						}
					}
				}
			}
		}

		if ( obV != null ) {
			for ( int i = 0; i < obV.size (); i++ ) {
				DMIElem m = obV.get ( i );
				if ( m != null ) {
					ISet<DMIElem> xV = m.itsUsedAsObjectStmtSet;
					for ( int j = 0; j < xV.size (); j++ ) {
						DMIElem n = xV.get ( j );
						if ( n != null ) {
							if ( n.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) { // 081111
								if ( tV == null || tV.size () == 0 ) {
									ans.addToSet ( n );
								} else if ( n.instanceOfEx ( tV ) ) {
									ans.addToSet ( n );
								}
							}
						}
					}
				}
			}
		}

		return ans;
	}

	// input: xV; collects items to mV; adds to view vw; diff goes to nV (only new things added to vw)
	private static void revealDMIList ( ISet<DMIElem> xV, ISet<DMIElem> mV, boolean subjobj, DMIView vw, ISet<DMIElem> nV ) {
		for ( int i = 0; i < xV.size (); i++ ) {
			DMIElem m = xV.get ( i );
			if ( m != null ) {
				//MakeVisible xL(i)
				revealDMINode ( m, mV, vw, nV );
				if ( subjobj ) {
					revealDMINode ( m.itsSubject, mV, vw, nV );
					revealDMINode ( m.itsObject, mV, vw, nV );
				}
			}
		}
	}

	private static void revealDMINode ( DMIElem m, ISet<DMIElem> mV, DMIView vw, ISet<DMIElem> nV ) {
		if ( m != null ) {
			if ( vw.addToView ( m ) ) {
				if ( nV != null )
					nV.addToSet ( m );
			}

			//vizSG.MoveHere m
			mV.addToSet ( m );
		}
	}

}

/*
private static ISet<DMIElem> FindRelations(ISet<DMIElem> mV, ISet<DMIElem> mLIn, int mode) {
	ISet<DMIElem> xV = null;

	for ( int i = 0; i < mV.size (); i++ ) {
		DMIElem m = mV.ElementAt(i);
		if ( m != null ) {
			ISet<DMIElem> nV = null;
			switch ( mode ) {
			case 0 :
				nV = FindLinkages(m, mLIn, mLIn);
				break;
			case -1 :
				nV = FindLinkages(m, mLIn, null);
				break;
			case 1 :
				nV = FindLinkages(m, null, mLIn);
				break;
			}
			if ( i == 1 ) {
				xV = nV;
			} else {
				xV.MergeListIntoSet(nV);
			}

		}
	}

	if ( xV == null ) {
		xV = new ISet<DMIElem>();
	}

	return xV;
}

private static ISet<DMIElem> FindLinkages(DMIElem vb, ISet<DMIElem> sbV, ISet<DMIElem> obV) {
	ISet<DMIElem> ans = new ISet<DMIElem>();

	if ( obV == null ) {
		for ( int i = 0; i < sbV.size (); i++ ) {
			DMIElem sb = sbV.ElementAt(i);
			if ( sb != null )
				ans.MergeListIntoSet(sb.FindStatementsFromUsedAsSubjectByVerbAndObject(vb, null));
		}
	} else if ( sbV == null ) {
		for ( int j = 0; j < obV.size (); j++ ) {
			DMIElem ob = obV.ElementAt(j);
			if ( ob != null )
				ans.MergeListIntoSet(ob.FindStatementsFromUsedAsObjectBySubjectAndVerb(null, vb));
		}
	} else {
		for ( int i = 0; i < sbV.size (); i++ ) {
			DMIElem sb = sbV.ElementAt(i);
			if ( sb != null ) {
				for ( int j = 0; j < obV.size (); j++ ) {
					DMIElem ob = obV.ElementAt(j);
					if ( ob != null ) {
						ans.MergeListIntoSet(sb.FindStatementsFromUsedAsSubjectByVerbAndObject(vb, ob));
						ans.MergeListIntoSet(ob.FindStatementsFromUsedAsObjectBySubjectAndVerb(sb, vb));
					}
				}
			}
		}
	}

	return ans;
}
*/
