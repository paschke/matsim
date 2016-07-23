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

	private Map<Link, Integer> expectedRequests;

	private Map<Link, Integer> expectedReturns;

	private Map<Link, CopyOnWriteArrayList<String>> vehicles;

	public RelocationZone(final Id<RelocationZone> id, SimpleFeature polygon) {
		this.id = id;
		this.polygon = polygon;
		this.expectedRequests = new HashMap<Link, Integer>();
		this.expectedReturns = new HashMap<Link, Integer>();
		this.vehicles = new ConcurrentHashMap<Link, CopyOnWriteArrayList<String>>();
	}

	@Override
	public Id<RelocationZone> getId() {
		return this.id;
	}

	public SimpleFeature getPolygon() {
		return this.polygon;
	}

	public Map<Link, Integer> getExpectedRequests() {
		return this.expectedRequests;
	}

	public Map<Link, Integer> getExpectedReturns() {
		return this.expectedReturns;
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

	public int getNumberOfExpectedRequests() {
		return getNumberOfExpectedRequests(1.0);
	}

	public int getNumberOfExpectedRequests(double safetyFactor) {
		int number = 0;

		for (Integer linkRequests : this.expectedRequests.values()) {
			number += linkRequests.intValue();
		}

		return (int) Math.ceil(number * safetyFactor);
	}

	public int getNumberOfExpectedReturns() {
		int number = 0;

		for (Integer linkReturns : this.expectedReturns.values()) {
			number += linkReturns.intValue();
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
		return this.getNumberOfSurplusVehicles(1.0);
	}

	public int getNumberOfSurplusVehicles(double safetyFactor) {
		return this.getNumberOfVehicles() + this.getNumberOfExpectedReturns() - this.getNumberOfExpectedRequests(safetyFactor);
	}

	public void addExpectedRequests(Link link, int numberOfRequests) {
		if (this.expectedRequests.containsKey(link)) {
			int previousNumberOfRequests = this.expectedRequests.get(link);
			numberOfRequests += previousNumberOfRequests;
		}

		expectedRequests.put(link, numberOfRequests);
	}

	public void addExpectedReturns(Link link, int numberOfReturns) {
		if (this.expectedReturns.containsKey(link)) {
			int previousNumberOfReturns = this.expectedReturns.get(link);
			numberOfReturns += previousNumberOfReturns;
		}

		expectedReturns.put(link, numberOfReturns);
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
		this.expectedRequests.clear();
		this.expectedReturns.clear();
		this.vehicles.clear();
	}
}
