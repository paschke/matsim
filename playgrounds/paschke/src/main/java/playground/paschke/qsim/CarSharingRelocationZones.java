package playground.paschke.qsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.paschke.qsim.CarSharingDemandTracker.RequestInfo;

public class CarSharingRelocationZones {
	private Scenario scenario;
	private QuadTree<RelocationZone> relocationZoneQuadTree;	

	public CarSharingRelocationZones(Scenario scenario) throws IOException {
		this.scenario = scenario;

		readRelocationZoneLocations();
	}

	public void readRelocationZoneLocations() {
		// TODO read relocation zone coords from config file here
		ArrayList<RelocationZone> relocationZones = new ArrayList<RelocationZone>();

		CoordImpl coord1 = new CoordImpl(0, 0);
		RelocationZone relocationZone1 = new RelocationZone(coord1);
    	relocationZones.add(relocationZone1);

		CoordImpl coord2 = new CoordImpl(1000, 0);
		RelocationZone relocationZone2 = new RelocationZone(coord2);
    	relocationZones.add(relocationZone2);

		// TODO read QuadTree dimensions from config (the following is just copied from FreeFloatingVehiclesLocation)
    	double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

	    for (Link l : scenario.getNetwork().getLinks().values()) {
	    	if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	    	if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	    	if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	    	if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
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

	public void reset() {
		for (RelocationZone r : this.relocationZoneQuadTree.values()) {
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
					return 1;
				} else if (o1.getNumberOfSurplusVehicles() > o2.getNumberOfSurplusVehicles()) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		// TODO: add the actual relocation logic here
		// for now, just pick the first and last of the relocation zones.
		RelocationZone first = relocationZones.get(0);
		RelocationZone last = relocationZones.get(relocationZones.size() - 1);

		if ((first.getNumberOfSurplusVehicles() > 0) && (last.getNumberOfSurplusVehicles() < 0)) {
			Link fromLink = (Link) ((Set<Link>) first.getVehicles().keySet()).iterator().next();
			String vehicleID = first.getVehicles().get(fromLink).get(0);

			// this will result in a null pointer exception if last has no vehicles - use request Map instead
			Link toLink = (Link) ((Set<Link>) last.getRequests().keySet()).iterator().next();

			relocations.add(new RelocationInfo(vehicleID, fromLink, toLink));
		}

		return relocations;
	}

	public class RelocationInfo {
		private String vehicleID;
		private Link from;
		private Link to;

		public RelocationInfo(String vehicleID, Link from, Link to) {
			this.vehicleID = vehicleID;
			this.from = from;
			this.to = to;
		}

		public String getVehicleID() {
			return this.vehicleID;
		}

		public Link getFrom() {
			return this.from;
		}

		public Link getTo() {
			return this.to;
		}
	}
}
