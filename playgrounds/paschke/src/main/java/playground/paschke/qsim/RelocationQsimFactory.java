package playground.paschke.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.qsim.CSAgentFactory;
import org.matsim.contrib.carsharing.qsim.ParkCSVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 *
 *
 *
 */

public class RelocationQsimFactory implements Provider<Netsim>{


	@Inject private Scenario scenario;
	@Inject private EventsManager eventsManager;
	
	@Inject private CarsharingSupplyContainer carsharingSupply;
	@Inject private CarsharingManagerNew carsharingManager;

	@Inject private CarsharingVehicleRelocation carsharingVehicleRelocation;
	@Inject private Provider<TripRouter> routerProvider;

	@Override
	public Netsim get() {
		QSim qSim = new QSim(this.scenario, this.eventsManager);

		ActivityEngine activityEngine = new ActivityEngine(this.eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		TeleportationEngine teleportationEngine = new TeleportationEngine(this.scenario, this.eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
		qSim.addDepartureHandler(teleportationEngine) ;

		AgentFactory agentFactory = new CSAgentFactory(qSim, this.carsharingManager);

		if (this.scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}

		PopulationAgentSource agentSource = new PopulationAgentSource(this.scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		ParkCSVehicles parkSource = new ParkCSVehicles(qSim, this.carsharingSupply);
		qSim.addAgentSource(parkSource);

		RelocationAgentSource relocationAgentSource = new RelocationAgentSource(this.scenario, qSim, carsharingVehicleRelocation, routerProvider);
		qSim.addAgentSource(relocationAgentSource);

		return qSim;
	}
		
}
