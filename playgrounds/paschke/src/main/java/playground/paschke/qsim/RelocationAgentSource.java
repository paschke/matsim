package playground.paschke.qsim;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
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

		// TODO: number of relocation agents should be configurable
		Id<Link> relocationAgentBaseLinkId = Id.createLinkId(150535);
		int counter = 0;
		while (counter < 100) {
			Id<Person> id = Id.createPersonId("DemonAgent" + counter);
			RelocationAgent agent = this.relocationAgentFactory.createRelocationAgent(id, relocationAgentBaseLinkId);
			agent.setGuidance(new Guidance(this.routerProvider.get()));
			agent.setMobsimTimer(qSim.getSimTimer());
			agent.setCarSharingVehicles(this.carSharingVehicles);

			relocationAgents.put(id, agent);
			qSim.insertAgentIntoMobsim(agent);

			counter++;
		}

		log.info("inserted " + relocationAgents.size() + " relocation agents into qSim");
	}

}
