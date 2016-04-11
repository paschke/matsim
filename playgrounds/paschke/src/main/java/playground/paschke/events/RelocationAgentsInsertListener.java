package playground.paschke.events;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripRouter;

import playground.paschke.qsim.Guidance;
import playground.paschke.qsim.RelocationAgent;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RelocationAgentsInsertListener implements MobsimInitializedListener {
	// DON'T USE THIS, REPLACED BY RelocationAgentSource

	private static final Logger log = Logger.getLogger("dummy");

	private Provider<TripRouter> routerProvider;

	Map<Id<Person>, RelocationAgent> relocationAgents;

	@Inject
	public RelocationAgentsInsertListener(Provider<TripRouter> routerProvider, Map<Id<Person>, RelocationAgent> relocationAgents) {
		this.routerProvider = routerProvider;
		this.relocationAgents = relocationAgents;		
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent event) {
		QSim qSim = (QSim) event.getQueueSimulation();

		for (RelocationAgent agent : this.relocationAgents.values()) {
			agent.reset();
			agent.setGuidance(new Guidance(this.routerProvider.get()));
			agent.setMobsimTimer(qSim.getSimTimer());
			qSim.insertAgentIntoMobsim(agent);
		}

		log.info("inserted " + this.relocationAgents.size() + " relocation agents into qSim");
	}
}
