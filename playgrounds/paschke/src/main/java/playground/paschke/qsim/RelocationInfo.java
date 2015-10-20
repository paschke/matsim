package playground.paschke.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class RelocationInfo {
	private String vehicleId;
	private Id<Link> startLinkId;
	private Id<Link> destinationLinkId;

	public RelocationInfo(String vehicleId, Id<Link> startLinkId, Id<Link> destinationLinkId) {
		this.vehicleId = vehicleId;
		this.startLinkId = startLinkId;
		this.destinationLinkId = destinationLinkId;
	}

	public String getVehicleId() {
		return this.vehicleId;
	}

	public Id<Link> getStartLinkId() {
		return this.startLinkId;
	}

	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId;
	}
}
