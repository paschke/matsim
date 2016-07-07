package playground.paschke.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class RelocationInfo {
	private String timeSlot;
	private String vehicleId;
	private Id<Link> startLinkId;
	private Id<Link> endLinkId;
	private String startZoneId;
	private String endZoneId;
	private double startTime;
	private double endTime;
	private Id<Person> agentId;

	public RelocationInfo(String timeSlot, String startZoneId, String endZoneId, String vehicleId, Id<Link> startLinkId, Id<Link> endLinkId) {
		this.timeSlot		= timeSlot;
		this.startZoneId	= startZoneId;
		this.endZoneId		= endZoneId;
		this.vehicleId 		= vehicleId;
		this.startLinkId 	= startLinkId;
		this.endLinkId 		= endLinkId;
	}

	public String getVehicleId() {
		return this.vehicleId;
	}

	public Id<Link> getStartLinkId() {
		return this.startLinkId;
	}

	public Id<Link> getEndLinkId() {
		return this.endLinkId;
	}

	public String getStartZoneId() {
		return this.startZoneId;
	}

	public String getEndZoneId() {
		return this.endZoneId;
	}

	public String getTimeSlot() {
		return this.timeSlot;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public double getEndTime() {
		return this.endTime;
	}

	public void setAgentId(Id<Person> id) {
		this.agentId = id;
	}

	public Id<Person> getAgentId() {
		return this.agentId;
	}

	public String toString() {
		return this.getTimeSlot() + "	" +
				this.getStartZoneId() + "	" +
				this.getEndZoneId() + "	" +
				Double.toString(this.getStartTime()) + "	" +
				Double.toString(this.getEndTime()) + "	" +
				this.getStartLinkId().toString() + "	" +
				this.getEndLinkId().toString() + "	" +
				this.getVehicleId() + "	" +
				this.getAgentId().toString();
	}
}
