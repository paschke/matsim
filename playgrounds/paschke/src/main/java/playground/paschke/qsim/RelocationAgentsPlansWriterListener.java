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
	protected int frequency = 0;

	public RelocationAgentsPlansWriterListener(Controler controler, int frequency) {
		this.controler = controler;
		this.frequency = frequency;
		this.writer = new RelocationAgentsPlansWriter();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % this.frequency == 0) {
			String filename = this.controler.getControlerIO().getIterationFilename(event.getIteration(), "relocation_agents_plans.xml");

			this.writer.write(filename, this.relocationAgents);
		}
	}

}
