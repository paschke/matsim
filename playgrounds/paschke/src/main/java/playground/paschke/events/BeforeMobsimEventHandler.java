package playground.paschke.events;

import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.controler.events.BeforeMobsimEvent;

public interface BeforeMobsimEventHandler extends EventHandler {

	public void handleEvent (BeforeMobsimEvent event);
}
