package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.core.utils.geometry.CoordImpl;

public class RelocationZone {
	private Coord coord;
	private Map<Link, Integer> requests;
	private Map<Link, CopyOnWriteArrayList<String>> vehicles;

	public RelocationZone(Coord coord) {
		this.coord = coord;
		this.requests = new HashMap<Link, Integer>();
		this.vehicles = new ConcurrentHashMap<Link, CopyOnWriteArrayList<String>>();
	}

	public Coord getCoord() {
		return coord;
	}

	public Map<Link, Integer> getRequests() {
		return this.requests;
	}

	public Map<Link, CopyOnWriteArrayList<String>> getVehicles() {
		return this.vehicles;
	}

	public List<String> getVehicleIds() {
		ArrayList<String> Ids = new ArrayList<String>();

		for (List<String> linkIds : this.getVehicles().values()) {
			Ids.addAll(linkIds);
		}

		return Ids;
	}

	public int getNumberOfRequests() {
		int number = 0;

		for (Integer linkRequests : requests.values()) {
			number += linkRequests.intValue();
		}

		return number;
	}

	public int getNumberOfVehicles() {
		int number = 0;

		for (CopyOnWriteArrayList<String> IDs : vehicles.values()) {
			number += IDs.size();
		}

		return number;
	}

	public int getNumberOfSurplusVehicles() {
		return this.getNumberOfVehicles() - this.getNumberOfRequests();
	}

	public void addRequests(Link link, int numberOfRequests) {
		if (this.requests.containsKey(link)) {
			int previousNumberOfRequests = this.requests.get(link);
			numberOfRequests += previousNumberOfRequests;
		}

		requests.put(link, numberOfRequests);
	}

	public void addVehicles(Link link, ArrayList<String> IDs) {
		if (this.getVehicles().containsKey(link)) {
			for (String ID : IDs) {
				this.getVehicles().get(link).add(ID);
			}
		} else {
			this.getVehicles().put(link, new CopyOnWriteArrayList<String>(IDs));
		}
	}

	public void reset() {
		this.requests.clear();
		this.vehicles.clear();
	}
}
