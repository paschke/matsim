package playground.paschke.events;

import java.util.ArrayList;
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
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.FreeFloatingVehiclesContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.CarsharingVehicleRelocation;
import playground.paschke.qsim.RelocationAgent;
import playground.paschke.qsim.RelocationInfo;

public class MobismBeforeSimStepRelocationListener implements MobsimBeforeSimStepListener {
	private static final Logger log = Logger.getLogger("dummy");

	@Inject private CarsharingSupplyInterface carsharingSupply;

	@Inject private CarSharingDemandTracker demandTracker;

	@Inject private CarsharingVehicleRelocation carsharingVehicleRelocation;

	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
		QSim qSim = (QSim) event.getQueueSimulation();
		Scenario scenario = qSim.getScenario();

		double now = Math.floor(qSim.getSimTimer().getTimeOfDay());
		double then;

		// relocation times will only be called if there are activities (usually starting with 32400.0), which makes sense
		for (Entry<String, List<Double>> entry : this.carsharingVehicleRelocation.getRelocationTimes().entrySet()) {
			String companyId = entry.getKey();
			List<Double> relocationTimes = entry.getValue();

			if (relocationTimes.contains(now)) {
				int index = relocationTimes.indexOf(now);
				try {
					then = relocationTimes.get(index + 1);
				} catch (IndexOutOfBoundsException e) {
					then = 86400.0;
				}

				log.info("time to relocate " + companyId + " vehicles: " + (Math.floor(qSim.getSimTimer().getTimeOfDay()) / 3600));

				this.carsharingVehicleRelocation.resetRelocationZones(companyId);

				// estimate demand in cells from logged CarSharingRequests
				for (RentalInfo rentalInfo : this.demandTracker.getRentalsInInterval("FF", companyId, now, then)) {
					Link originLink = scenario.getNetwork().getLinks().get(rentalInfo.getOriginLinkId());
					this.carsharingVehicleRelocation.addExpectedRequests(companyId, originLink, 1);

					Link endLink = scenario.getNetwork().getLinks().get(rentalInfo.getEndLinkId());
					this.carsharingVehicleRelocation.addExpectedReturns(companyId, endLink, 1);
				}

				// count number of vehicles in car sharing relocation zones
				// TODO: does this apply to several carsharing modes??
				FreeFloatingVehiclesContainer vehiclesContainer = (FreeFloatingVehiclesContainer) this.carsharingSupply.getCompany(companyId).getVehicleContainer("freefloating");
				Iterator<Entry<CSVehicle, Link>> ffVehiclesIterator = vehiclesContainer.getFfvehiclesMap().entrySet().iterator();
				while (ffVehiclesIterator.hasNext()) {
					Entry<CSVehicle, Link> vehicleEntry = ffVehiclesIterator.next();
					ArrayList<String> IDs = new ArrayList<String>();
					IDs.add(vehicleEntry.getKey().getVehicleId());
					this.carsharingVehicleRelocation.addVehicles(companyId, vehicleEntry.getValue(), IDs);
				}

				// compare available vehicles to demand for each zone, store result
				this.carsharingVehicleRelocation.storeStatus(companyId, now);

				for (RelocationInfo info : this.carsharingVehicleRelocation.calculateRelocations(companyId, "FF", now, then)) {
					log.info("RelocationZones suggests we move vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId() + " to " + info.getEndLinkId());

					if (this.carsharingVehicleRelocation.useRelocation()) {
						this.dispatchRelocation(qSim, info);
					}
				}
			}
		}
	}

	private void dispatchRelocation(QSim qSim, RelocationInfo info) {
		RelocationAgent agent = this.getRelocationAgent(qSim, info.getCompanyId());

		if (agent != null) {
			info.setAgentId(agent.getId());
			agent.dispatchRelocation(info);
		}
	}

	private RelocationAgent getRelocationAgent(QSim qSim, String companyId) {
		Map<String, Map<String, Double>> relocationAgentBasesList = this.carsharingVehicleRelocation.getRelocationAgentBases(companyId);

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
