package org.matsim.contrib.carsharing.vehicles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.utils.collections.QuadTree;

public class FreeFloatingVehiclesLocation {
	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private QuadTree<FreeFloatingStation> vehicleLocationQuadTree;	

	public FreeFloatingVehiclesLocation(Scenario scenario, ArrayList<FreeFloatingStation> stations) throws IOException {
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

	    vehicleLocationQuadTree = new QuadTree<FreeFloatingStation>(minx, miny, maxx, maxy);
	    
	    
	    for(FreeFloatingStation f: stations) {  
	    	
	    	vehicleLocationQuadTree.put(f.getLink().getCoord().getX(), f.getLink().getCoord().getY(), f);
	    }
	   
	  }	
	
	public QuadTree<FreeFloatingStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(Link link, String id) {
		Collection<FreeFloatingStation> stations = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY(), 0);

		if (stations.isEmpty()) {
			ArrayList<String> vehIDs = new ArrayList<String>();
			vehIDs.add(id);
			FreeFloatingStation station = new FreeFloatingStation(link, 1, vehIDs);		
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), station);
			log.info("added all new ff station at link " + link.getId() + ", containing ids " + station.getIDs().toString() + " to quadTree");

			return;
		} else {
			for (FreeFloatingStation station : stations) {
				if (station.getLink().getId().toString().equals(link.getId().toString())) {
					ArrayList<String> vehicleIDs = new ArrayList<String>();
					for (String vehicleId : station.getIDs()) {
						vehicleIDs.add(vehicleId);
					}
					vehicleIDs.add(id);
					FreeFloatingStation newStation = new FreeFloatingStation(link, station.getNumberOfVehicles() + 1, vehicleIDs);		
					boolean removeSuccess = vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), station);
					boolean addSuccess = vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), newStation);

					if (removeSuccess) {
						log.info("removed ff station at link " + station.getLink().getId() + ", containing ids " + station.getIDs().toString() + " from quadTree");
					} else {
						log.info("could not remove ff station at link " + station.getLink().getId() + ", containing ids " + station.getIDs().toString() + " from quadTree");
					}

					if (addSuccess) {
						log.info("added replacement ff station at link " + link.getId() + ", containing ids " + vehicleIDs.toString() + " to quadTree");
					} else {
						log.info("could not add replacement ff station at link " + link.getId() + ", containing ids " + vehicleIDs.toString() + " to quadTree");
					}

					return;
				}

				log.info("found an ff station (at link " + station.getLink().getId() + " containing " + station.getIDs().toString() + "), but do not like it!");
				log.info(station.getLink().getId().toString() + " does not look like " + link.getId().toString() + " to me!");
			}

			ArrayList<String> vehicleIDs = new ArrayList<String>();
			vehicleIDs.add(id);
			FreeFloatingStation newStation = new FreeFloatingStation(link, 1, vehicleIDs);		
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), newStation);
			log.info("added new ff station at link " + link.getId() + ", containing ids " + newStation.getIDs().toString() + " to quadTree");
		} 
	}
	
	public void removeVehicle(Link link, String id) {
		Collection<FreeFloatingStation> stations = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY(), 0);

		for (FreeFloatingStation station : stations) {
			if (station.getIDs().contains(id)) {
				if (station.getNumberOfVehicles() == 1) {
					vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), station);

					return;
				} else {
					ArrayList<String> vehicleIDs = new ArrayList<String>(station.getIDs());
					vehicleIDs.remove(id);
					FreeFloatingStation newStation = new FreeFloatingStation(link, vehicleIDs.size(), vehicleIDs);

					vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), station);
					vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), newStation);

					return;
				}
			}
		}

		throw new RuntimeException("could not remove ff vehicle " + id + " from link " + link.getId() + " because it could not be found!");
	}
}
