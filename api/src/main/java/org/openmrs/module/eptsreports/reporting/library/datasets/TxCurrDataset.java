/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.eptsreports.reporting.library.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openmrs.module.eptsreports.metadata.HivMetadata;
import org.openmrs.module.eptsreports.reporting.library.cohorts.AgeCohortQueries;
import org.openmrs.module.eptsreports.reporting.library.cohorts.CompositionCohortQueries;
import org.openmrs.module.eptsreports.reporting.library.cohorts.EncounterCohortQueries;
import org.openmrs.module.eptsreports.reporting.library.cohorts.GenderCohortQueries;
import org.openmrs.module.eptsreports.reporting.library.cohorts.SqlCohortQueries;
import org.openmrs.module.eptsreports.reporting.library.indicators.HivIndicators;
import org.openmrs.module.reporting.ReportingConstants;
import org.openmrs.module.reporting.cohort.definition.AgeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TxCurrDataset {
	
	@Autowired
	private AgeCohortQueries ageCohortQueries;
	
	@Autowired
	private GenderCohortQueries genderCohortQueries;
	
	@Autowired
	private SqlCohortQueries sqlCohortQueries;
	
	@Autowired
	private EncounterCohortQueries encountertQueries;
	
	@Autowired
	private HivMetadata hivMetadata;
	
	@Autowired
	private CompositionCohortQueries compositionCohortQueries;
	
	@Autowired
	private HivIndicators hivIndicators;
	
	public List<Parameter> getParameters() {
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(ReportingConstants.START_DATE_PARAMETER);
		parameters.add(ReportingConstants.END_DATE_PARAMETER);
		return parameters;
	}
	
	public CohortIndicatorDataSetDefinition constructTxNewDatset(List<Parameter> parameters) {
		
		CohortIndicatorDataSetDefinition dataSetDefinition = new CohortIndicatorDataSetDefinition();
		dataSetDefinition.setName("TX_CURR Data Set");
		dataSetDefinition.addParameters(parameters);
		
		/*
		 * Looks for patients enrolled in ART program (program 2=SERVICO TARV -
		 * TRATAMENTO) before or on end date
		 */
		SqlCohortDefinition inARTProgramDuringTimePeriod = sqlCohortQueries.getPatientsinARTProgramDuringTimePeriod();
		
		/*
		 * Looks for patients registered as START DRUGS (answer to question 1255 = ARV
		 * PLAN is 1256 = START DRUGS) in the first drug pickup (encounter type
		 * 18=S.TARV: FARMACIA) or follow up consultation for adults and children
		 * (encounter types 6=S.TARV: ADULTO SEGUIMENTO and 9=S.TARV: PEDIATRIA
		 * SEGUIMENTO) before or on end date
		 */
		SqlCohortDefinition patientWithSTARTDRUGSObs = sqlCohortQueries.getPatientWithSTARTDRUGSObs();
		
		/*
		 * Looks for with START DATE (Concept 1190=HISTORICAL DRUG START DATE) filled in
		 * drug pickup (encounter type 18=S.TARV: FARMACIA) or follow up consultation
		 * for adults and children (encounter types 6=S.TARV: ADULTO SEGUIMENTO and
		 * 9=S.TARV: PEDIATRIA SEGUIMENTO) where START DATE is before or equal end date
		 */
		SqlCohortDefinition patientWithHistoricalDrugStartDateObs = sqlCohortQueries.getPatientWithHistoricalDrugStartDateObs();
		
		// Looks for patients who had at least one drug pick up (encounter type
		// 18=S.TARV: FARMACIA) before end date
		EncounterCohortDefinition patientsWithDrugPickUpEncounters = encountertQueries.createEncounterParameterizedByDate("patientsWithDrugPickUpEncounters", Arrays.asList("onOrBefore"), hivMetadata.getARVPharmaciaEncounterType());
		
		// Looks for patients enrolled on art program (program 2 - SERVICO TARV -
		// TRATAMENTO) who left ART program
		SqlCohortDefinition patientsWhoLeftARTProgramBeforeOrOnEndDate = sqlCohortQueries.getPatientsWhoLeftARTProgramBeforeOrOnEndDate();
		
		// Looks for patients that from the date scheduled for next drug pickup (concept
		// 5096=RETURN VISIT DATE FOR ARV DRUG) until end date have completed 60 days
		// and have not returned
		SqlCohortDefinition patientsWhoHaveNotReturned = sqlCohortQueries.getPatientsWhoHaveNotReturned();
		
		// Looks for patients that from the date scheduled for next follow up
		// consultation (concept 1410=RETURN VISIT DATE) until the end date have not
		// completed 60 days
		SqlCohortDefinition patientsWhoHaveNotCompleted60Days = sqlCohortQueries.getPatientsWhoHaveNotCompleted60Days();
		
		// Looks for patients that were registered as abandonment (program workflow
		// state is 9=ABANDONED) but from the date scheduled for next drug pick up
		// (concept 5096=RETURN VISIT DATE FOR ARV DRUG) until the end date have not
		// completed 60 days
		SqlCohortDefinition abandonedButHaveNotcompleted60Days = sqlCohortQueries.getAbandonedButHaveNotcompleted60Days();
		
		CohortDefinition males = genderCohortQueries.MaleCohort();
		
		CohortDefinition females = genderCohortQueries.FemaleCohort();
		
		AgeCohortDefinition PatientBelow1Year = ageCohortQueries.patientWithAgeBelow(1);
		AgeCohortDefinition PatientBetween1And9Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween1And9Years", 1, 9);
		AgeCohortDefinition PatientBetween10And14Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween10And14Years", 10, 14);
		AgeCohortDefinition PatientBetween15And19Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween15And19Years", 15, 19);
		AgeCohortDefinition PatientBetween20And24Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween20And24Years", 20, 24);
		AgeCohortDefinition PatientBetween25And29Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween25And29Years", 25, 29);
		AgeCohortDefinition PatientBetween30And34Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween30And34Years", 30, 34);
		AgeCohortDefinition PatientBetween35And39Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween35And39Years", 35, 39);
		AgeCohortDefinition PatientBetween40And49Years = ageCohortQueries.createXtoYAgeCohort("PatientBetween40And49Years", 40, 49);
		AgeCohortDefinition PatientBetween50YearsAndAbove = ageCohortQueries.patientWithAgeAbove(50);
		PatientBetween50YearsAndAbove.setName("PatientBetween50YearsAndAbove");
		
		ArrayList<AgeCohortDefinition> agesRange = new ArrayList<AgeCohortDefinition>();
		// agesRange.add(PatientBelow1Year);
		// agesRange.add(PatientBetween1And9Years);
		agesRange.add(PatientBetween10And14Years);
		agesRange.add(PatientBetween15And19Years);
		agesRange.add(PatientBetween20And24Years);
		agesRange.add(PatientBetween25And29Years);
		agesRange.add(PatientBetween30And34Years);
		agesRange.add(PatientBetween35And39Years);
		agesRange.add(PatientBetween40And49Years);
		agesRange.add(PatientBetween50YearsAndAbove);
		
		return dataSetDefinition;
	}
}
