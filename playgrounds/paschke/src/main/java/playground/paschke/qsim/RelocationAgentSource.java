package playground.paschke.qsim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RelocationAgentSource implements AgentSource {

	private static final Logger log = Logger.getLogger("dummy");

	private RelocationAgentFactory relocationAgentFactory;

	private QSim qSim;

	private CarsharingVehicleRelocation carsharingVehicleRelocation;

	private Provider<TripRouter> routerProvider;

	private CarsharingSupplyInterface carsharingSupply;

	@Inject
	public RelocationAgentSource(Scenario scenario, QSim qSim, CarsharingVehicleRelocation carsharingVehicleRelocation, Provider<TripRouter> routerProvider, CarsharingSupplyInterface carsharingSupply) {
		this.relocationAgentFactory = new RelocationAgentFactory(scenario);
		this.qSim = qSim;
		this.carsharingVehicleRelocation = carsharingVehicleRelocation;
		this.routerProvider = routerProvider;
		this.carsharingSupply = carsharingSupply;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Map<Id<Person>, RelocationAgent> relocationAgents = new HashMap<Id<Person>, RelocationAgent>();

		Scenario scenario = this.qSim.getScenario();
		Network network = scenario.getNetwork();
		Map<String, Map<String, Map<String, Double>>> relocationAgentBasesList = this.carsharingVehicleRelocation.getRelocationAgentBases();

		Iterator<Entry<String, Map<String, Map<String, Double>>>> companiesIterator = relocationAgentBasesList.entrySet().iterator();
		while (companiesIterator.hasNext()) {
			Entry<String, Map<String, Map<String, Double>>> companyEntry = companiesIterator.next();
			String companyId = companyEntry.getKey();
			Iterator<Entry<String, Map<String, Double>>> baseIterator = companyEntry.getValue().entrySet().iterator();

			while (baseIterator.hasNext()) {
				Entry<String, Map<String, Double>> baseEntry = baseIterator.next();
				String baseId = baseEntry.getKey();
				HashMap<String, Double> agentBaseData = (HashMap<String, Double>) baseEntry.getValue();

				Coord coord = new Coord(agentBaseData.get("x"), agentBaseData.get("y"));
				Link link = (Link) NetworkUtils.getNearestLinkExactly(network, coord);

				int counter = 0;
				while (counter < agentBaseData.get("number")) {
					Id<Person> id = Id.createPersonId("RelocationAgent" + "_" + companyId + "_" + baseId + "_"  + counter);
					RelocationAgent agent = this.relocationAgentFactory.createRelocationAgent(id, companyId, link.getId());
					agent.setGuidance(new Guidance(this.routerProvider.get()));
					agent.setMobsimTimer(this.qSim.getSimTimer());
					agent.setCarsharingSupplyContainer(this.carsharingSupply);

					relocationAgents.put(id, agent);
					this.qSim.insertAgentIntoMobsim(agent);

					counter++;
				}
			}
		}

		log.info("inserted " + relocationAgents.size() + " relocation agents into qSim");
	}

}
