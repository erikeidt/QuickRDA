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

import com.hp.QuickRDA.L4.Build.BuildOptions;
import com.hp.QuickRDA.L5.ExcelTool.*;

public class DropDownGenerator implements IGeneratorPlugin {

	@Override
	public String generate ( GenerationInfo genInfo, String cmd ) {

		Dropdowns d = new Dropdowns ();
		d.generateDropdowns ( genInfo.itsBuilder, genInfo.itsFocusView, (cmd.indexOf ( ",full" ) >= 0), BuildOptions.gOptionIncludeUnderlyingMetaModelItems );

		return null;
	}

}
