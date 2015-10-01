package playground.paschke.events;

import org.matsim.api.core.v01.events.Event;

public class RelocationAgentsDispatchEvent extends Event {
	public static final String EVENT_TYPE = "dispatch relocation agents";

	public RelocationAgentsDispatchEvent(double time) {
		super(time);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
