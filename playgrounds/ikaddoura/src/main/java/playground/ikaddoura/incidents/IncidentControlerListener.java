/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkImpl;

/**
* @author ikaddoura
*/

public class IncidentControlerListener implements IterationStartsListener {
	private static final Logger log = Logger.getLogger(IncidentControlerListener.class);

	private String networkChangeEventsDirectory = null;
	private Controler controler;	
		
	public IncidentControlerListener(Controler controler, String networkChangeEventsDirectory) {
		this.networkChangeEventsDirectory = networkChangeEventsDirectory;
		this.controler = controler;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		String nce = networkChangeEventsDirectory + "nce_" + event.getIteration() + ".xml.gz";

		log.info("Setting network change events for the next iteration: " + nce);
						
		List<NetworkChangeEvent> networkChangeEvents = new NetworkChangeEventsParser(controler.getScenario().getNetwork()).parseEvents(nce);;
				
		NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
		network.getNetworkChangeEvents().clear();
		network.setNetworkChangeEvents(networkChangeEvents);
		event.getServices().getConfig().network().setChangeEventInputFile(networkChangeEventsDirectory + "nce_" + event.getIteration() + "xml.gz");
	}

}

