package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;

public class CarSharingDemandTracker implements IterationStartsListener, IterationEndsListener {
	@Inject private DemandHandler demandHandler;

	@Inject private CarsharingSupplyInterface carsharingSupplyContainer;

	private Map<Integer, Map<Id<Person>, AgentRentals>> agentRentalsMapHistory = new HashMap<Integer, Map<Id<Person>, AgentRentals>>();

	private int iteration;

	public ArrayList<RentalInfo> getRentalsInInterval(String carsharingType, String companyId, double startTime, double endTime) {
		ArrayList<RentalInfo> rentalsInInterval = new ArrayList<RentalInfo>();

		if (this.agentRentalsMapHistory.keySet().contains(this.iteration - 1)) {
			for (AgentRentals agentRentals : this.agentRentalsMapHistory.get(iteration - 1).values()) {
				for (RentalInfo rentalInfo : agentRentals.getArr()) {
					CSVehicle vehicle = this.carsharingSupplyContainer.getVehicleWithId(rentalInfo.getVehId().toString());

					if (
							(vehicle.getCompanyId().equals(companyId)) &&
							(rentalInfo.getCarsharingType().equals(carsharingType)) &&
							(rentalInfo.getAccessStartTime() > startTime) &&
							(rentalInfo.getAccessStartTime() < endTime)
					) {
						rentalsInInterval.add(rentalInfo);
					}
				}
			}
		}

		return rentalsInInterval;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.agentRentalsMapHistory.put(event.getIteration(), this.demandHandler.getAgentRentalsMap());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iteration = event.getIteration();
	}
}
