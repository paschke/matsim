package playground.paschke.qsim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.control.listeners.FFEventsHandler.RentalInfoFF;
import org.matsim.contrib.carsharing.control.listeners.TwoWayEventsHandler.RentalInfo;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;

public class CarSharingDemandTracker implements IterationStartsListener, IterationEndsListener {
	private Controler controler;

	private ArrayList<RequestInfo> requests;

	private HashMap<Double, Map<Coord, List<Integer>>> states = new HashMap<Double, Map<Coord, List<Integer>>>();

	public CarSharingDemandTracker(Controler controler) {
		this.controler = controler;
	}

	public ArrayList<RequestInfo> getCarSharingRequestsInInterval(double startTime, double endTime) {
		ArrayList<RequestInfo> requestsInInterval = new ArrayList<RequestInfo>();

		for (RequestInfo info: this.requests) {
			if ((info.getStartTime() > startTime) && (info.getStartTime() < endTime)) {
				requestsInInterval.add(info);
			}
		}

		return requestsInInterval;
	}

	public Map<Double, Map<Coord, List<Integer>>> getStates() {
		return this.states;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int iteration = event.getIteration();
		this.requests = new ArrayList<RequestInfo>();

	    try {
			BufferedReader reader = IOUtils.getBufferedReader(this.controler.getControlerIO().getIterationFilename((iteration - 1), "FF_CS"));
			String s;

			s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	String[] arr = s.split("\t", -1);

		    	RequestInfo info = new RequestInfo(Id.createPersonId(arr[0]), Double.parseDouble(arr[1]), Id.createLinkId(arr[3]));

		    	requests.add(info);
		    	s = reader.readLine();
		    }	
		} catch (IOException e) {
			// do nothing
		} catch (UncheckedIOException e) {
			// do nothing
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		final BufferedWriter relocationZones = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "relocation_zones"));
		try {
			relocationZones.write("time	coordX	coordY	available	requested");
			relocationZones.newLine();

			Iterator<Entry<Double, Map<Coord, List<Integer>>>> iterator = this.getStates().entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Double, Map<Coord, List<Integer>>> entry = iterator.next();
				Double time = entry.getKey();

				Iterator<Entry<Coord, List<Integer>>> subIterator = entry.getValue().entrySet().iterator();
				while (subIterator.hasNext()) {
					Entry<Coord, List<Integer>> subEntry = subIterator.next();
					Coord coord = subEntry.getKey();

					relocationZones.write(
						time.toString() + "	" + coord.getX() + "	" + coord.getY() + "	" + subEntry.getValue().get(0) + "	" + subEntry.getValue().get(1)
					);
					relocationZones.newLine();
				}
			}

			relocationZones.flush();
			relocationZones.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class RequestInfo {
		private Id<Person> personId = null;
		private double startTime = 0.0;
		private Id<Link> accessLinkId = null;

		public RequestInfo(Id<Person> personId, double startTime, Id<Link> accessLinkId) {
			this.personId = personId;
			this.startTime = startTime;
			this.accessLinkId = accessLinkId;
		}

		public double getStartTime() {
			return this.startTime;
		}

		public Id<Link> getAccessLinkId() {
			return this.accessLinkId;
		}
	}
}
