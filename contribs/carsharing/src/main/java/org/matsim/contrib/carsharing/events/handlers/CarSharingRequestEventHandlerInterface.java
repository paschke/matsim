package org.matsim.contrib.carsharing.events.handlers;

import org.matsim.contrib.carsharing.events.CarSharingRequestEvent;
import org.matsim.core.events.handler.EventHandler;

public interface CarSharingRequestEventHandlerInterface extends EventHandler{
	public void handleEvent (CarSharingRequestEvent event);
}
