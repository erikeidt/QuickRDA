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

public class DMIBaseVocabulary {

	public DMIElem	gConcept;
	public DMIElem	gClass;
	public DMIElem	gRelation;
	public DMIElem	gProperty;
	public DMIElem	gName;
	public DMIElem	gValue;
	public DMIElem	gString;
	public DMIElem	gSerialization;
	public DMIElem	gStringSerialization;
	public DMIElem	gNothing;
	public DMIElem	gboolean;
	public DMIElem	gtrue;
	public DMIElem	gfalse;
	public DMIElem	gVersion;
	public DMIElem	gSimpleName;
	public DMIElem	gQualifiedName;
	public DMIElem	gNamespace;
	public DMIElem	gSource;
	public DMIElem	gOptional;
	public DMIElem	gTemplate;

	public DMIElem	gAbstractProperty;

	public DMIElem	gEvokeAs;
	public DMIElem	gInTemplate;
	public DMIElem	gInEvocation;
	public DMIElem	gSubstitutes;
	public DMIElem	gOptionallyIncludedDefYes;
	public DMIElem	gOptionallyIncludedDefNo;
	public DMIElem	gIncludedOption;
	public DMIElem	gExcludedOption;

	public DMIElem	gHasName;
	public DMIElem	gHasQualifier;
	public DMIElem	gHasNameString;
	public DMIElem	gHasDescription;
	public DMIElem	gHasPlural;

	public DMIElem	gInstanceOf;
	public DMIElem	gSubclassOf;
	public DMIElem	gHasDomain;
	public DMIElem	gHasRange;
	public DMIElem	gHasPreferred;
	public DMIElem	gHasInverse;

	public DMIElem	gIsAttachedTo;
	public DMIElem	gIsInGroup;
	public DMIElem	gAttaches;
	public DMIElem	gGroups;

	public DMIElem	gFollowsContainer;
	public DMIElem	gFollowedByContainer;

	public DMIElem	gSources;
	public DMIElem	gAsserts;
	public DMIElem	gDenies;

	public DMIElem	gBegets;
	public DMIElem	gBegetsUsing;
	public DMIElem	gBegetsAs;
	public DMIElem	gQualifiesSO;
	public DMIElem	gQualifiedTo;

	public DMIElem	gTimeline;
	public DMIElem	gTimelineDate;

	public DMIElem	gComposingProperty;
	public DMIElem	gDecomposingProperty;

	public DMIElem	gSuccessorValue;

	public DMIElem	gDMI;

	//BCA
	public DMIElem	gComponentOf;
	public DMIElem	gKindOf;
	public DMIElem	gRefersTo;

	public DMIElem	gRespParent;
	public DMIElem	gAssignedTo;
	public DMIElem	gMemberOf;

	public DMIElem	gRole;
	public DMIElem	gActor;
	public DMIElem	gActorClass;

	public DMIElem	gIsActor;
	public DMIElem	gIsActorClass;

	public DMIElem	gResponsibility;

	public DMIElem	gRoleConsumes;
	public DMIElem	gRoleProvides;
	public DMIElem	gRespConsumes;
	public DMIElem	gRespProvides;
	public DMIElem	gInteractsWith;

	//CSA
	//public DMIElem	gService;
	//public DMIElem	gCRUDService;
	//public DMIElem	gOwnedBy;

	//public DMIElem	gOperation;
	//public DMIElem	gIsInService;

	//public DMIElem	gIsCRUDService;

	public void setBuiltIn ( DMIElem m, int ix ) {
		switch ( ix ) {
		case 1 :
			gConcept = m;
			// DMIElem.ggConcept = m;
			break;
		case 2 :
			gClass = m;
			// DMIElem.ggClass = m;
			break;
		case 3 :
			gRelation = m;
			// DMIElem.ggRelation = m;
			break;
		case 4 :
			gProperty = m;
			break;
		case 5 :
			gName = m;
			break;
		case 6 :
			gValue = m;
			break;
		case 7 :
			gString = m;
			break;
		case 8 :
			gSerialization = m;
			break;
		case 9 :
			gStringSerialization = m;
			break;
		case 10 :
			gNothing = m;
			break;
		case 11 :
			gboolean = m;
			break;
		case 12 :
			gtrue = m;
			break;
		case 13 :
			gfalse = m;
			break;
		case 16 :
			gVersion = m;
			break;

		case 17 :
			gSimpleName = m;
			break;
		case 18 :
			gQualifiedName = m;
			break;
		case 19 :
			gNamespace = m;
			break;

		case 20 :
			gSource = m;
			break;

		case 21 :
			gDMI = m;
			break;

		case 22 :
			gHasName = m;
			break;
		case 23 :
			gHasQualifier = m;
			break;
		case 24 :
			gHasNameString = m;
			break;
		case 25 :
			gHasDescription = m;
			break;
		case 26 :
			gHasPlural = m;
			break;
		case 27 :
			gAbstractProperty = m;
			break;
		case 28 :
			gTemplate = m;
			break;
		case 29 :
			gOptional = m;
			break;

		case 30 :
			gInstanceOf = m;
			break;
		case 31 :
			gSubclassOf = m;
			break;
		case 32 :
			gHasDomain = m;
			break;
		case 33 :
			gHasRange = m;
			break;

		case 34 :
			gHasPreferred = m;
			break;
		case 35 :
			gHasInverse = m;
			break;

		case 37 :
			gIsAttachedTo = m;
			break;
		case 38 :
			gIsInGroup = m;
			break;
		case 39 :
			gAttaches = m;
			break;
		case 40 :
			gGroups = m;
			break;

		case 43 :
			gFollowsContainer = m;
			break;
		case 45 :
			gFollowedByContainer = m;
			break;

		case 46 :
			gSources = m;
			break;
		case 47 :
			gAsserts = m;
			break;
		case 48 :
			gDenies = m;
			break;

		case 49 :
			gSuccessorValue = m;
			break;

		case 50 :
			gBegets = m;
			break;
		case 51 :
			gBegetsUsing = m;
			break;
		case 52 :
			gBegetsAs = m;
			break;
		case 54 :
			gQualifiesSO = m;
			break;
		case 55 :
			gQualifiedTo = m;
			break;
		case 56 :
			gInTemplate = m;
			break;
		case 57 :
			gEvokeAs = m;
			break;
		case 58 :
			gInEvocation = m;
			break;
		case 59 :
			gSubstitutes = m;
			break;
		case 60 :
			gOptionallyIncludedDefYes = m;
			break;
		case 61 :
			gOptionallyIncludedDefNo = m;
			break;
		case 62 :
			gIncludedOption = m;
			break;
		case 63 :
			gExcludedOption = m;
			break;

		case 64 :
			gTimeline = m;
			break;
		case 65 :
			gTimelineDate = m;

			//case 50: gHasSubject = m; break;
			//case 51: gHasObject = m;  break;

		case 66 :
			gComposingProperty = m;
			break;
		case 67 :
			gDecomposingProperty = m;
			break;

		case 200 :
			gRole = m;
			break;
		case 201 :
			gActor = m;
			break;
		case 202 :
			gActorClass = m;
			break;

		case 205 :
			gResponsibility = m;
			break;

		case 215 :
			gIsActor = m;
			break;
		case 216 :
			gIsActorClass = m;
			break;

		case 233 :
			gMemberOf = m;
			break;
		case 240 :
			gAssignedTo = m;
			break;
		case 245 :
			gRespParent = m;
			break;
		case 248 :
			gInteractsWith = m;
			break;
		case 250 :
			gRoleConsumes = m;
			gRespConsumes = m;
			break;
		case 251 :
			gRoleProvides = m;
			gRespProvides = m;
			break;
		case 260 :
			gComponentOf = m;
			break;
		case 261 :
			gKindOf = m;
			break;
		case 262 :
			gRefersTo = m;
			break;

//		case 300 :
//			gService = m;
//			break;
//		case 301 :
//			gCRUDService = m;
//			break;
//		case 310 :
//			gOperation = m;
//			break;
//
//		case 318 :
//			gIsInService = m;
//			break;
//		case 338 :
//			gIsCRUDService = m;
//			break;
//		case 340 :
//			gOwnedBy = m;
//			break;
		}
	}

}
