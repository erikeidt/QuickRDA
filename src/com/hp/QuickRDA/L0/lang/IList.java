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

import java.util.List;

public interface IList<ElementType> extends List<ElementType> {

	public boolean addToSet ( ElementType e );

	public boolean addToList ( ElementType e );

	public void clear ( int index );

	public void clearElement ( ElementType e );

	public XIterator<ElementType> setListIterator ();

	public boolean trim ();

	public void populateElementArray ( ElementType [] ea );

	public void pop (); // use List.add() to push

	public ElementType top ();

	public void memUsage ( MemRefCount mrc );

	public boolean substituteDupTrim ( ElementType replaceThis, ElementType withThis );

	public void substitute ( ElementType replaceThis, ElementType withThis );

	public Object clone ();

	public void merge ( IList<ElementType> il );

	public void sortList ( IComparator<ElementType> cmp );

	public void sortList ( int start, IComparator<ElementType> cmp );

	public boolean sameList ( IList<ElementType> lst );

}
