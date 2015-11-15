package playground.paschke.events;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.Guidance;
import playground.paschke.qsim.RelocationAgent;
import playground.paschke.qsim.CarSharingDemandTracker.RequestInfo;
import playground.paschke.qsim.CarSharingRelocationZones;
import playground.paschke.qsim.RelocationInfo;
import playground.paschke.qsim.RelocationZone;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class MobismBeforeSimStepRelocationAgentsDispatcher implements MobsimBeforeSimStepListener {
	private static final Logger log = Logger.getLogger("dummy");

	private Scenario scenario;

	private double timeOfDay;

	private CarSharingVehicles carSharingVehicles;

	private CarSharingRelocationZones relocationZones;

	private CarSharingDemandTracker demandTracker;

	private Provider<TripRouter> routerProvider;

	private Map<Id<Person>, RelocationAgent> relocationAgents;

	@Inject
	public MobismBeforeSimStepRelocationAgentsDispatcher(final CarSharingVehicles carSharingVehicles, final CarSharingRelocationZones relocationZones, final CarSharingDemandTracker demandTracker, final Provider<TripRouter> routerProvider, final Map<Id<Person>, RelocationAgent> relocationAgents) {
		this.carSharingVehicles = carSharingVehicles;
		this.relocationZones = relocationZones;
		this.demandTracker = demandTracker;
		this.routerProvider = routerProvider;
		this.relocationAgents = relocationAgents;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		QSim qSim = (QSim) event.getQueueSimulation();
		this.scenario = qSim.getScenario();

		// TODO: relocation times must be configurable
		// relocation times will only be called if there are activities (usually starting with 32400.0), which makes sense
		List<Double> relocationTimes = new ArrayList<Double>(Arrays.asList(0.0, 10800.0, 21600.0, 32400.0, 43200.0, 54000.0, 64800.0, 75600.0));

		if (relocationTimes.contains(Math.floor(qSim.getSimTimer().getTimeOfDay()))) {
			log.info("is it that time again? " + (Math.floor(qSim.getSimTimer().getTimeOfDay()) / 3600));
			this.timeOfDay = qSim.getSimTimer().getTimeOfDay();

			relocationZones.reset();

			// estimate demand in cells from logged CarSharingRequests
			// TODO: time interval must be configurable
			// TODO: adding request info could be wrapped inside RelocationZones
			for (RequestInfo info : demandTracker.getCarSharingRequestsInInterval(Math.floor(qSim.getSimTimer().getTimeOfDay()), 10800)) {
				Link link = scenario.getNetwork().getLinks().get(info.getAccessLinkId());
				RelocationZone relocationZone = relocationZones.getQuadTree().get(link.getCoord().getX(), link.getCoord().getY());
				relocationZone.addRequests(link, 1);
			}

			// count number of vehicles in car sharing relocation zones
			// TODO: adding vehicles could be wrapped inside RelocationZone
			for (FreeFloatingStation ffs : carSharingVehicles.getFreeFLoatingVehicles().getQuadTree().values()) {
				log.info("found " + ffs.getNumberOfVehicles() + " FF vehicles " + ffs.getIDs().toString() + " at this FF station " + ffs.getLink().getId());
				RelocationZone relocationZone = relocationZones.getQuadTree().get(ffs.getLink().getCoord().getX(), ffs.getLink().getCoord().getY());
				relocationZone.addVehicles(ffs.getLink(), ffs.getIDs());
			}

			// compare available vehicles to demand
			for (RelocationZone relocationZone : relocationZones.getQuadTree().values()) {
				log.info(
						"relocation zone " + relocationZone.getCoord().getX() + " " + relocationZone.getCoord().getY() +
						" counts " + relocationZone.getNumberOfVehicles() + " vehicles " + relocationZone.getVehicleIds().toString() +
						" and expects " + relocationZone.getNumberOfRequests() + " requests!"
				);
			}

			for (RelocationInfo info : relocationZones.getRelocations()) {
				log.info("RelocationZones suggests we move vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId() + " to " + info.getDestinationLinkId());

				this.dispatchRelocation(qSim, info);
			}
		}
	}

	private void dispatchRelocation(QSim qSim, RelocationInfo info) {
		RelocationAgent agent = this.getRelocationAgent(qSim);

		if (agent != null) {
			agent.dispatchRelocation(info);
		}
	}

	private RelocationAgent getRelocationAgent(QSim qSim) {
		for (RelocationAgent agent : this.relocationAgents.values()) {
			if ((agent.getState() == State.ACTIVITY) && (agent.getRelocations().size() == 0)) {
				return agent;
			}
		}

		return null;
	}
}
