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

import com.hp.QuickRDA.L0.lang.ISet;
import com.hp.QuickRDA.L1.Core.DMIElem;
import com.hp.QuickRDA.L1.Core.DMIGraph;
import com.hp.QuickRDA.L1.Core.DMIView;
import com.hp.QuickRDA.L2.Names.ConceptManager;

public interface IFilterPlugin {

	public class FilterInfo {
		String			itsUserFilterExpression;
		DMIGraph		itsGraph;
		ConceptManager	itsCM;
		DMIView			itsVW;
		DMIView			itsVWGray;

		public FilterInfo ( String userFilterExpression, DMIGraph g, ConceptManager cm, DMIView vw, DMIView vwGray ) {
			itsUserFilterExpression = userFilterExpression;
			itsGraph = g;
			itsCM = cm;
		}
	}

	public ISet<DMIElem> filter ( ISet<DMIElem> inSet, FilterInfo filterInfo );

}
