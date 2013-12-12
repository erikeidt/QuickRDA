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

import java.util.List;

import com.hp.JEB.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L4.Build.*;

public class ReportWriter {

	public Reporting	itsRept;
	public DMIView		itsVW;

	public ReportWriter ( Reporting rept, DMIView vw ) {
		this.itsRept = rept;
		this.itsVW = vw;
	}

	//GenerateReport
	public void generateReport ( String filePath, String wkbName ) {
		Application.StatusBar ( "Generating Report..." );

		Workbooks wkbs = Application.Workbooks ();
		Workbook wkb = wkbs.Add ();
		if ( wkb == null ) {
			wkb = wkbs.Open ( filePath + "\\" + wkbName );
			if ( wkb == null ) {
				wkb = wkbs.Add ();
				wkb.SaveAs ( filePath + "\\" + wkbName );
			}
		}
		Start.gMMWKB.Activate ();

		for ( int i = itsRept.eV.size () - 1; i >= 0; i-- ) {
			Worksheet wks = getSheetNext ( wkb );
			DMIElem m = itsRept.eV.get ( i );
			if ( m != null ) {
				DMISharedNameList snl = NameUtilities.getFirstName ( m );
				int r = wks.SetReport ( 15, snl.itsMCText, snl.itsPlural, filePath, wkbName );
				generateItemReport ( wks, r, m, itsRept.sL, itsRept.oL, itsRept.fL );
			}
		}

		overallReport ( filePath, wkbName, wkb, itsRept.gV, itsRept.eV );
	}

	private Worksheet getSheetNext ( Workbook wkb ) {
		Worksheet wks = null;
		wks = wkb.Worksheets ( "Sheet3" );
		if ( wks == null ) {
			wks = wkb.Worksheets ( "Sheet2" );
			if ( wks == null ) {
				wks = wkb.Worksheets ( "Sheet1" );
				if ( wks == null ) {
					wks = wkb.Worksheets ().Add ();
					Start.gMMWKB.Activate ();
				}
			}
		}
		return wks;
	}

	private int generateItemReport ( Worksheet wks, int r, DMIElem t, List<Reporting.ReportItem> sL, List<Reporting.ReportItem> oL, List<Reporting.ReportItem> fL ) {
		int ecnt = 0;
		int c0 = 3;

		int c = c0;

		r = r + 1;

		int c1;
		c = reportColumnHeaders ( wks, r, c, t, fL, Reporting.HdrTypeEnum.EvaluationOf, Constants.xlThemeColorAccent4 );
		c1 = c;

		DMISharedNameList snl = NameUtilities.getFirstName ( t );
		//Overall Headers
		Range cel = wks.Cells ( r - 1, c );
		cel.SetValue ( "Count of when each " + snl.itsMCText + " is the" );
		cel.Font ().SetItalic ( true );
		cel.SetReportColor ( Constants.xlThemeColorAccent5 );

		cel = wks.Cells ( r + 1, 2 );
		cel.SetValue ( snl.itsPlural );
		cel.Font ().SetBold ( true );
		cel.Font ().SetItalic ( true );
		cel.Merge ();
		cel.SetReportColor ( Constants.xlThemeColorAccent5 );

		c = reportColumnHeaders ( wks, r, c, t, sL, Reporting.HdrTypeEnum.SubjectOf, Constants.xlThemeColorAccent3 );
		c = reportColumnHeaders ( wks, r, c, t, oL, Reporting.HdrTypeEnum.ObjectOf, Constants.xlThemeColorAccent6 );

		if ( c > c1 + 1 ) {
			wks.Range ( wks.Cells ( r - 1, c1 ), wks.Cells ( r - 1, c - 1 ) ).Merge ();
		}

		int c3 = c;
		int k0 = r + 2;
		int k = k0;

		ISet<DMIElem> qV = t.itsFullInstanceSet;

		//Column values/counts
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null && itsVW.inView ( m ) ) {
				wks.Cells ( k, 2 ).SetValue ( NameUtilities.getMCText ( m ) );

				c = c1;

				//for ( int j = 0; j < sL.length; j++ ) {
				//	Reporting.ReportItem rj = sL [ j ];
				for ( Reporting.ReportItem rj : sL ) {
					if ( t.itsIndex == rj.itsReportType.itsIndex ) {
						ecnt = ecnt +
								setValueAndColorBasedOnDefault ( wks.Cells ( k, c ), itsRept.subjectReportCount ( m, rj.itsElementIndex ), rj );
						c = c + 1;
					}
				}

				// for ( int j = 0; j < oL.length; j++ ) {
				//	Reporting.ReportItem rj = oL [ j ];
				for ( Reporting.ReportItem rj : oL ) {
					if ( t.itsIndex == rj.itsReportType.itsIndex ) {
						ecnt = ecnt +
								setValueAndColorBasedOnDefault ( wks.Cells ( k, c ), itsRept.objectReportCount ( m, rj.itsElementIndex ), rj );
						c = c + 1;
					}
				}

				k = k + 1;
			}
		}

		ListObject tbl = wks.ListObjects ().Add ( Constants.xlSrcRange, wks.Range ( wks.Cells ( k0 - 1, 2 ), wks.Cells ( k - 1, c3 - 1 ) ), Constants.xlYes );

		if ( snl.itsPlural == null || "".equals ( snl.itsPlural ) ) {
			tbl.SetName ( snl.itsMCText );
		} else {
			tbl.SetName ( snl.itsPlural.replace ( ' ', '-' ) );
		}

		tbl.SetTableStyle ( "" );
		//tbl.ShowTotals = true

		c = c0;
		//should do a range formula and conditional formatting set
		if ( k > k0 ) {
			//for ( int j = 0; j < fL.length; j++ ) {
			//	Reporting.ReportItem rj = fL [ j ];
			for ( Reporting.ReportItem rj : fL ) {
				if ( t.itsIndex == rj.itsReportType.itsIndex ) {
					ecnt = ecnt +
							setValueAndColorBasedOnFormula ( wks.Range ( wks.Cells ( k0, c ), wks.Cells ( k - 1, c ) ), rj );
					c = c + 1;
				}
			}
		}

		wks.Cells ().Rows ().AutoFit ();

		return ecnt;
	}

	private int reportColumnHeaders ( Worksheet wks, int r, int c0, DMIElem t, List<Reporting.ReportItem> xL, Reporting.HdrTypeEnum fn, int clr ) {
		int c = c0;
		boolean f = false;

		//for ( int j = 0; j < xL.length; j++ ) {
		//	Reporting.ReportItem rj = xL [ j ];
		for ( Reporting.ReportItem rj : xL ) {
			if ( t.itsIndex == rj.itsReportType.itsIndex ) {
				if ( !f ) {
					switch ( fn ) {
					case EvaluationOf :
						wks.Cells ( r, c ).SetValue ( "Evaluation Of" );
						break;
					case SubjectOf :
						wks.Cells ( r, c ).SetValue ( "Subject Of" );
						break;
					case ObjectOf :
						wks.Cells ( r, c ).SetValue ( "Object Of" );
						break;
					}
					wks.Cells ( r, c ).Font ().SetItalic ( true );
					wks.Cells ( r, c ).SetReportColor ( clr );
					f = true;
				}
				switch ( fn ) {
				case EvaluationOf :
					wks.Cells ( r + 1, c ).SetValue ( rj.itsColumnName );
					break;
				case SubjectOf :
					wks.Cells ( r + 1, c ).SetValue ( "-" + NameUtilities.getMCText ( rj.itsProperty ) );
					break;
				case ObjectOf :
					wks.Cells ( r + 1, c ).SetValue ( NameUtilities.getMCText ( rj.itsProperty ) + "-" );
					break;
				}
				wks.Cells ( r + 1, c ).Font ().SetBold ( true );
				wks.Cells ( r + 1, c ).Font ().SetItalic ( true );
				wks.Columns ( c ).SetColumnWidth ( 15 );
				wks.Cells ( r + 1, c ).SetReportColor ( clr );
				c = c + 1;
			}
		}
		if ( c > c0 + 1 ) {
			wks.Range ( wks.Cells ( r, c0 ), wks.Cells ( r, c - 1 ) ).Merge ();
		}
		return c;
	}

	private int setValueAndColorBasedOnDefault ( Range r, int i, Reporting.ReportItem ri ) {
		int ans = 0;
		r.SetValue ( "" + i );
		if ( "1".equals ( ri.itsDefaultValue ) ) {
			if ( i == 1 ) {
				r.SetInteriorColor ( 65280 ); // Green
			} else {
				r.SetInteriorColor ( 255 ); // Red
				ans = ans + 1;
			}
		} else if ( "+".equals ( ri.itsDefaultValue ) ) {
			if ( i > 0 ) {
				r.SetInteriorColor ( 65280 ); // Green
			} else {
				r.SetInteriorColor ( 255 ); // Red
				ans = ans + 1;
			}
		}
		return ans;
	}

	private int setValueAndColorBasedOnFormula ( Range r, Reporting.ReportItem ri ) {
		int ans = 0;

		try {
			r.SetFormula ( "=" + ri.itsFormulaValue );
			//And set up conditional formatting
		} catch ( Exception e ) {
			r.SetValue ( ri.itsFormulaValue );
		}

		return ans;
	}

	private int overallReport ( String filePath, String wkbName, Workbook wkb, ISet<DMIElem> gV, ISet<DMIElem> eV ) {
		Worksheet wks = wkb.Worksheets ().Add ();
		Start.gMMWKB.Activate ();

		int r = wks.SetReport ( 48, "Overall", "Graph Health", filePath, wkbName );
		// Application.ActiveWorkbook.Activate();

		int gc = gV.size ();

		wks.Cells ( r, 1 ).SetValue ( "Number of Graphs" );
		wks.Cells ( r, 2 ).SetValue ( gc );

		int rc;

		if ( gc <= 1 ) {
			int x = r + 1;
			wks.Cells ( x, 1 ).SetValue ( "Number of Disconnected Graphs" );
			wks.Cells ( x, 2 ).SetValue ( gc - 1 );
			int c;
			c = 255;
			c = c * 256;

			wks.Range ( wks.Cells ( r, 2 ), wks.Cells ( x, 2 ) ).SetInteriorColor ( c );
			rc = x + 1;
		} else {
			int x = r + 1;
			wks.Cells ( x, 1 ).SetValue ( "Number of Disconnected Graphs" );
			wks.Cells ( x, 2 ).SetValue ( gc - 1 );
			wks.Range ( wks.Cells ( r, 2 ), wks.Cells ( x, 2 ) ).SetInteriorColor ( 255 );

			x = x + 2;
			wks.Cells ( x, 1 ).SetValue ( "List of Roots for Disconnected Graphs" );

			for ( int i = 0; i < gc; i++ ) {
				DMIElem m = gV.get ( i );
				if ( m != null ) {
					DMISharedNameList snl = NameUtilities.getFirstName ( m );
					if ( snl != null )
						wks.Cells ( x + i, 2 ).SetValue ( snl.itsMCText );
					else if ( m.itsVerb != null ) {
						snl = NameUtilities.getFirstName ( m.itsVerb );
						if ( snl != null )
							wks.Cells ( x + i, 2 ).SetValue ( snl.itsMCText );
					}
					wks.Cells ( x + i, 3 ).SetValue ( m.itsIndex );
				}
			}
			wks.Range ( wks.Cells ( x, 2 ), wks.Cells ( x + gc - 1, 2 ) ).SetInteriorColor ( 255 );
			rc = x + gc + 1;
		}

		rc = rc + 2;


		Range cel = wks.Cells ( rc, 3 );
		cel.SetValue ( "Assessments" );
		cel.Font ().SetBold ( true );
		cel.Font ().SetItalic ( true );
		wks.Range ( cel, wks.Cells ( rc, 5 ) ).Merge ();

		rc = rc + 1;
		cel = wks.Cells ( rc, 3 );
		cel.SetValue ( BuildOptions.gGoodComplete );
		wks.Cells ( rc, 3 ).Font ().SetBold ( true );
		wks.Cells ( rc, 3 ).Font ().SetItalic ( true );
		wks.Cells ( rc, 4 ).SetValue ( BuildOptions.gOK );
		wks.Cells ( rc, 4 ).Font ().SetBold ( true );
		wks.Cells ( rc, 4 ).Font ().SetItalic ( true );
		wks.Cells ( rc, 5 ).SetValue ( BuildOptions.gBadIncomplete );
		wks.Cells ( rc, 5 ).Font ().SetBold ( true );
		wks.Cells ( rc, 5 ).Font ().SetItalic ( true );

		wks.Cells ( rc, 2 ).SetValue ( "Subreport" );
		wks.Cells ( rc, 2 ).Font ().SetBold ( true );
		wks.Cells ( rc, 2 ).Font ().SetItalic ( true );

		for ( int i = 0; i < eV.size (); i++ ) {
			int curRow = rc + i + 1;
			DMIElem m = eV.get ( i );
			if ( m != null ) {
				DMISharedNameList snl = NameUtilities.getFirstName ( m );
				wks.Cells ( curRow, 2 ).SetValue ( snl.itsMCText );
				String g = snl.itsPlural;

				String f;

				//temporary fix, these formula assignments doesn't work when one of the Enumerated items (roles, responsibilities,...) comes up empty
				//probably because the tables aren't properly created, because Excel doesn't think a zero item table makes sense - thanks, Excel

				f = "=COUNTIF(" + g.replace ( ' ', '_' ) + "[[Assessment]],\"" + BuildOptions.gGoodComplete + "\")";
				setCellFormula ( wks.Cells ( curRow, 3 ), f );
				f = "=COUNTIF(" + g.replace ( ' ', '_' ) + "[[Assessment]],\"" + BuildOptions.gOK + "\")";
				setCellFormula ( wks.Cells ( curRow, 4 ), f );
				f = "=COUNTIF(" + g.replace ( ' ', '_' ) + "[[Assessment]],\"" + BuildOptions.gBadIncomplete + "\")";
				setCellFormula ( wks.Cells ( curRow, 5 ), f );
			}
		}

		return rc;
	}

	public void setCellFormula ( Range r, String f ) {
		try {
			r.SetFormula ( f );
		} catch ( Exception e ) {
			lang.errMsg ( "Could not set formula " + f + " on Cell " + r.Address () );
			// e.printStackTrace(Start.gErrLogFile);
			// r.SetValue("x"&f);
		}
	}


}
