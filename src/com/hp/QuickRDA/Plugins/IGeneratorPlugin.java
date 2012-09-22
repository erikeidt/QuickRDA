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

import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L4.Build.BuildOptions;
import com.hp.QuickRDA.L5.ExcelTool.*;

public interface IGeneratorPlugin {

	public enum SingleBatch {
		Single,
		Batch
	}

	public enum Viewer {
		NoViewer,
		UseDefaultViewer,
		UseAlternateViewer
	}

	public static class GenerationInfo {
		public ConceptManager		itsConceptMgr;
		public DMIBaseVocabulary	itsBaseVocab;
		public DMIGraph				itsGraph;
		public BuildOptions			itsBuildOptions;
		public Builder				itsBuilder;
		public DMIView				itsFocusView;
		public DMIView				itsGrayView;
		public SingleBatch			itsSB;
		public boolean				itsIsFirst;
		public Viewer				itsVX;

		String						itsFilePath;
		String						itsFilePrefix;
		String						itsFileSuffix;			// suggested suffix
		String						itsBuildColumnLabel;
		String						itsQuickRDAVersion;
		String						itsTempPath;
		String						itsLinkBackPath;

		public GenerationInfo ( Builder bldr, DMIView v1, DMIView v2, SingleBatch sb, boolean first, Viewer vx ) {
			itsConceptMgr = bldr.itsConceptMgr;
			itsBaseVocab = bldr.itsBaseVocab;
			itsGraph = bldr.itsGraph;
			itsBuilder = bldr;
			itsBuildOptions = bldr.itsOptions;
			itsFocusView = v1;
			itsGrayView = v2;
			itsSB = sb;
			itsIsFirst = first;
			itsVX = vx;
		}

		public void SetStrings ( String fPath, String fPrefix, String fSuffix, String label, String vers, String temp, String link ) {
			itsFilePath = fPath;
			itsFilePrefix = fPrefix;
			itsFileSuffix = fSuffix;
			itsBuildColumnLabel = label;
			itsQuickRDAVersion = vers;
			itsTempPath = temp;
			itsLinkBackPath = link;
		}
	}

	public String generate ( GenerationInfo genInfo, String cmd );

}
