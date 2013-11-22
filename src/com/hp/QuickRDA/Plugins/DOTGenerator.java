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

package com.hp.QuickRDA.Plugins;

import java.io.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L3.Inferencing.*;
import com.hp.QuickRDA.L4.Build.DOT.*;
import com.hp.QuickRDA.L5.ExcelTool.*;

public class DOTGenerator implements IGeneratorPlugin {

	@Override
	public String generate ( GenerationInfo genInfo, String cmd ) {
		LanguageReasoner dot = new LanguageReasoner ( genInfo.itsGraph, genInfo.itsBuildOptions, genInfo.itsFocusView, genInfo.itsGrayView, genInfo.itsVX == Viewer.UseAlternateViewer, genInfo.itsLinkBackPath );

		if ( genInfo.itsBuildOptions.gOptionTrunc )
			Visibility.diagramVisibilityReasoning ( genInfo.itsFocusView, genInfo.itsGrayView );

		dot.analyzeForDOT ();

		String genDir = genInfo.itsFilePath;

		PrintStream ps;
		try {
			ps = TextFile.openTheFileForCreateThrowing ( genDir, genInfo.itsFilePrefix, ".txt" );
		} catch ( Exception e ) {
			boolean net = false;
			if ( genDir.startsWith ( "http://" ) || genDir.startsWith ( "https://" ) ) {
				net = true;
			}
			else {
				lang.errMsg ( "Error creating output file in location: " + genInfo.itsFilePath );
				System.err.println ( e.getLocalizedMessage () );
			}
			try {
				genDir = genInfo.itsTempPath;
				ps = TextFile.openTheFileForCreate ( genDir, genInfo.itsFilePrefix, ".txt" );
				if ( !net )
					lang.errMsg ( "Temp directory being used instead: " + genInfo.itsTempPath );
			} catch ( Exception e2 ) {
				e2.printStackTrace ( System.err );
				lang.errMsg ( "Could not use temp directory." );
				return null;
			}
		}

		dot.generateDOTFile ( ps, genInfo.itsFilePrefix, genInfo.itsBuildColumnLabel, genInfo.itsQuickRDAVersion );

		// genInfo.itsSB == SingleBatch.Batch
		JobUtilities.startBatchJob ( genDir, genInfo.itsFilePrefix, ".txt", genInfo.itsIsFirst && (genInfo.itsVX != Viewer.NoViewer), genInfo.itsVX == Viewer.UseAlternateViewer );

		dot.generateLinkBacks ();

		return null;
	}
}
