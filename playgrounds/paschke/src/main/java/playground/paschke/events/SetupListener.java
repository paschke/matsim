package playground.paschke.events;

import java.io.IOException;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Inject;

import playground.paschke.qsim.CarsharingVehicleRelocationContainer;

public class SetupListener implements StartupListener {
	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation; 

	@Override
	public void notifyStartup(StartupEvent event) {
		try {
			this.carsharingVehicleRelocation.readRelocationZones();
			this.carsharingVehicleRelocation.readRelocationTimes();
			this.carsharingVehicleRelocation.readRelocationAgents();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
