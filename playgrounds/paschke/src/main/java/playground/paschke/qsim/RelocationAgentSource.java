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
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;

public class RelocationAgentSource implements AgentSource {

	private static final Logger log = Logger.getLogger("dummy");

	private RelocationAgentFactory relocationAgentFactory;
	private QSim qSim;
	private Provider<TripRouter> routerProvider;
	private CarSharingVehicles carSharingVehicles;

	public RelocationAgentSource(RelocationAgentFactory relocationAgentFactory, QSim qSim, Provider<TripRouter> routerProvider, CarSharingVehicles carSharingVehicles) {
		this.relocationAgentFactory = relocationAgentFactory;
		this.qSim = qSim;
		this.routerProvider = routerProvider;
		this.carSharingVehicles = carSharingVehicles; 
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Map<Id<Person>, RelocationAgent> relocationAgents = new HashMap<Id<Person>, RelocationAgent>();

		Scenario scenario = this.qSim.getScenario();
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Double>> relocationAgentBasesList = (Map<String, Map<String, Double>>) scenario.getScenarioElement("CarSharingRelocationAgents");

		Iterator<Entry<String, Map<String, Double>>> baseIterator = relocationAgentBasesList.entrySet().iterator();
		while (baseIterator.hasNext()) {
			Entry<String, Map<String, Double>> entry = baseIterator.next();
			String baseId = entry.getKey();
			HashMap<String, Double> agentBaseData = (HashMap<String, Double>) entry.getValue();

			Coord coord = new Coord(agentBaseData.get("x"), agentBaseData.get("y"));
			Link link = network.getNearestLinkExactly(coord );

			int counter = 0;
			while (counter < agentBaseData.get("number")) {
				Id<Person> id = Id.createPersonId("RelocationAgent" + "_" + baseId + "_"  + counter);
				RelocationAgent agent = this.relocationAgentFactory.createRelocationAgent(id, link.getId());
				agent.setGuidance(new Guidance(this.routerProvider.get()));
				agent.setMobsimTimer(qSim.getSimTimer());
				agent.setCarSharingVehicles(this.carSharingVehicles);

				relocationAgents.put(id, agent);
				qSim.insertAgentIntoMobsim(agent);

				counter++;
			}
		}

		log.info("inserted " + relocationAgents.size() + " relocation agents into qSim");
	}

}
