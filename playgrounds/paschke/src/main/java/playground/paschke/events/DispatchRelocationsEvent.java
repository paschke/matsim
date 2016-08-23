package playground.paschke.events;

import org.matsim.api.core.v01.events.Event;

import playground.paschke.qsim.RelocationZones;

public class DispatchRelocationsEvent extends Event {

	public static final String EVENT_TYPE = "Dispatch free-floating vehicle relocations";

	public RelocationZones relocationZones;

	public DispatchRelocationsEvent(double time, RelocationZones relocationZones) {
		super(time);

		this.relocationZones = relocationZones;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public RelocationZones getRelocationZones() {
		return this.relocationZones;
	}
}
