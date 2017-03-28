/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.parking.parkingsearch.RunParkingSearchExample;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunParkingSearchScenarioTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testRunOneTaxi() {
		String configFile = "./src/main/resources/parkingsearch/config.xml";
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory( utils.getOutputDirectory() );

		new RunParkingSearchExample().run(config,false);
		
	}
}
