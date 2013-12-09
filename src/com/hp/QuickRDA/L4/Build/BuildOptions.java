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

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;

public class BuildOptions {

	public static boolean		gDebug;

	public static final String	gQuickRDAOutputFolderName				= "QuickRDA";
	public static final String	gQuickRDALinkbackFolderName				= "links";

	public static final String	gQuickRDABasePatternsFileName			= "QuickRDA";
	public static final String	gQuickRDABasePatternsFileSuffix			= ".txt";

	public static final int		gOptionDifferentiator					= 2;																		// For inferred roles: 1 = Use Plan Differentiator, 2 = Use Parent Differentator

	// public static final String	kDefOptionRankFileSep				= "ranksep=\"1 equally\" nodesep=\"1 equally\"";
	public static final String	kDefOptionRankFileSep					= "ranksep=\"0.8\" nodesep=\"0.8\"";

	// The main difference between blank (presumably Times) and Sans is padding that makes the nodes a tad larger
	// public static final kDefNodeFontAndSize = "fontname=Times fontsize=12"
	// public static final kDefEdgeFontAndSize = "fontname=Times fontsize=10 labeldistance=5"
	public static final int		kDefNodeFontSize						= 12;
	public static final String	kDefNodeFontAndSize						= "fontname=\"Times New Roman Sans Serif\" fontsize=" + kDefNodeFontSize;	// "fontname=Sans fontsize=" + kDefNodeFontSize;
	public static final String	kDefEdgeFontAndSize						= "fontname=\"Times New Roamn Sans Serif\" fontsize=10 labeldistance=5";	// "fontname=Sans fontsize=10 labeldistance=5";
	// public static final kDefNodeFontAndSize = "fontsize=12"
	// public static final kDefEdgeFontAndSize = "fontsize=10 labeldistance=5"
	// public static final kDefNodeFontAndSize = "fontname=""Times-Italic"" fontsize=12"
	public String				gGVOpts;

	// Diagramming
	public static final boolean	gOptionShowAllAssignedTos				= true;																	// recent change

	public static final String	gErrorColor								= "red";
	public static final String	gErrorTextColor							= "red";

	public static final String	gHighlightColor							= "deeppink";																// "magenta" '"yellow"
	public static final String	gHighlightTextColor						= "deeppink3";																// "magenta3" '"lightgreen"
	public static final int		gHighlightLineWidth						= 6;

	public static final boolean	DOTdigraph								= true;
	// public static final boolean	gUseFinalRAint							= false;
	public static final boolean	gUseHeadTailLabels						= false;

	public static final int		gOptionMin								= 0;																		// 2
	public static final int		gOptionMax								= 0;																		// 1
	public static final boolean	DebugOutput								= false;

	public static final double	gLineBrightnessFactor					= 0.8;																		// 0.85      ' Used for edge or line coloring; they look much better a bit darker than fill colors

	public static final String	gGreyedNodeLabelFont					= "Times-Italic";
	public static final String	gGreyedEdgeLabelFont					= "Times-Italic";
	public static final String	gOffViewMiniNodeColor					= "Gray80";
	public static final String	gGreyedNodeLabelColor					= "Gray22";
	public static final double	gGreyedNodeBrightnessFactor				= 2.9;																		// Used for graying out nodes
	public static final double	gGreyedEdgeLineBrightnessFactor			= 2.7;																		// Used for graying out edges' lines
	public static final double	gGreyedEdgeLabelBrightnessFactor		= 2.4;																		// Used for graying out edges' labels

	public static final String	gVisualReplicaEdgeColor					= "lightsteelblue";
	public static final double	gVisualReplicaWeight					= 0.5;
	public static final String	gVisualReplicaLineStyle					= "dashed";
	public static final boolean	gUseBlankVisualReplicaEdgeLabel			= true;
	public static final boolean	gUseVisualReplicaEdgeLabel				= false;
	public static final String	gVisualReplicaEdgeLabel					= " ";
	public static final String	gVisualReplicaEdgeLabelFont				= "Times-Italic";
	public static final String	gVisualReplicaEdgeFontColor				= "firebrick";

	public static final boolean	gOptionIncludeDomainMetaModelItems		= false;																	// False=normal
	public static final boolean	gOptionIncludeUnderlyingMetaModelItems	= false;																	// False=normal

	// From Module Dropdowns
	public static final String	gDropDownSheetName						= ".QDropDowns";

	// From Reports
	public static final String	gGoodComplete							= "Complete";
	public static final String	gOK										= "OK";
	public static final String	gBadIncomplete							= "Incomplete";

	/*
	public static final String	gQuickRDASourceUnitMarkerName			= "QuickRDA";
	public static final String	gQuickRDABuildTableMarkerName			= "QuickRDA Build Table";
	public static final String	gQuickRDABuildTableID					= "QGraphSpec";
	*/

	public static final int		kVersionRow								= 1;
	public static final int		kVersionCol								= 6;


	//=== From Inferencing ===
	public boolean				gOptionAutoInPlan;																									//hide inferencing results from InPlan
	public boolean				gOptionAutoHide;																									//applies to infer, plus suppression of assigned to in diagramming
	public boolean				gOptionAutoRevealInferences;

	public boolean				gOptionContainment;																								//containment reasoning, figure out what's attached or grouped with what
	public boolean				gOptionAttach;
	public boolean				gOptionGroup;
	public boolean				gOptionNestedGroups;

	public boolean				gOptionPartialHide;
	public boolean				gOptionDoReport;

	public static boolean		DOT0									= false;

	//=== From ExportDOT ===
	public int					gOptionObfuscate;																									// 0=no 1=yes:replace model names with numbers 2=yes:replace model names with random strings of same length
	public boolean				gOptionTrimDiff;																									// = true 'True for normal, false for debugging of domain models
	public boolean				gOptionGraphDirectionTB;																							//Top-Bottom vs. Left-Right
	public boolean				gOptionAddTypeName;
	public boolean				gOptionAttachStacked;

	public ISet<DMIElem>		gOptionAddTypeNameExplicitSet;
	public ISet<DMIElem>		gOptionAttachStackedExplicitSet;

	public String				gOptionRankFileSep;
	public int					gNodeFontSize;
	public String				gNodeFontAndSize;
	public String				gEdgeFontAndSize;

	public boolean				gOptionZGRViewer;
	public boolean				gOptionDOT;
	public int					gOptionDBG;
	public boolean				gOptionDMI;
	public boolean				gOptionZDL;

	public boolean				gOptionReachNodeBruteForceAlog;

	public int					gOptionSmall;

	public boolean				gOptionIgnoreEdgeEnv;

	public boolean				gOptionTrunc;
	
	public boolean				gOptionNoViewer;

	public BuildOptions () {
		//Inferencing defaults
		gOptionAutoInPlan = true;
		gOptionAutoHide = true;
		gOptionAutoRevealInferences = true;

		gOptionContainment = true;
		gOptionAttach = true;
		gOptionGroup = true;
		gOptionNestedGroups = true;

		//Export Defaults
		gOptionTrimDiff = true;
		gOptionGraphDirectionTB = true;
		gOptionDOT = true;
		if ( gDebug )
			gOptionDBG = 1;

		gOptionRankFileSep = kDefOptionRankFileSep;
		gNodeFontSize = kDefNodeFontSize;
		gNodeFontAndSize = kDefNodeFontAndSize;
		gEdgeFontAndSize = kDefEdgeFontAndSize;
		gOptionNoViewer  = false;
	}

}
