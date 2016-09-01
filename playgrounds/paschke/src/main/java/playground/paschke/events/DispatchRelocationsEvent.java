package playground.paschke.events;

import org.matsim.api.core.v01.events.Event;

import playground.paschke.qsim.CarsharingVehicleRelocation;

public class DispatchRelocationsEvent extends Event {

	public static final String EVENT_TYPE = "Dispatch free-floating vehicle relocations";

	public CarsharingVehicleRelocation relocationZones;

	public DispatchRelocationsEvent(double time, CarsharingVehicleRelocation relocationZones) {
		super(time);

		this.relocationZones = relocationZones;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public CarsharingVehicleRelocation getRelocationZones() {
		return this.relocationZones;
	}
}
