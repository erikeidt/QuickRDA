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

package com.hp.QuickRDA.L5.ExcelTool;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import com.hp.JEB.*;
import com.hp.QuickRDA.Excel.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L0.lang.XSetList.AsListOrSet;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L3.Inferencing.*;
import com.hp.QuickRDA.L3.Inferencing.Query.*;
import com.hp.QuickRDA.L4.Build.*;

public class Builder {

	public DMIGraph				itsGraph;
	public DMIBaseVocabulary	itsBaseVocab;
	public BuildOptions			itsOptions;
	public ConceptManager		itsConceptMgr;
	public NamedPatternManager	itsNamedPatternMgr;
	private SourceUnitReader	itsSrcUnitReader;
	private DMISubgraph			itsUnderlyingMetamodel;
	private DMISubgraph			itsDomainLanguage;
	private MetamodelReader		itsMMReader;

	private List<String>		itsFilters			= new XSetList<String> ();
	private List<String>		itsActivePatterns	= new XSetList<String> ( AsListOrSet.AsSet, true );
	public List<String>			itsGeneratorPlugins	= new XSetList<String> ();

	public IList<TableReader>	itsHeaders			= new XSetList<TableReader> ();

	public static class DualView {
		public final DMIView	vw;
		public final DMIView	vwGray;

		/*
		 * A concept of a view with some items flagged
		 * 	intended use is that the flagged items are in the view but off focus.
		 * VW1 is the whole view, an VW2 is the subset that is off focus.
		 * The idea is that vw2 is a subset of vw1; and vw2 is the designation of items that should be grayed out.
		 * To see if an item in the graph is in the "view" as a whole, callers need only check vw1.
		 */
		public DualView ( DMIView vw1, DMIView vw2 ) {
			vw = vw1;
			vwGray = vw2;
		}
	}

	private void loadSystemPatterns ( boolean dropdowns ) {

		BufferedReader in = TextFile.openTheFileForRead ( Start.gAppInstallPath, BuildOptions.gQuickRDABasePatternsFileName, BuildOptions.gQuickRDABasePatternsFileSuffix, null );
		if ( in != null ) {
			// Hide template stuff from Dropdowns, TemplateFiler defined in QuickRDA.txt or similar dmp
			if ( dropdowns )
				addFilter ( "/filter=TemplateFilter;hide" );
			TextToPattern ttp = new TextToPattern ( in, itsNamedPatternMgr, itsConceptMgr );
			// Tracing.openLog ();
			// int a = 1;
			ttp.parseAndAddPatterns ( itsActivePatterns );
		}

	}

	public DualView build ( String filePath, TableReader buildTab, boolean isBuildTableV2, int ci, Range highlightR, boolean dropdowns ) {
		buildStart ();
		// buildMetaModels ( filePath, buildTab, isBuildTableV2, dropdowns );
		// System.out.println("mmbuilt");
		buildGraphFromInfo ( filePath, buildTab, isBuildTableV2, ci, highlightR, dropdowns );
		loadSystemPatterns ( dropdowns );
		return buildFinish ();
	}

	public void buildStart () {
		itsOptions = new BuildOptions ();

		itsGraph = new DMIGraph ();
		itsGraph.initialize ( new CacheBuilder ( itsGraph ) );
		itsBaseVocab = itsGraph.itsBaseVocab;

		DMISubgraph s;
		s = new DMISubgraph ( itsGraph, "Underlying/DMI Metamodel", DMISubgraph.SubgraphLevelEnum.kUnderlyingMetamodel, false, false, false );

		itsConceptMgr = new ConceptManager ();
		itsConceptMgr.bindToSubgraphConfiguration ( s, s, s, s, s, s, s );
		itsUnderlyingMetamodel = s;

		itsMMReader = new MetamodelReader ();
		itsMMReader.bindToConceptManagerConfiguration ( itsConceptMgr );

		// Application.SetScreenUpdating(0);
		// Start.gMMWKB.Activate();
		Range mmRng = Start.gMMWKS.FindTableRangeOnSheet ();
		// Application.ActiveWorkbook.Activate();
		// Application.SetScreenUpdating(1);

		// Range tblR = Utilities_SSU.RangeOfInterestMinusHeaders(tbl.Range(),1);
		TableReader mTab = new TableReader ( 2, mmRng, TableReader.AllCells | TableReader.TrackRows );

		itsMMReader.buildFromMMTable ( mTab, 1, 2 );

		DMISubgraph s2 = new DMISubgraph ( itsGraph, "Domain/RDA Metamodels", DMISubgraph.SubgraphLevelEnum.kDomainLanguageMetamodel, false, false, false );

		// Switch
		itsConceptMgr.bindToSubgraphConfiguration ( s2, s2, s2, s2, s2, s2, s2 );
		itsDomainLanguage = s2;
		s = s2;

		itsMMReader.bindToConceptManagerConfiguration ( itsConceptMgr );
		itsMMReader.buildFromMMTable ( mTab, 3, 6 );

		// StopStopwatch
		// ExportForDebug conceptmgr, graph, gQuickRDATEMPPath, "DMI", ".txt"

		// boolean supportsProvenance = true, Optional Boolean IsVisible = false, Optional
		// Boolean isMeta = False


		DMISubgraph s1 = new DMISubgraph ( itsGraph, "Names Information", DMISubgraph.SubgraphLevelEnum.kDomainLanguageMetamodel, false, false, true );

		// Dim DMISubgraph s2
		s2 = new DMISubgraph ( itsGraph, "Abstract Properties", DMISubgraph.SubgraphLevelEnum.kDomainLanguageMetamodel, true, false, false );

		DMISubgraph s3 = new DMISubgraph ( itsGraph, "Provenance Information", DMISubgraph.SubgraphLevelEnum.kDomainLanguageMetamodel, false, false, false );

		DMISubgraph s7 = new DMISubgraph ( itsGraph, "Cross Domain Models / Domain Language - Invisible", DMISubgraph.SubgraphLevelEnum.kCrossDomainModel, true, false, true );

		DMISubgraph s6 = new DMISubgraph ( itsGraph, "Domain/RDA Models - Invisible", DMISubgraph.SubgraphLevelEnum.kDomainModel, true, false, false );

		DMISubgraph s5 = new DMISubgraph ( itsGraph, "Domain/RDA Models - Visible", DMISubgraph.SubgraphLevelEnum.kDomainModel, true, true, false );

		// And switch again
		itsConceptMgr.bindToSubgraphConfiguration ( s5, s1, s2, s3, s5, s6, s7 );

		itsSrcUnitReader = new SourceUnitReader ( itsConceptMgr, itsOptions );

		itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsVisiblexSG );

		itsNamedPatternMgr = new NamedPatternManager ();

	}

	public DualView buildFinish () {
		// System.out.println("ssread");

		DMIView vwGray = null;

		DMISubgraph sg = new DMISubgraph ( itsGraph, "Inferred View/Filter", DMISubgraph.SubgraphLevelEnum.kDomainModel, true, itsOptions.gOptionAutoRevealInferences, false );
		itsConceptMgr.setDefaultSubgraph ( sg );

		DMIView vw;
		vw = new DMIView ();
		vw.initializeFromGraph ( itsGraph, DMIView.ViewInitializerEnum.AllVisible );

		// pick up additions as we go so that they're in the view, can be seen & manipulated
		// (e.g. hidden)
		// and we won't have to second guess if such additions should be in the view or not
		// this gives the right granularity
		//
		// This is fine to do but should be careful about traverses the view while it's
		// changing...
		// (in VBA, for loops capture the To value once; but even in Java we don't want our
		// index to get reset by a trim() operation, if the view is shrinking)
		//
		if ( sg.isVisible )
			itsConceptMgr.trackAdditionsInView ( vw );

		Application.StatusBar ( "Inferencing..." );

		Begetting.generative_Inferencing ( vw, this );

		vwGray = null;

		Application.StatusBar ( "Filtration & Abstraction..." );

		vwGray = runFilters ( vw, vwGray );

		// prefer view tracking instead of this enmass addition; better granularity this way
		// vw.AddSubgraph sg 'Pick up generated inferences & abstractions

		if ( itsOptions.gOptionContainment ) {
			Application.StatusBar ( "Containment..." );
			Containment.containmentInferencing ( itsBaseVocab, itsOptions, vw );
		}

		vw.trim ();

		itsConceptMgr.trackAdditionsInView ( null );

		Application.StatusBar ( "Generating..." );

		return new DualView ( vw, vwGray );
	}

	private void buildMMfromWorkSheet ( Worksheet wks, boolean mmvis ) {
		String sheetname = wks.Name ();
		if ( sheetname.endsWith ( "Metamodel" ) || sheetname.endsWith ( "Language" ) || sheetname.endsWith ( " DL") ) {
			Range mmtR = wks.FindTableRangeOnSheet ();
			if ( mmtR != null ) {
				itsConceptMgr.clearProvenance ();
				// Range tblR = Utilities_SSU.RangeOfInterestMinusHeaders(tbl.Range(),1);
				TableReader mTab = new TableReader ( 2, mmtR, TableReader.AllCells | TableReader.TrackRows );
				DMISubgraph sg = null;
				// System.out.println("Building metamodel from " + sheetname);
				if ( !mmvis )
					sg = itsConceptMgr.setDefaultSubgraph ( itsDomainLanguage );
				itsMMReader.bindToConceptManagerConfiguration ( itsConceptMgr );
				itsMMReader.buildFromMMTable ( mTab, 1, 9 );
				if ( sg != null )
					itsConceptMgr.setDefaultSubgraph ( sg );
			}
		}
	}

/*	private void buildMetaModels ( String filePath, TableReader buildTab, boolean isBuildTableV2, boolean dropdowns ) {
		boolean viz = dropdowns;
		for ( int r = isBuildTableV2 ? 1 : 2; r <= buildTab.RowLast () - (isBuildTableV2 ? 0 : 1); r++ ) {

			String wkbName = buildTab.GetValue ( r, 1 );
			String sheetName = buildTab.GetValue ( r, 2 );
			if ( "".equals ( wkbName ) && "".equals ( sheetName ) )
				continue;
			if ( sheetName.startsWith ( "\\" ) || sheetName.startsWith ( "/" ) )
				continue;
			String path = filePath;

			if ( wkbName.startsWith ( "~" ) ) {
				path = Start.gAppInstallPath;
				if ( wkbName.startsWith ( "~\\" ) || wkbName.startsWith ( "~/" ) )
					wkbName = wkbName.substring ( 2 );
				else
					wkbName = wkbName.substring ( 1 );
			}

			if ( !"".equals ( wkbName ) || !"".equals ( sheetName ) ) {
				WorkbookReference wkbRef = Start.openBook ( path, wkbName, true );
				if ( wkbRef.wkb != null ) {
					if ( !wkbRef.wasAlreadyOpen )
						Start.track ( wkbRef );
					if ( "".equals ( sheetName ) ) {
						Worksheets wkss = wkbRef.wkb.Worksheets ();
						int ixc = wkss.Count ();
						for ( int i = 1; i <= ixc; i++ ) {
							Worksheet wks = wkss.Item ( i );
							buildMMfromWorkSheet ( wks, viz ); //Invisible
						}
					} else {
						Worksheet wks = wkbRef.wkb.Worksheets ( sheetName );
						if ( wks == null )
							lang.errMsg ( "Worksheet not found: " + sheetName );
						else
							buildMMfromWorkSheet ( wks, viz );
					}
					if ( !wkbRef.wasAlreadyOpen ) {
						wkbRef.wasAlreadyOpen = false;
						wkbRef.wkb.Close ( false, null, false );
						wkbRef.wkb = null;
					}
				}
			}
		} // for each row
	}
*/
	
	private void buildGraphFromInfo ( String filePath, TableReader buildTab, boolean isBuildTableV2, int ci, Range highlightR, boolean dropdowns ) {
		buildGraphFromInfoPass ( filePath, buildTab, isBuildTableV2, ci, 0, highlightR, dropdowns );
		buildGraphFromInfoPass ( filePath, buildTab, isBuildTableV2, ci, 1, highlightR, dropdowns );
		itsConceptMgr.clearProvenance (); // 3/7/2011; pass 1 sets provenance, clean up after so we don't get erroneous provenance
		buildGraphFromInfoPass ( filePath, buildTab, isBuildTableV2, ci, 2, highlightR, dropdowns );
	}

	private DMIView runFilters ( DMIView vw, DMIView vwGray ) {
		Filtration.FilterOptions fo = new Filtration.FilterOptions ( itsOptions.gOptionReachNodeBruteForceAlog );
		// for ( int i = 0; i < itsFilters.length; i++ ) {
		//	String f = itsFilters [ i ];
		for ( String f : itsFilters ) {
			if ( "/subresp".equals ( f ) )
				Abstraction.promoteSubResponsibilities ( itsBaseVocab, itsConceptMgr, itsOptions.gOptionAutoHide, vw );
			else if ( "/resp".equals ( f ) )
				Abstraction.promoteResponsibiltiesToRoles ( itsBaseVocab, itsConceptMgr, itsOptions.gOptionAutoHide, vw );
			else if ( "/subrole".equals ( f ) )
				Abstraction.promotePCToParentRoles ( itsBaseVocab, itsConceptMgr, itsOptions.gOptionAutoHide, vw );
			else if ( "/role".equals ( f ) )
				Abstraction.promotePCRoleToInteractsWith ( itsBaseVocab, itsConceptMgr, itsOptions.gOptionAutoHide, vw );
			else if ( "/connect".equals ( f ) )
				Visibility.revealRelationshipsForNodesInView ( vw );
			else if ( "/reveal".equals ( f ) )
				Visibility.revealReferencedNodesInView ( vw );
			else {
				String abs = "/abstractall=";
				if ( f.startsWith ( abs ) )
					Abstraction.performAbstraction ( f.substring ( abs.length () ), itsConceptMgr, itsOptions.gOptionAutoHide, vw, true );
				else {
					abs = "/abstract=";
					if ( f.startsWith ( abs ) )
						Abstraction.performAbstraction ( f.substring ( abs.length () ), itsConceptMgr, itsOptions.gOptionAutoHide, vw, false );
					else {
						vwGray = Filtration.runFilter ( f, itsGraph, itsConceptMgr, itsNamedPatternMgr, vw, vwGray, fo );
					}
				}
			}
		}
		return vwGray;
	}

	private String extractPattern ( String fltrIn ) { // lifted from DMIView runfilter
		String f = Strings.Mid ( fltrIn, 2 );
		String pat = "";
		StringRef xx = new StringRef ();

		String cmd = Strings.tSplitAfter ( f, xx, ";" );
		f = xx.str;

		while ( !"".equals ( cmd ) && "".equals ( pat ) ) {
			String op = Strings.tSplitAfter ( cmd, xx, "=" ).toLowerCase ();
			cmd = xx.str;
			if ( "".equals ( op ) ) {
				// do nothing
			} else if ( "filter".equals ( op ) || "apply1".equals ( op ) || "apply*".equals ( op ) ) {
				pat = Strings.tSplitAfter ( cmd, xx, "," );
			}
			cmd = Strings.tSplitAfter ( f, xx, ";" );
			f = xx.str;
		}
		return pat;
	}

	private void addFilter ( String f ) {
		itsFilters.add ( f );
		String pat = extractPattern ( f ); // get referenced query pattern
		if ( !"".equals ( pat ) ) {
			itsActivePatterns.add ( pat );
			// System.out.println("Found pattern " + pat);
		}
	}

	private void processSwitches ( String incl, String mcIncl, int stage, String filePath ) {
		final String runEq = "/run=";
		final String gvOptEq = "/gvopt=";
		final String patFileEq = "/patfile=";
		final String sepEq = "/separation=";
		final String nodeFontEq = "/nodefont=";
		final String edgeFontEq = "/edgefont=";
		final String nodeSizeEq = "/nodesize=";
		final String showTypeEq = "/showtype=";
		final String vStackedEq = "/vstacked=";

		if ( stage == 0 ) {
			if ( "/ipoff".equals ( incl ) )
				itsOptions.gOptionAutoInPlan = false;
			else if ( "/hideoff".equals ( incl ) )
				itsOptions.gOptionAutoHide = false;
			else if ( "/hideinf".equals ( incl ) )
				itsOptions.gOptionAutoRevealInferences = false;
			else if ( "/hide1".equals ( incl ) )
				itsOptions.gOptionPartialHide = true;
			else if ( "/tdoff".equals ( incl ) )
				itsOptions.gOptionTrimDiff = false;
			else if ( "/showtype".equals ( incl ) ) {
				itsOptions.gOptionAddTypeName = true;
				itsOptions.gOptionAttachStacked = true;
			}
			else if ( "/lr".equals ( incl ) )
				itsOptions.gOptionGraphDirectionTB = false;
			else if ( "/compup".equals ( incl ) ) {
				itsBaseVocab.gComponentOf.itsDiagrammingInfo.itsDirection = "UpRev";
				itsBaseVocab.gKindOf.itsDiagrammingInfo.itsDirection = "UpRev";
				itsBaseVocab.gKindOf.itsDiagrammingInfo.itsWeight = "99";
				itsBaseVocab.gRefersTo.itsDiagrammingInfo.itsWeight = "0";
				itsBaseVocab.gRefersTo.itsDiagrammingInfo.itsRanking = "none";
			}
			else if ( "/bxrole".equals ( incl ) )
				itsBaseVocab.gRole.itsDiagrammingInfo.itsShape = "box";
			else if ( "/atob".equals ( incl ) )
				itsBaseVocab.gOwnedBy.itsDiagrammingInfo.itsContainment = "attach";
			else if ( "/atresp".equals ( incl ) ) {
				DMISubgraph sg = itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsInvisiblexSG );
				// itsBaseVocab.gAssignedTo.itsDiagrammingInfo.itsContainment = "attach";
				itsConceptMgr.enterStatement ( itsBaseVocab.gAssignedTo, itsBaseVocab.gSubclassOf, itsBaseVocab.gIsAttachedTo, true );
				itsConceptMgr.setDefaultSubgraph ( sg );
			}
			else if ( "/atprov".equals ( incl ) ) {
				DMISubgraph sg = itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsInvisiblexSG );
				// itsBaseVocab.gAssignedTo.itsDiagrammingInfo.itsContainment = "attach";
				itsConceptMgr.enterStatement ( itsBaseVocab.gRespProvides, itsBaseVocab.gSubclassOf, itsBaseVocab.gAttaches, true );
				itsConceptMgr.setDefaultSubgraph ( sg );
			}
			else if ( "/gpresp".equals ( incl ) ) {
				DMISubgraph sg = itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsInvisiblexSG );
				itsConceptMgr.enterStatement ( itsBaseVocab.gAssignedTo, itsBaseVocab.gSubclassOf, itsBaseVocab.gIsInGroup, true );
				itsConceptMgr.setDefaultSubgraph ( sg );
			}
			else if ( "/gpprov".equals ( incl ) ) {
				// itsConceptMgr.enterStatement ( itsBaseVocab.gRespProvides, itsBaseVocab.gSubclassOf, itsBaseVocab.gGroups, true );
				DMISubgraph sg = itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsInvisiblexSG );
				itsConceptMgr.enterStatement ( itsBaseVocab.gRespProvides, itsBaseVocab.gSubclassOf, itsBaseVocab.gFollowedByContainer, true );
				itsConceptMgr.setDefaultSubgraph ( sg );
			}
			else if ( "/nocont".equals ( incl ) )
				itsOptions.gOptionContainment = false;
			else if ( "/noattach".equals ( incl ) )
				itsOptions.gOptionAttach = false;
			else if ( "/nogroup".equals ( incl ) )
				itsOptions.gOptionGroup = false;
			else if ( "/nonstgrp".equals ( incl ) )
				itsOptions.gOptionNestedGroups = false;
			else if ( "/report".equals ( incl ) )
				itsOptions.gOptionDoReport = true;
			else if ( "/nodot".equals ( incl ) )
				itsOptions.gOptionDOT = false;
			else if ( "/dmi".equals ( incl ) )
				itsOptions.gOptionDMI = true;
			else if ( "/obfuscate".equals ( incl ) )
				itsOptions.gOptionObfuscate = 1;
			else if ( "/noedgelabels".equals ( incl ) )
				itsOptions.gOptionObfuscate = 3;
			else if ( "/nolabels".equals ( incl ) )
				itsOptions.gOptionObfuscate = 3;
			else if ( "/rnbf".equals ( incl ) )
				itsOptions.gOptionReachNodeBruteForceAlog = true;
			else if ( "/qdbg".equals ( incl ) )
				itsOptions.gOptionDBG = itsOptions.gOptionDBG + 1;
			else if ( "/egoff".equals ( incl ) || "/eeggoff".equals ( incl ) )
				itsOptions.gOptionIgnoreEdgeEnv = true;
			else if ( "/trunc".equals ( incl ) )
				itsOptions.gOptionTrunc = true;
			else if ( "/small".equals ( incl ) ) {
				itsOptions.gOptionSmall = itsOptions.gOptionSmall + 1;
				switch ( itsOptions.gOptionSmall ) {
				case 1 :
					itsOptions.gOptionRankFileSep = "ranksep=\"0.3\" nodesep=\"0.3\"";
					itsOptions.gNodeFontSize = 8;
					itsOptions.gNodeFontAndSize = "fontname=Sans fontsize=8";
					itsOptions.gEdgeFontAndSize = "fontname=Sans fontsize=5 labeldistance=3";
					break;
				case 2 :
					itsOptions.gOptionRankFileSep = "ranksep=\"0.2\" nodesep=\"0.2\"";
					itsOptions.gNodeFontSize = 6;
					itsOptions.gNodeFontAndSize = "fontname=Sans fontsize=6";
					itsOptions.gEdgeFontAndSize = "fontname=Sans fontsize=4 labeldistance=3";
					break;
				}
			} else if ( "/mmout".equals ( incl ) ) {
				itsDomainLanguage.itsLevel = DMISubgraph.SubgraphLevelEnum.kDomainModel;
				itsDomainLanguage.isVisible = true;
				itsDomainLanguage.isMeta = false;
				itsUnderlyingMetamodel.itsLevel = DMISubgraph.SubgraphLevelEnum.kDomainLanguageMetamodel;
			} else if ( incl.startsWith ( runEq ) ) {
				itsGeneratorPlugins.add ( mcIncl.substring ( runEq.length () ) );
			} else if ( incl.startsWith ( patFileEq ) ) {
				;
				/*
				try {
					String filePrefix = mcIncl.substring ( patFileEq.length () );
					// BufferedReader in = new BufferedReader (new FileReader (filePrefix));
					StringRef xx = new StringRef ();
					BufferedReader in = TextFile.openTheFileForRead ( new String [] { filePath, Start.gAppInstallPath }, filePrefix, BuildOptions.gQuickRDABasePatternsFileSuffix, xx );
					if ( in == null )
						lang.errMsg ( "Could not find the pattern file: " + filePrefix + BuildOptions.gQuickRDABasePatternsFileSuffix + "\nlooked in:\n\t" + filePath + ", and,\n\t" + Start.gAppInstallPath );
					else {
						TextToPattern ttp = new TextToPattern ( in, itsNamedPatternMgr, itsConceptMgr );
						ttp.parseAndAddPatterns ();
					}
				} catch ( Exception e ) {
					lang.errMsg ( e.getMessage () );
				}
				*/
			} else if ( incl.startsWith ( gvOptEq ) ) {
				if ( itsOptions.gGVOpts == null )
					itsOptions.gGVOpts = mcIncl.substring ( gvOptEq.length () );
				else
					itsOptions.gGVOpts += " " + mcIncl.substring ( gvOptEq.length () );
			} else if ( incl.startsWith ( sepEq ) ) {
				itsOptions.gOptionRankFileSep = mcIncl.substring ( sepEq.length () );
			} else if ( incl.startsWith ( nodeFontEq ) ) {
				itsOptions.gNodeFontAndSize = mcIncl.substring ( nodeFontEq.length () );
			} else if ( incl.startsWith ( edgeFontEq ) ) {
				itsOptions.gEdgeFontAndSize = mcIncl.substring ( edgeFontEq.length () );
			} else if ( incl.startsWith ( nodeSizeEq ) ) {
				itsOptions.gNodeFontSize = Integer.parseInt ( mcIncl.substring ( nodeSizeEq.length () ) );
			} else {
				addFilter ( mcIncl );
			}
		} else if ( stage == 1 ) {
			;
		} else if ( stage == 2 ) {
			if ( incl.startsWith ( patFileEq ) ) {
				try {
					String filePrefix = mcIncl.substring ( patFileEq.length () );
					// BufferedReader in = new BufferedReader (new FileReader (filePrefix));
					StringRef xx = new StringRef ();
					BufferedReader in = TextFile.openTheFileForRead ( new String [] { filePath, Start.gAppInstallPath }, filePrefix, BuildOptions.gQuickRDABasePatternsFileSuffix, xx );
					if ( in == null )
						lang.errMsg ( "Could not find the pattern file: " + filePrefix + BuildOptions.gQuickRDABasePatternsFileSuffix + "\nlooked in:\n\t" + filePath + ", and,\n\t" + Start.gAppInstallPath );
					else {
						TextToPattern ttp = new TextToPattern ( in, itsNamedPatternMgr, itsConceptMgr );
						ttp.parseAndAddPatterns ( itsActivePatterns );
					}
				} catch ( Exception e ) {
					lang.errMsg ( e.getMessage () );
				}
			}
			else if ( incl.startsWith ( showTypeEq ) )
				itsOptions.gOptionAddTypeNameExplicitSet = addToOptionSet ( mcIncl.substring ( showTypeEq.length () ), itsOptions.gOptionAddTypeNameExplicitSet );
			else if ( incl.startsWith ( vStackedEq ) )
				itsOptions.gOptionAttachStackedExplicitSet = addToOptionSet ( mcIncl.substring ( vStackedEq.length () ), itsOptions.gOptionAttachStackedExplicitSet );
		}
	}

	private ISet<DMIElem> addToOptionSet ( String stl, ISet<DMIElem> st ) {
		if ( st == null )
			st = new XSetList<DMIElem> ();
		StringRef xx = new StringRef ();
		IList<DMIElem> tl = InferencingUtilities.makeList ( stl, xx, itsBaseVocab.gClass, itsConceptMgr );
		ISet<DMIElem> ts = itsOptions.gOptionAttachStackedExplicitSet != null ? itsOptions.gOptionAttachStackedExplicitSet : new XSetList<DMIElem> ();
		for ( int i = 0; i < tl.size (); i++ )
			ts.add ( tl.get ( i ) );
		return ts;
	}

	private void buildGraphFromInfoPass ( String filePath, TableReader buildTab, boolean isBuildTableV2, int ci, int stage, Range highlightR, boolean dropdowns ) {
		for ( int r = isBuildTableV2 ? 1 : 2; r <= buildTab.RowLast () - (isBuildTableV2 ? 0 : 1); r++ ) { // qgs.Rows().Count()

			String wkbName = buildTab.GetValue ( r, 1 ); // qgs.Cells(r, 1).Value()
			String sheetName = buildTab.GetValue ( r, 2 ); // qgs.Cells(r, 2).Value();
			String mcIncl = buildTab.GetValue ( r, ci );

			String incl = dropdowns ? "x" : mcIncl.toLowerCase (); // was XTrim(qgs.Cells(r, ci).Value()
			if ( "".equals ( incl ) )
				;
			else if ( incl.charAt ( 0 ) == '/' ) {
				processSwitches ( incl, mcIncl, stage, filePath );
			} else if ( incl.charAt ( 0 ) == '\\' ) {
				// accept \ as comment char
			} else {
				boolean vis = (!"i".equals ( incl ) && !incl.startsWith ( "i-" ));
				if ( sheetName.startsWith ( "\\" ) )
					;
				else if ( sheetName.startsWith ( "/" ) )
					if ( dropdowns )
						;
					else
						processSwitches ( sheetName.toLowerCase (), sheetName, stage, filePath );
				else if ( stage == 1 ) {
					DMISubgraph defSG = null;
					if ( !vis )
						defSG = itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsInvisiblexSG );

					String path = filePath;
					List<String> columnExclusions = null;
					if ( mcIncl.length () > 0 )
						columnExclusions = parseColumnExclusions ( mcIncl.substring ( 1 ) );

					// if we cause a local workbook to be opened, we'll leave it open.
					boolean closeWhenDone = false; 

					if ( wkbName.startsWith ( "~" ) ) {
						// but if we load a QuickRDA system file, then we will close it
						closeWhenDone = true; 
						path = Start.gAppInstallPath;
						if ( wkbName.startsWith ( "~\\" ) || wkbName.startsWith ( "~/" ) )
							wkbName = wkbName.substring ( 2 );
						else
							wkbName = wkbName.substring ( 1 );
					}

					if (wkbName.startsWith ( "\\" )) ;
						// Accept \ as comment
					else if ( wkbName.endsWith ( ".dmi" ) ) {
						buildGraphFromDMIFile ( path, wkbName, !vis );
					} else {
						if ( !"".equals ( wkbName ) || !"".equals ( sheetName ) ) {
							Start.WorkbookReference wkbRef = Start.openBook ( path, wkbName, true );
							if ( wkbRef.wkb != null ) {
								if ( closeWhenDone ) {
									if ( wkbRef.wasAlreadyOpen )									
										closeWhenDone = false;
									else
										Start.track ( wkbRef );
								}
								if ( "".equals ( sheetName ) ) {
									buildGraphFromWorkBook ( wkbRef.wkb, highlightR, vis, columnExclusions );
								} else {
									Worksheet wks = wkbRef.wkb.Worksheets ( sheetName );
									if ( wks == null )
										Start.AbEnd ( "Worksheet: " + sheetName + " not found in Workbook " + wkbRef.wkb.Name() + "\n\n" );
									else
										buildGraphFromWorkSheet ( wkbRef.wkb, wks, sheetName, highlightR, vis, columnExclusions );
								}
								if ( closeWhenDone ) {
									wkbRef.wasAlreadyOpen = false;
									wkbRef.wkb.Close ( false, null, false );
									wkbRef.wkb = null;
								}
							}
						}

					}

					if ( defSG != null )
						itsConceptMgr.setDefaultSubgraph ( defSG );

				}
			}
		}
	}

	private List<String> parseColumnExclusions ( String incl ) {
		List<String> ans = null;
		incl = incl.trim ();
		if ( !"".equals ( incl ) ) {
			ans = new ArrayList<String> ();
			while ( incl.length () > 0 && incl.charAt ( 0 ) == '-' ) {
				int pos = incl.indexOf ( '-', 1 );
				if ( pos < 0 ) {
					ans.add ( incl.substring ( 1 ).trim () );
					break;
				}
				ans.add ( incl.substring ( 1, pos ).trim () );
				incl = incl.substring ( pos + 1 );
			}
		}
		return ans;
	}

	private void buildGraphFromWorkBook ( Workbook wkb, Range highlightR, boolean mmvis, List<String> columnExclusions ) {
		Worksheets wkss = wkb.Worksheets ();
		int ixc = wkss.Count ();
		for ( int i = 1; i <= ixc; i++ ) {
			Worksheet wks = wkss.Item ( i );
			buildGraphFromWorkSheet ( wkb, wks, null, highlightR, mmvis, columnExclusions );
		}
	}

	private void buildGraphFromWorkSheet ( Workbook wkb, Worksheet wks, String name, Range highlightR, boolean mmvis, List<String> columnExclusions ) {
		// Application.SetScreenUpdating(0);
		// Start.gMMWKB.Activate();

		// try {
		if ( wks.Visible () == Constants.xlSheetVisible ) {
			Range sutR = wks.GetSourceUnitTable ();
			if ( sutR != null ) {
				itsConceptMgr.setProvenanceInfo ( wks.Parent ().Name (), wks.Name (), sutR.Row () );
				Application.StatusBar ( "Working on workbook: " + wks.Parent ().Name () + ", worksheet: " + wks.Name () );
				buildGraphFromRangeObject ( wkb, wks, sutR, highlightR, columnExclusions );
			}
			else {
				buildMMfromWorkSheet ( wks, mmvis );
			}
		}
		/*
		 * } finally { // don't leave excel in a bad state!!
		 * Application.ActiveWorkbook.Activate(); // Application.SetScreenUpdating(1); }
		 */
	}

	// relies on caller getting callbacks activated
	private void buildGraphFromRangeObject ( Workbook wkb, Worksheet wks, Range hdrR, Range highlightR, List<String> columnExclusions ) {
		TableReader headerTab = new TableReader ( hdrR, SourceUnitReader.kTableToFirstRowOffset, TableReader.FirstAllRestVisibleByFirst | TableReader.TrackRows );
		headerTab.SetWk ( wkb, wks );
		// itsHeaders = TableReader.addToList  (headerTab, itsHeaders);
		itsHeaders.add ( headerTab );
		// Range tblR = Utilities_SSU.RangeOfInterestMinusHeaders(hdrR);
		TableReader bodyTab = new TableReader ( SourceUnitReader.kTableToFirstRowOffset + 1, hdrR, TableReader.VisibleCellsOnly );
		int [] highlightRows = null;
		if ( highlightR.Parent ().Name ().equals ( hdrR.Parent ().Name () ) ) {
			int start = hdrR.Row () + SourceUnitReader.kTableToFirstRowOffset;
			highlightRows = RowSet ( highlightR, start, start + bodyTab.RowLast () - 1 );
		}
		itsSrcUnitReader.buildDeclarativeTable ( headerTab, bodyTab, highlightRows, columnExclusions );
	}

	private static int [] RowSet ( Range rng, int first, int last ) {
		// String aa = rng.Address();
		int [] ans = new int [ 0 ];
		Areas a = rng.Areas ();
		int ixc = a.Count ();
		for ( int ix = 1; ix <= ixc; ix++ ) {
			Range r = a.Item ( 1 );
			// String bb = r.Address();

			int rx = r.Row ();
			int rxc = rx + r.Rows ().Count () - 1;

			if ( rx < first ) // shorten highlight rows to size of range at front
				rx = first; // reset start

			if ( rxc > last ) // shorten highlight rows to size of range at end
				rxc = last; // shorten length

			while ( rx <= rxc ) {
				ans = lang.addToSet ( rx - first + 1, ans ); // add every row in the range
				rx++;
			}
		}
		return ans;
	}

	private void buildGraphFromDMIFile ( String filePath, String fileName, boolean vis ) {
		UnitOfInterchange uoi = new UnitOfInterchange ();
		try {
			uoi.importJSONDMI ( filePath, fileName, "", fileName );
		} catch ( Exception e ) {
			e.printStackTrace ();
			return;
		}
		DMIImport di = new DMIImport ();
		di.subgraphFrom ( uoi, itsConceptMgr );
	}
}
