/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.analysis.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.studies.matrix2014.gis.ValidateFacilities;
import playground.johannes.studies.matrix2014.gis.ZoneSetLAU2Class;
import playground.johannes.studies.matrix2014.sim.run.ZoneFacilityTask;
import playground.johannes.synpop.analysis.AnalyzerTaskComposite;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.LegAttributePredicate;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.util.Executor;

import java.util.Collection;
import java.util.Random;

/**
 * @author johannes
 */
public class PopulationAnalyzer {

    private static final String MODULE_NAME = "synPopSim";

    private static final Logger logger = Logger.getLogger(PopulationAnalyzer.class);

    public static void main(String args[]) {
        Logger.getRootLogger().setLevel(Level.TRACE);

        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Load parameters...
         */
        Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
        FileIOContext ioContext = new FileIOContext(configGroup.getValue("output"));
        /*
        Load GIS data...
         */
        DataPool dataPool = new DataPool();
        dataPool.register(new FacilityDataLoader(configGroup.getValue("facilities"), random), FacilityDataLoader.KEY);
        dataPool.register(new ZoneDataLoader(configGroup), ZoneDataLoader.KEY);

        ValidateFacilities.validate(dataPool, "nuts3");

        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        new ZoneSetLAU2Class().apply(lau2Zones);
        /*
        Load population...
         */
//        logger.info("Loading persons...");
//        Set<Person> persons = PopulationIO.loadFromXML(config.findParam(MODULE_NAME, "simPopulation"), new PlainFactory());
//        logger.info(String.format("Loaded %s persons.", persons.size()));
//
//        logger.info("Loading persons...");
//        Set<Person> refPersons = PopulationIO.loadFromXML(config.findParam(MODULE_NAME, "popInputFile"), new
//                PlainFactory());
//        logger.info(String.format("Loaded %s persons.", persons.size()));
//
//        logger.info("Validating persons...");
//        TaskRunner.validatePersons(new ValidateMissingAttribute(CommonKeys.PERSON_WEIGHT), persons);
//        TaskRunner.validatePersons(new ValidatePersonWeight(), persons);
        /*
        Build analyzer...
         */
        Predicate<Segment> carPredicate = new LegAttributePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);
        AnalyzerTaskComposite<Collection<? extends Person>> tasks = new AnalyzerTaskComposite<>();

        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
        ZoneCollection modenaZones = zoneData.getLayer("modena");

        ActivityFacilities facilities = ((FacilityData)dataPool.get(FacilityDataLoader.KEY)).getAll();
//        LegStatsPerZone legStatsPerZone = new LegStatsPerZone(zoneData.getLayer("nuts3"), facilities, ioContext);
//        legStatsPerZone.setLegPredicate(carPredicate);
//        tasks.addComponent(legStatsPerZone);
//
//        ZoneMobilityRate zoneMobilityRate = new ZoneMobilityRate(
//                MiDKeys.PERSON_LAU2_CLASS,
//                lau2Zones,
//                carPredicate);
//        zoneMobilityRate.analyze(refPersons, null);
//
//        new TransferZoneAttribute().apply(lau2Zones, modenaZones, MiDKeys.PERSON_LAU2_CLASS);
//
//        ValidatePopulationDensity popDensity = new ValidatePopulationDensity(
//                dataPool,
//                zoneMobilityRate.getMobilityRatePerZone(modenaZones),
//                "modena");
//
//        popDensity.setIoContext(ioContext);
//        tasks.addComponent(popDensity);
//
//        ValidateLAU2Attribute lau2Attr = new ValidateLAU2Attribute(dataPool);
//        tasks.addComponent(lau2Attr);

        ZoneFacilityTask fTask = new ZoneFacilityTask(facilities, ioContext);

//        AnalyzerTaskRunner.run(persons, tasks, ioContext);

        fTask.analyze(modenaZones, null);

        Executor.shutdown();
        logger.info("Done.");
    }
}
