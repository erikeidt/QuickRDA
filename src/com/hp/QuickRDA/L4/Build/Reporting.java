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

package com.hp.QuickRDA.L4.Build;

import java.util.List;

import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;

public class Reporting {
	public enum HdrTypeEnum {
		EvaluationOf,
		SubjectOf,
		ObjectOf
	}

	public static class ReportItem {
		public int		itsElementIndex;
		public DMIElem	itsProperty;
		public String	itsDefaultValue;
		public DMIElem	itsReportType;
		public String	itsFormulaValue;
		public String	itsColumnName;
	}

	public static class CountType {
		int []	itsCounts;
	}

	private int				itsXTBase;
	private CountType []	itsXReportAsSubjectCounts;
	private CountType []	itsXReportAsObjectCounts;
	private int []			itsXGraphLinkage;
	private DMIElem []		itsXFullGraph;

	private ConceptManager	cm;
	private DMIView			vw;

	public ISet<DMIElem>	gV;
	public ISet<DMIElem>	eV;
	public List<ReportItem>	sL;
	public List<ReportItem>	oL;
	public List<ReportItem>	fL;

	public Reporting ( DMIGraph g, ConceptManager cm, DMIView vw ) {
		this.cm = cm;
		this.vw = vw;

		itsXTBase = g.getMinIndex ();
		int u;
		u = g.getMaxIndex ();

		itsXReportAsSubjectCounts = new CountType [ u - itsXTBase ];
		itsXReportAsObjectCounts = new CountType [ u - itsXTBase ];
		itsXGraphLinkage = new int [ u - itsXTBase ];

		zeroCountArray ( itsXReportAsSubjectCounts );
		zeroCountArray ( itsXReportAsObjectCounts );

		itsXFullGraph = vw.viewToMap ( g );

		eV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		sL = new XSetList<ReportItem> ();
		oL = new XSetList<ReportItem> ();
		fL = new XSetList<ReportItem> ();
	}

	public void analyzeGraph () {
		setupReportCounts ( cm.itsBaseVocab );
		sumCountsInView ( vw );
		gV = analyzeOverallGraph ( vw );
	}

	private void setupReportCounts ( DMIBaseVocabulary b ) {
		eV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		ISet<DMIElem> qV = b.gClass.itsFullInstanceSet; // Reports are on Classes only, at least for now

		//Pass 1: find all report counts defined in the metamodel
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				DMIElementDiagrammingInfo d = m.itsDiagrammingInfo;

				if ( d != null ) {
					if ( d.itsReportEnumerate ) {
						eV.addToList ( m );
					}

					if ( d.itsReportAsSubject != null && !"".equals ( d.itsReportAsSubject ) ) {
						sL = addCSLItemsToList ( m, d.itsReportAsSubject, sL );
					}

					if ( d.itsReportAsObject != null && !"".equals ( d.itsReportAsObject ) ) {
						oL = addCSLItemsToList ( m, d.itsReportAsObject, oL );
					}

					if ( d.itsReportOnFormula != null && !"".equals ( d.itsReportOnFormula ) ) {
						fL = addFMItemToList ( m, d.itsReportOnFormula, fL );
					}
				}
			}
		}
	}

	private void sumCountsInView ( DMIView vw ) {
		ISet<DMIElem> spV = propertyListFromReportItem ( sL );
		ISet<DMIElem> opV = propertyListFromReportItem ( oL );

		int y = spV.size ();
		int z = opV.size ();

		ISet<DMIElem> qV = vw.toSet ();

		//Pass 2: accumulate counts according to the desired reports
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {

				DMIElem vb;
				vb = m.itsVerb;

				if ( vb != null ) {
					int x;
					x = spV.indexOf ( vb );
					if ( x >= 0 ) {
						incrementSubjectReportsCount ( m.itsSubject, x, y );
					}

					x = opV.indexOf ( vb );
					if ( x >= 0 ) {
						incrementObjectReportsCount ( m.itsObject, x, z );
					}
				}
			}
		}

	}

	private ISet<DMIElem> analyzeOverallGraph ( DMIView vw ) {
		//Pass 1: merge separate subgraphs together according to the user-defined relationships
		// approach: set each element to the lowest seen element in the graph using two passes.
		//           Each node has a notion of some subgraph that is in identify by the lowest numbered node sofar in that subgraph
		//           On the first pass we merge graphs and identify pseudo roots (the low numbered nodes in relation)
		//           whenever we encounter a node that has already has a root and has to be switched to another root
		//           it is because we're merging graphs.  So, in addition to setting the local nodes involved,
		//           the roots are also updated to reflect the new merge.
		//           On the second pass, we update all the non-roots by following to their roots.
		//           What's left is a list of roots and if there's more than one, we've got disconnected graphs.
		ISet<DMIElem> ans = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		ISet<DMIElem> qV = vw.toSet ();
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				if ( m.isStatement () ) {
					linkStatement ( m, m.itsSubject, m.itsObject );
					/*
					boolean siv = vw.InView(m.itsSubject);
					boolean oiv = vw.InView(m.itsObject);

					if ( siv && oiv ) {
						LinkStatement(m, m.itsSubject, m.itsObject);
					} else if ( siv ) {
						LinkStatement(null, m, m.itsSubject);
					} else if ( oiv ) {
						LinkStatement(null, m, m.itsObject);
					}
					*/
				}

				setGraphLinkageFromSet ( m, m.attachedToSet () );
				//if ( m.itsAttachedTo != null ) {
				//    LinkThree null, m, m.itsAttachedTo
				//}

				setGraphLinkageFromSet ( m, m.groupedToSet () );
				//if ( m.itsGroupTo != null ) {
				//    LinkThree null, m, m.itsGroupTo
				//}

				DMISharedNameList snl = NameUtilities.getFirstName ( m );
				if ( snl != null ) {
					//This should be done in inferencing -
					// by establishing the proper formal link from the differentiated item to the base concept
					// (which can be hidden from graphing by level)
					//instead of being done here, but well...
					String x = NameUtilities.trimDifferentiator ( snl.itsLCKey );
					if ( !x.equals ( NameUtilities.getLCKey ( m ) ) ) {
						DMIElem n = cm.findConcept ( x, null );
						if ( n != null )
							linkStatement ( null, m, n );
					}
				}
			}
		}

		//Fix all the non-root nodes to point to the true roots.
		for ( int i = 0; i < qV.size (); i++ ) {
			DMIElem m = qV.get ( i );
			if ( m != null ) {
				DMIElem xm = m;
				int xi = graphLinkage ( xm );

				if ( xi == 0 ) {
					xi = m.itsIndex;
				} else {
					for ( ;; ) {
						if ( xi == xm.itsIndex ) {
							break;
						} else {
							DMIElem xn = graphElem ( xi );
							assert xn != null : "Reporting.AnalyzeOverallGraph: GraphElem for " + xi + " is null";
							if ( xn == null ) {
								break;
							}
							xm = xn;
							xi = graphLinkage ( xm );
						}
					}
					setGraphLinkage ( m, xi );
				}

				//[New] roots are accumulated to a list
				ans.addToSet ( xm );
			}
		}
		return ans;
	}

	private void zeroCountArray ( CountType [] cL ) {
		for ( int i = 0; i < cL.length; i++ ) {
			cL [ i ] = new CountType ();
			cL [ i ].itsCounts = new int [ 0 ];
		}
	}

	private void setGraphLinkageFromSet ( DMIElem x, ISet<DMIElem> mV ) {
		for ( int i = 0; i < mV.size (); i++ ) {
			DMIElem m = mV.get ( i );
			if ( m != null )
				linkStatement ( null, x, m );
		}
	}

	private void linkStatement ( DMIElem vb, DMIElem sb, DMIElem ob ) {
		int minLink = 0;

		ISet<DMIElem> xV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );

		if ( vw.inView ( sb ) )
			minLink = addElementAndRootToList ( sb, xV, minLink );
		if ( vw.inView ( ob ) )
			minLink = addElementAndRootToList ( ob, xV, minLink );
		if ( vb != null && vw.inView ( vb ) )
			minLink = addElementAndRootToList ( vb, xV, minLink );

		for ( int i = 0; i < xV.size (); i++ ) {
			DMIElem m = xV.get ( i );
			if ( m != null )
				setGraphLinkage ( m, minLink );
		}
	}

	private int addElementAndRootToList ( DMIElem m, ISet<DMIElem> xV, int minLinkIn ) {
		xV.addToSet ( m );

		int aLink = graphLinkage ( m );
		if ( aLink == 0 ) {
			aLink = m.itsIndex;
		} else if ( aLink != m.itsIndex ) {
			DMIElem x = graphElem ( aLink );
			if ( x != null ) {
				aLink = addElementAndRootToList ( x, xV, minLinkIn );
			}
		}

		if ( minLinkIn != 0 && minLinkIn < aLink ) {
			aLink = minLinkIn;
		}

		return aLink;
	}

	private ISet<DMIElem> propertyListFromReportItem ( List<ReportItem> rL ) {
		ISet<DMIElem> mV = new XSetList<DMIElem> ( XSetList.AsSet, XSetList.HashOnDemand );
		/*
		int u = rL.length;
		for ( int i = 0; i < u; i++ ) {
			mV.addToList  (rL [ i ].itsProperty);
		}
		*/
		for ( ReportItem r : rL )
			mV.addToList ( r.itsProperty );
		return mV;
	}

	private List<ReportItem> addCSLItemsToList ( DMIElem m, String csl, List<ReportItem> xL ) {
		StringRef xx = new StringRef ();
		String f = Strings.splitAfterComma ( csl, xx );
		csl = xx.str;
		while ( !"".equals ( f ) ) {
			ReportItem ri = new ReportItem ();
			String p = Strings.splitAfter ( f, xx, "=" );
			f = xx.str;
			DMIElem x = cm.findConcept ( p, null );
			if ( x == null ) {
				lang.errMsg ( "Report Forumla references an unknown relationship: " + p );
			} else {
				if ( x.oreferredSet ().size () > 0 ) {
					x = x.oreferredSet ().get ( 0 );
				}
				ri.itsProperty = x;
				ri.itsReportType = m;
				ri.itsDefaultValue = f;

				ri.itsElementIndex = xL.size ();
				xL.add ( ri );
				f = Strings.splitAfterComma ( csl, xx );
				csl = xx.str;
			}
		}
		return xL;
	}

	private List<ReportItem> addFMItemToList ( DMIElem m, String fm, List<ReportItem> xL ) {
		StringRef xx = new StringRef ();
		String f = Strings.splitAfter ( fm, xx, ";" );
		fm = xx.str;
		while ( !"".equals ( f ) ) {
			ReportItem ri = new ReportItem ();
			String p = Strings.splitAfter ( f, xx, "=" );
			f = xx.str;
			ri.itsFormulaValue = f;
			ri.itsColumnName = p;
			ri.itsReportType = m;

			ri.itsElementIndex = xL.size ();
			xL.add ( ri );
			f = Strings.splitAfter ( fm, xx, ";" );
			fm = xx.str;
		}
		return xL;
	}

	private int graphLinkage ( DMIElem m ) {
		return itsXGraphLinkage [ m.itsIndex - itsXTBase ];
	}

	private void setGraphLinkage ( DMIElem m, int ix ) {
		itsXGraphLinkage [ m.itsIndex - itsXTBase ] = ix;
	}

	private DMIElem graphElem ( int ix ) {
		return itsXFullGraph [ ix - itsXTBase ];
	}


	private void incrementSubjectReportsCount ( DMIElem m, int x, int mx ) {
		int ix = m.itsIndex;

		CountType ct = itsXReportAsSubjectCounts [ ix - itsXTBase ];
		if ( ct.itsCounts == null || ct.itsCounts.length == 0 ) {
			ct.itsCounts = lang.expandPreserve ( ct.itsCounts, mx );
		}
		ct.itsCounts [ x ]++;
	}

	private void incrementObjectReportsCount ( DMIElem m, int x, int mx ) {
		int ix = m.itsIndex;

		CountType ct = itsXReportAsObjectCounts [ ix - itsXTBase ];
		if ( ct.itsCounts == null || ct.itsCounts.length == 0 ) {
			ct.itsCounts = lang.expandPreserve ( ct.itsCounts, mx );
		}
		ct.itsCounts [ x ]++;
	}

	public int subjectReportCount ( DMIElem m, int x ) {
		int ix = m.itsIndex;

		CountType ct = itsXReportAsSubjectCounts [ ix - itsXTBase ];
		if ( ct.itsCounts == null || ct.itsCounts.length == 0 ) {
			return 0;
		} else {
			return ct.itsCounts [ x ];
		}
	}

	public int objectReportCount ( DMIElem m, int x ) {
		int ix = m.itsIndex;

		CountType ct = itsXReportAsObjectCounts [ ix - itsXTBase ];
		if ( ct.itsCounts == null || ct.itsCounts.length == 0 ) {
			return 0;
		} else {
			return ct.itsCounts [ x ];
		}
	}

}
