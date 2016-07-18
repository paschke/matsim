package playground.paschke.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripRouter;
import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.RelocationAgent;
import playground.paschke.qsim.CarSharingDemandTracker.RentalInfoFF;
import playground.paschke.qsim.RelocationZones;
import playground.paschke.qsim.RelocationInfo;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class MobismBeforeSimStepRelocationAgentsDispatcher implements MobsimBeforeSimStepListener {
	private static final Logger log = Logger.getLogger("dummy");

	private Scenario scenario;

	private CarSharingVehicles carSharingVehicles;

	private CarSharingDemandTracker demandTracker;

	@Inject
	public MobismBeforeSimStepRelocationAgentsDispatcher(final CarSharingVehicles carSharingVehicles, final CarSharingDemandTracker demandTracker, final Provider<TripRouter> routerProvider) {
		this.carSharingVehicles = carSharingVehicles;
		this.demandTracker = demandTracker;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		QSim qSim = (QSim) event.getQueueSimulation();
		this.scenario = qSim.getScenario();

		CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		double now = Math.floor(qSim.getSimTimer().getTimeOfDay());
		double then;

		// relocation times will only be called if there are activities (usually starting with 32400.0), which makes sense
		@SuppressWarnings("unchecked")
		List<Double> relocationTimes = (List<Double>) this.scenario.getScenarioElement("CarSharingRelocationTimes");
		final RelocationZones relocationZones = (RelocationZones) this.scenario.getScenarioElement(RelocationZones.ELEMENT_NAME);

		if (relocationTimes.contains(now)) {
			int index = relocationTimes.indexOf(now);
			try {
				then = relocationTimes.get(index + 1);
			} catch (IndexOutOfBoundsException e) {
				then = 86400.0;
			}

			log.info("is it that time again? " + (Math.floor(qSim.getSimTimer().getTimeOfDay()) / 3600));

			relocationZones.resetRelocationZones();

			// estimate demand in cells from logged CarSharingRequests
			for (RentalInfoFF info : demandTracker.getRentalsInInterval(now, then)) {
				Link accessLink = scenario.getNetwork().getLinks().get(info.accessLinkId);
				relocationZones.addExpectedRequests(accessLink, 1);

				Link endLink = scenario.getNetwork().getLinks().get(info.endLinkId);
				relocationZones.addExpectedReturns(endLink, 1);
			}

			// count number of vehicles in car sharing relocation zones
			for (FreeFloatingStation ffs : carSharingVehicles.getFreeFLoatingVehicles().getQuadTree().values()) {
				Link ffsLink = scenario.getNetwork().getLinks().get(ffs.getLinkId());
				relocationZones.addVehicles(ffsLink, ffs.getIDs());
			}

			// compare available vehicles to demand for each zone, store result
			relocationZones.storeStatus(now);

			for (RelocationInfo info : relocationZones.calculateRelocations(now, then)) {
				log.info("RelocationZones suggests we move vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId() + " to " + info.getEndLinkId());

				if (confGroup.useRelocation()) {
					this.dispatchRelocation(qSim, info);
				}
			}
		}
	}

	private void dispatchRelocation(QSim qSim, RelocationInfo info) {
		RelocationAgent agent = this.getRelocationAgent(qSim);
		info.setAgentId(agent.getId());

		if (agent != null) {
			agent.dispatchRelocation(info);
		}
	}

	private RelocationAgent getRelocationAgent(QSim qSim) {
		Scenario scenario = qSim.getScenario();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Double>> relocationAgentBasesList = (Map<String, Map<String, Double>>) scenario.getScenarioElement("CarSharingRelocationAgents");

		Iterator<Entry<String, Map<String, Double>>> baseIterator = relocationAgentBasesList.entrySet().iterator();
		while (baseIterator.hasNext()) {
			Entry<String, Map<String, Double>> entry = baseIterator.next();
			String baseId = entry.getKey();
			HashMap<String, Double> agentBaseData = (HashMap<String, Double>) entry.getValue();

			int counter = 0;
			while (counter < agentBaseData.get("number")) {
				Id<Person> id = Id.createPersonId("RelocationAgent" + "_" + baseId + "_"  + counter);
				RelocationAgent agent = (RelocationAgent) qSim.getAgentMap().get(id);

				if ((agent.getState() == State.ACTIVITY) && (agent.getRelocations().size() == 0)) {
					log.info("RelocationAgent " + agent.getId() + " reused from the agent pool");
					return agent;
				}

				counter++;
			}
		}

		return null;
	}
}
