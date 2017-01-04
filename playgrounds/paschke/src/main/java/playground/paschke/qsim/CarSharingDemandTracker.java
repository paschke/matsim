package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
import org.matsim.matrices.Matrices;

import com.google.inject.Inject;

import playground.paschke.events.handlers.DemandDistributionHandler;

public class CarSharingDemandTracker implements IterationStartsListener, IterationEndsListener {
	@Inject private DemandHandler demandHandler;

	@Inject private DemandDistributionHandler demandDistributionHandler;

	@Inject private CarsharingSupplyInterface carsharingSupplyContainer;

	private Map<Integer, Map<Id<Person>, AgentRentals>> agentRentalsMapHistory = new HashMap<Integer, Map<Id<Person>, AgentRentals>>();

	private TreeMap<Integer,Map<String,Map<Double,Matrices>>> ODMatricesHistory = new TreeMap<Integer, Map<String, Map<Double, Matrices>>>();

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

		this.ODMatricesHistory.put(event.getIteration(), this.demandDistributionHandler.getODMatrices());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iteration = event.getIteration();
	}

	public Map<String, Map<Double, Matrices>> getODMatrices(int iteration) {
		if (this.ODMatricesHistory.keySet().contains(iteration)) {
			return this.ODMatricesHistory.get(iteration);
		}

		return null;
	}

	public Map<Double, Matrices> getODMatrices(int iteration, String companyId) {
		Map<String, Map<Double, Matrices>> ODMatrices = this.getODMatrices(iteration);

		if ((null != ODMatrices) && (ODMatrices.keySet().contains(companyId))) {
			return ODMatrices.get(companyId);
		}

		return null;
	}

	public Matrices getODMatrices(int iteration, String companyId, Double time) {
		Map<Double, Matrices> ODMatrices = this.getODMatrices(iteration, companyId);

		if ((null != ODMatrices) && (ODMatrices.keySet().contains(time))) {
			return ODMatrices.get(time);
		}

		return null;
	}
}
