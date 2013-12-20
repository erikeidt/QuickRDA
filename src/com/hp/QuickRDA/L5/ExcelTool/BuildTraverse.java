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

import com.hp.JEB.*;
import com.hp.QuickRDA.Excel.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L4.Build.BuildUtilities;
import com.hp.QuickRDA.Plugins.*;

public class BuildTraverse {

	public static void buildDeclarativeWorkbook ( Workbook wkb, boolean zgrv, boolean dropdowns, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems ) {
		Application.StatusBar ( "Working..." );

		Range it = wkb.HasInfoTable ();
		if ( it == null ) {
			lang.errMsg ( "Active Workbook has no QuickRDA Build Table" );
		}
		else {
			int crq = 0;
			Worksheet act = null;
			Worksheet itwks = null;

			Range sel = null;
			itwks = it.Parent ();

			// We normally keep MMWKB active, so that Java to Excel VBA QuickRDA callbacks work
			// So, whenever we need to switch to the active workbook instead of the .xlam, we make sure to switch back.
			Application.ActiveWorkbook.Activate ();
			{

				// Pick the cell selection of the Build worksheet
				if ( !itwks.Name ().equals ( Application.ActiveSheet.Name () ) ) {
					act = Application.ActiveSheet;
					itwks.Activate ();
				}

				try {
					sel = Application.Selection ();
				} catch ( Exception e ) {}

				if ( act != null )
					act.Activate ();
			}
			Start.gMMWKB.Activate ();

			// And use it to specify what gets built
			if ( sel != null ) {
				if ( sel.Columns ().Count () == 1 ) {
					crq = sel.Column () - it.Column () + 1;
				}
			}

			BuildTraverse.traverseInfoTable ( wkb.Path (), "", it, crq, zgrv, dropdowns, inclDomainModelItems, inclUnderlyingMetaModelItems );
		}

		Application.StatusBar ( "Done." );

	}

	private static void traverseInfoTable ( String filePath, String filePrefixIn, Range qgs, int crq, boolean zgrv, boolean dropdowns, boolean inclDomainModelItems, boolean inclUnderlyingMetaModelItems ) {
		TableReader buildTab = new TableReader ( qgs, TableReader.VisibleCellsOnly | TableReader.TrackRows );

		int ci1 = 3; // Utilities_SSU.FindColumnIndex(buildTab, "M1");	    

		boolean doAll = true;
		if ( crq >= ci1 && crq <= buildTab.ColLast () ) { // qgs.Columns().Count()
			ci1 = crq;
			doAll = false;
		}

		if ( zgrv || dropdowns ) {
			doAll = false;
		}

		int ci = ci1;
		for ( ;; ) {
			if ( ci > 0 && ci <= buildTab.ColLast () ) // qgs.Columns().Count()
				;
			else
				break;
	
			
			boolean isBuildTableV2 = buildTab.GetValue ( 1, 1 ).startsWith ( "!" );
			String prefix;
			if ( zgrv || dropdowns ) {
				prefix = filePrefixIn;
			} else {
				// prefix = qgs.Cells(qgs.Rows().Count(), ci).Value().trim();
				prefix = buildTab.GetValue ( isBuildTableV2 ? 1 : buildTab.RowLast (), ci );
			}
			
			Start.openLogFile( filePath + "\\" + BuildUtilities.escapeTextForFile (prefix) + "_log.txt", false);
			lang.msgln( "QuickRDA j" + Start.jVers + "x" + Start.xptXLVers + " building: '" + prefix + "'");
			Builder bldr = new Builder ();

			Application.ActiveWorkbook.Activate ();
			Range sel = null;
			try {
				sel = Application.Selection ();
			} catch ( Exception e ) {}
			Start.gMMWKB.Activate ();

			Builder.DualView dv = bldr.build ( filePath, buildTab, isBuildTableV2, ci, sel, dropdowns );



			IGeneratorPlugin.Viewer vx;
			IGeneratorPlugin.SingleBatch sb = (doAll && (buildTab.ColLast () - ci1 + 1 > 1)) ? IGeneratorPlugin.SingleBatch.Batch : IGeneratorPlugin.SingleBatch.Single;
			if (bldr.itsOptions.gOptionNoViewer)  vx = IGeneratorPlugin.Viewer.NoViewer;
			else  vx = zgrv ? IGeneratorPlugin.Viewer.UseAlternateViewer : IGeneratorPlugin.Viewer.UseDefaultViewer;
			IGeneratorPlugin.GenerationInfo genInfo = new IGeneratorPlugin.GenerationInfo ( bldr, dv.vw, dv.vwGray, sb, ci == ci1, vx );
			genInfo.SetStrings ( filePath, prefix, ".txt", prefix, Start.getQuickRDAVersionNumber (), Start.gQuickRDATEMPPath, Start.gLinkbackPath );

			if ( bldr.itsOptions.gOptionDBG >= 3 )
				bldr.itsGeneratorPlugins.add ( DbgGenerator.class.getName () /*"Dbg"*/);

			if ( dropdowns ) {
				String nm = DropDownGenerator.class.getName ();
				bldr.itsGeneratorPlugins.add ( inclDomainModelItems ? nm + ",full" : nm );
			}
			else {
				if ( bldr.itsOptions.gOptionDMI )
					bldr.itsGeneratorPlugins.add ( DMIGenerator.class.getName () /*"DMI"*/);

				if ( bldr.itsOptions.gOptionDOT )
					bldr.itsGeneratorPlugins.add ( DOTGenerator.class.getName () /*"DOT"*/);

				if ( bldr.itsOptions.gOptionDoReport )
					bldr.itsGeneratorPlugins.add ( XLReportGenerator.class.getName () /*"XLReport"*/);

			}

			if ( bldr.itsOptions.gOptionDBG >= 1 )
				bldr.itsGeneratorPlugins.add ( DbgGenerator.class.getName () /*"Dbg"*/);

			for ( String cmd : bldr.itsGeneratorPlugins ) {
				// IGeneratorPlugin igp = GeneratorPluginFinder.findPlugin (cmd, IGeneratorPlugin.class);
				IGeneratorPlugin igp = PluginFinder.findGeneratorPlugin ( cmd );
				if ( igp != null ) {
				// lang.msgln("Running Plugin: " + cmd);
					@SuppressWarnings("unused")
					String status = igp.generate ( genInfo, cmd );
				}
			}
			lang.msgln ( "Done." );
			Start.closeLogFile();
			
			if ( !doAll )
				break;
			ci = ci + 1;
		}
	}
}
