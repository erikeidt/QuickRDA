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

package com.hp.QuickRDA.L4.Build.DOT;

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.DMIElem;

public class DiagrammingAlias {

	public final DMIElem	itsConcept;
	public String			itsGraphID;
	public String			itsPortID;
	public DMIElem			itsEnvironment;

	public boolean			itsWasGenerated;


	public DiagrammingAlias ( DMIElem m ) {
		itsConcept = m;
	}

	public DiagrammingAlias ( DMIElem m, DMIElem env ) {
		this ( m );
		itsEnvironment = env;
	}

	public boolean isGrouping () {
		return false;
	}

	public int getGroupCount () {
		return 0;
	}

	public int getAttachCount () {
		return 0;
	}

	private static List<DiagrammingAlias>	za	= new XSetList<DiagrammingAlias> ();

	public List<DiagrammingAlias> getAttach () {
		return za;
	}

	public List<DiagrammingAlias> getGroup () {
		return za;
	}

	public boolean hasReferenceTarget () {
		return false;
	}

	public DiagrammingAlias getReferenceTarget () {
		return null;
	}

}
