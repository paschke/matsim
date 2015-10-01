package org.matsim.contrib.carsharing.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

import java.io.IOException;

/**
 *
 *
 *
 */

public class CarsharingQsimFactory implements Provider<Netsim>{
	private final Scenario sc;
	private final Provider<TripRouter> tripRouterProvider;	
	private final EventsManager eventsManager;
	private final CarSharingVehicles carSharingVehicles;

	@Inject	
	public CarsharingQsimFactory(Scenario sc,
			Provider<TripRouter> tripRouterProvider, EventsManager eventsManager, CarSharingVehicles carSharingVehicles) {
		this.sc = sc;
		this.tripRouterProvider = tripRouterProvider;
		this.eventsManager = eventsManager;
		this.carSharingVehicles = carSharingVehicles;
	}


	@Override
	public Netsim get() {
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);


        QNetsimEngineModule.configure(qSim);
		
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
				
		AgentFactory agentFactory = null;			
			
		try {
			this.carSharingVehicles.readVehicleLocations();

			agentFactory = new CarsharingAgentFactory(qSim, sc, tripRouterProvider, this.carSharingVehicles);

			if (sc.getConfig().network().isTimeVariantNetwork()) {
				qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
			}

			PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

			//we need to park carsharing vehicles on the network
			ParkCSVehicles parkSource = new ParkCSVehicles(sc.getPopulation(), agentFactory, qSim,
					this.carSharingVehicles.getFreeFLoatingVehicles(), this.carSharingVehicles.getOneWayVehicles(), this.carSharingVehicles.getTwoWayVehicles());
			qSim.addAgentSource(agentSource);
			qSim.addAgentSource(parkSource);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return qSim;
	}
		
}
