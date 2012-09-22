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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class XSetList<ElementType> implements ISet<ElementType> {

	public enum AsListOrSet {
		AsSet
	}

	public static final AsListOrSet	AsSet	= AsListOrSet.AsSet;

	public enum HashOrNot {
		DontBotherHashing,
		HashOnDemand
	}

	public static final HashOrNot	HashOnDemand			= HashOrNot.HashOnDemand;
	public static final HashOrNot	DontBotherHashing		= HashOrNot.DontBotherHashing;

	private boolean					itsAsSet				= false;
	private boolean					itsHashOnThreshold		= false;
	private boolean					itsUseEquals			= false;

	private static final int		kInitialHashPW			= 4;
	private int						itsHashPW;
	private int						itsHashMask;

	private int						itsLength;
	private ElementType []			itsElements;

	private int						itsScaleCount;

	private boolean					itsNeedsTrim;
	private int []					itsHashNexts;											// length == itsElements.length
	private int []					itsHashHeads;											// length == hash bucket size

	private static final int		HHLengthThreshold		= 18;
	private static final int		HHRepThreshold			= 200;
	private static final int		HHChainLengthThreshold	= 14;
	private static final int		HHChainRepThreshold		= 1000;

	private static int				itsNextIndex;											// just an aid for debugging
	@SuppressWarnings("unused")
	private int						itsIndex;												// just an aid for debugging

	public XSetList () {
		itsIndex = itsNextIndex++;
	}

	public XSetList ( HashOrNot hash ) {
		this ( false, hash == HashOrNot.HashOnDemand, false );
	}

	public XSetList ( AsListOrSet asSet ) {
		this ( asSet == AsListOrSet.AsSet, false, false );
	}

	public XSetList ( AsListOrSet asSet, boolean useEquals ) {
		this ( asSet == AsListOrSet.AsSet, true, useEquals );
	}

	public XSetList ( AsListOrSet asSet, HashOrNot hash, boolean useEquals ) {
		this ( asSet == AsListOrSet.AsSet, hash == HashOnDemand, useEquals );
	}

	public XSetList ( AsListOrSet asSet, HashOrNot hash ) {
		this ( asSet == AsListOrSet.AsSet, hash == HashOnDemand, false );
	}

	private XSetList ( boolean asSet, boolean hashOnDemand, boolean useEquals ) {
		this ();
		itsAsSet = asSet;
		itsHashOnThreshold = hashOnDemand;
		itsUseEquals = useEquals;
	}

	@Override
	public void memUsage ( MemRefCount mrc ) {
		mrc.objCount++; // self
		mrc.atrCount += 3; // Length + sc + 2 booleans
		if ( itsLength > 0 ) {
			mrc.objCount++; // array of ptr
			mrc.ptrCount += itsElements.length;
		}
		if ( itsHashHeads != null ) {
			mrc.objCount += +2; // 2 arrays of long
			mrc.atrCount += itsHashHeads.length + itsHashNexts.length;
		}
	}

	/*
	public static <SomeType> XSetList<SomeType> AllocateIfNull (XSetList<SomeType> v) {
		if ( v == null )
			return new XSetList<SomeType> ();
		else
			return v;
	}

	private static XSetList<Object>	zV	= new XSetList<Object> ();	// the Common zero length item

	@SuppressWarnings("unchecked")
	public static <SomeType> XSetList<SomeType> ReadAccessTo (XSetList<SomeType> v) {
		if ( v == null )
			return (XSetList<SomeType>) zV;
		else
			return v;
	}
	*/

	@Override
	public boolean add ( ElementType e ) {
		if ( itsAsSet )
			return addToSet ( e );
		else
			return addToList ( e );
	}

	@Override
	public void add ( int index, ElementType element ) {
		throw new RuntimeException ( "XSetList.add(int,E) positional addition not supported" );
	}

	@Override
	public boolean addToSet ( ElementType e ) {
		assert e != null : "XSetList.addToSet: can't add null to set";
		if ( indexOf ( e ) < 0 )
			return addItem ( e );
		return false;
	}

	@Override
	public boolean addToList ( ElementType e ) {
		// assert itsAsSet == false : "XSetList.addToList called on Set";
		// assert e != null : "IList<DMIElem> .addToList : can't add null to list";
		return addItem ( e );
	}

	@SuppressWarnings("unchecked")
	private boolean addItem ( ElementType e ) {
		// selfCheck ();
		if ( itsElements == null ) {
			assert itsLength == 0 : "XSetList.addItem no list, but length was > 0: " + itsLength;
			itsElements = (ElementType []) new Object [ 1 ];
			itsLength = 0;
		} else if ( itsLength >= itsElements.length ) {
			expand ();
		}
		itsElements [ itsLength ] = e;
		if ( itsHashHeads != null )
			hashIn ( e, itsLength );
		itsLength++;
		// selfCheck ();

		return true;
	}


	@Override
	public boolean addAll ( Collection<? extends ElementType> c ) {
		throw new RuntimeException ( "XSetList.addAll is unsupported" );
	}

	@Override
	public boolean addAll ( int index, Collection<? extends ElementType> c ) {
		throw new RuntimeException ( "XSetList.addAll is unsupported" );
	}

	@Override
	public void clear () {
		itsLength = 0;
		clearHash ();
		if ( itsLength > 0 )
			java.util.Arrays.fill ( itsElements, 0, itsLength, null );
	}

	@Override
	public boolean contains ( Object o ) {
		return indexOf ( o ) >= 0;
	}

	@Override
	public boolean containsAll ( Collection<?> c ) {
		throw new RuntimeException ( "XSetList.containsAll is unsupported" );
	}

	@Override
	public ElementType get ( int index ) {
		return itsElements [ index ];
	}

	@Override
	public int indexOf ( Object e ) {
		if ( itsHashHeads == null ) { // if ( ! hh && ! hhOff )
			if ( itsHashOnThreshold && itsScaleCount > HHRepThreshold )
				createHash ();
		}

		if ( itsHashHeads != null ) {
			if ( itsHashOnThreshold && itsScaleCount > HHChainRepThreshold )
				reHash ();
			int x = itsHashHeads [ hashValue ( e ) ];
			int cnt = 0;
			if ( itsUseEquals ) {
				while ( x != -1 ) {
					if ( itsElements [ x ].equals ( e ) )
						return x;
					x = itsHashNexts [ x ];
					if ( itsHashOnThreshold && ++cnt >= HHChainLengthThreshold )
						itsScaleCount += cnt;
				}
			}
			else {
				while ( x != -1 ) {
					if ( e == itsElements [ x ] )
						return x;
					x = itsHashNexts [ x ];
					if ( itsHashOnThreshold && ++cnt >= HHChainLengthThreshold )
						itsScaleCount += cnt;
				}
			}
			return -1;
		} else {
			if ( itsHashOnThreshold && itsLength >= HHLengthThreshold )
				itsScaleCount += itsLength;
			if ( itsUseEquals ) {
				for ( int i = 0; i < itsLength; i++ )
					if ( itsElements [ i ].equals ( e ) )
						return i;
			}
			else {
				for ( int i = 0; i < itsLength; i++ )
					if ( e == itsElements [ i ] )
						return i;
			}
			return -1;
		}
	}

	@Override
	public boolean isEmpty () {
		return itsLength == 0;
	}

	@Override
	public XIterator<ElementType> setListIterator () {
		return new XIterator<ElementType> ( this, 0 );
	}

	@Override
	public Iterator<ElementType> iterator () {
		// return new XIterator<ElementType> (this, 0);
		return setListIterator ();
	}

	@Override
	public void merge ( IList<ElementType> il ) {
		if ( il != null ) {
			int cnt = il.size ();
			for ( int i = 0; i < cnt; i++ )
				add ( il.get ( i ) );
		}
	}

	@Override
	public int lastIndexOf ( Object o ) {
		throw new RuntimeException ( "XSetList.lastIndexOf is unsupported" );
	}

	@Override
	public ListIterator<ElementType> listIterator () {
		throw new RuntimeException ( "XSetList.listIterator is unsupported" );
	}

	@Override
	public ListIterator<ElementType> listIterator ( int index ) {
		throw new RuntimeException ( "XSetList.listIterator(int) is unsupported" );
	}

	@Override
	public boolean remove ( Object o ) {
		int index = indexOf ( o );
		if ( index >= 0 ) {
			remove ( index );
			return true;
		}
		return false;
	}

	@Override
	public ElementType remove ( int index ) {
		ElementType ans = itsElements [ index ];
		clear ( index );
		trim ();
		return ans;
	}

	@Override
	public boolean removeAll ( Collection<?> c ) {
		throw new RuntimeException ( "XSetList.removeAll is unsupported" );
	}

	@Override
	public boolean retainAll ( Collection<?> c ) {
		throw new RuntimeException ( "XSetList.retainAll is unsupported" );
	}

	@Override
	public ElementType set ( int index, ElementType element ) {
		substitute ( index, element );
		return element;
		// throw new RuntimeException ("XSetList.set is unsupported");
	}

	@Override
	public int size () {
		return itsLength;
	}

	@Override
	public List<ElementType> subList ( int fromIndex, int toIndex ) {
		throw new RuntimeException ( "XSetList.subList is unsupported" );
	}

	@Override
	public Object [] toArray () {
		return itsElements.clone (); // yuck...
	}

	@Override
	public <T> T [] toArray ( T [] a ) {
		throw new RuntimeException ( "XSetList.toArray([]) is unsupported" );
	}

	@Override
	public void populateElementArray ( ElementType [] ea ) {
		System.arraycopy ( itsElements, 0, ea, 0, itsLength );
	}

	@Override
	public boolean trim () {
		selfCheck ();
		boolean ans = false;

		if ( itsNeedsTrim ) {
			int j = 0;

			for ( int i = 0; i < itsLength; i++ ) {
				ElementType e = itsElements [ i ];
				if ( e != null ) {
					if ( i != j )
						itsElements [ j ] = e;
					j++;
				}
			}

			for ( int i = j; i < itsLength; i++ ) {
				itsElements [ i ] = null;
				if ( itsHashNexts != null )
					itsHashNexts [ i ] = -1;
			}

			if ( j - 1 != itsLength ) {
				itsLength = j;
				ans = true;
			}

			clearHash ();
			itsNeedsTrim = false;
		}

		selfCheck ();
		return ans;
	}

	@Override
	public void clearElement ( ElementType e ) {
		int index = indexOf ( e );
		if ( index >= 0 )
			clear ( index );
	}

	@Override
	public void clear ( int index ) {
		selfCheck ();
		if ( itsHashHeads != null )
			hashOut ( itsElements [ index ], index );
		itsElements [ index ] = null;
		itsNeedsTrim = true;
		selfCheck ();
	}

	public boolean substituteDupTrim ( ElementType sb, ElementType ob ) {
		boolean ans = false;
		if ( ob == null || indexOf ( ob ) >= 0 ) {
			// substituting something for nothing, or,
			// substituting something for something that is already there
			// thus, this substitution is really a remove
			clearElement ( sb );
			trim ();
			ans = true;
		} else {
			substitute ( sb, ob );
		}
		return ans;
	}

	public void substitute ( ElementType sb, ElementType ob ) {
		int index = indexOf ( sb );
		substitute ( index, ob );
	}

	private void substitute ( int index, ElementType ob ) {
		if ( index >= 0 ) {
			selfCheck ();
			if ( itsHashHeads != null ) {
				ElementType sb = get ( index );
				hashOut ( sb, index );
			}
			itsElements [ index ] = ob;
			if ( ob == null )
				itsNeedsTrim = true;
			else if ( itsHashHeads != null )
				hashIn ( ob, index );
			selfCheck ();
		}
	}

	@Override
	public void pop () {
		int u = itsLength - 1;
		if ( u >= 0 )
			remove ( u ); // Shrink, really
	}

	@Override
	public ElementType top () {
		int u = itsLength - 1;
		if ( u >= 0 )
			return get ( u );
		return null;
	}

	@Override
	public void mergeListIntoSet ( IList<ElementType> v ) {
		for ( ElementType e : v )
			if ( e != null )
				addToSet ( e );
	}

	@Override
	public ISet<ElementType> clone () {
		trim ();
		XSetList<ElementType> ans = new XSetList<ElementType> ( itsAsSet, itsHashOnThreshold, itsUseEquals );
		ans.itsLength = itsLength;
		if ( itsLength > 0 )
			ans.itsElements = itsElements.clone ();
		// ans.SetList(Length, itsElements);
		return ans;
	}

	@Override
	public void sortList ( IComparator<ElementType> cmp ) {
		sortList ( 0, cmp );
	}

	@Override
	public void sortList ( int startPos, IComparator<ElementType> cmp ) {

		ElementType t;

		clearHash ();

		for ( int i = startPos; i < itsLength - 1; i++ ) {
			for ( int j = i + 1; j < itsLength; j++ ) {
				boolean swap = false;
				/*
				if ( cmp == null )
					swap = itsElements [ i ].itsIndex > itsElements [ j ].itsIndex;
				else
				*/
				swap = cmp.follows ( itsElements [ i ], itsElements [ j ] );
				if ( swap ) {
					t = itsElements [ i ];
					itsElements [ i ] = itsElements [ j ];
					itsElements [ j ] = t;
				}
			}
		}
	}

	public boolean sameList ( IList<ElementType> mV2 ) {
		if ( itsLength == mV2.size () ) {
			for ( int i = 0; i < itsLength; i++ ) {
				if ( itsElements [ i ] != mV2.get ( i ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	@SuppressWarnings("unchecked")
	private void expand () {
		int u = upSize ( itsLength );
		itsElements = (ElementType []) lang.expandPreserve ( itsElements, u );
		if ( itsHashNexts != null ) {
			itsHashNexts = lang.expandPreserve ( itsHashNexts, u );
			java.util.Arrays.fill ( itsHashNexts, itsLength, u, -1 );
		}
	}

	private int upSize ( int n ) {
		int ans = n + 1;
		if ( n <= 3 ) {
			ans = n + 1;
		} else if ( n <= 6 ) {
			ans = n + 2;
		} else if ( n <= 12 ) {
			ans = n + 3;
		} else {
			ans = n + (n >> 1);
		}
		return ans;
	}

	private int hashValue ( Object o ) {
		int ans = 0;
		int bits = 32;
		int h = o.hashCode ();
		while ( bits >= 0 ) {
			ans ^= h;
			h >>= itsHashPW;
			bits -= itsHashPW;
		}
		ans &= itsHashMask;
		return ans;
	}

	private void clearHash () {
		itsHashHeads = null;
		itsHashNexts = null;
	}

	private void createHash () {
		hashUp ( kInitialHashPW );
	}

	private void reHash () {
		int pw = kInitialHashPW;
		if ( itsHashHeads != null ) {
			if ( itsHashPW >= 12 )
				return;
			pw = itsHashPW + 1;
		}
		hashUp ( pw );
	}

	private void hashUp ( int pw ) {
		itsHashPW = pw;
		int size = 1 << pw;
		itsHashMask = size - 1;
		itsHashHeads = new int [ size ];
		java.util.Arrays.fill ( itsHashHeads, 0, size, -1 );
		itsHashNexts = new int [ itsElements.length ];
		java.util.Arrays.fill ( itsHashNexts, 0, itsElements.length, -1 );
		for ( int i = 0; i < itsLength; i++ )
			hashIn ( itsElements [ i ], i );
		selfCheck ();
		itsScaleCount = 0;
	}

	private void hashIn ( ElementType e, int ix ) {
		if ( e != null ) {
			int v = hashValue ( e );
			int x = itsHashHeads [ v ];

			itsHashHeads [ v ] = ix;
			itsHashNexts [ ix ] = x;
		}
	}

	private void hashOut ( ElementType e, int ix ) {
		if ( e != null ) {
			int v = hashValue ( e );
			int x = itsHashHeads [ v ];
			int last_x = -1;
			while ( x != -1 ) {
				int y = itsHashNexts [ x ];
				if ( x == ix ) {
					if ( last_x == -1 )
						itsHashHeads [ v ] = y;
					else
						itsHashNexts [ last_x ] = y;
					return;
				}
				last_x = x;
				x = y;
			}
			throw new RuntimeException ( "Hash not found" );
		}
	}

	@SuppressWarnings("all")
	private void selfCheck () {
		if ( itsHashHeads != null ) {
			itsHashOnThreshold = false;
			for ( int i = 0; i < itsLength; i++ ) {
				ElementType e = itsElements [ i ];
				if ( e != null ) {
					if ( indexOf ( e ) != i )
						throw new RuntimeException ( "Hash Table Error" );
				}
			}
			itsHashOnThreshold = true;
		}
	}

}
