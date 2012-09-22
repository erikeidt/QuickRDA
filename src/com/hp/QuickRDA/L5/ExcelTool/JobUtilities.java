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

import java.lang.ProcessBuilder;

public class JobUtilities {

	public static void startBatchJob ( String filePath, String filePrefix, String fileSuffix, boolean view, boolean zgrv ) {
		//Run batch file to generate .svg and bring up viewer (browser)
		String prgName = "\\StartBatchJob.bat";
		if ( view ) {
			if ( zgrv )
				prgName = "\\StartZGRViewer.bat";
			else
				prgName = "\\StartBatchBrowser.bat";
		}
		String prg = Start.gAppInstallPath + prgName;

		String arg = filePath + "\\" + filePrefix;

		try {
			new ProcessBuilder ( prg, arg ).start ();
		} catch ( Exception e ) {}
	}

	/*
	public static boolean RefreshViewer(Variant processID) {
		boolean OK;
		OK = false;

		String wkbName;
		wkbName = ActiveWorkbook.Name;
		On Error GoTo 9999;

		//this won't help...
		//Application.ScreenUpdating = false

		AppActivate "ZGRViewer"; //processID
		SendKeys "^{r}";
		AppActivate wkbName;
		OK = true;

	9999:;
		//Application.ScreenUpdating = true
		RefreshViewer = OK;
	}

	public static Variant StartZGRViewer(String fullPath) {
		String wkbName;
		wkbName = ActiveWorkbook.Name;
		Variant processID;
		processID = 0;
		String ff;
		//ff = "cmd /c start """ + gAppInstallPath + "\zgrviewer-0.9.0-SNAPSHOT.jar"""
		//ff = "cmd /c ""start E:\Install\ZVTM\(dontbackup)\svn\zgrviewer\trunk\target\zgrviewer-0.9.0-SNAPSHOT.jar"""
		ff = """" + gAppInstallPath + "\StartZGRViewer.bat"" """ + fullPath + """";
		processID = Shell(ff, vbReadOnly);
		//no longer needed to send keys to open the text file, its passed as a parameter
		if ( false ) {
	T2:     Sleep 3;
			On Error GoTo E1;
			AppActivate "ZGRViewer";
			SendKeys "^{d}";
			Sleep 1;
			SendKeys gQuickRDATEMPPath + "\RDAWB.txt";
			Sleep 1;
			SendKeys Chr(13);
			Sleep 2;
			AppActivate wkbName;
			}
		GoTo 9999;

	E1: Resume R1;
	R1: On Error GoTo 9999;
		GoTo T2;

	9999:;
		StartZGRViewer = processID;
	}

	public static void Sleep(int s) {
		int n;
		n = (Format(Time, "ss") + s) Mod 60;
		Do Until Format(Time, "ss") = n;
			DoEvents;
		Loop;
	}
	*/

}
