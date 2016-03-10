package playground.paschke.qsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.paschke.qsim.CarSharingDemandTracker.RequestInfo;

public class CarSharingRelocationZones {
	private static final Logger log = Logger.getLogger("dummy");
	public static final String ELEMENT_NAME = "carSharingRelocationZones";
	private QuadTree<RelocationZone> relocationZoneQuadTree;
	private HashMap<Double, Map<Coord, List<Integer>>> status= new HashMap<Double, Map<Coord, List<Integer>>>();

	public void setRelocationZoneLocations(List<RelocationZone> relocationZones) {
    	// TODO read QuadTree dimensions from config (the following is just copied from FreeFloatingVehiclesLocation)
    	double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

	    for (RelocationZone r: relocationZones) {
	    	if (r.getCoord().getX() < minx) minx = r.getCoord().getX();
	    	if (r.getCoord().getY() < miny) miny = r.getCoord().getY();
	    	if (r.getCoord().getX() > maxx) maxx = r.getCoord().getX();
	    	if (r.getCoord().getY() <= maxy) continue; maxy = r.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    relocationZoneQuadTree = new QuadTree<RelocationZone>(minx, miny, maxx, maxy);

	    for (RelocationZone r: relocationZones) {  
	    	relocationZoneQuadTree.put(r.getCoord().getX(), r.getCoord().getY(), r);
	    }
	}

	public QuadTree<RelocationZone> getQuadTree() {
		return this.relocationZoneQuadTree;
	}

	public Map<Double, Map<Coord, List<Integer>>> getStatus() {
		return this.status;
	}

	public void putStatus(double time, Map<Coord, List<Integer>> status) {
		this.status.put(new Double(time), status);
	}

	public void reset() {
		for (RelocationZone r : this.getQuadTree().values()) {
			r.reset();
		}
	}

	public ArrayList<RelocationInfo> getRelocations() {
		ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

		List<RelocationZone> relocationZones = new ArrayList<RelocationZone>(this.getQuadTree().values());
		Collections.sort(relocationZones, new Comparator<RelocationZone>() {

			@Override
			public int compare(RelocationZone o1, RelocationZone o2) {
				if (o1.getNumberOfSurplusVehicles() < o2.getNumberOfSurplusVehicles()) {
					return -1;
				} else if (o1.getNumberOfSurplusVehicles() > o2.getNumberOfSurplusVehicles()) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		Iterator iterator = relocationZones.iterator();
		while (iterator.hasNext()) {
			RelocationZone nextZone = (RelocationZone) iterator.next();
			log.info("relocationZone with " + nextZone.getNumberOfSurplusVehicles() + " surplus vehicles");

			if (nextZone.getNumberOfSurplusVehicles() < 0) {
				for (int i = 0; i < Math.abs(nextZone.getNumberOfSurplusVehicles()); i++) {
					log.info("counting down surplus vehicles: " + i);
					Link fromLink = null;
					Link toLink = (Link) ((Set<Link>) nextZone.getRequests().keySet()).iterator().next();
					String vehicleId = null;
					Collection<RelocationZone> adjacentZones = this.getAdjacentZones(nextZone);

					Iterator adjacentIterator = adjacentZones.iterator();
					while (adjacentIterator.hasNext()) {
						RelocationZone adjacentZone = (RelocationZone) adjacentIterator.next();
						log.info("adjacentZone has " + adjacentZone.getNumberOfSurplusVehicles() + " surplus vehicles");

						if (adjacentZone.getNumberOfSurplusVehicles() > 0) {
							Iterator<Link> links = adjacentZone.getVehicles().keySet().iterator();
							fromLink = links.next();
							CopyOnWriteArrayList<String> vehicleIds = adjacentZone.getVehicles().get(fromLink);

							if (vehicleIds.size() == 1) {
								log.info("found one vehicle at link " + fromLink.getId());
								vehicleId = vehicleIds.get(0);
								adjacentZone.getVehicles().remove(fromLink);
							} else {
								log.info("found multiple vehicles at link " + fromLink.getId());
								vehicleId = vehicleIds.remove(0);
								adjacentZone.getVehicles().put(fromLink, vehicleIds);
							}

							break;
						}
					}

					if ((fromLink != null) && (vehicleId != null)) {
						relocations.add(new RelocationInfo(vehicleId, fromLink.getId(), toLink.getId()));
					}
				}
			} else {
				break;
			}
		}

		return relocations;
	}

	protected Collection<RelocationZone> getAdjacentZones(RelocationZone currentZone) {
		// FIXME: hard-coded zone distance here
		Collection<RelocationZone> zones = this.getQuadTree().get(currentZone.getCoord().getX(), currentZone.getCoord().getY(), 5001);
		zones.remove(currentZone);

		return zones;
	}
}
