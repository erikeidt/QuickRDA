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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
// import java.text.DecimalFormat;

import com.hp.JEB.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L4.Build.*;

public class Start {

	private static String		jVers		= "4.4.5";		// QuickRDA.jar & installation version
	private static String		xptXLVers	= "4.4.5";		// The expected Excel Add-in version

	// private static DecimalFormat	vFormat		= new DecimalFormat ("#.##");

	public static boolean		gInitialized;

	public static String		gMMWKBName;
	public static Workbook		gMMWKB;
	public static final String	gMMWKSName	= "MetaModel";
	public static Worksheet		gMMWKS;
	public static String		gAppInstallPath;
	public static String		gQuickRDATEMPPath;
	public static String		gLinkbackPath;

	public static boolean initialize ( String path, String mmwkb ) {
		if ( gInitialized )
			return true;

		gAppInstallPath = path;
		gQuickRDATEMPPath = getQuickRDATEMPDirectory ();
		Tracing.setTraceFileDirectory ( gQuickRDATEMPPath );
		gLinkbackPath = getLinkBackDirectory ();

		gMMWKBName = mmwkb;

		if ( !Application.Initialize ( false ) )
			throw new RuntimeException ( "Could not connect to a running copy of Excel" );

		gMMWKB = Application.Workbooks ().Item ( mmwkb ); // gMMWKB = ThisWorkbook;
		if ( gMMWKB == null )
			throw new RuntimeException ( "Could not locate workbook " + mmwkb );
		gMMWKS = gMMWKB.Worksheets ( "DMI" );
		if ( gMMWKS == null )
			throw new RuntimeException ( "Could not locate MetaModel worksheet.;" );
		gMMWKB.Activate ();
		gInitialized = true;

		return gInitialized;
	}

	private static List<WorkbookReference>	gWorkOpenedWorkbooks	= new ArrayList<WorkbookReference> ();

	public static void track ( WorkbookReference wkbRef ) {
		gWorkOpenedWorkbooks.add ( wkbRef );
	}

	public static void release () {
		for ( int i = 0; i < gWorkOpenedWorkbooks.size (); i++ ) {
			WorkbookReference wkbRef = gWorkOpenedWorkbooks.get ( i );
			if ( !wkbRef.wasAlreadyOpen && wkbRef.wkb != null ) {
				wkbRef.wasAlreadyOpen = false;
				wkbRef.wkb.Close ( false, null, false );
				wkbRef.wkb = null;
			}
		}
		try {
			// don't leave excel in a bad state!!
			if ( Application.ActiveWorkbook != null )
				Application.ActiveWorkbook.Activate ();
		} catch ( Exception e ) {}

		try {
			// don't leave excel in a bad state!!
			if ( Application.connected () )
				Application.SetScreenUpdating ( 1 );
		} catch ( Exception e ) {}

		try {
			// and clean up any COM stuff
			Application.Release ();
		} catch ( Exception e ) {}
	}

	//File Utilities
	public static String getQuickRDATEMPDirectory () {
		String theFolderName = BuildOptions.gQuickRDAOutputFolderName;

		String thePath = System.getProperty ( "java.io.tmpdir" );
		/*
		String thePath = System.getenv("TEMP");
		if ( "".equals(thePath) ) {
			thePath = System.getenv("TMP");
		}
		*/

		if ( "".equals ( thePath ) ) {
			thePath = Start.gAppInstallPath;
			theFolderName = "Output";
		}

		if ( thePath.charAt ( thePath.length () - 1 ) == '\\' ) {
			thePath = thePath.substring ( 0, thePath.length () - 1 );
		}


		return createDirectory ( thePath, theFolderName );
	}

	public static String getLinkBackDirectory () {
		return createDirectory ( getQuickRDATEMPDirectory (), BuildOptions.gQuickRDALinkbackFolderName );
	}

	public static String createDirectory ( String filePath, String fileName ) {
		String d = filePath + "\\" + fileName;
		File f = new File ( d );
		if ( !f.exists () ) {
			try {
				new File ( d ).mkdir ();
			} catch ( Exception e ) {
				d = "";
			}
		}
		return d;
	}

	public static void a5_ReportVersion () {
		lang.errMsg ( "\r\n*** QuickRDA Version Info ***\r\n" );
		lang.errMsg ( "\tThe Java component, QuickRDA.jar, has version:\t" + jVers );
		lang.errMsg ( "\tThis .jar is expecting Excel Add-in version:\t" + xptXLVers );
		lang.errMsg ( "\r\n\tThe actual Excel Add-in, QuickRDA.xlam, is:\t" + getQuickRDAExcelVersionNumber () );
		lang.errMsg ( "\r\n\r\n\tBuilds will show version:\t\t\t" + getQuickRDAVersionNumber () + "\r\n\r\n" );
	}

	public static String getQuickRDAVersionNumber () {
		String xLVers = getQuickRDAExcelVersionNumber ();
		String ans = jVers;
		if ( !xptXLVers.equals ( xLVers ) )
			ans = "j" + jVers + "x" + xLVers;
		return ans;
	}

	private static String getQuickRDAExcelVersionNumber () {
		// Can't get Names.Item(str) to work; might be because its got optional args with restrictions.
		// return Start.gMMWKB.Names("QuickRDA_Version_Number").RefersToRange().Cells(1, 1).Value();
		if ( Start.gMMWKS != null )
			try {
				return Start.gMMWKS.Cells ( BuildOptions.kVersionRow, BuildOptions.kVersionCol ).Value ();
			} catch ( Exception e ) {}
		lang.errMsg ( "\tCould not determine Excel component version number." );
		return "?";
	}

	public static class WorkbookReference {
		Workbook	wkb;
		boolean		wasAlreadyOpen;

		WorkbookReference ( Workbook w, boolean alreadyOpen ) {
			wkb = w;
			wasAlreadyOpen = alreadyOpen;
		}
	}

	public static WorkbookReference openBook ( String filePath, String bookName, boolean loadIfNotOpen ) {
		boolean alreadyOpen = true;
		Workbook wkb = null;

		if ( "".equals ( bookName ) ) {
			return new WorkbookReference ( Application.ActiveWorkbook, true );
		} else {
			try {
				String simpleName = bookName;
				int slash = simpleName.lastIndexOf ( '\\' );
				if ( slash >= 0 )
					simpleName = simpleName.substring ( slash + 1 );
	
				wkb = Application.Workbooks ( simpleName + ".xlsx" );
				if ( wkb == null ) {
					wkb = Application.Workbooks ( simpleName + ".csv" );
					if ( wkb == null ) {
						wkb = Application.Workbooks ( simpleName + ".xlsm" );
						if ( wkb == null ) {
							wkb = Application.Workbooks ( simpleName + ".xlam" );

							if ( wkb == null && loadIfNotOpen ) {

								int count = Application.Workbooks ().Count ();
								Workbooks wkbs = Application.Workbooks ();

								wkb = wkbs.Open ( filePath + "\\" + bookName + ".xlsx" );
								if ( wkb == null ) {
									wkb = wkbs.Open ( filePath + "\\" + bookName + ".csv" );
									if ( wkb == null ) {
										wkb = wkbs.Open ( filePath + "\\" + bookName + ".xlsm" );
									}
								}
								
								if ( wkb == null ) {
									wkb = wkbs.Open ( filePath + "\\" + bookName + ".xlam" );
									// don't close .xlam's, even if we open them
									alreadyOpen = true;
								}
								else {
									int newCount = Application.Workbooks ().Count ();
									alreadyOpen = (newCount == count);
								}
							}
						}
					}
				}
			} catch ( Exception e ) {
				e.printStackTrace ();
			}
		}

		return new WorkbookReference ( wkb, alreadyOpen );
	}

}
