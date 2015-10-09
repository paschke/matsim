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

import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.Guidance;
import playground.paschke.qsim.RelocationAgent;
import playground.paschke.qsim.CarSharingDemandTracker.RequestInfo;
import playground.paschke.qsim.CarSharingRelocationZones;
import playground.paschke.qsim.CarSharingRelocationZones.RelocationInfo;
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
	public MobismBeforeSimStepRelocationAgentsDispatcher(final CarSharingVehicles carSharingVehicles, final CarSharingRelocationZones relocationZones, final CarSharingDemandTracker demandTracker, final Provider<TripRouter> routerProvider) {
		this.carSharingVehicles = carSharingVehicles;
		this.relocationZones = relocationZones;
		this.demandTracker = demandTracker;
		this.routerProvider = routerProvider;

		this.relocationAgents = new HashMap<Id<Person>, RelocationAgent>();
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		QSim qSim = (QSim) event.getQueueSimulation();
		this.scenario = qSim.getScenario();

		// TODO: relocation times must be configurable
		// relocation times will only be called if there are activities (usually starting with 32400.0), which makes sense
		List<Double> relocationTimes = new ArrayList(Arrays.asList(0.0, 10800.0, 21600.0, 32400.0, 43200.0, 54000.0, 64800.0, 75600.0));

		if (relocationTimes.contains(Math.floor(qSim.getSimTimer().getTimeOfDay()))) {
			log.info("is it that time again? " + (Math.floor(qSim.getSimTimer().getTimeOfDay()) / 3600));
			this.timeOfDay = qSim.getSimTimer().getTimeOfDay();

			relocationZones.reset();

			// estimate demand in cells from logged CarSharingRequests
			// TODO: time interval must be configurable
			// TODO: adding request info could be wrapped inside RelocationZones
			for (RequestInfo info : demandTracker.getCarSharingRequestsInInterval(Math.floor(qSim.getSimTimer().getTimeOfDay()), 10800)) {
				log.info("logging some request here!");
				Link link = scenario.getNetwork().getLinks().get(info.getAccessLinkId());
				RelocationZone relocationZone = relocationZones.getQuadTree().get(link.getCoord().getX(), link.getCoord().getY());
				relocationZone.addRequests(link, 1);
			}

			// count number of vehicles in car sharing relocation zones
			// TODO: adding vehicles could be wrapped inside RelocationZone
			for (FreeFloatingStation ffs : carSharingVehicles.getFreeFLoatingVehicles().getQuadTree().values()) {
				RelocationZone relocationZone = relocationZones.getQuadTree().get(ffs.getLink().getCoord().getX(), ffs.getLink().getCoord().getY());
				relocationZone.addVehicles(ffs.getLink(), ffs.getIDs());
			}

			// compare available vehicles to demand
			for (RelocationZone relocationZone : relocationZones.getQuadTree().values()) {
				log.info("relocation zone " + relocationZone.getCoord().getX() + " " + relocationZone.getCoord().getY() + " counts " + relocationZone.getNumberOfVehicles() + " vehicles and expects " + relocationZone.getNumberOfRequests() + " requests!");
			}

			for (RelocationInfo info : relocationZones.getRelocations()) {
				log.info("RelocationZones suggests we move one vehicle from zone " + info.getFrom().getCoord().getX() + " " + info.getFrom().getCoord().getY() + " to " + info.getTo().getCoord().getX() + " " + info.getTo().getCoord().getY());

				this.dispatchRelocation(qSim, info.getVehicleID(), info.getFrom(), info.getTo());
			}
		}
	}

	private void dispatchRelocation(QSim qSim, String vehicleId, Link fromLink, Link toLink) {
		RelocationAgent agent = this.getRelocationAgent(qSim);

		agent.dispatch(vehicleId, fromLink.getId(), toLink.getId());

		qSim.insertAgentIntoMobsim(agent);
	}

	private RelocationAgent getRelocationAgent(QSim qSim) {
		for (RelocationAgent agent : this.relocationAgents.values()) {
			if (agent.getState() == State.ABORT) {
				log.info("RelocationAgent " + agent.getId() + " reused from the agent pool");
				return agent;
			}
		}

		Guidance guidance = new Guidance(this.routerProvider.get(), qSim.getScenario());

		int counter = 0;
		Id<Person> id = Id.createPersonId("DemonAgent" + counter);
		while (this.relocationAgents.containsKey(id)) {
			counter++;
			id = Id.createPersonId("DemonAgent" + counter);
		}

		RelocationAgent agent = new RelocationAgent(id, guidance, qSim.getSimTimer(), qSim.getScenario(), carSharingVehicles);
		log.info("RelocationAgent " + id + " created and added to agent pool");
		this.relocationAgents.put(id, agent);

		return agent;
	}
}
