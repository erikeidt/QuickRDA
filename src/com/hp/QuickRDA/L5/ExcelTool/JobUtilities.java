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
import java.io.InputStreamReader;
import java.lang.ProcessBuilder;

import com.hp.QuickRDA.L0.lang.lang;

public class JobUtilities {

	public static void startBatchJob ( String filePath, String filePrefix, String fileSuffix, boolean view, boolean zgrv ) {
		//Run batch file to generate .svg and bring up viewer (browser)
		BufferedReader batout;
		Process batproc;
		String prgName = "\\StartBatchJob.bat";
		if ( view ) {
			if ( zgrv )
				prgName = "\\StartZGRViewer.bat";
			else
				prgName = "\\StartBatchBrowser.bat";
		}
		String prg = "\"" + Start.gAppInstallPath + prgName + "\"" ;

		String arg =  "\"" + filePath + "\\" + filePrefix + "\"";

		try {
			lang.msgln("Running " + prgName.substring ( 1 ) + " on " + filePrefix + "...");
			final ProcessBuilder bat = new ProcessBuilder ( prg, arg );
			bat.redirectErrorStream(true); // only want to have one stream to deal with
			batproc = bat.start();
			batout  = new BufferedReader(new InputStreamReader(batproc.getInputStream ()));
			batproc.getOutputStream ().close(); // we don't have anything to say to him: bat pauses won't block
			while (true) {  // read the output from bat and write it to log
						String mess = batout.readLine ();
						if (mess != null) lang.msgln ( mess );
						else break;
			}
			//lang.msgln("BatchJob: done.");
		} catch ( Exception e ) {
			lang.errMsg ( "Exception in startBatchJob: " + e.getMessage () );
		}
	}

}
