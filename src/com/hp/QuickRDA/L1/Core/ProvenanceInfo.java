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

public class ProvenanceInfo {
	public static final boolean	kSingleProv	= true;

	public DMIElem				itsProvenanceWkb;
	public DMIElem				itsProvenanceWks;
	public int					itsProvenanceRow;

	public ProvenanceInfo ( DMIElem w, DMIElem s, int r ) {
		itsProvenanceWkb = w;
		itsProvenanceWks = s;
		itsProvenanceRow = r;
	}

}
