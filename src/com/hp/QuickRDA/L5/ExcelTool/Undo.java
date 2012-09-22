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

package com.hp.QuickRDA.L5.ExcelTool;

public class Undo {
	/*
		private Type ChangeRecord;
			Range r;
			boolean f;
			String v;
		End Type;

		private ChangeRecord[] gUndoChangeList;

		public static void StartNewUndo() {
			ReDim gUndoChangeList(0);
		}

		public static void PerformUndo() {
			boolean Sup;
			boolean evt;
			int calc;
			SaveAndSuspendAppSettings calc, Sup, evt;
			On Error GoTo 9999;

			int i;
			for ( i=1-1; i < gUndoChangeList.length; i++ ) {
				ChangeRecord cr;
				cr = gUndoChangeList(i);
				if ( cr.f ) {
					cr.r.Formula = cr.v;
				} else {
					cr.r.Value = cr.v;
				}
			}
			ReDim gUndoChangeList(0);
		9999:;
			SetAppSettings calc, Sup, evt;
		}

		public static void FinishUndo(String msg) {
			if ( gUndoChangeList) > 0 .length {
				Application.OnUndo "Undo " + msg, "PerformUndo";
			}
		}

		public static void CaptureChange(Range r, boolean f) {
			int n;
			n = gUndoChangeList.length + 1;
			ReDim Preserve gUndoChangeList(n);
			ChangeRecord cr;
			cr.r = r;
			if ( f ) {
				cr.v = r.Formula;
				cr.f = true;
			} else {
				cr.v = r.Value;
				cr.f = false;
			}
			gUndoChangeList(n) = cr;
		}

		public static void UpdateLastChangeValue() {
			int n;
			n = gUndoChangeList.length;
			gUndoChangeList(n).v = gUndoChangeList(n - 1).v;
		}
	*/
}
