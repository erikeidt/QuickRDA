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

package com.hp.QuickRDA.L1.Core;

public class DMIElementDiagrammingInfo {
	private static DMIElementDiagrammingInfo	zD	= new DMIElementDiagrammingInfo ();

	public static DMIElementDiagrammingInfo allocateIfNull ( DMIElementDiagrammingInfo d ) {
		if ( d == null )
			d = new DMIElementDiagrammingInfo ();
		return d;
	}

	public static DMIElementDiagrammingInfo readAccessTo ( DMIElementDiagrammingInfo d ) {
		if ( d == null )
			return zD;
		else
			return d;
	}

	public DMIElementDiagrammingInfo () {
		itsDDVizSet = new DMIElem [ 0 ];
	}

	public String	itsDDViz;
	DMIElem []		itsDDVizSet;
	public String	itsDefaultViz;
	public String	itsDisplay;
//	public String	itsContainment;
	public String	itsDirection;
	public String	itsBiDirect;
	public String	itsArrowHead;
	public String	itsArrowTail;
	public String	itsWeight;
	public String	itsRanking;
	public String	itsShape;
	public int		itsColor;
	public int		itsTextColor;
	public int		itsPriority;

	public boolean	itsReportEnumerate;
	public String	itsReportAsSubject;
	public String	itsReportAsObject;
	public String	itsReportOnFormula;

	public void setDDVizSet ( DMIElem [] xL ) {
		itsDDVizSet = xL;
	}

	public DMIElem [] getDDVizSet () {
		return itsDDVizSet;
	}

}
