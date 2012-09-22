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

package com.hp.QuickRDA.Excel;

import com.hp.JEB.*;
import com.hp.QuickRDA.L0.lang.*;

public class TableReader {

	public static final int	TrackRows					= 4;
	public static final int	AllCells					= 0;
	public static final int	VisibleCellsOnly			= 8;
	public static final int	FirstAllRestVisibleByFirst	= 16;

	private String			itsWholeTable;
	private int				itsRow						= 1;
	private int				itsCol						= 1;
	private int				itsRowMark					= 0;
	private int				itsColMark					= 0;
	private int				itsColEnd					= 0;

	private int []			itsRowTrack;

	private int				itsLastRow;
	private int				itsLastCol;

	public Range			itsRange;
	public Worksheet		itsWks;
	public Workbook			itsWkb;
	private int				itsFirstRow;						// used only in conjunction with itsRange
	private int				itsFirstCol;						// used only in conjunction with itsRange

	// TableReader from whole Excel Range
	public TableReader ( Range rng, int scFlag ) {
		this ( 1, rng.Rows ().Count (), rng, scFlag );
	}

	// TableReader for top of Excel Range to include n rows. (n=rows)
	public TableReader ( Range rng, int rows, int scFlag ) {
		this ( 1, rows, rng, scFlag );
	}

	// TableReader for bottom of Excel Range, start n rows in.(n=rows).
	public TableReader ( int rows, Range rng, int scFlag ) {
		this ( rows, rng.Rows ().Count (), rng, scFlag );
	}

	// Add the column start
	private TableReader ( int row1, int row2, Range rng, int scFlag ) {
		this ( rng, row1, row2, 1, rng.Columns ().Count (), scFlag );
	}

	private TableReader ( Range rng, int row1, int row2, int col1, int col2, int scFlag ) {
		itsRange = rng;
		// if ( (scFlag & FirstGet) != 0) {
		// Application.SetScreenUpdating(0);
		// Start.gMMWKB.Activate();
		// }
		int vis = 0;
		if ( (scFlag & FirstAllRestVisibleByFirst) != 0 ) {
			// This request asks the bulk export to give all items of the first row regardless of visibility
			// then, the rest of the rows are given according to visibility of the cells in the same column in the first row
			// This provides a close replication of the way visibility is treated in the VBA only version
			// which is to say that the 2 metadata rows can be hidden by the user, and their values still must be seen by this tool
			// however, when a whole column is hidden, then the metadata for those column shouldn't be seen,
			// yet the header (which is hidden) should still be seen, allowing references to them from applicable metadata,
			// though their columns values will always be blank/empty.  If we didn't allow the headers thru, then hiding an 
			// arbitrary column might result in reference errors instead of the intended silencing of the column.
			vis = 2;
		} else if ( (scFlag & VisibleCellsOnly) != 0 ) {
			vis = 1;
		}
		itsWholeTable = rng.GetQuickTab ( row1, row2, col1, col2, vis );
		//String wholeTab = Application.GetQuickRow(tblR, 1, 1, 35);
		// if ( (scFlag & LastGet) != 0 ) {
		// Application.ActiveWorkbook.Activate();
		// Application.SetScreenUpdating(1);
		// }
		itsFirstRow = row1;
		itsFirstCol = col1;
		itsLastRow = row2 - row1 + 1;
		itsLastCol = col2 - col1 + 1;
		ResetRow ();
		if ( (scFlag & TrackRows) != 0 ) {
			itsRowTrack = new int [ itsLastRow + 1 ];
		}
	}

	public void SetWk ( Workbook wkb, Worksheet wks ) {
		itsWkb = wkb;
		itsWks = wks;
	}

	public int RowLast () {
		return itsLastRow;
	}

	public int ColLast () {
		return itsLastCol;
	}

	// One-based column index, c
	public Range Cell ( int c ) {
		return itsRange.Cells ( itsRow + itsFirstRow - 1, c + itsFirstCol - 1 );
	}

	// One-based row & column index, r, c
	public String GetValue ( int r, int c ) {
		SetRow ( r );
		return GetValue ( c );
	}

	// One-based row index, r
	public void SetRow ( int r ) {
		assert r >= 1 && r <= itsLastRow : "Table.GetValue row out of bounds";
		if ( r != itsRow ) {
			if ( itsRowTrack == null ) {
				// if its earlier, then just start over...
				// we could search backward but then the caller should have used
				// row tracking if they really wanted to jump around
				if ( r < itsRow )
					ResetRow ();
			} else {
				// find the nearest tracked row
				int s = r - 1;
				int x = 0;
				for ( ;; ) {
					x = itsRowTrack [ s ];
					if ( x > 0 || s == 0 ) // found or reached the beginning
						break;
					s--;
				}
				itsRowMark = x;
				itsRow = s + 1;
			}
			while ( itsRow < r ) {
				itsRowMark = itsWholeTable.indexOf ( '\r', itsRowMark ) + 1;
				if ( itsRowTrack != null )
					itsRowTrack [ itsRow ] = itsRowMark; // itsRow+1-1
				itsRow++;
			}
			ResetCol ();
		}
	}

	// One-based column index
	public String GetValue ( int c ) {
		assert c >= 1 && c <= itsLastCol : "Table.GetValue col out of bounds";

		if ( itsColEnd >= 0 && c == itsCol + 1 ) {
			itsCol++;
			itsColMark = itsColEnd + 1;
			FindColEnd ();
		} else {
			if ( c < itsCol )
				ResetCol ();
			if ( c > itsCol )
				FindCol ( c );
			if ( itsColEnd < 0 )
				FindColEnd ();
		}

		if ( itsColMark == itsColEnd )
			return "";
		return Recompose ( itsWholeTable.substring ( itsColMark, itsColEnd ) );
	}

	/// One-based column index
	private void FindCol ( int c ) {
		while ( itsCol < c ) {
			itsColMark = itsWholeTable.indexOf ( '\t', itsColMark ) + 1;
			itsCol++;
		}
		ResetColEnd ();
	}

	private void ResetRow () {
		itsRow = 1; // One-based row index
		itsRowMark = 0;
		ResetCol ();
	}

	private void ResetCol () {
		itsCol = 1; // One-based col index
		itsColMark = itsRowMark;
		ResetColEnd ();
	}

	private void ResetColEnd () {
		itsColEnd = -1;
	}


	private void FindColEnd () {
		itsColEnd = itsWholeTable.indexOf ( '\t', itsColMark );
	}

	private static String Recompose ( String s ) {
		return Strings.Replace ( s, '\\', '1', "\r\t\\" );
		/*
		String ans = Strings.Replace(s, '\\', '1', "\r\t\\");
		if ( !ans.equals(s) ) {
			int a = 1;
		}
		return ans;
		*/
		/*
		s = lang.Replace(s, "\\1", "\r");
		s = lang.Replace(s, "\\2", "\t");
		s = lang.Replace(s, "\\3", "\\");
		return s;
		*/
	}

}
