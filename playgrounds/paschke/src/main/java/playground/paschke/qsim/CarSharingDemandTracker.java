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

public class CarSharingDemandTracker implements IterationStartsListener {
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
