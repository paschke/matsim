package playground.paschke.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;

public class CarSharingDemandTracker implements IterationStartsListener {
	private Controler controler;

	private ArrayList<RentalInfoFF> rentals;

	private HashMap<Double, Map<Coord, List<Integer>>> states = new HashMap<Double, Map<Coord, List<Integer>>>();

	public CarSharingDemandTracker(Controler controler) {
		this.controler = controler;
	}

	public ArrayList<RentalInfoFF> getRentalsInInterval(double startTime, double endTime) {
		ArrayList<RentalInfoFF> rentalsInInterval = new ArrayList<RentalInfoFF>();

		for (RentalInfoFF info: this.rentals) {
			if ((info.startTime > startTime) && (info.startTime < endTime)) {
				rentalsInInterval.add(info);
			}
		}

		return rentalsInInterval;
	}

	public Map<Double, Map<Coord, List<Integer>>> getStates() {
		return this.states;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int iteration = event.getIteration();
		this.rentals = new ArrayList<RentalInfoFF>();

	    try {
			BufferedReader reader = IOUtils.getBufferedReader(this.controler.getControlerIO().getIterationFilename((iteration - 1), "FF_CS"));
			String s;

			s = reader.readLine();
		    s = reader.readLine();
		    while(s != null) {
		    	String[] arr = s.split("\t", -1);

		    	RentalInfoFF info = new RentalInfoFF();
		    	info.personId = Id.createPersonId(arr[0]);
		    	info.startTime = Double.parseDouble(arr[1]);
		    	info.endTime = Double.parseDouble(arr[2]);
		    	info.accessLinkId = Id.createLinkId(arr[3]);
		    	info.startLinkId = Id.createLinkId(arr[4]);
		    	info.endLinkId = Id.createLinkId(arr[5]);
		    	info.distance = Double.parseDouble(arr[6]);
		    	info.accessStartTime = Double.parseDouble(arr[7]);
		    	info.accessEndTime = Double.parseDouble(arr[8]);
		    	info.egressStartTime = Double.parseDouble(arr[9]);
		    	info.egressEndTime = Double.parseDouble(arr[10]);
		    	info.vehId = Id.createVehicleId(arr[11]);

		    	rentals.add(info);
		    	s = reader.readLine();
		    }	
		} catch (IOException e) {
			// do nothing
		} catch (UncheckedIOException e) {
			// do nothing
		}
	}

	public class RentalInfoFF {
		public Id<Person> personId = null;
		public double startTime = 0.0;
		public double endTime = 0.0;
		public Id<Link> accessLinkId = null;
		public Id<Link> startLinkId = null;
		public Id<Link> endLinkId = null;
		public double distance = 0.0;
		public double accessStartTime = 0.0;
		public double accessEndTime = 0.0;
		public double egressStartTime = 0.0;
		public double egressEndTime = 0.0;
		public Id<Vehicle> vehId = null;

		public String toString() {
			return personId + "	" +
					Double.toString(startTime) + "	" +
					Double.toString(endTime) + "	" +
					accessLinkId.toString() + "	" +
					startLinkId.toString() + "	" +
					endLinkId.toString()+ "	" +
					Double.toString(distance) + "	" +
					Double.toString(accessStartTime) + "	" +
					Double.toString(accessEndTime) + "	" +
					Double.toString(egressStartTime) + "	" +
					Double.toString(egressEndTime) + "	" +
					vehId;
		}
	}
}
