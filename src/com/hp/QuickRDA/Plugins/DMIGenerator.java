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

import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L3.Inferencing.*;
import com.hp.QuickRDA.L4.Build.*;
import com.hp.QuickRDA.L5.ExcelTool.*;

public class DMIGenerator implements IGeneratorPlugin {

	@Override
	public String generate ( GenerationInfo genInfo, String cmd ) {
		if ( genInfo.itsBuildOptions.gOptionDMI ) {
			Visibility.xDMIExportVisibilityReasoning ( genInfo.itsFocusView );

			if ( genInfo.itsBuildOptions.gOptionDBG >= 3 ) {
				Debug.exportView ( "Before Build Names", genInfo.itsFocusView, genInfo.itsGraph, genInfo.itsTempPath, "DMI", ".txt" );
			}

			DMIExport expDMI = new DMIExport ();
			expDMI.buildNamesAndSerializationsForExport ( genInfo.itsFocusView, genInfo.itsBaseVocab, genInfo.itsConceptMgr, genInfo.itsConceptMgr.itsNamesSG );

			if ( genInfo.itsBuildOptions.gOptionDBG >= 3 ) {
				Debug.exportView ( "After Build Names", genInfo.itsFocusView, genInfo.itsGraph, genInfo.itsTempPath, "DMI", ".txt" );
			}

			expDMI.exportToDMI ( genInfo.itsGraph, genInfo.itsFilePath, genInfo.itsFilePrefix, ".dmi", genInfo.itsFilePrefix );

			// instead of relying on DiagramVisRes to filter out the added names...
			Builder.DualView dv = new Builder.DualView ( new DMIView (), genInfo.itsGrayView );
			dv.vw.initializeFromGraph ( genInfo.itsGraph, DMIView.ViewInitializerEnum.AllVisible );
			genInfo.itsFocusView = dv.vw;
		}

		return null;
	}

}
