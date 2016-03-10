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
	    	
	    	vehicleLocationQuadTree.put(f.getCoord().getX(), f.getCoord().getY(), f);
	    }
	   
	  }	
	
	public QuadTree<FreeFloatingStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(Link link, String id) {
		
		Collection<FreeFloatingStation> stations = vehicleLocationQuadTree.getDisk(link.getCoord().getX(), link.getCoord().getY(), 0.0);
		
		if (stations.isEmpty()) {
			ArrayList<String> vehIDs = new ArrayList<String>();
			vehIDs.add(id);
			
			FreeFloatingStation fNew = new FreeFloatingStation(link, 1, vehIDs);		
			
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
		}
		else {
			
			for(FreeFloatingStation ffStation: stations) {
				
				if (ffStation.getLinkId().toString().equals(link.getId().toString())) {
					
					ArrayList<String> vehIDs = ffStation.getIDs();
					ArrayList<String> newvehIDs = new ArrayList<String>();
					for (String s : vehIDs) {
						newvehIDs.add(s);
					}
					newvehIDs.add(id);
					FreeFloatingStation fNew = new FreeFloatingStation(link, ffStation.getNumberOfVehicles() + 1, newvehIDs);		
					
					vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), ffStation);
					
					vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
					
					return;
					
				}
				
			}
			
			ArrayList<String> vehIDs = new ArrayList<String>();
			
			vehIDs.add(id);
			
			FreeFloatingStation fNew = new FreeFloatingStation(link, 1, vehIDs);		
			
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
			
		}
		
		
		
	}
	
	public void removeVehicle(Link link, String id) {
		
		FreeFloatingStation f = vehicleLocationQuadTree.getClosest(link.getCoord().getX(), link.getCoord().getY());
		
		if ( f.getLinkId().toString().equals(link.getId().toString())) {
			
			if (f.getNumberOfVehicles() == 1)
				vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), f);
			
			
			else {
				ArrayList<String> vehIDs = f.getIDs();
				ArrayList<String> newvehIDs = new ArrayList<String>();
				for (String s : vehIDs) {
					newvehIDs.add(s);
				}
			}
		}

		throw new RuntimeException("could not remove ff vehicle " + id + " from link " + link.getId() + " because it could not be found!");
	}
}
