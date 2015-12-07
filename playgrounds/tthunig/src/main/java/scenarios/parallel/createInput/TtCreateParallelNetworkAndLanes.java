/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.parallel.createInput;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.v20.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to create network and lanes for the parallel scenario.
 * 
 * @author gthunig
 * 
 */
public final class TtCreateParallelNetworkAndLanes {

	private static final Logger log = Logger.getLogger(TtCreateParallelNetworkAndLanes.class);

	private Scenario scenario;

	private static final double LINK_LENGTH = 300.0; // m
	private static final double FREESPEED = 10.0; // m/s
	private static final double CAPACITY = 2000.0; // veh/h

	private boolean useSecondODPair = false;

	private Map<String, Id<Link>> links = new HashMap<>();

	public TtCreateParallelNetworkAndLanes(Scenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * Creates the Network for the parallel scenario and the required lanes.
     */
	public void createNetworkWithLanes() {
		Network net = this.scenario.getNetwork();
		if (net.getCapacityPeriod() != 3600.0){
			throw new IllegalStateException();
		}
		((NetworkImpl)net).setEffectiveLaneWidth(1.0);
		NetworkFactory fac = net.getFactory();

		// create nodes

		double scale = LINK_LENGTH;
		Node n1, n2, n3, n4, n5, n6, n7, n8;
		net.addNode(n1 = fac.createNode(Id.create(1, Node.class), new Coord(0.0, 0.0)));
		net.addNode(n2 = fac.createNode(Id.create(2, Node.class), new Coord(1.0 * scale, 0.0)));
		net.addNode(n3 = fac.createNode(Id.create(3, Node.class), new Coord(2.0 * scale, 1.0 * scale)));
		net.addNode(n4 = fac.createNode(Id.create(4, Node.class), new Coord(3.0 * scale, 1.0 * scale)));
		net.addNode(n5 = fac.createNode(Id.create(5, Node.class), new Coord(4.0 * scale, 0.0)));
		net.addNode(n6 = fac.createNode(Id.create(6, Node.class), new Coord(5.0 * scale, 0.0)));
		net.addNode(n7 = fac.createNode(Id.create(7, Node.class), new Coord(2.0 * scale, -1.0 * scale)));
		net.addNode(n8 = fac.createNode(Id.create(8, Node.class), new Coord(3.0 * scale, -1.0 * scale)));
		Node n9 = null, n10 = null, n11 = null, n12 = null;
		if (useSecondODPair) {
			net.addNode(n9 = fac.createNode(Id.create(9, Node.class), new Coord(2.5 * scale, 2.0 * scale)));
			net.addNode(n10 = fac.createNode(Id.create(10, Node.class), new Coord(2.5 * scale, 3.0 * scale)));
			net.addNode(n11 = fac.createNode(Id.create(11, Node.class), new Coord(2.5 * scale, -2.0 * scale)));
			net.addNode(n12 = fac.createNode(Id.create(12, Node.class), new Coord(2.5 * scale, -3.0 * scale)));
		}
		
		// create links

		initLinkIds();

		Link l = fac.createLink(links.get("1_2"), n1, n2);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_1"), n2, n1);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_3"), n2, n3);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("3_2"), n3, n2);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("3_4"), n3, n4);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("4_3"), n4, n3);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("4_5"), n4, n5);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_4"), n5, n4);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_6"), n5, n6);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("6_5"), n6, n5);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("2_7"), n2, n7);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("7_2"), n7, n2);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("7_8"), n7, n8);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("8_7"), n8, n7);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("8_5"), n8, n5);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);
		l = fac.createLink(links.get("5_8"), n5, n8);
		setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
		net.addLink(l);

		if (useSecondODPair) {
			l = fac.createLink(links.get("3_7"), n3, n7);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("7_3"), n7, n3);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("4_8"), n4, n8);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("8_4"), n8, n4);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("3_9"), n3, n9);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("9_3"), n9, n3);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("4_9"), n4, n9);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("9_4"), n9, n4);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("9_10"), n9, n10);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("10_9"), n10, n9);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("7_11"), n7, n11);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("11_7"), n11, n7);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("8_11"), n8, n11);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("11_8"), n11, n8);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("11_12"), n11, n12);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
			l = fac.createLink(links.get("12_11"), n12, n11);
			setLinkAttributes(l, CAPACITY, LINK_LENGTH, FREESPEED);
			net.addLink(l);
		}
		createLanes();
	}

	private void initLinkIds() {
		links.put("1_2", Id.create("12", Link.class));
		links.put("2_1", Id.create("21", Link.class));
		links.put("2_3", Id.create("23", Link.class));
		links.put("3_2", Id.create("32", Link.class));
		links.put("3_4", Id.create("34", Link.class));
		links.put("4_3", Id.create("43", Link.class));
		links.put("4_5", Id.create("45", Link.class));
		links.put("5_4", Id.create("54", Link.class));
		links.put("5_6", Id.create("56", Link.class));
		links.put("6_5", Id.create("65", Link.class));
		links.put("2_7", Id.create("27", Link.class));
		links.put("7_2", Id.create("72", Link.class));
		links.put("7_8", Id.create("78", Link.class));
		links.put("8_7", Id.create("87", Link.class));
		links.put("5_8", Id.create("58", Link.class));
		links.put("8_5", Id.create("85", Link.class));
		if (useSecondODPair) {
			links.put("3_7", Id.create("37", Link.class));
			links.put("7_3", Id.create("73", Link.class));
			links.put("4_8", Id.create("48", Link.class));
			links.put("8_4", Id.create("84", Link.class));
			links.put("3_9", Id.create("39", Link.class));
			links.put("9_3", Id.create("93", Link.class));
			links.put("4_9", Id.create("49", Link.class));
			links.put("9_4", Id.create("94", Link.class));
			links.put("9_10", Id.create("910", Link.class));
			links.put("10_9", Id.create("109", Link.class));
			links.put("7_11", Id.create("711", Link.class));
			links.put("11_7", Id.create("117", Link.class));
			links.put("8_11", Id.create("811", Link.class));
			links.put("11_8", Id.create("118", Link.class));
			links.put("11_12", Id.create("1112", Link.class));
			links.put("12_11", Id.create("1211", Link.class));
		}
	}

	private static void setLinkAttributes(Link link, double capacity,
			double length, double freeSpeed) {
		
		link.setCapacity(capacity);
		link.setLength(length);
		// agents have to reach the end of the link before the time step ends to
		// be able to travel forward in the next time step (matsim time step logic)
		link.setFreespeed(freeSpeed);
	}

	/**
	 * creates a lane for every turning direction
	 */
	private void createLanes() {
		
		Lanes laneDef20 = this.scenario.getLanes();
		LaneDefinitionsFactory20 fac = laneDef20.getFactory();

		// create link assignment of link 1_2
		LanesToLinkAssignment20 linkAssignment = fac.createLanesToLinkAssignment(links.get("1_2"));

		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("1_2.ol", Lane.class), CAPACITY,
				LINK_LENGTH, 0, 1, null,
				Arrays.asList(Id.create("1_2.l", Lane.class),
				Id.create("1_2.r", Lane.class)));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.l", Lane.class), CAPACITY,
					LINK_LENGTH / 2, -1, 1,
					Collections.singletonList(links.get("2_3")), null);
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.r", Lane.class), CAPACITY,
					LINK_LENGTH / 2, 1, 1,
					Collections.singletonList(links.get("2_7")), null);

		laneDef20.addLanesToLinkAssignment(linkAssignment);

		// create link assignment of link 6_5
		linkAssignment = fac.createLanesToLinkAssignment(links.get("6_5"));

		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("6_5.ol", Lane.class), CAPACITY,
				LINK_LENGTH, 0, 1, null,
				Arrays.asList(Id.create("6_5.l", Lane.class),
						Id.create("6_5.r", Lane.class)));

		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("6_5.l", Lane.class), CAPACITY,
				LINK_LENGTH / 2, -1, 1,
				Collections.singletonList(links.get("5_8")), null);
		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("6_5.r", Lane.class), CAPACITY,
				LINK_LENGTH / 2, 1, 1,
				Collections.singletonList(links.get("5_4")), null);

		laneDef20.addLanesToLinkAssignment(linkAssignment);

		if (useSecondODPair) {
			// create link assignment of link 10_9
			linkAssignment = fac.createLanesToLinkAssignment(links.get("10_9"));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("10_9.ol", Lane.class), CAPACITY,
					LINK_LENGTH, 0, 1, null,
					Arrays.asList(Id.create("10_9.l", Lane.class),
							Id.create("10_9.r", Lane.class)));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("10_9.l", Lane.class), CAPACITY,
					LINK_LENGTH / 2, -1, 1,
					Collections.singletonList(links.get("9_4")), null);
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("10_9.r", Lane.class), CAPACITY,
					LINK_LENGTH / 2, 1, 1,
					Collections.singletonList(links.get("9_3")), null);

			laneDef20.addLanesToLinkAssignment(linkAssignment);

			// create link assignment of link 12_11
			linkAssignment = fac.createLanesToLinkAssignment(links.get("12_11"));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("12_11.ol", Lane.class), CAPACITY,
					LINK_LENGTH, 0, 1, null,
					Arrays.asList(Id.create("12_11.l", Lane.class),
							Id.create("12_11.r", Lane.class)));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("12_11.l", Lane.class), CAPACITY,
					LINK_LENGTH / 2, -1, 1,
					Collections.singletonList(links.get("11_7")), null);
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("12_11.r", Lane.class), CAPACITY,
					LINK_LENGTH / 2, 1, 1,
					Collections.singletonList(links.get("11_8")), null);

			laneDef20.addLanesToLinkAssignment(linkAssignment);
		}
	}

	public void writeNetworkAndLanes(String directory) {
		new NetworkWriter(scenario.getNetwork()).write(directory + "network.xml");
		new LaneDefinitionsWriter20(scenario.getLanes()).write(directory + "lanes.xml");
	}

	/**
	 * Setting this flag true will expand the
	 * {@link playground.dgrether.koehlerstrehlersignal.figure9scenario.DgFigure9ScenarioGenerator}
	 * with a second origin_destination pair.
	 * @param useSecondODPair
     */
	public void setUseSecondODPair(boolean useSecondODPair) {
		this.useSecondODPair = useSecondODPair;
	}

}