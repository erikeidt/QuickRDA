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

import java.io.File;
import java.net.URLClassLoader;
import java.net.URL;

import com.hp.QuickRDA.Excel.StatusMessage;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L5.ExcelTool.Start;

public class PluginFinder {

	static public IGeneratorPlugin findGeneratorPlugin ( String cmd ) {
		return (IGeneratorPlugin) findPlugin ( cmd );
	}

	static public IFilterPlugin findFilterPlugin ( String cmd ) {
		return (IFilterPlugin) findPlugin ( cmd );
	}

	static private Object findPlugin ( String cmd ) {
		Object igp = null;
		StringRef xx = new StringRef ();
		String clsName = Strings.tSplitAfter ( cmd, xx, "," );

		StatusMessage.StatusUpdate ( "Loading " + clsName + "..." );
		lang.msgln ( "Loading " + clsName + "..." );
		
		String fpClsName = clsName;
		if ( clsName.indexOf ( "." ) < 0 )
			fpClsName = "com.hp.QuickRDA.Plugins." + clsName;

		igp = load ( fpClsName );
		if ( igp == null )
			igp = loadJar ( fpClsName );

		if ( igp == null )
			lang.errMsg ( "Could not find or instantiate " + cmd );

		return igp;
	}

	static private Object load ( String fpClsName ) {
		return load ( fpClsName, PluginFinder.class.getClassLoader () );
	}

	private static Object load ( String fpClsName, ClassLoader classLoader ) {
		try {
			Class<?> cls = classLoader.loadClass ( fpClsName );
			try {
				// Application.StatusBar ("Creating " + cmd + "...");
				return (IGeneratorPlugin) cls.newInstance ();
			} catch ( Exception e ) {
				 e.printStackTrace ( Start.gErrLogFile );
			}
		} catch ( NoClassDefFoundError e ) {
			 e.printStackTrace ( Start.gErrLogFile );
		} catch ( ClassNotFoundException e ) {
			 e.printStackTrace ( Start.gErrLogFile );
		} catch ( Exception e ) {
			 e.printStackTrace ( Start.gErrLogFile );
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private static Object loadJar ( String fpClsName ) {
		try {
			String jarName = fpClsName;
			int p = fpClsName.lastIndexOf ( "." );
			if ( p >= 0 )
				jarName = fpClsName.substring ( p + 1 );
			URL [] urls = { new File ( jarName + ".jar" ).toURL () };
			return load ( fpClsName, new URLClassLoader ( urls ) );
		} catch ( Exception e ) {
			lang.errMsg ( "Exception while loading plugin" );
			lang.errMsg( e.getMessage ());
		}
		return null;
	}

}
