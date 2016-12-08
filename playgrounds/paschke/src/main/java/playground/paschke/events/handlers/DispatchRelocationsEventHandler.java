package playground.paschke.events.handlers;

import org.matsim.core.events.handler.EventHandler;

import playground.paschke.events.DispatchRelocationsEvent;

public interface DispatchRelocationsEventHandler extends EventHandler {
	public void handleEvent(DispatchRelocationsEvent event);
}
