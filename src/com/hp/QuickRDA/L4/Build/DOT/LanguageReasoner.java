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

package com.hp.QuickRDA.L4.Build.DOT;

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L3.Inferencing.*;
import com.hp.QuickRDA.L4.Build.BuildOptions;
import com.hp.QuickRDA.L4.Build.BuildUtilities;

import java.io.PrintStream;

public class LanguageReasoner {

	private DMIGraph				itsGraph;																			// Used only for GetName and GetDescription...
	private BuildOptions			itsOptions;
	private DMIBaseVocabulary		itsBaseVocab;
	private DMIView					itsVW;
	private DMIView					itsVWGray;																			// if in this set then graph the as grayed-out

	private PrintStream				itsDOTOutput;

	private String					itsLKPath;
	// private ProvenanceInfo []		itsLinkBacks;
	private ISet<ProvenanceInfo>	itsLinkBacks;

	private int						itsXTBase;
	private DiagrammingAdornment []	itsXDiagAdornment;

	private ISet<DMIElem>			itsOffViewNodes	= new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

	public LanguageReasoner ( DMIGraph gx1, BuildOptions opt1, DMIView vw1, DMIView vwGray1, boolean zgrv, String linkBackPath1 ) {
		itsGraph = gx1;
		itsBaseVocab = gx1.itsBaseVocab;
		itsOptions = opt1;
		itsLKPath = linkBackPath1;
		itsVW = vw1;
		itsVWGray = vwGray1;
		itsOptions.gOptionZGRViewer = zgrv;

		// itsLinkBacks = new ProvenanceInfo [ 0 ];
		itsLinkBacks = new XSetList<ProvenanceInfo> ( XSetList.AsSet, XSetList.HashOnDemand );

		itsXTBase = itsGraph.getMinIndex ();
		int u = itsGraph.getMaxIndex () - itsXTBase;
		itsXDiagAdornment = new DiagrammingAdornment [ u ];
	}

	@SuppressWarnings("unused")
	public void analyzeForDOT () {
		if ( (!BuildOptions.gOptionShowAllAssignedTos) && itsOptions.gOptionAutoHide ) {
			RDA.suppressAssignedTos ( itsGraph, itsBaseVocab, itsVW );
		}

		makeDiagrammaticParentChoices ();
		containmentAnalysis ();
		environmentAnalysis ();
		referenceAnalysis ();
		nameConceptSet ();
	}

	private void containmentAnalysis () {
		ISet<DMIElem> qV = itsVW.toSet ();

		// Determine the environment for the main item itself
		for ( int i = 0; i < qV.size (); i++ ) {

			DMIElem m = qV.get ( i );
			if ( m != null ) {
				DiagrammingAdornment dda = getAdornment ( m );
				DiagrammingAlias a = dda.getPrimaryAlias ();

				boolean useSecondaryAliasesForAttachment = true;
				boolean useSecondaryAliasesForGrouping = true;

				if ( m.isAttachedToOnce () ) {
					useSecondaryAliasesForAttachment = false;
					DMIElem n = m.attachedToSet ().get ( 0 ); // probably should have an "OnlyElement"...
					getAdornment ( n ).getPrimaryAlias ().addAsAttachment ( a );
				} else if ( m.isGroupedToOnce () ) {
					useSecondaryAliasesForGrouping = false;
					DMIElem n = m.groupedToSet ().get ( 0 ); // probably should have an "OnlyElement"...
					getAdornment ( n ).getPrimaryAlias ().addAsGroupment ( a );
				} else {
					dda.itsIsRoot = true;
				}

				if ( useSecondaryAliasesForAttachment ) {
					ISet<DMIElem> s = m.attachedToSet ();
					for ( int j = 0; j < s.size (); j++ ) {
						DMIElem n = s.get ( j );
						if ( n != null ) {
							DiagrammingAdornment dd = getAdornment ( n );
							DiagrammingAlias ax = dda.getSecondaryAlias ( n, true, dd );
							dd.getPrimaryAlias ().addAsAttachment ( ax );
						}
					}
				}

				if ( useSecondaryAliasesForGrouping ) {
					ISet<DMIElem> s = m.groupedToSet ();
					for ( int j = 0; j < s.size (); j++ ) {
						DMIElem n = s.get ( j );
						if ( n != null ) {
							// if it's a statement, only use secondary aliases when not /egoff
							if ( !m.isStatement () || !itsOptions.gOptionIgnoreEdgeEnv ) {
								DiagrammingAdornment dd = getAdornment ( n );
								DiagrammingAlias ax = dda.getSecondaryAlias ( n, false, dd );
								dd.getPrimaryAlias ().addAsGroupment ( ax );
							}
						}
					}
				}

			}
		}

		// Ensure "reference target" for attaching and grouping arrows, so we can attach or group to something
		// This means that these arrows will get a (mini) node (only not so mini in these cases).
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				DiagrammingAdornment dda = getAdornment ( m );
				DiagrammingAlias a = dda.getPrimaryAlias ();
				if ( m.isStatement () && (a.isGrouping () || a.isAttaching ()) ) {
					dda.forceReferenceTargetAlias ();
				}
			}
		}
	}

	private void environmentAnalysis () {
		ISet<DMIElem> qV = itsVW.toSet ();

		// Determine the environment for the main item itself
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				DiagrammingAdornment a = getAdornment ( m );
				a.getPrimaryAlias ().itsEnvironment = computeEnvironment ( m );
			}
		}
	}

	private DMIElem computeEnvironment ( DMIElem m ) {
		DMIElem env = null;
		if ( m.isAttachedToOnce () )
			env = computeEnvironment ( m.attachedToSet ().get ( 0 ) );
		else if ( m.isGroupedToOnce () )
			env = m.groupedToSet ().get ( 0 );
		return env;
	}

	private void referenceAnalysis () {
		ISet<DMIElem> qV = itsVW.toSet ();

		// Determine the environment for the main item itself
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && m.isStatement () ) {
				DiagrammingAdornment a = getAdornment ( m );
				DiagrammingPrimary p = a.getPrimaryAlias ();
				DMIElem env = a.getEnvironment ();
				DiagrammingAdornment ea = null;
				if ( env != null )
					ea = getAdornment ( env );

				p.itsSubjectAlias = referenceTarget ( m.itsSubject, env, ea );
				p.itsObjectAlias = referenceTarget ( m.itsObject, env, ea );

				// for statements, /egoff says we don't need accurate placement using mini node
				if ( !itsOptions.gOptionIgnoreEdgeEnv &&
						(p.itsSubjectAlias.itsEnvironment != p.itsObjectAlias.itsEnvironment || p.itsSubjectAlias.itsEnvironment != env) ) {
					a.forceReferenceTargetAlias ();
				}
			}
		}
	}

	private DiagrammingAlias referenceTarget ( DMIElem m, DMIElem env, DiagrammingAdornment a ) {
		boolean offView = !itsVW.inView ( m );
		if ( offView )
			itsOffViewNodes.addToSet ( m );

		DiagrammingAdornment da = getAdornment ( m );
		/*
		if ( da.itsIsValue )
			return da.makeValueRefTarget ( env );
		*/

		return da.getReferenceTargetAlias ( env, a, offView );
	}

	private void generateOffViewNodes () {
		for ( int i = 0; i < itsOffViewNodes.size (); i++ ) {
			DMIElem m = itsOffViewNodes.get ( i );
			genOffViewNode ( m, BuildOptions.gOffViewMiniNodeColor );
		}
	}

	private void makeDiagrammaticParentChoices () {
		ISet<DMIElem> qV = itsVW.toSet ();

		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );

			if ( m != null && !m.isStatement () /* m.IsSimpleConcept() */) {
				assignDiagrammaticParentForConcept ( m );
			}
		}

		//for concepts, we're going to choose the most specialized type as the diagrammatic parent
		//as long as there are no conflicts with that notion, and, if there are, then
		//we'll resolve them via their priority
		//so, first, let's produce a narrowed list of conflicting candidates.
		//then choose the highest among them

		//} else if ( Specializes(t, m.itsDiagParent)  ) {

		//for statements, we'll just choose the verb
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && m.isStatement () )
				setDiagParent ( m, bestOf ( getAdornment ( m ).itsDiagParent, m.itsVerb ) );
		}
	}

	private void assignDiagrammaticParentForConcept ( DMIElem m ) {
		DMIElem candidate = m;
		if ( candidate.itsDiagrammingInfo == null ) {
			ISet<DMIElem> cV = m.itsFullTypeSet;
			ISet<DMIElem> iV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

			if ( cV.size () > 1 ) {
				ISet<DMIElem> jV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
				for ( int i = 0; i < cV.size (); i++ ) {
					DMIElem t = cV.get ( i );
					// if we're diagramming domain model, then t having diagramming info is relevant to the extent that t is in the domain metamodel
					if ( t.itsDiagrammingInfo != null ) {
						if ( t.itsSubgraph.isAccessible ( DMISubgraph.SubgraphLevelEnum.kDomainModel ) ) {
							iV.addToSet ( t );
						} else {
							jV.addToSet ( t );
						}
					}
				}

				if ( iV.size () == 0 )
					iV = jV;

				boolean obviated = false;
				for ( ;; ) {
					boolean lp = false;
					if ( iV.size () > 1 ) {
						for ( int i = 0; i < iV.size (); i++ ) {
							DMIElem typCand = iV.get ( i );

							obviated = true;

							for ( int j = 0; j < iV.size (); j++ ) {
								if ( j != i ) {
									DMIElem n = iV.get ( j );
									if ( n != null && !n.specializes ( typCand ) ) {
										obviated = false;
										break;
									}
								}
							}

							if ( obviated ) {
								iV.clear ( i );
								iV.trim ();
								lp = true;
								break;
							}
						}
					}
					if ( lp )
						continue;
					break;
				}
			}

			int priority;
			priority = -1;

			for ( int i = 0; i < iV.size (); i++ ) {
				DMIElem t = iV.get ( i );
				if ( t != null && t.itsDiagrammingInfo != null ) {
					if ( t.itsDiagrammingInfo.itsPriority > priority ) {
						candidate = t;
						priority = t.itsDiagrammingInfo.itsPriority;
					}
				}
			}
		}
		setDiagParent ( m, candidate );
	}

	/*
	 * If a relationship has secondary aliases, then it's going to need a mini-node for dotted line linkage
	 * 	The reference target could be:
	 * 		the primary (common case of nodes)
	 * 		none (common case of relationship)
	 * 		a mini node (common case of relations)
	 * 		a fake node (common case of groups)
	 * 		a replica (secondary alias) within a group of same env as reference
	 * 	
	 */

	private void nameConceptSet () {
		IList<DMIElem> qV = itsVW.toSet ();
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );

			if ( m != null ) {
				DiagrammingAdornment dda = getAdornment ( m );
				dda.issueAliases ();
			}
		}

		for ( int i = 0; i < itsOffViewNodes.size (); i++ ) {
			DMIElem m = itsOffViewNodes.get ( i );
			getAdornment ( m ).getPrimaryAlias ().itsGraphID = java.lang.Integer.toString ( m.itsIndex );
		}
	}

	// String filePath, String filePrefix, String fileSuffix, 
	public void generateDOTFile ( PrintStream ps, String filePrefix, String diagramLabel, String quickRDAVers ) {
		//On Error GoTo 9999
		itsDOTOutput = ps;

		String graph;
		if ( BuildOptions.DOTdigraph ) {
			graph = "digraph";
		} else {
			graph = "graph";
		}

		itsDOTOutput.println ( graph + " \"" + NameUtilities.escapeTextForHTMLCommon(diagramLabel) + " (" + quickRDAVers + ")\" {" );

		if ( itsOptions.gGVOpts != null )
			itsDOTOutput.println ( itsOptions.gGVOpts );

		itsDOTOutput.println ( "compound=true" );

		if ( !itsOptions.gOptionGraphDirectionTB ) {
			itsDOTOutput.println ( "rankdir=LR" );
		}
		if ( !"".equals ( diagramLabel ) ) {
			itsDOTOutput.println ( "label=\"" + diagramLabel + "\" fontsize=24 labelloc=t" );
		}

		itsDOTOutput.println ( "node [ " + itsOptions.gNodeFontAndSize + " ]" );
		itsDOTOutput.println ( "edge [ " + itsOptions.gEdgeFontAndSize + " ]" );
		//fileNum.println("pad=0.35 ranksep=0.75 nodesep=0.5 searchsize=90000"
		//fileNum.println("pad=0.35 ranksep=1 nodesep=0.75 searchsize=90000"
		//fileNum.println("pad=0.35 ranksep=1.25 nodesep=1 searchsize=1800000"
		itsDOTOutput.println ( "pad=0.35 " + itsOptions.gOptionRankFileSep + " searchsize=1800000 remincross=true" );
		//fileNum.println("pad=0 ranksep=1 nodesep=0.75 searchsize=90000"
		//fileNum.println("subgraph cluster_legend { style=filled; fillcolor=lightgrey; label=Legend labelloc=b; 120000; 120001; 220000; 320000 }"

		minMaxOptions ();
		printGraph ();
		generateOffViewNodes ();
		linkSecondaries ();

		itsDOTOutput.println ( "}" );

		TextFile.closeTheFile ( itsDOTOutput );
	}

	@SuppressWarnings("all")
	private void minMaxOptions () {
		if ( BuildOptions.gOptionMin == 2 ) {
			itsDOTOutput.println ( "\"0\" [style=invis,label=\"\"]" );
			itsDOTOutput.println ( "{ rank=min; \"0\"; }" );
		}
		if ( BuildOptions.gOptionMax == 2 ) {
			itsDOTOutput.println ( "\"1\" [style=invis,label=\"\"]" );
			itsDOTOutput.println ( "{ rank=max; \"1\"; }" );
		}
	}

	private static class ValueComparator extends DMIElem.Comparator {
		// reverse sort by metamodel depth: show underlying metamodel items after domain model items.
		// otherwise (when equal) leave order as is, so the dropdowns appear in the order declared in the metamodel.
		@SuppressWarnings("deprecation")
		public boolean follows ( DMIElem e1, DMIElem e2 ) {
			java.util.Date d1 = new java.util.Date ( e1.value () );
			java.util.Date d2 = new java.util.Date ( e2.value () );
			return d1.before ( d2 );
		}
	}

	private static DMIElem.Comparator	cmp	= new ValueComparator ();

	private void printGraph () {
		ISet<DMIElem> sV = itsVW.toSet ();

		/*
		if ( itsVW.InView(itsBaseVocab.gTimeline) ) {
			itsDOTOutput.println("subgraph {");
		}
		*/

		ISet<DMIElem> siV = itsBaseVocab.gTimelineDate.itsFullInstanceSet;
		IList<DMIElem> lstV = new XSetList<DMIElem> ( XSetList.HashOnDemand );
		for ( int j = 0; j < siV.size (); j++ ) {
			DMIElem n = siV.get ( j );
			if ( n != null && itsVW.inView ( n ) ) {
				lstV.addToList ( n );
			}
		}

		lstV.sortList ( cmp );

		if ( lstV.size () > 0 ) {
			//-----------------------
			itsDOTOutput.println ( "subgraph cluster_" + itsBaseVocab.gTimelineDate.itsIndex + " { ranksep=\"0.3\" rank=same" );
			itsDOTOutput.println ( "label=\"" + /* DOTEscapeText(NameUtilities.FirstName(m, itsGraph, false, false)) + */"\"" );
			for ( int j = 0; j < lstV.size (); j++ ) {
				DMIElem n = lstV.get ( j );
				DiagrammingAdornment dda = getAdornment ( n );
				if ( !dda.itsIsValue )
					printRootNode ( n, dda );
			}
			itsDOTOutput.println ( "}" );
			if ( lstV.size () > 1 ) {
				DMIElem n0 = lstV.get ( 0 );
				for ( int j = 1; j < lstV.size (); j++ ) {
					DMIElem n1 = lstV.get ( j );
					itsDOTOutput.println ( "" + n1.itsIndex + "->" + n0.itsIndex + " [ style=invis ]" );
					n0 = n1;
				}
			}

			//-----------------------
			//						DiagrammingAlias [] aV = null;
			//						for ( int j = 0; j < lstV.size (); j++ ) {
			//							DMIElem n = lstV.ElementAt(j);
			//							if ( n != null && itsVW.InView(n) ) {
			//								DiagrammingAdornment dda = GetAdornment(n);
			//								aV = DiagrammingAlias.addToList (dda.GetPrimaryAlias(), aV);
			//							}
			//						}
			//						NodeFormatInfo x = new NodeFormatInfo();
			//						SetNodeValues(x);
			//						String lab = AttachLabel(aV, "", x, "");
			//						itsDOTOutput.println(m.itsIndex + " [ label=\"<" + lab + "\"> ]");
			//-----------------------

		}

		for ( int i = 0; i < sV.size (); i++ ) {
			DMIElem m = sV.get ( i );
			if ( m != null && !m.isStatement () /* m.IsSimpleConcept() */) {
				DiagrammingAdornment dda = getAdornment ( m );
				if ( dda.itsIsRoot && (!dda.itsIsValue || dda.itsIsRelationTarget) )
					if ( !dda.itsIsValue )
						printRootNode ( m, dda );
			}
		}

		for ( int i = 0; i < sV.size (); i++ ) {
			DMIElem m = sV.get ( i );
			if ( m != null && m.isStatement () ) {
				DiagrammingAdornment dda = getAdornment ( m );
				/* if ( dda.itsIsRoot ) */
				printRootEdge ( m, dda );
			}
		}

		//		if ( itsVW.InView(itsBaseVocab.gTimeline) ) {
		//			// itsDOTOutput.println(" 0 [ style=invis ] } ");
		//			// itsDOTOutput.println(" subgraph { " + itsBaseVocab.gTimeline.itsIndex + " 1 [ style=invis ] rank=sink }");
		//			// itsDOTOutput.println("0->1");
		//			itsDOTOutput.println(" subgraph { " + itsBaseVocab.gTimeline.itsIndex + " rank=sink }");
		//		}
	}

	private static class FormatInfo {
		String	aType;
		String	AName;
		String	shapeColor;
		String	borderColor;
		String	fontColor;
		String	urlStr;
	}

	private static class NodeFormatInfo extends FormatInfo {
		String	theShape;
		// String wrapStr;
		boolean	stack;
		@SuppressWarnings("unused")
		boolean	wrap;
		String	styleStr;
		String	fontStr;
	};

	private static class LabelFormatInfo extends FormatInfo {
		String	dir;
		boolean	swapSbOb;
		String	edgeStr;
		String	lab;
		String	arrowStr;
		String	fontName;
	}


	private void setNodeValues ( DMIElem elm, String graphID, DMIElem parIn, NodeFormatInfo x ) {
		setNodeValues ( elm, graphID, parIn, x, false );
	}
	
	private void setNodeValues ( DMIElem elm, String graphID, DMIElem parIn, NodeFormatInfo x, boolean hasArrowColoring ) {
		DMIElem par;
		DMIElementDiagrammingInfo pd = null;
		if ( parIn == null ) {
			par = null;
		} else {
			par = parIn;
			pd = par.itsDiagrammingInfo;
		}

		pd = DMIElementDiagrammingInfo.readAccessTo ( pd );

		if ( itsOptions.gOptionObfuscate == 1 ) {
			x.aType = "hello";
			x.AName = graphID;
		} else {
			if ( par == null ) {
				x.aType = "Concept";
			} else {
				if ( par != elm ) {
					if ( pd.itsDisplay != null && !"".equals ( pd.itsDisplay ) ) // ELE 1/25/2013, take display name for type when available
						x.aType = pd.itsDisplay;
					else
						x.aType = NameUtilities.firstName ( par, itsGraph, false, false );
				} else {
					ISet<DMIElem> z = elm.itsDeclaredTypeList;
					if ( z.size () > 0 ) {
						x.aType = NameUtilities.firstName ( z.get ( 0 ), itsGraph, false, false );
					}
				}
			}
			if ( elm == null ) {} else {
				if ( elm.isStatement () ) { // ELE 1/25/2013, give arrows empty name
					x.AName = "";
				}
				else {
					if ( itsOptions.gOptionTrimDiff ) {
						x.AName = NameUtilities.trimDifferentiator ( NameUtilities.firstName ( elm, itsGraph, false, false ) );
					} else {
						x.AName = NameUtilities.firstName ( elm, itsGraph, false, false );
					}
				}
			}
		}

		// x.wrapStr = "";
		x.stack = false;
		x.wrap = false;
		if ( "circle".equals ( pd.itsShape ) || "doublecircle".equals ( pd.itsShape ) || "tripleoctagon".equals ( pd.itsShape ) ) {
			x.stack = true;
			if ( elm == null || elm.attachedSet ().size () == 0 ) {
				x.AName = wrapText ( x.AName );
				x.wrap = true;
			}
		}

		if ( !x.stack && attachStackedOption ( elm ) )
			x.stack = true;

		//if ( elm != null ) {
		//    if ( elm.itsDerived != "" ) {
		//        AName = "(" + elm.itsDerived + ") " + AName
		//    }
		//}
		//AName = DOTEscapeText(AName)

		if ( itsOptions.gOptionObfuscate == 2 ) {
			x.aType = "hello";
			x.AName = mangle ( x.AName );
		}

		x.styleStr = "filled,rounded";

		boolean gry = false;
		if ( itsVWGray != null ) {
			if ( itsVWGray.inView ( elm ) ) {
				gry = true;
				x.fontStr = " fontname=\"" + BuildOptions.gGreyedNodeLabelFont + "\" fontColor=\"" + BuildOptions.gGreyedNodeLabelColor + "\" ";
			}
		}

		if ( pd.itsShape == null || "".equals ( pd.itsShape ) ) {
			x.theShape = "diamond";
			x.shapeColor = BuildOptions.gErrorColor; // "red"
			x.borderColor = "black";
			x.fontColor = null;
		} else {
			if ( "rbox".equals ( pd.itsShape ) ) { // rbox == rectangular box
				x.theShape = "box";
				x.styleStr = "filled"; // filled without rounded
			} else {
				x.theShape = pd.itsShape;
			}
			if ( pd.itsColor == 0 ) {
				x.shapeColor = BuildOptions.gErrorColor; // "red"
			} else {
				if ( gry ) {
					x.shapeColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsColor, BuildOptions.gGreyedNodeBrightnessFactor ) ) );
				} else if ( hasArrowColoring ) {
					x.shapeColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsColor, 1d / BuildOptions.gLineBrightnessFactor ) ) );
				} else {
					x.shapeColor = "#" + myHex ( convertBGRtoRGB ( pd.itsColor ) );
				}
			}
		}

		if ( pd.itsTextColor != 0 && pd.itsTextColor != 16777215 ) {
			if ( gry ) {
				x.fontColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsTextColor, BuildOptions.gGreyedNodeBrightnessFactor ) ) );
			} else {
				x.fontColor = "#" + myHex ( convertBGRtoRGB ( pd.itsTextColor ) );
			}
		}

		if ( "doublecircle".equals ( pd.itsShape ) || "tripleoctagon".equals ( pd.itsShape ) ) {
			x.borderColor = "black";
			//} else if ( "box".equals(pd.itsShape) ) {
			//    borderColor = "white"
		} else {
			x.borderColor = x.shapeColor;
		}

		if ( "doublecircle".equals ( x.theShape ) ) {
			x.theShape = "circle";
		} else if ( "tripleoctagon".equals ( x.theShape ) ) {
			x.theShape = "doublecircle";
			x.borderColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsColor, BuildOptions.gLineBrightnessFactor * BuildOptions.gLineBrightnessFactor ) ) );
		}

		DMIElem pe = elm;
		if ( pe == null )
			pe = par;

		String xURLStr = composeLinkBackURL ( pe );
		String desc = NameUtilities.getDescriptionFor ( pe, itsGraph );
		if (desc == null) desc = "ToolTip";

		x.urlStr = "";

		if ( !"".equals ( xURLStr ) ) {
			x.urlStr = "URL=\"" + escapeTextForDOTTooltip ( xURLStr ) + "\" ";
		}
		if ( !"".equals ( desc ) ) {
			x.urlStr += "tooltip=\"" + escapeTextForDOTTooltip ( desc ) + "\" ";
		} else {
			x.urlStr += "tooltip=\" \" ";
		}
	}

	private void printRootNode ( DMIElem m, DiagrammingAdornment dda ) {
		DiagrammingPrimary p = dda.getPrimaryAlias ();
		NodeFormatInfo x = new NodeFormatInfo ();
		setNodeValues ( m, p.itsGraphID, dda.itsDiagParent, x );
		printAlias ( m, p, x, null );
	}

	private void printSecAlias ( DMIElem m, DiagrammingAdornment dda, DiagrammingAlias s ) {
		NodeFormatInfo x = new NodeFormatInfo ();
		setNodeValues ( m, s.itsGraphID, dda.itsDiagParent, x );
		printAlias ( m, s, x, null );
	}

	private void printAlias ( DMIElem m, DiagrammingAlias a, NodeFormatInfo x, NodeFormatInfo parX ) {
		boolean hasGroupCount = a.getGroupCount () > 0;

		a.itsWasGenerated = true;

		if ( a.getAttachCount () > 0 ) {
			if ( showTypeOption ( m ) ) {
				// x.AName += " [" + x.aType + "]";
				x.AName += "\r\n[" + x.aType + "]";
			}
			x.AName = "<" + attachLabel ( a, x.AName, x, null ) + ">";
		} else {
			if ( showTypeOption ( m ) )
				x.AName += (hasGroupCount ? " " : "\\n") + "[" + x.aType + "]";
			x.AName = "\"" + esacpeTextForDOT ( x.AName ) + "\"";
		}

		String content;
		if ( hasGroupCount ) {
			content = "subgraph cluster_" + a.itsGraphID + " { ";
			if ( a.hasReferenceTarget () )
				content += a.getReferenceTarget ().itsGraphID + " [ label=\" \" style=invis width=0 height=0 ] ";
			content += "label=" + x.AName;
		} else {
			content = a.itsGraphID + " [ " + "label=" + x.AName;
		}

		content += " " + "shape=\"" + x.theShape + "\" " + "fillcolor=\"" + x.shapeColor + "\" ";

		if ( x.fontColor != null )
			content += "fontcolor=\"" + x.fontColor + "\" ";

		if ( m.itsBoldTag ) {
			// "color=""red"" "
			content += "color=\"" + BuildOptions.gHighlightColor + "\" "
					+ "style=\"" + x.styleStr 
					+ "\" penwidth=\"" + BuildOptions.gHighlightLineWidth + "\" "; // style&=setlinewidth(4),fontsize=14
		} else {
			// special coloring for the border of nested items so that nested items can be seen better
			if ( a.getGroupCount () == 0 ) {
				if ( parX != null && parX.shapeColor.equals ( x.borderColor ) )
					content += "color=\"black\" ";
				else
					content += "color=\"" + x.borderColor + "\" ";
			}

			if ( itsOptions.gOptionZGRViewer ) {
				content += "style=\"" + x.styleStr + ",setlinewidth(0)\" "; // style&=""setlinewidth(-1)"", fontname=""Times-Roman""
			} else {
				content += "style=\"" + x.styleStr + ",setlinewidth(-1)\" "; // style&=""setlinewidth(-1)"", fontname=""Times-Roman""
			}
			//& "color=""" + borderColor + """," _
			//& "style=""filled,rounded"",penwidth=""-1""" ' style&=""setlinewidth(-1)"", fontname=""Times-Roman""
			//& "color=""" + shapeColor + """," _
			//& "style=""filled,rounded"",penwidth=""-1""" ' style&=""setlinewidth(-1)"", fontname=""Times-Roman""
		}
		if ( x.fontStr != null && !"".equals ( x.fontStr ) ) {
			content += x.fontStr;
		}

		if ( a.getGroupCount () > 0 ) {
			boolean nestedGroup = false;
			List<DiagrammingAlias> theGroup = a.getGroup ();
			//for ( int i = 0; i < theGroup.length; i++ ) {
			//	DiagrammingAlias e = theGroup [ i ];
			for ( DiagrammingAlias e : theGroup ) {
				if ( getAdornment ( e.itsConcept ).itsOffView )
					content += " " + e.itsGraphID;
				else
					nestedGroup = true;
			}
			if ( nestedGroup ) {
				content += x.urlStr;
				itsDOTOutput.println ( content );
				//for ( int i = 0; i < theGroup.length; i++ ) {
				//	DiagrammingAlias e = theGroup [ i ];
				for ( DiagrammingAlias e : theGroup ) {
					DMIElem n = e.itsConcept;
					DiagrammingAdornment dda = getAdornment ( n );
					if ( !dda.itsOffView ) {
						if ( n.isStatement () ) {
							// DOTDiagrammingPrimary q = dda.GetPrimaryAlias();
							LabelFormatInfo y = new LabelFormatInfo ();
							setEdgeValues ( n, e.itsGraphID, dda.itsDiagParent, y );
							printEdgeAlias ( n, e, y );
							// PrintPrimaryEdge(n, q, y, true);
						} else {
							if ( !(e instanceof DiagrammingPrimary) || !dda.getPrimaryAlias ().itsSuppress ) {
								// Suppressed in case of Value copies that are referred to
								NodeFormatInfo y = new NodeFormatInfo ();
								setNodeValues ( n, e.itsGraphID, dda.itsDiagParent, y );
								printAlias ( n, e, y, x );
							}
						}
					}
				}
				content = "}";
				itsDOTOutput.println ( content );
			} else {
				content += x.urlStr + "}";
				itsDOTOutput.println ( content );
				/*
				for ( int i = 0; i < theGroup.length; i++ ) {
					DOTDiagrammingAlias e = theGroup[i];
					if ( ! (e instanceof DOTDiagrammingPrimary) ) {
						DMIElem n = e.itsConcept;
						PrintSecondary(n, GetAdornment(n), true);
					}
				}
				*/
			}
		} else {
			content += x.urlStr + "]";
			itsDOTOutput.println ( content );
		}
	}

	private void linkSecondaries () {
		ISet<DMIElem> sV = itsVW.toSet ();

		for ( int i = 0; i < sV.size (); i++ ) {
			DMIElem m = sV.get ( i );

			DiagrammingAdornment dda = getAdornment ( m );
			if ( dda.itsIsValue /* && !da.itsIsRelationTarget */) {
				List<DiagrammingAlias> sec = dda.getSecondaries ();
				if ( sec != null ) {
					for ( DiagrammingAlias x : sec ) {
						if ( !x.itsWasGenerated )
							printSecAlias ( m, dda, x );
					}
				}
				/*
				List<DiagrammingAlias> sec = da.getSecondaries ();
				for ( DiagrammingAlias x : sec ) {
					NodeFormatInfo n = new NodeFormatInfo ();
					setNodeValues ( m, x.itsGraphID, da.itsDiagParent, n );
					printAlias ( m, x, n, null );
				}
				*/
			}
			else {
				DiagrammingPrimary p = dda.getPrimaryAlias ();
				/*
				DiagrammingAdornment db = getAdornment ( p.itsConcept );
				assert da == db : "da != db";
				// not sure why we fetch db from p; must be historical.
				List<DiagrammingAlias> sec = db.getSecondaries ();
				*/
				List<DiagrammingAlias> sec = dda.getSecondaries ();
				if ( sec != null ) {
					//for ( int j = 0; j < sec.length; j++ ) {
					for ( DiagrammingAlias x : sec ) {
						String trg = p.itsGraphID;
						if ( p.hasReferenceTarget () )
							trg = p.getReferenceTarget ().itsGraphID;
						String content = x.itsGraphID + "->" + trg + " [ style=\"" + BuildOptions.gVisualReplicaLineStyle + "\" arrowhead=\"ovee\" color=\"" + BuildOptions.gVisualReplicaEdgeColor + "\" weight=\"" + BuildOptions.gVisualReplicaWeight + "\" ";

						if ( BuildOptions.gUseBlankVisualReplicaEdgeLabel )
							content += "label=\" \" ";
						else if ( BuildOptions.gUseVisualReplicaEdgeLabel )
							content += "label=\"" + BuildOptions.gVisualReplicaEdgeLabel + "\" fontcolor=\"" + BuildOptions.gVisualReplicaEdgeFontColor + "\" fontname=\"" + BuildOptions.gVisualReplicaEdgeLabelFont + "\" ";
						content += "]";
						itsDOTOutput.println ( content );
					}
				}
			}
		}
	}

	private String attachLabel ( DiagrammingAlias a, String n, NodeFormatInfo parX, NodeFormatInfo atX ) {
		a.itsWasGenerated = true;
		return attachLabel ( a, n, parX, atX, 0 );
	}

	private String attachLabel ( DiagrammingAlias a, String n, NodeFormatInfo parX, NodeFormatInfo atX, int level ) {
		a.itsWasGenerated = true;
		return attachLabel ( a.getAttach (), n, parX, atX, NameUtilities.getDescriptionFor ( a.itsConcept, itsGraph ), level );
	}

	private String attachLabel ( List<DiagrammingAlias> aV, String n, NodeFormatInfo parX, NodeFormatInfo atX, String desc, int level ) {
		String ans = "<table align=\"center\" valign=\"middle\" border=\"0\" cellborder=\"0\" cellspacing=\"3\" cellpadding=\"2\">";

		if ( aV.size () == 0 ) {
			ans += "<tr><td align=\"center\" valign=\"middle\" border=\"0\">";
			if ( level > 0 )
				ans += "<font point-size=\"" + (itsOptions.gNodeFontSize - level) + "\">";
			ans += NameUtilities.escapeTextForHTML ( n );
			if ( level > 0 )
				ans += "</font>";
			ans += "</td></tr></table><tr>";
		} else {
			boolean stacked = parX.stack;

			if ( stacked ) {
				ans += "\r\n\t<tr><td align=\"center\" valign=\"middle\" border=\"0\">";
				if ( level > 0 )
					ans += "<font point-size=\"" + (itsOptions.gNodeFontSize - level) + "\">";
				ans += NameUtilities.escapeTextForHTML ( n );
				if ( level > 0 )
					ans += "</font>";
				ans += "</td></tr>";
			}
			else
				ans += "<tr>";

			//for ( int i = 0; i < aV.length; i++ ) {
			//	DiagrammingAlias e = aV [ i ];
			for ( DiagrammingAlias e : aV ) {
				NodeFormatInfo x = new NodeFormatInfo ();
				setNodeValues ( e.itsConcept, e.itsPortID, getAdornment ( e.itsConcept ).itsDiagParent, x );

				String nm = x.AName;
				if ( showTypeOption ( e.itsConcept ) )
					nm += " [" + x.aType + "]";

				if ( e.getAttachCount () > 0 )
					nm = attachLabel ( e, nm, parX, x, level + 1 );
				else
					nm = NameUtilities.escapeTextForHTML ( nm );

				e.itsWasGenerated = true;

				if ( stacked )
					ans += "<tr>";

				ans += "\r\n\t" + "<td align=\"center\" valign=\"middle\" port=\"" + e.itsPortID + "\" ";

				if ( atX != null && atX.shapeColor.equals ( x.borderColor ) )
					ans += "color=\"gray\" ";
				else
					ans += "color=\"" + x.borderColor + "\" ";

				if ( x.fontColor != null )
					ans += "fontcolor=\"" + x.fontColor + "\" ";

				ans += " bgcolor=\"" + x.shapeColor + "\" ";

				String xURLStr = composeLinkBackURL ( e );
				if ( xURLStr != null && !"".equals ( xURLStr ) ) {
					ans += "href=\"" + NameUtilities.escapeTextForHTMLCommon ( xURLStr ) + "\" ";
				}
				if ( desc != "" ) {
					ans += "tooltip=\"" + NameUtilities.escapeTextForHTMLCommon ( desc ) + "\" ";
				} else {
					ans += "tooltip=\" \" ";
				}
				ans += ">";
				if ( e.getAttachCount () == 0 && level + 1 > 0 )
					ans += "<font point-size=\"" + (itsOptions.gNodeFontSize - (level + 1)) + "\">";
				ans += nm;
				if ( e.getAttachCount () == 0 && level + 1 > 0 )
					ans += "</font>";
				ans += "</td>";

				if ( stacked )
					ans += "</tr>";
			}

			if ( stacked )
				ans += "\r\n\t</table>";
			else {
				ans += "</tr>\r\n\t<tr><td align=\"center\" valign=\"middle\" colspan=\"" + aV.size () + "\" border=\"0\">";
				if ( level > 0 )
					ans += "<font point-size=\"" + (itsOptions.gNodeFontSize - level) + "\">";
				ans += NameUtilities.escapeTextForHTML ( n );
				if ( level > 0 )
					ans += "</font>";
				ans += "</td></tr></table>";
			}
		}
		/*
		if ( ac > 0 ) {
			ans += "</tr><tr><td colspan=\"" + ac + "\" border=\"0\">" + HTMLEscapeText(n) + "</td></tr></table>";
		} else {
				    <tr><td colspan=\"" + ac + "\" border=\"0\">" + HTMLEscapeText(n) + "</td></tr>
			ans += "<tr><td border=\"0\">" + HTMLEscapeText(n) + "</td></tr></table>";
		}
		*/

		return ans;
	}

	private void setEdgeValues ( DMIElem elm, String graphID, DMIElem parIn, LabelFormatInfo x ) {
		DMIElem par;
		par = parIn;
		DMIElementDiagrammingInfo pd = null;
		if ( par != null ) {
			pd = par.itsDiagrammingInfo;
		}

		pd = DMIElementDiagrammingInfo.readAccessTo ( pd );

		if ( itsOptions.gOptionObfuscate == 1 ) {
			x.aType = "hello";
			//AName = aID
			x.AName = graphID;
		} else {
			//aID = GetAdornment(m).itsGraphID
			if ( par == null ) {
				x.aType = "unknown";
			} else {
				x.aType = esacpeTextForDOT ( NameUtilities.firstName ( par, itsGraph, false, false ) );
			}
			if ( elm != null ) {
				if ( elm.itsVerb == null ) {
					x.AName = esacpeTextForDOT ( NameUtilities.firstName ( elm, itsGraph, false, false ) );
				} else {
					//hoping par == elm.itsVerb here
					//                  if ( true ) {
					x.AName = DMIElementDiagrammingInfo.readAccessTo ( elm.itsVerb.itsDiagrammingInfo ).itsDisplay;
					if ( x.AName == null || "".equals ( x.AName ) ) {
						x.AName = NameUtilities.firstName ( elm.itsVerb, itsGraph, false, false );
					}
					//                  } else {
					//                      x.AName = pd.itsDisplay;
					//                      if ( x.AName == null || "".equals(x.AName) ) {
					//                          x.AName = Utilities_DMI.FirstName(par, gx, false, false);
					//                      }
					//                  }
				}
			}
		}

		if ( itsOptions.gOptionTrimDiff ) {
			x.AName = NameUtilities.trimDifferentiator ( x.AName );
		}

		x.AName = esacpeTextForDOT ( x.AName );

		if ( itsOptions.gOptionObfuscate == 2 ) {
			x.aType = "hello";
			x.AName = mangle ( x.AName );
		} else if ( itsOptions.gOptionObfuscate == 3 ) {
			x.AName = "";
		}

		for ( ;; ) {
			if ( "".equals ( pd.itsShape ) && getAdornment ( par ).itsDiagParent != null ) {
				if ( par != itsBaseVocab.gConcept ) {
					par = getAdornment ( par ).itsDiagParent;
					continue;
				}
			}
			break;
		}

		@SuppressWarnings("unused")
		boolean offView = false;

		if ( pd.itsRanking == null || "".equals ( pd.itsRanking ) ) {
			x.edgeStr = ""; //"minlen=2"
		} else if ( Strings.isNumeric ( pd.itsRanking ) ) {
			x.edgeStr = "minlen=" + (java.lang.Double.parseDouble ( pd.itsRanking ) + 1) + " ";
		} else {
			x.edgeStr = "constraint=false ";
		}

		if ( pd.itsWeight == null || "".equals ( pd.itsWeight ) ) {} else {
			x.edgeStr += "weight=\"" + pd.itsWeight + "\" ";
		}

		if ( BuildOptions.gUseHeadTailLabels ) {
			if ( "forward".equals ( x.dir ) ) {
				x.lab = "taillabel=\"" + x.AName + "\" labelangle=\"15\" ";
			} else {
				x.lab = "headlabel=\"" + x.AName + "\" labelangle=\"5\" ";
			}
		} else {
			x.lab = "label=\"" + x.AName + "\" ";
		}

		x.arrowStr = "arrowhead=\"normal\" ";

		if ( pd.itsDirection == null ) {
			x.dir = "";
		} else if ( "Up".equals ( pd.itsDirection ) ) {
			x.dir = "back";
		} else if ( "Down".equals ( pd.itsDirection ) ) {
			x.dir = "forward";
		} else if ( "UpRev".equals ( pd.itsDirection ) ) {
			x.dir = "back";
			x.swapSbOb = true;
		} else if ( "DownRev".equals ( pd.itsDirection ) ) {
			x.dir = "forward";
			x.swapSbOb = true;
		}

		if ( pd.itsBiDirect == null || "".equals ( pd.itsBiDirect ) ) {
			// x.arrowStr = "";
		} else if ( "Yes".equals ( pd.itsBiDirect ) ) {
			x.arrowStr = "arrowhead=\"normal\" arrowtail=\"normal\"";
			x.dir = "both";
		} else if ( "Open".equals ( pd.itsBiDirect ) ) {
			if ( "Down".equals ( pd.itsDirection ) ) {
				x.arrowStr = "arrowhead=\"normal\" arrowtail=\"odiamond\"";
			} else {
				x.arrowStr = "arrowhead=\"odiamond\" arrowtail=\"normal\"";
				//arrowStr = "arrowhead=""empty"" arrowtail=""normal"""
			}
			x.dir = "both";
		} else if ( "Diam".equals ( pd.itsBiDirect ) ) {
			//
		}

		if ( pd.itsColor == 0 || pd.itsColor == 16777215 || pd.itsShape == null || "".equals ( pd.itsShape ) ) { // 0 == black; 1677215 == white
			x.shapeColor = BuildOptions.gErrorColor; //"red"
			x.fontColor = BuildOptions.gErrorTextColor; //"darkred"
		} else {
			boolean gry = false;
			if ( itsVWGray != null ) {
				if ( itsVWGray.inView ( elm ) )
					gry = true;
			}

			if ( gry ) {
				x.shapeColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsColor, BuildOptions.gGreyedEdgeLineBrightnessFactor ) ) );
				x.fontColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsColor, BuildOptions.gGreyedEdgeLabelBrightnessFactor ) ) );
				x.fontName = BuildOptions.gGreyedEdgeLabelFont;
				//} else if ( offView ) {
				//    shapeColor = "#" + MyHex(BGRtoRGB(FactorBrightness(pd.itsColor, GreyedEdgeLineBrightnessFactor)))
				//    fontColor = "#" + MyHex(BGRtoRGB(FactorBrightness(pd.itsColor, GreyedEdgeLabelBrightnessFactor)))
			} else {
				x.shapeColor = "#" + myHex ( convertBGRtoRGB ( pd.itsColor ) );
				x.fontColor = "#" + myHex ( convertBGRtoRGB ( factorBrightness ( pd.itsColor, BuildOptions.gLineBrightnessFactor ) ) );
			}
		}

		if ( elm != null ) {
			String xURLStr = composeLinkBackURL ( elm );
			if ( xURLStr != null && !"".equals ( xURLStr ) ) {
				x.urlStr = "URL=\"" + esacpeTextForDOT ( xURLStr ) + "\" ";
			}
		}
	}

	private void genOffViewNode ( DMIElem m, String shapeColor ) {
		DiagrammingAlias p = getAdornment ( m ).getPrimaryAlias ();
		genMiniNode ( p, shapeColor, false, false );
	}

	private void genMiniNode ( DiagrammingAlias a, String shapeColor, boolean onView, boolean lrg ) {
		String tt = NameUtilities.getMCText ( a.itsConcept );
		if ( (tt == null || "".equals ( tt )) && a.itsConcept.itsVerb != null )
			tt = NameUtilities.getMCText ( a.itsConcept.itsVerb );
		String content;

		content = a.itsGraphID + " [ " + "label=\"\" " + "shape=\"circle\" " + "fillcolor=\"" + shapeColor + "\" " + "color=\"" + shapeColor + "\" ";

		String lk = composeLinkBackURL ( a );
		if ( lk != null )
			content += "URL=\"" + esacpeTextForDOT ( lk ) + "\" ";
		if ( tt != null )
			content += "tooltip=\"" + escapeTextForDOTTooltip ( tt ) + "\" ";

		if ( onView && (a.itsConcept.itsBoldTag || lrg) ) {
			content += "style=\"filled\" width=\"0.1\" height=\"0.1\" ]";
		} else {
			content += "style=\"filled\" width=\"0.05\" height=\"0.05\" ]";
		}

		itsDOTOutput.println ( content );
	}

	private void printEdgeAlias ( DMIElem m, DiagrammingAlias a, LabelFormatInfo x ) {
		boolean lrg = false;
		if ( a instanceof DiagrammingPrimary ) {
			DiagrammingPrimary p = (DiagrammingPrimary) a;
			if ( p.itsReferenceTarget == null || p.itsMiniNodeAlreadyGenerated )
				return;
			if ( p.itsReferenceTarget != null )
				a = p.itsReferenceTarget;
			p.itsMiniNodeAlreadyGenerated = true;
			lrg = false;
		} else if ( a instanceof DiagrammingSecondaryAlias )
			lrg = true;
		genMiniNode ( a, x.shapeColor, true, lrg );
	}

	private void printRootEdge ( DMIElem m, DiagrammingAdornment dda ) {
		DiagrammingPrimary p = dda.getPrimaryAlias ();
		LabelFormatInfo x = new LabelFormatInfo ();
		setEdgeValues ( m, p.itsGraphID, getAdornment ( m ).itsDiagParent, x );
		printPrimaryEdge ( m, p, x, false, dda );
	}

	private void printPrimaryEdge ( DMIElem m, DiagrammingPrimary p, LabelFormatInfo x, boolean onlyMiniNode, DiagrammingAdornment dda ) {
		if ( p.itsReferenceTarget != null && !p.itsMiniNodeAlreadyGenerated ) {
			// ELE 1/2/2013 this arrow has a "reference target":
			// if it is grouping or attaching, then it gets a full node otherwise a mini node.
			if ( p.isGrouping () || p.isAttaching () ) {
				DiagrammingAlias s = p.itsReferenceTarget;
				dda.getPrimaryAlias ().itsGraphID = s.itsGraphID;
				NodeFormatInfo xx = new NodeFormatInfo ();
				setNodeValues ( m, s.itsGraphID, dda.itsDiagParent, xx, true );
				xx.theShape = "box";
				printAlias ( m, p, xx, null );
			}
			else {
				genMiniNode ( p.itsReferenceTarget, x.shapeColor, true, false );
			}
			p.itsMiniNodeAlreadyGenerated = true;
		}

		if ( onlyMiniNode )
			return;

		String arrow;
		if ( BuildOptions.DOTdigraph ) {
			arrow = "->";
		} else {
			arrow = "--";
		}

		String sbID = p.itsSubjectAlias.itsGraphID;
		String obID = p.itsObjectAlias.itsGraphID;

		assert sbID != null : "LanguageReasoner: subject is null in arrow";
		assert obID != null : "LanguageReasoner: object is null in arrow";

		String subx = null;
		String objx = null;

		if ( p.itsSubjectAlias.isGrouping () ) {
			subx = "ltail=" + sbID + " ";
		}
		if ( p.itsObjectAlias.isGrouping () ) {
			objx = "lhead=" + obID + " ";
		}

		if ( x.swapSbOb ) {
			String t = sbID;
			sbID = obID;
			obID = t;
			t = subx;
			subx = objx;
			objx = t;
		}

		String content;
		if ( p.itsReferenceTarget != null ) {
			//node for arrow that can be referenced externally (and will be here also)
			//                    + "color=""white"","

			if ( Strings.InStr ( 1, x.edgeStr, "constraint=false" ) > 0 ) { //constraint=false
				if ( "forward".equals ( x.dir ) ) {
					content = "subgraph { " + sbID + " " + p.itsReferenceTarget.itsGraphID + " rank=same }";
				} else {
					content = "subgraph { " + obID + " " + p.itsReferenceTarget.itsGraphID + " rank=same }";
				}
				itsDOTOutput.println ( content );
			}

			String firstArrow;
			String secondArrow;
			String firstLab;
			String secondLab;
			String edgeStr1;
			String edgeStr2;

			if ( "forward".equals ( x.dir ) ) {
				firstLab = x.lab + x.urlStr + " ";
				firstArrow = "arrowhead=none ";
				secondLab = x.urlStr + " tooltip=\"" + escapeTextForDOTTooltip ( x.AName ) + "\" "; // urlStr '""
				secondArrow = "arrowhead=normal ";
				edgeStr1 = x.edgeStr;
				edgeStr2 = Strings.Replace ( x.edgeStr, "weight=", "weightx=" );
			} else {
				firstLab = x.urlStr + " tooltip=\"" + escapeTextForDOTTooltip ( x.AName ) + "\" "; // urlStr '""
				firstArrow = "arrowtail=normal ";
				secondLab = x.lab + x.urlStr + " ";
				secondArrow = "arrowtail=none ";
				edgeStr2 = x.edgeStr;
				edgeStr1 = Strings.Replace ( x.edgeStr, "weight=", "weightx=" );
			}

			//begin arrow from real subject to node for arrow
			// #if ( addType ) {
			/*
			content = subID + arrow + GetAdornment(m).itsGraphID + " ["
								+ subx
								+ "type=\"" + aType + "\" "
								+ firstLab
								+ edgeStr1
								+ "dir=\"" + dir + "\" "
								+ "color=\"" + shapeColor + "\" ";
			*/
			// #} else {
			content = sbID + arrow + p.itsReferenceTarget.itsGraphID + " [ "
					+ (subx == null ? "" : subx)
					+ firstLab
					+ edgeStr1
					+ "dir=\"" + x.dir + "\" "
					+ "color=\"" + x.shapeColor + "\" ";
			// #}
			if ( m.itsBoldTag ) {
				// content = content + "fontcolor=""red"" penwidth=""4"" " ' style=""setlinewidth(3)"",fontsize=""12""
				content += "fontcolor=\"" + BuildOptions.gHighlightTextColor
						+ "\" penwidth=\"" + BuildOptions.gHighlightLineWidth
						+ "\" "; // style=""setlinewidth(3)"",fontsize=""12""
			} else {
				content += "fontcolor=\"" + x.fontColor + "\" ";
			}
			if ( x.fontName != null && !"".equals ( x.fontName ) && !"".equals ( firstLab ) ) {
				content += "fontname=\"" + x.fontName + "\" ";
			}
			content += firstArrow + "]";
			itsDOTOutput.println ( content );

			//end arrow from node for arrow to real object for arrow
			// #if ( addType ) {
			/*
			content = GetAdornment(m).itsGraphID + arrow + objID + " ["
								+ objx 
								+ "type=\"" + aType + "\" "
								+ secondLab
								+ edgeStr2
								+ "dir=\"" + dir + "\" "
								+ "color=\"" + shapeColor + "\" ";
			*/
			// #} else {
			content = p.itsReferenceTarget.itsGraphID + arrow + obID + " [ "
					+ (objx == null ? "" : objx)
					+ secondLab
					+ edgeStr2
					+ "dir=\"" + x.dir + "\" "
					+ "color=\"" + x.shapeColor + "\" ";
			// #}
			if ( m.itsBoldTag ) {
				//content = content + "fontcolor=""red"" penwidth=""4"" " ' style=""setlinewidth(3)"",fontsize=""12""
				content += "fontcolor=\"" + BuildOptions.gHighlightTextColor + "\" penwidth=\"" + BuildOptions.gHighlightLineWidth + "\" "; // style=""setlinewidth(3)"",fontsize=""12""
			} else {
				content += "fontcolor=\"" + x.fontColor + "\" ";
			}
			if ( x.fontName != null && !"".equals ( x.fontName ) && !"".equals ( secondLab ) ) {
				content += "fontname=\"" + x.fontName + "\" ";
			}
			content += secondArrow + "]"; //,fontname=""Lucida""
			itsDOTOutput.println ( content );
		} else { //single arrow
			//            if ( false ) { // ddz == ExportFormat.ZDL && "back".equals(dir)  ) {
			//               content = x.objID + arrow + x.subID + " [";
			// #if ( addType ) {
			//              content += " type=\"" + x.aType + "\"";
			// #}
			//            } else {
			content = sbID + arrow + obID + " [ ";
			// #if ( addType ) {
			//              content += " type=\"" + aType + "\"";
			// #}
			//            }

			if ( subx != null )
				content += subx;
			if ( objx != null )
				content += objx;
			if ( x.lab != null )
				content += x.lab;
			if ( x.edgeStr != null )
				content += x.edgeStr;
			if ( x.dir != null )
				content += "dir=\"" + x.dir + "\" ";
			if ( x.shapeColor != null )
				content += "color=\"" + x.shapeColor + "\" ";

			if ( m.itsBoldTag ) {
				//content = content + "fontcolor=""red"" penwidth=""4"" " ' style=""setlinewidth(3)"",fontsize=""12""
				content += "fontcolor=\"" + BuildOptions.gHighlightTextColor + "\" penwidth=\"" + BuildOptions.gHighlightLineWidth + "\" "; // style=""setlinewidth(3)"",fontsize=""12""
			} else {
				content += "fontcolor=\"" + x.fontColor + "\" ";
			}
			if ( x.fontName != null && !"".equals ( x.fontName ) ) {
				content += "fontname=\"" + x.fontName + "\" ";
			}
			if ( x.arrowStr != null )
				content += x.arrowStr;
			if ( x.urlStr != null )
				content += x.urlStr;
			content += "]";

			itsDOTOutput.println ( content );
		}
	}

	//Phase 1 diagrammatic choices, coloring, etc...  'Phase 2 IDs

	//
	// set diagrammatic parent for all nodes (itsDiagParent); diagrammatic parents must have diagramming info (itsDiagrammingInfo)
	//
	private DiagrammingAdornment getAdornment ( DMIElem m ) {
		DiagrammingAdornment dda = itsXDiagAdornment [ m.itsIndex - itsXTBase ];
		if ( dda == null ) {
			dda = new DiagrammingAdornment ( m, itsBaseVocab );
			itsXDiagAdornment [ m.itsIndex - itsXTBase ] = dda;
		}
		return dda;
	}

	private void setDiagParent ( DMIElem m, DMIElem p ) {
		getAdornment ( m ).itsDiagParent = p;
	}

	/*
		private String GraphID(DMIElem m) {
			return itsXGraphID[m.itsIndex - itsXTBase];
		}

		private DMIElem DiagParent(DMIElem m) {
			return itsXDiagParent[m.itsIndex - itsXTBase];
		}

		private void SetGraphID(DMIElem m, String id) {
			itsXGraphID[m.itsIndex - itsXTBase] = id;
		}

		private void SetDiagParent(DMIElem m, DMIElem p) {
			itsXDiagParent[m.itsIndex - itsXTBase] = p;
		}
	*/

	public static String escapeTextForDOTTooltip ( String sIn ) {
		//it's not really HTML, but it's not really DOT either...
		return NameUtilities.escapeTextForHTML ( sIn );
		// return esacpeTextForDOT ( sIn );
		//DOTTooltipEscapeText = URLEncode(sIn, true, false, True)
	}

	public static String esacpeTextForDOT ( String sIn ) {
		return BuildUtilities.makeJSONEscapeText ( sIn );
	}

	
	
	public static String wrapText ( String sIn ) {
		String ans = "";
		String str = sIn;

		for ( ;; ) {
			int p = str.indexOf ( ' ' ) + 1; // Strings.InStr(1, str, " ");
			//if ( (long word, word)
			if ( p > 4 ) {
				//Then split at space
			} else if ( p > 0 ) {
				int q = str.indexOf ( ' ', p ) + 1; // Strings.InStr(p + 1, str, " ");
				int i;
				if ( q <= 0 ) {
					i = str.length () - p;
				} else {
					i = q - p - 1;
				}
				if ( (p - 1 <= 2 && i <= 3) || (p - 1 == 3 && i <= 2) ) {
					if ( q <= 0 )
						break;
					p = q;
				}
			} else
				break;
			ans = ans + Strings.Mid ( str, 1, p - 1 ) + "\\n";
			str = Strings.Mid ( str, p + 1 );
			continue;
		}

		return ans + str;
	}

	public static String checkShape ( String s ) {
		if ( "".equals ( s ) )
			return "diamond";
		return s;
	}

	public static int checkColor ( int c, int def ) {
		if ( c == 0 )
			return def;
		return c;
	}

	public static int convertBGRtoRGB ( int bgr ) {
		int v;
		v = bgr;

		int r;
		int b;
		int g;

		r = v & 0xFF;
		v = v - r;
		v = v >> 8;

		g = v & 0xFF;
		v = v - g;
		v = v >> 8;

		b = v;

		return (((r << 8) + g) << 8) + b;
	}

	public static int factorBrightness ( int clr, Double factor ) {
		int v;
		v = clr;

		int a;
		int b;
		int c;

		a = v & 0xFF;
		v = v - a;
		v = v >> 8;

		b = v & 0xFF;
		v = v - b;
		c = v >> 8;

		if ( factor > 1 ) {
			a = 255 - (int) (((255 - a) / factor) + 0.5);
			b = 255 - (int) (((255 - b) / factor) + 0.5);
			c = 255 - (int) (((255 - c) / factor) + 0.5);
		} else {
			a = (int) (a * factor + 0.5); // rounding up to be the same as VBA version, so that we can diff output files
			b = (int) (b * factor + 0.5);
			c = (int) (c * factor + 0.5);
		}

		return (((c << 8) + b) << 8) + a;
	}

	public static String myHex ( int i ) {
		String s = java.lang.Integer.toHexString ( i ).toUpperCase ();
		if ( s.length () < 6 )
			s = Strings.Mid ( "000000", 1, 6 - s.length () ) + s;
		return s;
	}

	public static String mangle ( String s ) {
		return s;
	}

	private static DMIElem bestOf ( DMIElem x, DMIElem y ) {
		if ( x == null )
			return y;
		else
			return x;
	}

	public void generateLinkBacks () {

		// for ( int i = 0; i < itsLinkBacks.length; i++ ) {
		// 	ProvenanceInfo p = itsLinkBacks [ i ];
		for ( ProvenanceInfo p : itsLinkBacks ) {
			try {
				String fn = composeLinkBackPartialFN ( p );
				PrintStream ps = TextFile.openTheFileForCreate ( itsLKPath, fn, ".xyz" );
				TextFile.closeTheFile ( ps );
			} catch ( Exception e ) {}
		}
	}

	private String composeLinkBackPartialFN ( ProvenanceInfo pi ) {
		return NameUtilities.firstName ( pi.itsProvenanceWkb, itsGraph, false, false ) + "..." + NameUtilities.firstName ( pi.itsProvenanceWks, itsGraph, false, false ) + "..." + pi.itsProvenanceRow;
	}

	private String composeLinkBackURL ( DMIElem m ) {
		return composeLinkBackURL ( m, getAdornment ( m ).getPrimaryAlias ().itsGraphID );
	}

	private String composeLinkBackURL ( DiagrammingAlias a ) {
		return composeLinkBackURL ( a.itsConcept, a.itsGraphID );
	}

	private String composeLinkBackURL ( DMIElem m, String graphID ) {
		// default is "#" because null or "", the natural other choices, seems to suppress display of any tooltip by the browser...
		// 		could be GraphViz not passing tooltip thru with no URL, or, could be the browser...
		String ans = "#";
		ProvenanceInfo pi = m.provInfo ();
		if ( pi != null ) {
			ans = "file:\\\\\\" + itsLKPath + "\\" + composeLinkBackPartialFN ( pi ) + ".xyz#" + graphID;
			ans = ans.replace ( '\\', '/' );
			// itsLinkBacks = ProvenanceInfo.addToSet (pi, itsLinkBacks);
			itsLinkBacks.add ( pi );
		}
		return ans;
	}

	private boolean showTypeOption ( DMIElem p ) {
		if ( itsOptions.gOptionAddTypeName )
			return true;
		return inOptionSet ( p, itsOptions.gOptionAddTypeNameExplicitSet );
	}

	private boolean attachStackedOption ( DMIElem p ) {
		if ( itsOptions.gOptionAttachStacked )
			return true;
		return inOptionSet ( p, itsOptions.gOptionAttachStackedExplicitSet );
	}

	private boolean inOptionSet ( DMIElem p, ISet<DMIElem> sl ) {
		if ( p != null && sl != null ) {
			for ( int i = 0; i < sl.size (); i++ ) {
				DMIElem c = sl.get ( i );
				if ( p.instanceOf ( c ) )
					return true;
			}
		}
		return false;
	}
}
