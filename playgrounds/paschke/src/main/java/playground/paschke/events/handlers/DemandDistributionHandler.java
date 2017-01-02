package playground.paschke.events.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.NoVehicleCarSharingEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;

import com.google.inject.Inject;

import playground.paschke.qsim.CarsharingVehicleRelocationContainer;
import playground.paschke.qsim.RelocationZone;

public class DemandDistributionHandler implements StartRentalEventHandler, NoVehicleCarSharingEventHandler {

	@Inject Scenario scenario;

	@Inject CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	Map<String, Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>> ODMatrices = new HashMap<String, Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>>();

	@Override
	public void reset(int iteration) {
		this.ODMatrices = new HashMap<String, Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>>();
	}

	public void reset(String companyId, double time) {
		if (!this.ODMatrices.keySet().contains(companyId)) {
			this.ODMatrices.put(companyId, new TreeMap<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>());
		}

		this.ODMatrices.get(companyId).put(time, new HashMap<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>());
		this.ODMatrices.get(companyId).get(time).put("rentals", new HashMap<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>());
		this.ODMatrices.get(companyId).get(time).put("no_vehicle", new HashMap<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>());
	}

	@Override
	public void handleEvent(StartRentalEvent event) {
		Network network = this.scenario.getNetwork();

		Link originLink = network.getLinks().get(event.getOriginLinkId());
		Link destinationLink = network.getLinks().get(event.getDestinationLinkId());

		String companyId = event.getCompanyId();

		RelocationZone originZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, originLink.getCoord());
		RelocationZone destinationZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, destinationLink.getCoord());

		if ((originZone != null) && (destinationZone != null)) {
			this.addODRelation(companyId, "rentals", originZone, destinationZone);
		}
	}

	@Override
	public void handleEvent(NoVehicleCarSharingEvent event) {
		Network network = this.scenario.getNetwork();

		Link originLink = network.getLinks().get(event.getOriginLinkId());
		Link destinationLink = network.getLinks().get(event.getDestinationLinkId());

		String companyId = event.getCompanyId();

		RelocationZone originZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, originLink.getCoord());
		RelocationZone destinationZone = this.carsharingVehicleRelocation.getRelocationZone(companyId, destinationLink.getCoord());

		if ((originZone != null) && (destinationZone != null)) {
			this.addODRelation(companyId, "no_vehicle", originZone, destinationZone);
		}
	}

	public Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>> getODMatrix(String companyId, String eventType, Double time) {
		Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>> companyODMatrices = this.getODMatrices(companyId);

		if ((null != companyODMatrices) && (companyODMatrices.keySet().contains(time)) && (companyODMatrices.get(time).keySet().contains(eventType))) {
			return companyODMatrices.get(time).get(eventType);
		}

		return null;
	}

	public Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>> getODMatrices(String companyId) {
		Map<String, Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>> ODMatrices = this.getODMatrices();

		if (ODMatrices.keySet().contains(companyId)) {
			return this.ODMatrices.get(companyId);
		}

		return null;
	}

	public Map<String, Map<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>> getODMatrices() {
		return this.ODMatrices;
	}

	protected void addODRelation(String companyId, String eventType, RelocationZone originZone, RelocationZone destinationZone) {
		if (!this.ODMatrices.keySet().contains(companyId)) {
			this.ODMatrices.put(companyId, new TreeMap<Double, Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>>());
		}

		if (this.ODMatrices.get(companyId).isEmpty()) {
			this.ODMatrices.get(companyId).put(new Double(0), new HashMap<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>>());
			this.ODMatrices.get(companyId).get(new Double(0)).put("rentals", new HashMap<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>());
			this.ODMatrices.get(companyId).get(new Double(0)).put("no_vehicle", new HashMap<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>());
		}

		SortedSet<Double> keySet = (SortedSet<Double>) this.ODMatrices.get(companyId).keySet();
		Map<String, Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>>> companyODMatrices = this.ODMatrices.get(companyId).get(keySet.last());
		Map<Id<RelocationZone>, Map<Id<RelocationZone>, Integer>> ODMatrix = companyODMatrices.get(eventType);

		if (!ODMatrix.keySet().contains(originZone.getId())) {
			Map<Id<RelocationZone>, Integer> destinations = new HashMap<Id<RelocationZone>, Integer>();
			ODMatrix.put(originZone.getId(), destinations);
		}

		if (!ODMatrix.get(originZone.getId()).keySet().contains(destinationZone.getId())) {
			ODMatrix.get(originZone.getId()).put(destinationZone.getId(), new Integer(1));
		} else {
			Integer oldCount = ODMatrix.get(originZone.getId()).get(destinationZone.getId());
			ODMatrix.get(originZone.getId()).put(destinationZone.getId(), new Integer(oldCount.intValue() + 1));
		}
	}
}
