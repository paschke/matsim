package playground.paschke.qsim;

import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

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
}
