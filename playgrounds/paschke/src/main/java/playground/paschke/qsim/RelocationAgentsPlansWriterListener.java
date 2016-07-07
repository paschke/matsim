package playground.paschke.qsim;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class RelocationAgentsPlansWriterListener implements IterationEndsListener {
	protected Controler controler;
	protected Map<Id<Person>, RelocationAgent> relocationAgents;
	protected RelocationAgentsPlansWriter writer;

	public RelocationAgentsPlansWriterListener(Controler controler) {
		this.controler = controler;
		this.writer = new RelocationAgentsPlansWriter();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String filename = this.controler.getControlerIO().getIterationFilename(event.getIteration(), "relocation_agents_plans.xml");

		this.writer.write(filename, this.relocationAgents);
	}

}
