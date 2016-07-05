package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.opengis.feature.simple.SimpleFeature;

public class RelocationZone implements Identifiable<RelocationZone>{
	private Id<RelocationZone> id;

	private SimpleFeature polygon;

	private Map<Link, Integer> requests;

	private Map<Link, CopyOnWriteArrayList<String>> vehicles;

	public RelocationZone(final Id<RelocationZone> id, SimpleFeature polygon) {
		this.id = id;
		this.polygon = polygon;
		this.requests = new HashMap<Link, Integer>();
		this.vehicles = new ConcurrentHashMap<Link, CopyOnWriteArrayList<String>>();
	}

	@Override
	public Id<RelocationZone> getId() {
		return this.id;
	}

	public SimpleFeature getPolygon() {
		return this.polygon;
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
