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

import com.hp.QuickRDA.L4.Build.*;
// import com.hp.QuickRDA.L0.lang.*;

public class QuickRDADbgMainBuild {

	public static void main ( String [] args0 ) {
		commonMainDebug ( "build", false /* XLSM */);
	}

	/*
	 * You should have the current working directory set to XLDev in you run/debug configurations
	 */
	public static void commonMainDebug ( String command, boolean XLSM ) {
		String myPath = ".";
		try {
			java.io.File currentDirectory = new java.io.File(new java.io.File(".").getAbsolutePath());
			myPath = currentDirectory.getCanonicalPath();
		} catch (Exception e) {}
		
		String args[] = new String [] { "build", myPath, "QuickRDA.xlam" };

		args [ 0 ] = command;

		if ( XLSM )
			args [ 2 ] = "QuickRDA.xlsm";

		BuildOptions.gDebug = true;

		// try {
		int errorLevel = QuickRDAMain.commonMain ( args, true );
		Start.release ();
		/*
		 * } catch ( Exception e ) { Start.Release(); e.printStackTrace(); }
		 */
		if ( errorLevel != 0 )
			System.exit ( errorLevel );
	}
}
