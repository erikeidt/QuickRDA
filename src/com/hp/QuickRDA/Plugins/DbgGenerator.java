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

import com.hp.QuickRDA.L5.ExcelTool.Debug;

public class DbgGenerator implements IGeneratorPlugin {

	@Override
	public String generate ( GenerationInfo genInfo, String cmd ) {
		/*
		 * cmd should inform whether specified file name, build file name, or DMI
		 * 	whether specified directory, build file directory, or temp path
		 *   whether ".txt" or other
		 * Also, whether whole graph or view
		 */

		Debug.exportForDebug ( genInfo.itsConceptMgr, genInfo.itsGraph, false, genInfo.itsTempPath, "DMI", ".txt" );
		Debug.exportView ( "Before Build Names", genInfo.itsFocusView, genInfo.itsGraph, genInfo.itsTempPath, "DMI", ".txt" );
		return null;
	}

}
