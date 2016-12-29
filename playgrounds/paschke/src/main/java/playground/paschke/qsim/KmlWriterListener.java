package playground.paschke.qsim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.MultiPolygon;

import playground.paschke.events.handlers.DemandDistributionHandler;

public class KmlWriterListener implements IterationStartsListener, IterationEndsListener {
	int frequency = 0;

	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	@Inject private OutputDirectoryHierarchy outputDirectoryHierarchy;

	@Inject private DemandDistributionHandler demandDistributionHandler;

	public KmlWriterListener(int frequency) {
		this.frequency = frequency;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.carsharingVehicleRelocation.reset();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() % this.frequency == 0) {
			// write relocation zone KML files
			RelocationZoneKmlWriter writer = new RelocationZoneKmlWriter();

			for (Entry<String, List<RelocationZone>> relocationZoneEntry : this.carsharingVehicleRelocation.getRelocationZones().entrySet()) {
				String companyId = relocationZoneEntry.getKey();
				List<RelocationZone> relocationZones = relocationZoneEntry.getValue();
				Map<Id<RelocationZone>, MultiPolygon> polygons = new HashMap<Id<RelocationZone>, MultiPolygon>();

				for (RelocationZone relocationZone : relocationZones) {
					polygons.put(relocationZone.getId(), (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom"));
				}
				writer.setPolygons(polygons);

				Iterator<Entry<Double, Map<Id<RelocationZone>, Map<String, Integer>>>> statusIterator = this.carsharingVehicleRelocation.getStatus().get(companyId).entrySet().iterator();
				while (statusIterator.hasNext()) {
					Entry<Double, Map<Id<RelocationZone>, Map<String, Integer>>> statusEntry = statusIterator.next();
					Double time = statusEntry.getKey();
					String filename = this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), companyId + "." + time + ".relocation_zones.xml");

					writer.writeFile(time, filename, statusEntry.getValue());
				}
			}

			// log relocations
			final BufferedWriter outRelocations = IOUtils.getBufferedWriter(this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "relocations.txt"));
			try {
				for (Entry<String, List<RelocationInfo>> companyEntry : this.carsharingVehicleRelocation.getRelocations().entrySet()) {
					List<RelocationInfo> companyRelocations = companyEntry.getValue();

					outRelocations.write("timeSlot	startZone	endZone	startTime	endTime	startLink	endLink	companyID	vehicleID	agentID");
					outRelocations.newLine();

					for (RelocationInfo i: companyRelocations) {
						outRelocations.write(i.toString());
						outRelocations.newLine();
					}
				}

				outRelocations.flush();
				outRelocations.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// log trip OD-Matrix
			final BufferedWriter outODMatrix = IOUtils.getBufferedWriter(this.outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "CS-OD-Matrix.txt"));
			try {
				for (Entry<String, Map<Double, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>> companyEntry : this.demandDistributionHandler.getODMatrices().entrySet()) {
					String companyId = companyEntry.getKey();
					outODMatrix.write(companyId);
					outODMatrix.newLine();

					Map<Double, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>> companyODMatrices = companyEntry.getValue();

					for (Entry<Double, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>> companyODMatrix : companyODMatrices.entrySet()) {
						outODMatrix.newLine();

						Double start = companyODMatrix.getKey();
						outODMatrix.write(start.toString());

						for (RelocationZone relocationZone : this.carsharingVehicleRelocation.getRelocationZones(companyId)) {
							outODMatrix.write("	" + relocationZone.getId().toString());
						}

						Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>> origins = companyODMatrix.getValue();

						for (RelocationZone originRelocationZone : this.carsharingVehicleRelocation.getRelocationZones(companyId)) {
							outODMatrix.newLine();
							outODMatrix.write(originRelocationZone.getId().toString());

							for (RelocationZone destinationRelocationZone : this.carsharingVehicleRelocation.getRelocationZones(companyId)) {
								if (origins.keySet().contains(originRelocationZone.getId())) {
									if (origins.get(originRelocationZone.getId()).keySet().contains(destinationRelocationZone.getId())) {
										outODMatrix.write("	" + origins.get(originRelocationZone.getId()).get(destinationRelocationZone.getId()).toString());
									} else {
										outODMatrix.write("	0");
									}
								} else {
									outODMatrix.write("	0");
								}
							}
						}
					}

					outODMatrix.newLine();
				}

				outODMatrix.flush();
				outODMatrix.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
