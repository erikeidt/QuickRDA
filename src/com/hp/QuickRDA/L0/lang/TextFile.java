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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;


public class TextFile {

	private static final int mBufferSize = 262144; 
	
	public static PrintStream openTheFileForCreate ( String filePath, String filePrefix, String fileSuffix ) {
		String ans = filePath + "\\" + filePrefix + fileSuffix;
		// Open ans For fileNum Output;
		try {
			return new PrintStream ( new BufferedOutputStream (new FileOutputStream ( ans, false ), mBufferSize ), false );
		} catch ( Exception e ) {}
		return null;
	}

	public static PrintStream openTheFileForCreateThrowing ( String filePath, String filePrefix, String fileSuffix ) throws FileNotFoundException {
		String ans = filePath + "\\" + filePrefix + fileSuffix;
		// Open ans For fileNum Output;
		return  new PrintStream ( new BufferedOutputStream (new FileOutputStream ( ans, false ), mBufferSize ), false );
	}

	public static PrintStream openTheFileForAppend ( String filePath, String filePrefix, String fileSuffix ) {
		String ans = filePath + "\\" + filePrefix + fileSuffix;
		// Open ans For fileNum Append;
		try {
			return new PrintStream ( new BufferedOutputStream ( new FileOutputStream ( ans, true ), mBufferSize ), false);
		} catch ( Exception e ) {}
		return null;
	}

	public static void closeTheFile ( OutputStream fileNum ) {
		try {
			fileNum.close ();
		} catch ( Exception e ) {}
	}

	public static BufferedReader openTheFileForRead ( String filePath, String filePrefix, String fileSuffix, StringRef xx ) {
		String fName = filePath + "\\" + filePrefix + fileSuffix;
		try {
			BufferedReader ans = new BufferedReader ( new FileReader ( fName ) );
			if ( xx != null )
				xx.str = fName;
			return ans;
		} catch ( Exception e ) {}
		return null;
	}

	public static BufferedReader openTheFileForRead ( String [] filePaths, String filePrefix, String fileSuffix, StringRef xx ) {
		for ( int i = 0; i < filePaths.length; i++ ) {
			BufferedReader ans = openTheFileForRead ( filePaths [ i ], filePrefix, fileSuffix, xx );
			if ( ans != null )
				return ans;
		}
		return null;
	}

	
}



