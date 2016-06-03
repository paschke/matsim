package playground.paschke.qsim;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class RelocationZoneKmlWriterListener implements IterationEndsListener {
	Controler controler;
	int frequency = 0;
	RelocationZoneKmlWriter writer;
	
	public RelocationZoneKmlWriterListener(Controler controler, int frequency) {
		this.controler = controler;
		this.frequency = frequency;
		this.writer = new RelocationZoneKmlWriter();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		CarSharingRelocationZones relocationZones = (CarSharingRelocationZones) this.controler.getScenario().getScenarioElement(CarSharingRelocationZones.ELEMENT_NAME);
//		Iterator<Entry<Double, Map<Coord, List<Integer>>>> statusIterator = relocationZones.getStatus().entrySet().iterator();
/*
		while (statusIterator.hasNext()) {
			Map.Entry<Double, Map<Coord, List<Integer>>> entry = statusIterator.next();
			Double time = entry.getKey();
			String filename = this.controler.getControlerIO().getIterationFilename(event.getIteration(), time + ".relocation_zones.xml");

			this.writer.write(filename, entry.getValue());
		}*/
	}
}
