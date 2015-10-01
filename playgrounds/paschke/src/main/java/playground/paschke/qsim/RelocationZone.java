package playground.paschke.qsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.CoordImpl;

public class RelocationZone {
	private Coord coord;
	private int numberOfVehicles = 0;
	private int numberOfRequests = 0;

	public RelocationZone(Coord coord) {
		this.coord = coord;
	}

	public Coord getCoord() {
		return coord;
	}

	public int getNumberOfVehicles() {
		return numberOfVehicles;
	}

	public void setNumberOfVehicles(int numberOfVehicles) {
		this.numberOfVehicles = numberOfVehicles;
	}

	public void increaseNumberOfVehicles(int numberOfVehicles) {
		this.numberOfVehicles += numberOfVehicles;
	}

	public int getNumberOfRequests() {
		return numberOfRequests;
	}

	public void setNumberOfRequests(int numberOfRequests) {
		this.numberOfRequests = numberOfRequests;
	}

	public void increaseNumberOfRequests(int numberOfRequests) {
		this.numberOfRequests += numberOfRequests;
	}

	public void reset() {
		this.setNumberOfVehicles(0);
		this.setNumberOfRequests(0);
	}
}
