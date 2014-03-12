/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelWithJointModesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.scoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.ivt.scoring.BlackListedActivityScoringFunction;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction;
import playground.ivt.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;

import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class CharyparNagelWithJointModesScoringFunctionFactory implements ScoringFunctionFactory {

	private final StageActivityTypes blackList;
	private final CharyparNagelScoringParameters params;
	private final Map<String, LegScoringParameters> parametersPerMode = new HashMap<String, LegScoringParameters>();
    private final Scenario scenario;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
    public CharyparNagelWithJointModesScoringFunctionFactory(
			final StageActivityTypes typesNotToScore,
			final Scenario scenario) {
		this.params = new CharyparNagelScoringParameters( scenario.getConfig().planCalcScore() );
		this.scenario = scenario;
		this.blackList = typesNotToScore;

		// standard modes
		parametersPerMode.put(
					TransportMode.car,
					LegScoringParameters.createForCar(
						params ));
		parametersPerMode.put(
					TransportMode.pt,
					LegScoringParameters.createForPt(
						params ));
		parametersPerMode.put(
					TransportMode.walk,
					LegScoringParameters.createForWalk(
						params ));
		parametersPerMode.put(
					TransportMode.bike,
					LegScoringParameters.createForBike(
						params ));
		parametersPerMode.put(
					TransportMode.transit_walk,
					LegScoringParameters.createForWalk(
						params ));

		// joint modes
		parametersPerMode.put(
					JointActingTypes.DRIVER,
					LegScoringParameters.createForCar(
						params ));
		parametersPerMode.put(
					JointActingTypes.PASSENGER,
					new LegScoringParameters(
						params.modeParams.get(TransportMode.car).constant,
						params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s,
						// passenger doesn't pay gasoline
						0 ));
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(
				new BlackListedActivityScoringFunction(
					blackList,
					new CharyparNagelActivityScoring(
						params ) ) );

		for ( Map.Entry<String, LegScoringParameters> entry : parametersPerMode.entrySet() ) {
			scoringFunctionAccumulator.addScoringFunction(
					new ElementalCharyparNagelLegScoringFunction(
						entry.getKey(),
						entry.getValue(),
						scenario.getNetwork()));
		}

		// other standard stuff
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelMoneyScoring( params ));
		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelAgentStuckScoring( params ));

		return scoringFunctionAccumulator;
	}
}

