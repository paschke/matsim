/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.analysis.srv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dziemke.analysis.AnalysisFileWriter;
import playground.dziemke.analysis.AnalysisUtils;
import playground.dziemke.analysis.Trip;

/**
 * @author dziemke
 * adapted from TripAnalyzer04
 *
 */
public class SrVTripAnalyzer {

	private final static Logger log = Logger.getLogger(SrVTripAnalyzer.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Parameters
		boolean useWeights = true;			//wt
		boolean onlyCar = false;			//car
		boolean onlyCarAndCarPool = true;	//carp
		boolean onlyHomeAndWork = false;	//hw
		boolean distanceFilter = true;		//dist
		boolean ageFilter = false;
		
		double minDistance_km = 0;
		double maxDistance_km = 100;
		
		Integer minAge = 80;
		Integer maxAge = 119;	
		
		int maxBinDuration_min = 120;
	    int binWidthDuration_min = 1;
	    
	    int maxBinTime_h = 23;
	    int binWidthTime_h = 1;
	    
	    int maxBinDistance_km = 60;
	    int binWidthDistance_km = 1;
	    	    
	    int maxBinSpeed_km_h = 60;
	    int binWidthSpeed_km_h = 1;
	    
	    
		// Input and output files
	    String inputFileTrips = "/Users/dominik/Workspace/data/srv/input/W2008_Berlin_Weekday.dat";
		String inputFilePersons = "/Users/dominik/Workspace/data/srv/input/P2008_Berlin2.dat";
		
		String networkFile = "/Users/dominik/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//		String shapeFile = "/Users/dominik/Workspace/data/srv/input/RBS_OD_STG_1412/RBS_OD_STG_1412.shp";
				
		String outputDirectory = "/Users/dominik/Workspace/data/srv/output/wd_neu_7";
		
		if (useWeights == true) {
			outputDirectory = outputDirectory + "_wt";
		}
		
		if (onlyCar == true) {
			outputDirectory = outputDirectory + "_car";
		}
		
		if (onlyCarAndCarPool == true) {
			outputDirectory = outputDirectory + "_carp";
		}
		
		if (onlyCar == false && onlyCarAndCarPool == false) {
			outputDirectory = outputDirectory + "_all";
		}
				
		if (distanceFilter == true) {
			outputDirectory = outputDirectory + "_dist";
		}
		
		if (onlyHomeAndWork == true) {
			outputDirectory = outputDirectory + "_hw";
		}		
				
		if (ageFilter == true) {
			outputDirectory = outputDirectory + "_" + minAge.toString();
			outputDirectory = outputDirectory + "_" + maxAge.toString();
		}

		outputDirectory = outputDirectory + "/";
		
		
		// parse trip file
		log.info("Parsing " + inputFileTrips + ".");		
		SrV2008TripParser tripParser = new SrV2008TripParser();
		tripParser.parse(inputFileTrips);
		log.info("Finished parsing trips.");
		
		// parse person file
		log.info("Parsing " + inputFilePersons + ".");		
		SrV2008PersonParser personParser = new SrV2008PersonParser();
		personParser.parse(inputFilePersons);
		log.info("Finished parsing persons.");
		
		
		// create objects
		
		// for writing plans files (newer ones...)
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = new TreeMap<Id<Person>, TreeMap<Double, Trip>>();
		
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		
		NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(scenario.getNetwork());
		networkReader.parse(networkFile);
		
		List<Event> events = new ArrayList<Event>();
//		TreeMap<Double, Event> eventsMap = new TreeMap<Double, Event>();
		
		String fromCRS = "EPSG:31468"; // GK4
		String toCRS = "EPSG:31468"; // GK4
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
		
		
		// for calculations and storage of these calculation to files (older ones...)
    	int tripCounter = 0;
    	
    	Map <Integer, Double> tripDurationMap = new TreeMap <Integer, Double>();
	    double aggregateTripDuration = 0.;
	    double aggregateWeightTripDuration = 0.;
	    
	    Map <Integer, Double> departureTimeMap = new TreeMap <Integer, Double>();
	    double aggregateWeightDepartureTime = 0.;
	    
	    Map <String, Double> activityTypeMap = new TreeMap <String, Double>();
	    double aggregateWeightActivityTypes = 0.;
		
		Map <Integer, Double> tripDistanceRoutedMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceRouted = 0.;
		double aggregateWeightTripDistanceRouted = 0.;
		
		Map <Integer, Double> tripDistanceBeelineMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceBeeline = 0.;
		double aggregateWeightTripDistanceBeeline = 0.;
	    
		Map <Integer, Double> averageTripSpeedRoutedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsRouted = 0.;
	    double aggregateWeightTripSpeedRouted = 0.;

	    Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsBeeline = 0.;
	    double aggregateWeightTripSpeedBeeline = 0.;
	    
	    Map <Integer, Double> averageTripSpeedProvidedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsProvided = 0.;
	    double aggregateWeightTripSpeedProvided = 0.;

	    int numberOfTripsWithNoCalculableSpeed = 0;
	    
	    Map <Id<Trip>, Double> distanceRoutedMap = new TreeMap <Id<Trip>, Double>();
	    Map <Id<Trip>, Double> distanceBeelineMap = new TreeMap <Id<Trip>, Double>();
	    
	    
	    // Go through all trips
	    for (Trip trip : tripParser.getTrips().values()) {

	    	// filters
	    	boolean considerTrip = false;

	    	// mode of transport and activity type
	    	// reliant on variable "V_HHPKW_F": 0/1
	    	int useHouseholdCar = trip.getUseHouseholdCar();
	    	// reliant on variable "V_ANDPKW_F": 0/1
	    	int useOtherCar = trip.getUseOtherCar();
	    	// reliant on variable "V_HHPKW_MF": 0/1
	    	int useHouseholdCarPool = trip.getUseHouseholdCarPool();
	    	// reliant on variable "V_ANDPKW_MF": 0/1
	    	int useOtherCarPool = trip.getUseOtherCarPool();

	    	String activityEndActType = trip.getActivityEndActType();
	    	String activityStartActType = trip.getActivityStartActType();

	    	if (onlyHomeAndWork == true) {
	    		if ((activityEndActType.equals("home") && activityStartActType.equals("work")) || 
	    				(activityEndActType.equals("work") && activityStartActType.equals("home"))) {
	    			if (onlyCar == true) {
	    				if (useHouseholdCar == 1 || useOtherCar == 1) {		 
	    					considerTrip = true;
	    				}
	    			} else if (onlyCarAndCarPool == true) {
	    				if (useHouseholdCar == 1 || useOtherCar == 1 || 
	    						useHouseholdCarPool == 1 || useOtherCarPool == 1) {		 
	    					considerTrip = true;
	    				}
	    			} else {
	    				considerTrip = true;
	    			}
	    		}
	    	} else {
	    		if (onlyCar == true) {
	    			if (useHouseholdCar == 1 || useOtherCar == 1) {		 
	    				considerTrip = true;
	    			}
	    		} else if (onlyCarAndCarPool == true) {
	    			if (useHouseholdCar == 1 || useOtherCar == 1 || 
	    					useHouseholdCarPool == 1 || useOtherCarPool == 1) {		 
	    				considerTrip = true;
	    			}
	    		} else {
	    			considerTrip = true;
	    		}
	    	}


	    	// distance
	    	double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
	    	if (distanceFilter == true) {
	    		if (tripDistanceBeeline_km >= maxDistance_km) {
	    			considerTrip = false;
	    		}
	    		if (tripDistanceBeeline_km <= minDistance_km) {
	    			considerTrip = false;
	    		}
	    	}

	    	
	    	// age
	    	String personId = trip.getPersonId().toString();
	    	
	    	if (ageFilter == true) {
	    		int age = (int) personParser.getPersonAttributes().getAttribute(personId, "age");
	    		if (age < minAge) {
	    			considerTrip = false;
	    		}
	    		if (age > maxAge) {
	    			considerTrip = false;
	    		}
	    	}


	    	// use all filtered trips to construct plans and do calculations 
	    	if (considerTrip == true) {
	    		
	    		
	    		// collect and store information to create plans later
	    		Id<Person> id = Id.create(personId, Person.class);
	    			    		
		    	if (!personTripsMap.containsKey(id)) {
		    		TreeMap<Double, Trip> tripsMap = new TreeMap<Double, Trip>();
		    		personTripsMap.put(id, tripsMap);
		    	}

	    		double departureTime_s = trip.getDepartureTime_s();
	    		if (personTripsMap.get(id).containsKey(departureTime_s)) {
	    			new RuntimeException("Person may not have two activites ending at the exact same time.");
	    		} else {
	    			personTripsMap.get(id).put(departureTime_s, trip);
	    		}
	    		

	    		// do calculations
	    		tripCounter++;

	    		// weights
	    		double weight;
	    		if (useWeights == true) {
	    			weight = trip.getWeight();
	    		} else {
	    			weight = 1.;
	    		}

	    		// calculate travel times and store them in a map
	    		// reliant on variable "V_ANKUNFT": -9 = no data, -10 = implausible
	    		// and on variable "V_BEGINN": -9 = no data, -10 = implausible
	    		// trip.getArrivalTime() / trip.getDepartureTime() yields values in minutes!
	    		double arrivalTime_min = trip.getArrivalTime_s() / 60.;
	    		//double departureTimeInMinutes = trip.getDepartureTime();
	    		double departureTime_h = departureTime_s / 3600.;
	    		double tripDuration_min = arrivalTime_min - (departureTime_s * 60.);
	    		//double tripDurationInMinutes = trip.getDuration();
	    		double weightedTripDuration_min = tripDuration_min * weight;
	    		double tripDuration_h = tripDuration_min / 60.;
	    		// there are also three cases where time < 0; they need to be excluded
	    		if (arrivalTime_min >= 0 && (departureTime_s * 60.) >= 0 && tripDuration_min >= 0) {
	    			AnalysisUtils.addToMapIntegerKey(tripDurationMap, tripDuration_min, binWidthDuration_min, maxBinDuration_min, weight);
	    			//aggregateTripDuration = aggregateTripDuration + tripDurationInMinutes;
	    			aggregateTripDuration = aggregateTripDuration + weightedTripDuration_min;
	    			aggregateWeightTripDuration = aggregateWeightTripDuration + weight;
	    			//tripDurationCounter++;
	    		}


	    		// store departure times in a map
	    		if (departureTime_h >= 0) {
	    			AnalysisUtils.addToMapIntegerKey(departureTimeMap, departureTime_h, binWidthTime_h, maxBinTime_h, weight);
	    			aggregateWeightDepartureTime = aggregateWeightDepartureTime + weight;
	    		}


	    		// store activities in a map
	    		// reliant on variable "V_ZWECK": -9 = no data
	    		// "V_ZWECK" - end of trip = start of activity
	    		String activityType = trip.getActivityStartActType();
	    		AnalysisUtils.addToMapStringKey(activityTypeMap, activityType, weight);
	    		aggregateWeightActivityTypes = aggregateWeightActivityTypes + weight;


	    		// reliant on variable "V_START_ZWECK": -9 = no data
	    		// "V_START_ZWECK" - start of trip = end of activity
	    		// String activityTypePrevious = trip.getActivityEndActType();
	    		// addToMapStringKey(activityTypePreviousMap, activityTypePrevious, weight);


	    		// In SrV, a routed distance (according to some software) is already given
	    		// reliant on SrV variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
	    		double tripDistanceRouted_km = trip.getDistanceRoutedShortest_m() / 1000.;
	    		double weightedTripDistanceRouted = weight * tripDistanceRouted_km;
	    		if (tripDistanceRouted_km >= 0.) {
	    			AnalysisUtils.addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted_km, binWidthDistance_km, maxBinDistance_km, weight);
	    			aggregateTripDistanceRouted = aggregateTripDistanceRouted + weightedTripDistanceRouted;
	    			distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted_km);
	    			aggregateWeightTripDistanceRouted = aggregateWeightTripDistanceRouted + weight;
	    		}


	    		// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
	    		//double tripDistanceBeeline = trip.getDistanceBeeline();
	    		double weightedTripDistanceBeeline = weight * tripDistanceBeeline_km;
	    		if (tripDistanceBeeline_km >= 0.) {				
	    			AnalysisUtils.addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, maxBinDistance_km, weight);
	    			aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + weightedTripDistanceBeeline;
	    			distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline_km);
	    			aggregateWeightTripDistanceBeeline = aggregateWeightTripDistanceBeeline + weight;
	    		}


	    		// calculate speeds and and store them in a map
	    		if (tripDuration_h > 0.) {
	    			// reliant to SrV variable variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
	    			if (tripDistanceRouted_km >= 0.) {
	    				double averageTripSpeedRouted_km_h = tripDistanceRouted_km / tripDuration_h;
	    				AnalysisUtils.addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted_km_h, binWidthSpeed_km_h, maxBinSpeed_km_h, weight);
	    				aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted_km_h;
	    				aggregateWeightTripSpeedRouted = aggregateWeightTripSpeedRouted + weight;
	    			}

	    			// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
	    			if (tripDistanceBeeline_km >= 0.) {			
	    				double averageTripSpeedBeeline_km_h = tripDistanceBeeline_km / tripDuration_h;
	    				AnalysisUtils.addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline_km_h, binWidthSpeed_km_h, maxBinSpeed_km_h, weight);
	    				aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline_km_h;
	    				aggregateWeightTripSpeedBeeline = aggregateWeightTripSpeedBeeline + weight;
	    			}
	    		} else {
	    			numberOfTripsWithNoCalculableSpeed++;
	    		}


	    		// get provided speeds and store them in a map
	    		// reliant on variable "E_GESCHW": -7 = Calculation not possible	    		
	    		double averageTripSpeedProvided_km_h = trip.getSpeedFromSurvey_m_s() / 3.6;
	    		if (averageTripSpeedProvided_km_h >= 0) {
	    			AnalysisUtils.addToMapIntegerKey(averageTripSpeedProvidedMap, averageTripSpeedProvided_km_h, binWidthSpeed_km_h, maxBinSpeed_km_h, weight);
	    			aggregateOfAverageTripSpeedsProvided = aggregateOfAverageTripSpeedsProvided + averageTripSpeedProvided_km_h;
	    			aggregateWeightTripSpeedProvided = aggregateWeightTripSpeedProvided + weight;
	    		}
	    	}
	    }
	    
	    
	    // calculate averages (taking into account weights if applicable)
	    double averageTime = aggregateTripDuration / aggregateWeightTripDuration;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / aggregateWeightTripDistanceRouted;
	    double averageTripDistanceBeeline = aggregateTripDistanceBeeline / aggregateWeightTripDistanceBeeline;
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / aggregateWeightTripSpeedRouted;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / aggregateWeightTripSpeedBeeline;
	    double averageOfAverageTripSpeedsProvided = aggregateOfAverageTripSpeedsProvided / aggregateWeightTripSpeedProvided;
	    
	    
	    // write results to files
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "tripDuration.txt", binWidthDuration_min, aggregateWeightTripDuration, averageTime);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "departureTime.txt", binWidthTime_h, aggregateWeightDepartureTime, -99);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "activityTypes.txt", aggregateWeightActivityTypes);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "tripDistanceRouted.txt", binWidthDistance_km, aggregateWeightTripDistanceRouted, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeeline.txt", binWidthDistance_km, aggregateWeightTripDistanceBeeline, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "averageTripSpeedRouted.txt", binWidthSpeed_km_h, aggregateWeightTripSpeedRouted, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeeline.txt", binWidthSpeed_km_h, aggregateWeightTripSpeedBeeline, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedProvidedMap, outputDirectory + "averageTripSpeedProvided.txt", binWidthSpeed_km_h, aggregateWeightTripSpeedProvided, averageOfAverageTripSpeedsProvided);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "tripDurationCumulative.txt", binWidthDuration_min, aggregateWeightTripDuration, averageTime);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeelineCumulative.txt", binWidthDistance_km, aggregateWeightTripDistanceBeeline, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, aggregateWeightTripSpeedBeeline, averageOfAverageTripSpeedsBeeline);
//	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "beeline.txt", tripCounter);

	    
	    // return number of trips that have no calculable speed
	    log.warn("Number of trips that have no calculable speed is: " + numberOfTripsWithNoCalculableSpeed);
	    
	    
	    // add activities from map to plans
	    int tripMapEntryCounter = 0;
	    
	    for (Id<Person> personId : personTripsMap.keySet()) {
	    	
	    	// add person to population
	    	if (!population.getPersons().containsKey(personId)) {
	    		Person person = populationFactory.createPerson(personId);
	    		Plan plan = populationFactory.createPlan();
    			person.addPlan(plan);
    			population.addPerson(person);
    		}
	    	
	    	TreeMap<Double, Trip> tripsMap = personTripsMap.get(personId);
	    	Person person = population.getPersons().get(personId);
	    	
	    	// TODO exclude trip if first activity is not "home"
	    	
	    	for (double departureTime : tripsMap.keySet()) {
	    		tripMapEntryCounter++;
	    		
	    		// plans
	    		Plan plan = person.getPlans().get(0);
	    		
	    		Trip trip = tripsMap.get(departureTime);

	    		// TODO substitute zone by something better; or use alternative (new... as discussed earlier...) data structure that can handle zones
	    		double x = Double.parseDouble(trip.getDepartureZoneId().toString());
	    		double y = x;
	    		// TODO add appropriate coordinate transformation
				Coord departureCoordinates = new Coord(x, y);
	    		

				Id<Person> idToBeChecked = Id.create("1363_1", Person.class);
				
				String activityTypeEndingActivity = trip.getActivityEndActType();	
				if (personId == idToBeChecked) {
					System.err.println("personId = " + personId + " -- trip.getActivityEndActType() = "	+ activityTypeEndingActivity);
				}
				
				Activity endingActivity = populationFactory.createActivityFromCoord(activityTypeEndingActivity, ct.transform(departureCoordinates));
	    		double departureTimeInMinutes = trip.getDepartureTime_s();
	    		double departureTimeInSeconds = departureTimeInMinutes * 60;
				endingActivity.setEndTime(departureTimeInSeconds);
				
				plan.addActivity(endingActivity);
	    		
	    		// TODO make mode adjustable; right now its okay since non-car trips are excluded anyways
	    		Leg leg = populationFactory.createLeg("car");
	    		plan.addLeg(leg);
	    		
	    		// last activity
	    		String activityTypeStartingActivity = trip.getActivityStartActType();
	    		
	    		if (departureTime == tripsMap.lastKey()) {
		    		double x2 = Double.parseDouble(trip.getArrivalZoneId().toString());
		    		double y2 = x2;
		    		Coord arrivalCoordinates = new Coord(x2, y2);
		    		Activity startingActivity = populationFactory.createActivityFromCoord(activityTypeStartingActivity, ct.transform(arrivalCoordinates));
		    		plan.addActivity(startingActivity);
	    		}
				
	    		
				// events
				ActivityEndEvent activityEndEvent = new ActivityEndEvent(departureTimeInSeconds, personId, null, null, activityTypeEndingActivity);
				events.add(activityEndEvent);
//				eventsMap.put(departureTimeInSeconds, activityEndEvent);
				// TODO make mode adjustable
				PersonDepartureEvent personDepartureEvent = new PersonDepartureEvent(departureTimeInSeconds, personId, null, "car");
				events.add(personDepartureEvent);
//				eventsMap.put(departureTimeInSeconds, personDepartureEvent);
				
				double arrivalTimeInMinutes = trip.getArrivalTime_s();
				double arrivalTimeInSeconds = arrivalTimeInMinutes * 60;
				// TODO make mode adjustable
				PersonArrivalEvent personArrivalEvent = new PersonArrivalEvent(arrivalTimeInSeconds, personId, null, "car");
				events.add(personArrivalEvent);
//				eventsMap.put(arrivalTimeInSeconds, personArrivalEvent);
				ActivityStartEvent activityStartEvent = new ActivityStartEvent(arrivalTimeInSeconds, personId, null, null, activityTypeStartingActivity);
				events.add(activityStartEvent);	
//				eventsMap.put(arrivalTimeInSeconds, activityStartEvent);
	    	}  	
	    }	    
	    
	    // write population
	    MatsimWriter popWriter = new PopulationWriter(population, scenario.getNetwork());
	    popWriter.write(outputDirectory + "plans.xml");
	    
	    //  write events
	    // TODO have events sorted by time
	    int eventsCounter = 0;
	    EventWriterXML eventWriter = new EventWriterXML(outputDirectory + "events.xml");
//	    for (Event event : eventsMap.values()) {
	    for (Event event : events) {
	    	eventWriter.handleEvent(event);
	    	eventsCounter++;
	    }
	    eventWriter.closeFile();
	    
	    // print counters
	    System.out.println("tripMapEntryCounter = " + tripMapEntryCounter);
	    System.out.println("events added: " + eventsCounter);
	}
}
