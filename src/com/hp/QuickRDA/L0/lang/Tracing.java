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

package com.hp.QuickRDA.L0.lang;

import java.io.PrintStream;

public class Tracing {

	public static String		gTraceFileDirectory;
	public static PrintStream	gTraceFile;

	public static void setTraceFileDirectory ( String directoryName ) {
		gTraceFileDirectory = directoryName;
	}

	private static String getTraceFileDirectory () {
		if ( gTraceFileDirectory == null ) {
			String tmpPath = System.getProperty ( "java.io.tmpdir" );

			if ( tmpPath.charAt ( tmpPath.length () - 1 ) == '\\' ) {
				tmpPath = tmpPath.substring ( 0, tmpPath.length () - 1 );
			}

			gTraceFileDirectory = tmpPath;

		}
		return gTraceFileDirectory;
	}

	public final static int	TraceWave	= 1;
	public final static int	TraceFrame	= 2;
	public final static int	TraceFrameX	= 4;
	public final static int	TraceFrameY	= 8;
	public final static int	TraceFrameZ	= 16;
	public final static int	TraceParse	= 32;

	public static int		gTraceSet;

	public static void startTracing ( int traceSet ) {
		gTraceSet = traceSet;
		openLog ();
	}

	public static void openLog () {
		if ( gTraceFile == null )
			gTraceFile = TextFile.openTheFileForCreate ( getTraceFileDirectory (), "log", ".txt" );
	}

	/*
	private static void OpenLogForAppend() {
		gTraceFile = TextFile.OpenTheFileForAppend(Start.gQuickRDATEMPPath, "log", ".txt");
	}
	*/

	public static void flushLog () {
		if ( gTraceFile != null ) {
			gTraceFile.flush ();
			/*
			CloseLog;
			OpenLogForAppend;
			*/
		}
	}

	public static void closeLog () {
		if ( gTraceFile != null ) {
			TextFile.closeTheFile ( gTraceFile );
			gTraceFile = null;
		}
	}

}
