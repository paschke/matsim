package playground.paschke.qsim;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RelocationAgentSource implements AgentSource {

	private static final Logger log = Logger.getLogger("dummy");

	private QSim qSim;

	private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	private Provider<TripRouter> routerProvider;

	private CarsharingSupplyInterface carsharingSupply;

	@Inject
	public RelocationAgentSource(Scenario scenario, QSim qSim, CarsharingVehicleRelocationContainer carsharingVehicleRelocation, Provider<TripRouter> routerProvider, CarsharingSupplyInterface carsharingSupply) {
		this.qSim = qSim;
		this.carsharingVehicleRelocation = carsharingVehicleRelocation;
		this.routerProvider = routerProvider;
		this.carsharingSupply = carsharingSupply;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Map<String, Map<Id<Person>, RelocationAgent>> relocationAgents = this.carsharingVehicleRelocation.getRelocationAgents();

		Iterator<Entry<String, Map<Id<Person>, RelocationAgent>>> companiesIterator = relocationAgents.entrySet().iterator();
		int counter = 0;
		while (companiesIterator.hasNext()) {
			Entry<String, Map<Id<Person>, RelocationAgent>> companyEntry = companiesIterator.next();
			Iterator<Entry<Id<Person>, RelocationAgent>> agentIterator = companyEntry.getValue().entrySet().iterator();

			while (agentIterator.hasNext()) {
				Entry<Id<Person>, RelocationAgent> agentEntry = agentIterator.next();
				RelocationAgent agent = agentEntry.getValue();
				agent.setGuidance(new Guidance(this.routerProvider.get()));
				agent.setMobsimTimer(this.qSim.getSimTimer());
				agent.setCarsharingSupplyContainer(this.carsharingSupply);

				this.qSim.insertAgentIntoMobsim(agent);
				counter++;
			}
		}

		log.info("inserted " + counter + " relocation agents into qSim");
	}

}
