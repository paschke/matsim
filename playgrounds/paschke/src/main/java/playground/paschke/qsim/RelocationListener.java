package playground.paschke.qsim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.MultiPolygon;

public class RelocationListener implements IterationStartsListener, IterationEndsListener {
	Controler controler;
	int frequency = 0;
	
	public RelocationListener(Controler controler, int frequency) {
		this.controler = controler;
		this.frequency = frequency;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		RelocationZones relocationZones = (RelocationZones) this.controler.getScenario().getScenarioElement(RelocationZones.ELEMENT_NAME);
		relocationZones.reset();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		RelocationZones relocationZones = (RelocationZones) this.controler.getScenario().getScenarioElement(RelocationZones.ELEMENT_NAME);

		if (event.getIteration() % this.frequency == 0) {
			// write relocation zone KML files
			RelocationZoneKmlWriter writer = new RelocationZoneKmlWriter();
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

				writer.write(time, filename, entry.getValue());
			}

			// log relocations
			ArrayList<RelocationInfo> relocations = (ArrayList<RelocationInfo>) relocationZones.getRelocations();
		
			final BufferedWriter outRelocations = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "relocations"));
			try {
				outRelocations.write("timeSlot	startZone	endZone	startTime	endTime	startLink	endLink	vehicleID	agentID");
				outRelocations.newLine();

				for (RelocationInfo i: relocations) {
					outRelocations.write(i.toString());
					outRelocations.newLine();
				}

				outRelocations.flush();
				outRelocations.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
