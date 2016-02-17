/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.contrib.noise;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.data.GridParameters;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.routing.NoiseTollDisutilityCalculatorFactory;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */

public class NoiseConfigGroupTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test0(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config0.xml";
		Config config = ConfigUtils.loadConfig(configFile, new GridParameters());
				
		GridParameters gridParameters = (GridParameters) config.getModule("noiseGrid");

		// test the config parameters
		Assert.assertEquals("wrong config parameter", 12345., gridParameters.getReceiverPointGap(), MatsimTestUtils.EPSILON);
		
		String actForRecPtGrid = gridParameters.getConsideredActivitiesForReceiverPointGridArray()[0] + "," + gridParameters.getConsideredActivitiesForReceiverPointGridArray()[1] + "," + gridParameters.getConsideredActivitiesForReceiverPointGridArray()[2];
		Assert.assertEquals("wrong config parameter", "home,sleep,eat", actForRecPtGrid);		
		
		String actForSpatFct = gridParameters.getConsideredActivitiesForSpatialFunctionalityArray()[0] + "," + gridParameters.getConsideredActivitiesForSpatialFunctionalityArray()[1] + "," + gridParameters.getConsideredActivitiesForSpatialFunctionalityArray()[2];
		Assert.assertEquals("wrong config parameter", "work,leisure,other", actForSpatFct);		
			
	}
	
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config1.xml";
		Config config = ConfigUtils.loadConfig(configFile, new GridParameters());
				
		GridParameters gridParameters = (GridParameters) config.getModule("noiseGrid");

		// see if the custom config group is written into the output config file
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		
		NoiseContext noiseContext = new NoiseContext(scenario, gridParameters, new NoiseParameters());

		final NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(noiseContext);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});
		controler.addControlerListener(new NoiseCalculationOnline(noiseContext));

		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
//				
//		String workingDirectory = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immisions/";
//		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
//
//		ProcessNoiseImmissions readNoiseFile = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, gridParameters.getReceiverPointGap());
//		readNoiseFile.run();
//		

	}

}
