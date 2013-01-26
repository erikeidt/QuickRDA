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
import com.hp.QuickRDA.L1.Core.*;

public class DiagrammingPrimary extends DiagrammingAlias {
	private List<DiagrammingAlias>	itsGroupments;
	private List<DiagrammingAlias>	itsAttachments;

	public DiagrammingAlias			itsSubjectAlias;
	public DiagrammingAlias			itsObjectAlias;

	public DiagrammingAlias			itsReferenceTarget;

	// public boolean					itsMiniNodeInvisible;
	public boolean					itsMiniNodeAlreadyGenerated;
	public boolean					itsSuppress;

	public DiagrammingPrimary ( DMIElem c ) {
		super ( c );
	}

	public boolean hasReferenceTarget () {
		return itsReferenceTarget != null;
	}

	public DiagrammingAlias getReferenceTarget () {
		return itsReferenceTarget;
	}

	public void addAsAttachment ( DiagrammingAlias a ) {
		//itsAttachments = AddToSet (a, itsAttachments);
		if ( itsAttachments == null )
			itsAttachments = new XSetList<DiagrammingAlias> ();
		itsAttachments.add ( a );
	}

	public void addAsGroupment ( DiagrammingAlias a ) {
		//itsGroupments = AddToSet (a, itsGroupments);
		if ( itsGroupments == null )
			itsGroupments = new XSetList<DiagrammingAlias> ();
		itsGroupments.add ( a );
	}

	public boolean isGrouping () {
		return getGroupCount () > 0;
	}

	public int getGroupCount () {
		return (itsGroupments == null ? 0 : itsGroupments.size ());
	}

	@Override
	public List<DiagrammingAlias> getGroup () {
		return itsGroupments;
	}

	public boolean isAttaching() {
		return getAttachCount () > 0;
	}
	
	public int getAttachCount () {
		return (itsAttachments == null ? 0 : itsAttachments.size ());
	}

	@Override
	public List<DiagrammingAlias> getAttach () {
		return itsAttachments;
	}
}
