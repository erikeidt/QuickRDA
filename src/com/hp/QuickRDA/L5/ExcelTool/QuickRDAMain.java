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
// import com.hp.QuickRDA.Trial1.*;
import com.hp.QuickRDA.L0.lang.lang;

public class QuickRDAMain {

	/**
	 * @param args
	 */
	public static void main ( String [] args ) {
		int errorLevel = -1;
		try {
			errorLevel = commonMain ( args, false );
		} catch (Exception e) {
			e.printStackTrace ( Start.gErrLogFile );
		} finally {
			Start.release ();
		}
		if ((errorLevel != 0) && ((Start.gLogFileCount == 0) || (Start.gLogFileErr != 0))) System.exit ( errorLevel );
	}

	public static int commonMain ( String [] args, boolean dbg ) {
		if ( args.length != 3 ) {
			System.out.println ( "QuickRDA for Java Usage:\n\tjava -jar QuickRDA.jar command path workbook\nwhere:\n\tcommand is one of: version, reset, trial, build, dropdowns1, dropdowns2\n\tpath is the windows path to the QuickRDA.xlam workbook and installation directory,\n\tand workbook is the name of the QuickRDA.xlam workbook, usually QuickRDA.xlam.\n\tNOTE: the current working directory should the QuickRDA folder as well so that Java finds the Jacob COM Interoperability DLLs.\n" );
			return 1;
		}

		/*
		 * System.out.println ( "QuickRDA4J: arg count = " + args.length); for ( int i = 0; i <
		 * args.length; i++ ) { System.out.println(" arg " + i + " = '" + args[i] + "'"); }
		 */

		String operation = args [ 0 ];

		// lang.errMsg ("heap size = " + Runtime.getRuntime ().totalMemory ());

		if ( "unitTrial".equals ( operation ) ) {
			// Trial.UnitTrial ();
		} else if ( "version".equals ( operation ) ) {
			try {
				Start.initialize ( args [ 1 ], args [ 2 ] );
			} catch ( Exception e ) {
				e.printStackTrace ();
				return 1;
			}
			Start.a5_ReportVersion ();
			return 1;
		} else {
			Start.initialize ( args [ 1 ], args [ 2 ] );

			if ( !dbg )
				Application.SetScreenUpdating ( 0 );

			if ( "build".equals ( operation ) ) {
				Buttons.a1_StartBrowserButton ();
			} else if ( "version".equals ( operation ) ) {
				Start.a5_ReportVersion ();
			} else if ( "buildZ".equals ( operation ) ) {
				Buttons.a1_StartZGRViewerButton ();
			} else if ( "dropdowns1".equals ( operation ) ) {
				Buttons.a3_SetDropDownsButton ();
			} else if ( "dropdowns2".equals ( operation ) ) {
				Buttons.a4_SetDropDownsButton ();
			} else if ( "reset".equals ( operation ) ) {
				Application.SetScreenUpdating ( 1 );
			} else if ( "liveTrial".equals ( operation ) ) {
				// Trial.LiveTrial ();
			}
		}
		return lang.getMsgCount ();
	}

}
