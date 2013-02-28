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

import java.util.ArrayList;
import java.util.List;

import com.hp.JEB.*;
import com.hp.QuickRDA.Excel.*;
import com.hp.QuickRDA.L0.lang.*;
import com.hp.QuickRDA.L1.Core.*;
import com.hp.QuickRDA.L2.Names.*;
import com.hp.QuickRDA.L4.Build.*;

public class SourceUnitReader {

	private static final int	kTableColNameRow		= 1;
	private static final int	kTableTypeRow			= 2;
	private static final int	kTableFormulaRow		= 3;
	public static final int		kTableToFirstRowOffset	= 3;

	private enum ColumnRelationExpressionEnum {
		NoneEmptyBlank,
		ConstantString,
		ColumnReference,
		AttributeName
	}

	private enum CellStateEnum {
		NotStarted,
		Pass1Done,
		Pass2Done
	}

	private static class FChain {
		public ColumnRelationExpressionEnum	opType;
		public boolean						includeRelations;
		public boolean						includeEntities;
		public String						nameStr;
		public int							colIndex;
		public FChain						chain;

		public FChain () {
			this.opType = ColumnRelationExpressionEnum.NoneEmptyBlank;
		}
	}

	private enum FCResultStatusEnum {
		NoneBlankEmpty,
		ColumnCellValues,
		ConstantValue
	}

	private static class FCResultRecord {
		FCResultStatusEnum	status;

		int					colIndex;	// when ColumnCellValues

		DMIElem				typ;		// when ConstantValue
		DMIElem				val;		// when ConstantValue

		private FCResultRecord () {
			this.status = FCResultStatusEnum.NoneBlankEmpty;
		}
	}

	private static class PSOSet {
		public boolean	hidden;
		FChain			p;
		FChain			s;
		FChain			o;
	}

	private static class SubscriptHolder {
		FChain		  subscript;
		String        prefix;
		String        suffix;
		public SubscriptHolder(FChain subs, String pref, String suf) {
			this.subscript = subs;
			this.prefix    = pref;
			this.suffix    = suf;
		}
	}
	private static class PSOHolder {
		boolean		  hasNode;
		PSOSet []	  pso;
		DMIElem		  tm;
		List<SubscriptHolder>  subscriptChain;
	}

	private static class VALHolder {
		CellStateEnum	state;
		int				vPos;
		int				vLen;
		DMIElem []		vL;
		DMIElem []		tL;

		public VALHolder () {
			this.state = CellStateEnum.NotStarted;
		}
	}


	private ConceptManager		itsConceptMgr;
	private DMIBaseVocabulary	itsBaseVocab;
	private BuildOptions		itsOptions;
	private PSOHolder []		itsPSOL;


	public SourceUnitReader ( ConceptManager cm1, BuildOptions options ) {
		itsConceptMgr = cm1;
		itsBaseVocab = itsConceptMgr.itsBaseVocab;
		itsOptions = options;
	}

	public void buildDeclarativeTable ( TableReader hdrTab, TableReader bodyTab, int [] highlightRows, List<String> columnExclusions ) {
		itsConceptMgr.incProvenanceRow ();
		itsConceptMgr.incProvenanceRow ();

		buildPSOFromTable ( hdrTab, columnExclusions );
		buildVALFromTable ( bodyTab, highlightRows );
	}

	private void buildPSOFromTable ( TableReader hdrTab, List<String> columnExclusions ) {
		itsPSOL = new PSOHolder [ 1 ];

		for ( int c = 1; c <= hdrTab.ColLast (); c++ )
			itsPSOL = addToList ( buildPSOFromColumn ( hdrTab, c, columnExclusions ), itsPSOL );
	}

	private enum EntityTypeEnum {
		Property,
		Concept,
		Literal
	}

	//
	// Formula cells as follows:
	//   blank implies: generate node as if ":" were present
	//   : directs node generation from cell value as name, using column type
	//   "<<" begins definition of subscript formula
	//   ">>" ends definition of subscript formula
	//   "," separates items in a formula chain
	//   ^ directs link generation from relation, subject, object taken as follows
	//   "", i.e. blank means cell value taken as value
	//   name, use this as constant as the value
	//   !name, i.e. bang name, use the main concepts from this row but column name as value
	//   "!", i.e. bang blank relation has no meaning
	//	 "!+" use all entities and relationships from the named column
	//	 "!-" use only relationships from the named column
	//   ">" Selection of alternate columns if previous is blank !PreferedColumnName>AlternateColumnName...
	//    TODO Executive decision: ColumnName = [A-Za-z][A-Za-z0-9_/ ]+
	//    TODO Backslash escape mechanism !Column3\Fred

	private PSOHolder buildPSOFromColumn ( TableReader hdrTab, int c, List<String> columnExclusions ) {
		boolean hasNode = false;

		PSOSet [] pso = new PSOSet [ 0 ];
		PSOHolder psoL = new PSOHolder ();
		psoL.subscriptChain = new ArrayList<SubscriptHolder>();
		DMIElem tm = null;

		String t = columnTypeValue ( hdrTab, c );
		StringRef xx = new StringRef ();

		//don't handle typeless columns in this format; skip whole column
		//also allow ~ to comment out column
		if ( !"".equals ( t ) && !t.startsWith ( "~" ) && !inExclusionList ( hdrTab, c, columnExclusions ) ) {
			tm = TypeExpressions.getTypeFromNameExpression ( t, itsConceptMgr, itsBaseVocab );

			String f = formulaValue ( hdrTab, c );

			if ( "".equals ( f ) ) {
				hasNode = true;
			}

			//Check to see if making node
			if ( Strings.initialMatch ( f, xx, ":" ) ) {
				f = xx.str;
				hasNode = true;

				Boolean done = false;
				do {
					if ( Strings.initialMatch ( f, xx, "<<" ) ) {
						f = xx.str;
						psoL.subscriptChain.add (new SubscriptHolder( parseLinkSubscript ( hdrTab, f, xx, c, true, ">>" ),"<<",">>"));
						f = xx.str;
					} else if ( Strings.initialMatch ( f, xx, "(" ) ) {
						f = xx.str;
						psoL.subscriptChain.add (new SubscriptHolder( parseLinkSubscript ( hdrTab, f, xx, c, true, ")" ),"(",")"));
						f = xx.str;
					} else {
						done = true;
					}
				} while ( !done );
			}

			//Check to see if making edge
			while ( Strings.initialMatch ( f, xx, "^" ) ) {
				f = xx.str;
				pso = addToList ( parseLinkFormula ( hdrTab, f, xx, c ), pso );
				f = xx.str;
			}
		}


		psoL.hasNode = hasNode;
		psoL.pso = pso;
		psoL.tm = tm;
		return psoL;
	}

	private boolean inExclusionList ( TableReader hdrTab, int c, List<String> columnExclusions ) {
		if ( columnExclusions == null )
			return false;

		String colHdr = hdrTab.GetValue ( kTableColNameRow, c );
		for ( int i = 0; i < columnExclusions.size (); i++ )
			if ( columnExclusions.get ( i ).equals ( colHdr ) )
				return true;

		return false;
	}

	private void buildVALFromTable ( TableReader tblTab, int [] highlightRows ) {
		int ccnt = tblTab.ColLast ();

		boolean commentColumn = false;
		if ( itsPSOL [ 1 ].tm == this.itsBaseVocab.gNothing )
			commentColumn = true;

		for ( int r = 1; r <= tblTab.RowLast (); r++ ) {
			itsConceptMgr.incProvenanceRow ();
			// Range tblR1 = tblR.Rows(r);

			VALHolder [] val = new VALHolder [ ccnt + 1 ];
			allocateValHolders ( val );

			for ( int c = 1; c <= ccnt; c++ ) {
				boolean done = false;
				val [ c ].vL = new DMIElem [ 0 ]; //shouldn't be necessary due to "done"
				if ( itsPSOL [ c ].pso.length == 0 && !itsPSOL [ c ].hasNode )
					done = true;
				if ( done )
					val [ c ].state = CellStateEnum.Pass2Done;
			}

			tblTab.SetRow ( r );
			boolean highlight = false;

			if ( highlightRows != null ) {
				// if ( highlightR.Parent() == tblR1.Parent() ) {
				highlight = (lang.indexOf ( r, highlightRows ) >= 0);
			}
			itsConceptMgr.setHighlight ( highlight );

			for ( int c = 1; c <= ccnt; c++ ) {
				if ( c == 1 && commentColumn ) {
					// if this is a comment Columnm
					String n = Strings.ztrim ( tblTab.GetValue ( c ) );
					// skip comment column if it is blank
					if ( "".equals ( n ) )
						continue;
					// skip whole row if first column is non-blank
					break;
				}
				if ( val [ c ].state != CellStateEnum.Pass2Done ) {
					buildVALFromColumn ( val, c, tblTab, false );
					buildVALFromColumn ( val, c, tblTab, true );
				}
			}
		}
	}

	private static void allocateValHolders ( VALHolder [] val ) {
		for ( int c = 1; c < val.length; c++ )
			val [ c ] = new VALHolder ();
	}

	private int buildVALFromColumn ( VALHolder [] val, int c, TableReader tblTab, boolean pass2 ) {
		if ( !pass2 && val [ c ].state == CellStateEnum.Pass1Done || pass2 && val [ c ].state == CellStateEnum.Pass2Done ) {
			if ( val [ c ].vLen > 0 ) {
				return c;
			} else {
				return 0;
			}
		}

		StringRef xx = new StringRef ();
		boolean hasNode = itsPSOL [ c ].hasNode;

		PSOSet [] pso = itsPSOL [ c ].pso;

		DMIElem tm = itsPSOL [ c ].tm;

		DMIElem [] node = new DMIElem [ 0 ];
		DMIElem [] nodeT = new DMIElem [ 0 ];
		DMIElem [] edge = new DMIElem [ 0 ];
		DMIElem [] edgeT = new DMIElem [ 0 ];

		String n = tblTab.GetValue ( c );

		boolean exactLiteral = tm.subclassOf ( itsBaseVocab.gString );
		if ( !exactLiteral )
			n = Strings.ztrim ( n ); //was XTrim

		//skip blank cells; can't think of a reason not to
		if ( !"".equals ( n ) ) {


			n += getSubscript ( c, tblTab );
			
			if ( hasNode ) {
				if ( exactLiteral ) {
					DMIElem nd = itsConceptMgr.enterTypedConcept ( n, tm, true );

					if ( nd != null ) {
						node = DMIElem.addToList ( nd, node );
						nodeT = DMIElem.addToList ( tm, nodeT );
					}
				}
				else {
					String n2 = n;
					for ( ;; ) {
						String N1 = Strings.splitafterItemSeparator ( n2, xx );
						n2 = xx.str;

						DMIElem nd = itsConceptMgr.enterTypedConcept ( N1, tm, true );

						if ( nd != null ) {
							node = DMIElem.addToList ( nd, node );
							nodeT = DMIElem.addToList ( tm, nodeT );
						}

						if ( "".equals ( n2 ) )
							break;
					}
				}
			}

			if ( pass2 ) {
				for ( int i = 0; i < pso.length; i++ ) {
					/*
					DMIElem pt;
					if ( hasNode ) {
						//if this column generates a node by formula, then any properties here are not of the node type
						pt = null;
					} else {
						//if this column doesn't generate a node by formula, then the type of the property is available
						pt = tm;
					}
					*/

					FCResultRecord si; // = new FCResultRecord();
					FCResultRecord oi; // = new FCResultRecord();

					if ( pso [ i ].p.opType == ColumnRelationExpressionEnum.AttributeName ) {
						si = followFChainToSubjectOrObject ( val, pso [ i ].s, tblTab, c );
						switch ( si.status ) {
						case NoneBlankEmpty :
							break;
						case ColumnCellValues :
							int ci = si.colIndex;
							int pos = 0;
							int len = val [ ci ].vLen;
							if ( !pso [ i ].s.includeEntities )
								pos = val [ ci ].vPos;
							if ( pso [ i ].s.includeRelations )
								len = val [ ci ].vL.length;
							for ( int ix = pos; ix < len; ix++ ) {
								enterColumnAttribute ( val [ ci ].vL [ ix ], pso [ i ].p.nameStr, n, tblTab, c );
							}
							break;
						case ConstantValue :
							enterColumnAttribute ( si.val, pso [ i ].p.nameStr, n, tblTab, c );
							break;
						}
					} else {
						String pn = getPropertyNameFromExpression ( n, pso [ i ].p );
						if ( "".equals ( pn ) )
							break;

						// Kludge -- forwarding edge information related to previously created edges for this same pass, same column...
						if ( !hasNode && edge.length > 0 ) {
							val [ c ].vL = edge;
							val [ c ].tL = edgeT;
						}

						si = followFChainToSubjectOrObject ( val, pso [ i ].s, tblTab, c );
						oi = followFChainToSubjectOrObject ( val, pso [ i ].o, tblTab, c );

						/*
						if ( si.status != FCResultStatusEnum.ColumnCellValues || oi.status == FCResultStatusEnum.NoneBlankEmpty )
							break;
						*/
						if ( si.status == FCResultStatusEnum.ColumnCellValues && oi.status != FCResultStatusEnum.NoneBlankEmpty ) {

							int ci = si.colIndex;
							int sPos = 0;
							int sLen = val [ ci ].vLen;
							if ( !pso [ i ].s.includeEntities )
								sPos = val [ ci ].vPos;
							if ( pso [ i ].s.includeRelations )
								sLen = val [ ci ].vL.length;
							for ( int ix = sPos; ix < sLen; ix++ ) {
								if ( oi.status == FCResultStatusEnum.ConstantValue ) {
									BuildRelationResult brr = buildAndRecordRelation ( val [ ci ].vL [ ix ], val [ ci ].tL [ ix ], pn, oi.val, oi.typ, edge, edgeT, tblTab, c, pso [ i ].hidden );
									edge = brr.edge;
									edgeT = brr.edgeT;
								} else {
									int di = oi.colIndex;
									int oPos = 0;
									int oLen = val [ di ].vLen;
									if ( !pso [ i ].o.includeEntities )
										oPos = val [ di ].vPos;
									if ( pso [ i ].o.includeRelations )
										oLen = val [ di ].vL.length;
									for ( int jx = oPos; jx < oLen; jx++ ) {
										BuildRelationResult brr = buildAndRecordRelation ( val [ ci ].vL [ ix ], val [ ci ].tL [ ix ], pn, val [ di ].vL [ jx ], val [ di ].tL [ jx ], edge, edgeT, tblTab, c, pso [ i ].hidden );
										edge = brr.edge;
										edgeT = brr.edgeT;
									}
								}
							}
						}
					}
				} //link
			}
		}

		int ans = 0;
		if ( node.length > 0 ) {
			val [ c ].vPos = node.length;
			val [ c ].vLen = node.length;
			ans = c;
			if ( edge.length > 0 ) {
				val [ c ].vL = new DMIElem [ node.length + edge.length ];
				val [ c ].tL = new DMIElem [ node.length + edge.length ];
				System.arraycopy ( node, 0, val [ c ].vL, 0, node.length );
				System.arraycopy ( edge, 0, val [ c ].vL, node.length, edge.length );
				System.arraycopy ( nodeT, 0, val [ c ].tL, 0, node.length );
				System.arraycopy ( edgeT, 0, val [ c ].tL, node.length, edge.length );
			} else {
				val [ c ].vL = node;
				val [ c ].tL = nodeT;
			}
		} else if ( edge.length > 0 ) {
			val [ c ].vPos = 0;
			val [ c ].vLen = edge.length;
			val [ c ].vL = edge;
			val [ c ].tL = edgeT;
			ans = c;
		} else {
			val [ c ].vPos = 0;
			val [ c ].vLen = 0;
			val [ c ].vL = node;
			val [ c ].tL = nodeT;
		}

		if ( pass2 ) {
			val [ c ].state = CellStateEnum.Pass2Done;
		} else {
			val [ c ].state = CellStateEnum.Pass1Done;
		}
		return ans;
	}

	private static class BuildRelationResult {
		DMIElem []	edge;
		DMIElem []	edgeT;

		BuildRelationResult ( DMIElem [] e, DMIElem [] eT ) {
			edge = e;
			edgeT = eT;
		}
	}

	private BuildRelationResult buildAndRecordRelation ( DMIElem sbIn, DMIElem sbTyp, String pn, DMIElem obIn, DMIElem obTyp,
			DMIElem [] edge, DMIElem [] edgeT, TableReader tblTab, int colNum, boolean hidden ) {
		booleanRef added = new booleanRef ();

		DMIElem pm = itsConceptMgr.enterProperty ( pn, sbTyp, obTyp, false, added );
		if ( added.bool ) {
			errMsg ( false, tblTab, colNum, true, pn, "The named relationship was not found in the metamodel." );
			/*
			lang.ErrMsg("In Workbook:\t" + tblTab.itsRange.Parent().Parent().Name() +
						"\r\nOn Worksheet:\t" + tblTab.itsRange.Parent().Name() +
						"\r\nIn Column:\t" + ColumnName(tblTab.itsRange, colNum) + "  (#" + colNum + " in table)" +
						"\r\nOn Row:\t" + tblTab.Cell(colNum).Row() +
						"\r\nRegarding:\t" + pn +
						"\r\n  Warning:\t\r\n");
			*/
		}

		if ( !pm.subclassOf ( itsConceptMgr.itsBaseVocab.gNothing ) ) {
			DMIElem sb;
			DMIElem ob;

			//swap to preferred form when there's an inverse property
			if ( pm.inverseSet ().size () > 0 ) {
				sb = obIn;
				ob = sbIn;
				pm = pm.inverseSet ().get ( 0 );
			} else {
				sb = sbIn;
				ob = obIn;
			}

			//use preferred form when there is one
			if ( pm.oreferredSet ().size () > 0 ) {
				pm = pm.oreferredSet ().get ( 0 );
			}

			if ( ob != itsBaseVocab.gfalse ) {
				int u = edge.length;
				edge = DMIElem.expandPreserve ( edge, u + 1 );
				edgeT = DMIElem.expandPreserve ( edgeT, u + 1 );

				DMISubgraph defSG = null;
				if ( itsOptions.gOptionAutoHide && hidden )
					defSG = itsConceptMgr.setDefaultSubgraph ( itsConceptMgr.itsInvisiblexSG );

				edge [ u ] = itsConceptMgr.enterStatement ( sb, pm, ob, itsOptions.gOptionAutoHide );
				edgeT [ u ] = pm;

				if ( defSG != null )
					itsConceptMgr.setDefaultSubgraph ( defSG );
			}
		}
		return new BuildRelationResult ( edge, edgeT );
	}

	private String getPropertyNameFromExpression ( String def, FChain fc ) {
		String ans = "";

		if ( fc.opType == ColumnRelationExpressionEnum.ColumnReference && fc.colIndex > 0 ) {
			; // assert !(fc.opType == ColumnRelationExpressionEnum.ColumnReference && fc.colIndex > 0) : "non-self property reference";
		} else if ( fc.opType == ColumnRelationExpressionEnum.AttributeName ) {
			assert fc.opType != ColumnRelationExpressionEnum.AttributeName : "attribute property reference";
		} else if ( fc.opType == ColumnRelationExpressionEnum.ConstantString ) {
			if ( "".equals ( fc.nameStr ) ) {
				ans = def;
			} else {
				ans = fc.nameStr;
			}
		}

		return ans;
	}

	private FCResultRecord followFChainToSubjectOrObject ( VALHolder [] val, FChain fcIn, TableReader tblTab, int c ) {
		FChain fc = fcIn;
		FCResultRecord fcrs = new FCResultRecord ();

		if ( fc.opType == ColumnRelationExpressionEnum.ColumnReference && fc.colIndex > 0 ) {
			for ( ;; ) {
				//if we're referencing ourself, don't do pass2!
				int ci = buildVALFromColumn ( val, fc.colIndex, tblTab, fc.colIndex != c );
				if ( ci == 0 ) {
					fc = fc.chain;
					if ( fc == null ) {
						return fcrs;
					}
					continue;
				}
				fcrs.status = FCResultStatusEnum.ColumnCellValues;
				fcrs.colIndex = fc.colIndex;
				break;
			}
		} else if ( fc.opType == ColumnRelationExpressionEnum.AttributeName )
			;
		else if ( fc.opType == ColumnRelationExpressionEnum.ConstantString ) {
			DMIElem m = itsConceptMgr.findConcept ( fc.nameStr, itsBaseVocab.gboolean );

			boolean takeIt = (m != null);
			if ( takeIt ) {
				takeIt = !m.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel );
			}

			if ( takeIt ) {
				fcrs.status = FCResultStatusEnum.ConstantValue;
				fcrs.val = m;
				fcrs.typ = m.itsDeclaredTypeList.get ( 0 );
			} else {
				if ( fc.nameStr.startsWith ( "^" ) )
					errMsg ( true, tblTab, c, false, fc.nameStr, "Either there a comma is missing in the column-specified relationship list, or, an obsolete feature that has been removed is being used." );
				else
					errMsg ( true, tblTab, c, false, fc.nameStr, "An obsolete feature that has been removed is being used.  Move the value '" +
							fc.nameStr + "' from the relationship metadata into column value cells in an appropriate column." );
				/*
				lang.ErrMsg("In Workbook:\t" + tblTab.itsRange.Parent().Parent().Name() +
							"\r\nOn worksheet:\t" + tblTab.itsRange.Parent().Name() +
							"\r\nIn Column:\t" + ColumnName(tblTab.itsRange, c) +
							"\r\nRegarding:\t" + fc.nameStr +
							"\r\n    Error:\tAn obsolete feature has been removed.  Move the value '" +
								fc.nameStr + "' from the relationship metadata into column value cells in an appropriate column.\r\n\r\n");
				 */
			}
		}

		return fcrs;
	}

	private PSOSet parseLinkFormula ( TableReader hdrTab, String f, StringRef xx, int c ) {
		PSOSet pso = new PSOSet ();

		if ( Strings.initialMatch ( f, xx, "-" ) ) {
			f = xx.str;
			pso.hidden = true;
		}

		pso.p = parseLinkUnit ( hdrTab, f, xx, c, EntityTypeEnum.Property );
		f = xx.str;
		pso.s = parseLinkUnit ( hdrTab, f, xx, c, EntityTypeEnum.Concept );
		f = xx.str;
		pso.o = parseLinkUnit ( hdrTab, f, xx, c, EntityTypeEnum.Concept );
		f = xx.str;
		return pso;
	}

	//Updates to parameter f
	private FChain parseLinkUnit ( TableReader hdrTab, String f, StringRef xx, int c, EntityTypeEnum et ) {
		FChain ifc = new FChain ();
		FChain fc = ifc;

		String v = Strings.splitAfterComma ( f, xx ).trim ();
		f = xx.str;

		if ( Strings.initialMatch ( v, xx, "!" ) ) {
			v = xx.str;

			if ( Strings.initialMatch ( v, xx, "+" ) ) {
				v = xx.str;
				fc.includeEntities = true;
				fc.includeRelations = true;
			} else if ( Strings.initialMatch ( v, xx, "-" ) ) {
				v = xx.str;
				fc.includeEntities = false;
				fc.includeRelations = true;
			} else {
				fc.includeEntities = true;
				fc.includeRelations = false;
			}

			String v1 = v;
			for ( ;; ) {
				fc.nameStr = Strings.splitAfterGtr ( v1, xx );
				v1 = xx.str;

				fc.colIndex = findColIndexFromCurrent ( hdrTab, fc.nameStr, c );

				if ( fc.colIndex == 0 ) {
					errMsg ( true, hdrTab, c, false, fc.nameStr, "The column reference was not found." );
					/*
					lang.ErrMsg("In Workbook:\t" + hdrTab.itsRange.Parent().Parent().Name() +
							"\r\nOn Worksheet:\t" + hdrTab.itsRange.Parent().Name() +
							"\r\nIn Column:\t" + ColumnName(hdrTab.itsRange, c) + "  (#" + c + " in table)" +
							"\r\nRegarding:\t" + fc.nameStr +
							"\r\n    Error:\tThe column reference was not found.\r\n");
					*/
				}

				fc.opType = ColumnRelationExpressionEnum.ColumnReference;
				if ( !"".equals ( v1 ) ) {
					fc.chain = new FChain ();
					fc = fc.chain;
					continue;
				}
				break;
			}
		} else if ( Strings.initialMatch ( v, xx, "." ) ) {
			v = xx.str;
			fc.opType = ColumnRelationExpressionEnum.AttributeName;
			fc.nameStr = v;
		} else if ( "".equals ( v ) && !et.equals ( EntityTypeEnum.Property ) ) {
			fc.opType = ColumnRelationExpressionEnum.ColumnReference;
			fc.nameStr = "";
			fc.colIndex = c;
			fc.includeEntities = true;
		} else {
			fc.opType = ColumnRelationExpressionEnum.ConstantString;
			fc.nameStr = v;
			if ( !"".equals ( v ) ) {
				if ( et.equals ( EntityTypeEnum.Property ) ) {
					DMIElem m = itsConceptMgr.findConcept ( v, null );
					if ( m == null ) {
						errMsg ( false, hdrTab, c, false, v, "No relationship of that name is in the currently loaded metamodel(s)." );
						/*
						lang.ErrMsg("In Workbook:\t" + hdrTab.itsRange.Parent().Parent().Name() +
									"\r\nOn Worksheet:\t" + hdrTab.itsRange.Parent().Name() +
									"\r\nIn Column:\t" + ColumnName(hdrTab.itsRange, c) + "  (#" + c + " in table)" +
									"\r\nRegarding:\t" + v +
									"\r\n  Warning:\tNo relationship of that name is in the currently loaded metamodel(s).\r\n");
						*/
					}
				} else if ( et.equals ( EntityTypeEnum.Concept ))  {
					DMIElem m = itsConceptMgr.findConcept ( v, itsBaseVocab.gboolean );

					// duplicated code... yuk!
					boolean takeIt = (m != null);
					if ( takeIt ) {
						takeIt = !m.itsSubgraph.atLevel ( DMISubgraph.SubgraphLevelEnum.kDomainModel );
					}

					if ( !takeIt ) {
						errMsg ( false, hdrTab, c, false, v, "No concept of that name is in the currently loaded metamodel(s)." );
						/*
						lang.ErrMsg("In Workbook:\t" + hdrTab.itsRange.Parent().Parent().Name() +
									"\r\nOn Worksheet:\t" + hdrTab.itsRange.Parent().Name() +
									"\r\nIn Column:\t" + ColumnName(hdrTab.itsRange, c) + "  (#" + c + " in table)" +
									"\r\nRegarding:\t" + v +
									"\r\n  Warning:\tNo concept of that name is in the currently loaded metamodel(s).\r\n");
						*/
					}

				}
			}
		}

		xx.str = f;
		return ifc;
	}
	
	private FChain parseLinkSubscript ( TableReader hdrTab, String f, StringRef xx, int c, boolean isProperty, String endMatch ) {
		FChain ifc = new FChain ();
		FChain fc = ifc;
		
		String s = Strings.splitAfter ( f, xx, endMatch );
		f = xx.str;
		if ( "".equals ( s ) ) {
			errMsg ( false, hdrTab, c, false, s, "Missing '" + endMatch + "' in subscript expression." );
		}
		while ( !"".equals ( s ) ) {
			fc.chain = parseLinkUnit ( hdrTab, s, xx, c, EntityTypeEnum.Literal );
			s = xx.str;
			fc = fc.chain;
		}

		xx.str = f;
		return ifc;
	}

	private String getSubscript ( int c, TableReader tblTab ) {
		PSOHolder psh = itsPSOL [ c ];
		String v = new String();

		for (Integer i=0; i<psh.subscriptChain.size(); i++ ) {
			SubscriptHolder sh = psh.subscriptChain.get(i);
			FChain s = sh.subscript.chain;
			if ( s != null ) {
				v += " " + sh.prefix;
				do {
					if ( s.opType == ColumnRelationExpressionEnum.ColumnReference ) {
						v += tblTab.GetValue ( s.colIndex );
					} else {
						v += s.nameStr;
					}
					s = s.chain;
					if ( s != null ) {
						v += ".";
					}
				} while ( s != null );
				v += sh.suffix;
			}
		}
		return v;
	}
	
	public static boolean hasSubscriptNodeFormula(TableReader hdrTab, int c ) {
		String f = formulaValue ( hdrTab, c );
		
		return ( "".equals ( f ) || f.startsWith ( ":<<") || f.startsWith (":(" ) || !f.startsWith ( ":^" ));
	}
	
	private void enterColumnAttribute ( DMIElem m, String attr, String val, TableReader tblTab, int c ) {
		DMIElementDiagrammingInfo di;
		di = m.itsDiagrammingInfo;

		//Case "Name"
		//    'more dangerous but useful to give relations other names
		//    'they already have given names, of course (subj--verb->obj)
		//    m.itsName = val
		//Case "URL"
		//    if ( "".equals(m.itsURL) ) {
		//        m.itsURL = val
		//    }
		if ( "Description".equals ( attr ) ) {
			if (m.itsDescription == null || "".equals ( m.itsDescription ) ) {
				m.itsDescription = val;
			}
		} else if ( "Color".equals ( attr ) ) {
			di = DMIElementDiagrammingInfo.allocateIfNull ( di );
			if ( di.itsColor == 0 ) {
				if ( val.length () > 2 ) {
					di.itsColor = parseColor ( val );
				} else {
					di.itsColor = tblTab.Cell ( c ).InteriorColor ();
				}
			}
			if ( "".equals ( di.itsShape ) ) {
				di.itsShape = "box";
			}
		} else if ( "Shape".equals ( attr ) ) {
			di = DMIElementDiagrammingInfo.allocateIfNull ( di );
			if ( "".equals ( di.itsShape ) ) {
				di.itsShape = val;
			}
		}
		/*
		else if ( "DP".equals(attr) ) {
				if ( m.itsDiagParent == null ) {
					m.itsDiagParent = cm.FindConcept(val, null);
				}
		}
		*/

		m.itsDiagrammingInfo = di;

		//AddProvenance m
	}

	public static int parseColor ( String c ) {
		int ans = (((55 * 256))); // + 0) * 256 + 55) * 256
		if ( Strings.InStr ( 1, c, "0x" ) == 1 && c.length () == 8 ) {
			int r = twoHex ( Strings.Mid ( c, 3, 2 ) );
			int g = twoHex ( Strings.Mid ( c, 5, 2 ) );
			int b = twoHex ( Strings.Mid ( c, 7, 2 ) );
			ans = ((r * 256) + g) * 256 + b;
		} else if ( Strings.isNumeric ( c ) ) {
			ans = java.lang.Integer.parseInt ( c );
		}
		return ans;
	}

	private static int twoHex ( String s ) {
		return java.lang.Integer.valueOf ( s, 16 ).intValue ();
	}

	private static String columnName ( Range r, int c ) {
		String t = r.Rows ( 1 ).Columns ( c ).Address ( true, false );
		int p = Strings.InStr ( 1, t, "$" );
		return Strings.Mid ( t, 1, p - 1 );
	}


	private static int findColIndexFromCurrent ( TableReader hdrTab, String n, int c ) {
		if ( "".equals ( n ) ) {
			return c;
		} else {
			return findColumnIndex ( hdrTab, n );
		}
	}

	public static String columnTypeValue ( TableReader hdrTab, int c ) {
		return hdrTab.GetValue ( kTableTypeRow, c );
	}

	private static String formulaValue ( TableReader hdrTab, int c ) {
		return hdrTab.GetValue ( kTableFormulaRow, c );
	}


	public static int findColumnIndex ( TableReader hdrTab, String cn ) {
		int cx = hdrTab.ColLast ();
		for ( int c = 1; c <= cx; c++ ) {
			if ( cn.equals ( hdrTab.GetValue ( kTableColNameRow, c ) ) ) {
				return c;
			}
		}
		return 0;
	}

	private void errMsg ( boolean err, TableReader tab, int colNum, boolean doRow, String txt, String msg ) {
		Worksheet parent = tab.itsRange.Parent ();
		String emsgtx = "  In Workbook:\t" + parent.Parent ().Name () +
				"\r\n On Worksheet:\t" + parent.Name ();
		if ( colNum > 0 ) {
			emsgtx += "\r\n    In Column:\t" + columnName ( tab.itsRange, colNum ) + "  (#" + colNum + " in table)";
			if ( doRow )
				emsgtx += "\r\n       On Row:\t" + tab.Cell ( colNum ).Row ();
		}
		if ( !"".equals ( txt ) )
			emsgtx += "\r\n    Regarding:\t" + txt;
		if ( err )
			emsgtx += "\r\n        Error:\t";
		else
			emsgtx += "\r\n      Warning:\t";
		emsgtx += msg;
		emsgtx += "\r\n";
		lang.errMsg ( emsgtx );
	}

	public static PSOHolder [] addToList ( PSOHolder s, PSOHolder [] sL ) {
		int u = sL.length;
		sL = expandPreserve ( sL, u + 1 );
		sL [ u ] = s;
		return sL;
	}

	private static PSOHolder [] expandPreserve ( PSOHolder [] v, int u ) {
		PSOHolder [] ans = new PSOHolder [ u ];
		int len = v.length;
		if ( u < len )
			len = u;
		System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}


	public static PSOSet [] addToList ( PSOSet s, PSOSet [] sL ) {
		int u = sL.length;
		sL = expandPreserve ( sL, u + 1 );
		sL [ u ] = s;
		return sL;
	}

	private static PSOSet [] expandPreserve ( PSOSet [] v, int u ) {
		PSOSet [] ans = new PSOSet [ u ];
		int len = v.length;
		if ( u < len )
			len = u;
		System.arraycopy ( v, 0, ans, 0, len );
		return ans;
	}

}
