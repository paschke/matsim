package playground.paschke.qsim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class RelocationZoneKmlWriterListener implements IterationEndsListener {
	Controler controler;
	int frequency = 0;
	
	public RelocationZoneKmlWriterListener(Controler controler, int frequency) {
		this.controler = controler;
		this.frequency = frequency;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		RelocationZoneKmlWriter writer = new RelocationZoneKmlWriter();
		CarSharingRelocationZones relocationZones = (CarSharingRelocationZones) this.controler.getScenario().getScenarioElement(CarSharingRelocationZones.ELEMENT_NAME);
		Map<Id<RelocationZone>, MultiPolygon> polygons = new HashMap<Id<RelocationZone>, MultiPolygon>();

		for (RelocationZone relocationZone : relocationZones.getRelocationZones()) {
			polygons.put(relocationZone.getId(), (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom"));
		}

		writer.setPolygons(polygons);
		Iterator<Entry<Double, Map<Id<RelocationZone>, Map<String, Integer>>>> statusIterator = relocationZones.getStatus().entrySet().iterator();

		while (statusIterator.hasNext()) {
			Entry<Double, Map<Id<RelocationZone>, Map<String, Integer>>> entry = statusIterator.next();
			Double time = entry.getKey();
			String filename = this.controler.getControlerIO().getIterationFilename(event.getIteration(), time + ".relocation_zones.xml");

			writer.write(filename, entry.getValue());
		}
	}
}
