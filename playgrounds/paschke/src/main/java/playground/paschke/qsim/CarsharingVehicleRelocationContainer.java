package playground.paschke.qsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class CarsharingVehicleRelocationContainer {
	private Scenario scenario;

	private PointFeatureFactory pointFeatureFactory;

	public static final String ELEMENT_NAME = "carSharingRelocationZones";

	private RelocationAgentFactory relocationAgentFactory;

	private Map<String, List<RelocationZone>> relocationZones;

	private Map<String, List<Double>> relocationTimes;

	private Map<String, Map<Id<Person>, RelocationAgent>> relocationAgents;

	private Map<String, List<RelocationInfo>> relocations;

	private Map<String, Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>>> status = new HashMap<String, Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>>>();

	private Integer moduleEnableAfterIteration = null;

	public CarsharingVehicleRelocationContainer(Scenario sc) {
		this.scenario = sc;
		this.relocationAgentFactory = new RelocationAgentFactory(this.scenario);
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();

		this.relocationAgents = new HashMap<String, Map<Id<Person>, RelocationAgent>>();

		this.relocations = new HashMap<String, List<RelocationInfo>>();

		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		this.moduleEnableAfterIteration = confGroup.moduleEnableAfterIteration();
	}

	public void readRelocationZones() throws IOException {
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		RelocationZonesReader reader = new RelocationZonesReader();
		reader.readFile(confGroup.getRelocationZones());
		this.relocationZones = reader.getRelocationZones();
	}

	public final void readRelocationTimes() throws IOException {
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		RelocationTimesReader reader = new RelocationTimesReader();
		reader.readFile(confGroup.getRelocationTimes());
		this.relocationTimes = reader.getRelocationTimes();
	}

	public final void readRelocationAgents() throws IOException {
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		RelocationAgentsReader reader = new RelocationAgentsReader();
		reader.readFile(confGroup.getRelocationAgents());

		Network network = this.scenario.getNetwork();
		Map<String, Map<String, Map<String, Double>>> relocationAgentBases = reader.getRelocationAgentBases();

		Iterator<Entry<String, Map<String, Map<String, Double>>>> companiesIterator = relocationAgentBases.entrySet().iterator();
		while (companiesIterator.hasNext()) {
			Entry<String, Map<String, Map<String, Double>>> companyEntry = companiesIterator.next();
			String companyId = companyEntry.getKey();
			this.relocationAgents.put(companyId, new HashMap<Id<Person>, RelocationAgent>());
			Iterator<Entry<String, Map<String, Double>>> baseIterator = companyEntry.getValue().entrySet().iterator();

			while (baseIterator.hasNext()) {
				Entry<String, Map<String, Double>> baseEntry = baseIterator.next();
				String baseId = baseEntry.getKey();
				HashMap<String, Double> agentBaseData = (HashMap<String, Double>) baseEntry.getValue();

				Coord coord = new Coord(agentBaseData.get("x"), agentBaseData.get("y"));
				Link link = (Link) NetworkUtils.getNearestLinkExactly(network, coord);

				int counter = 0;
				while (counter < agentBaseData.get("number")) {
					Id<Person> id = Id.createPersonId("RelocationAgent" + "_" + companyId + "_" + baseId + "_"  + counter);
					RelocationAgent agent = this.relocationAgentFactory.createRelocationAgent(id, companyId, link.getId());
					//agent.setGuidance(new Guidance(this.routerProvider.get()));
					//agent.setMobsimTimer(this.qSim.getSimTimer());
					//agent.setCarsharingSupplyContainer(this.carsharingSupply);

					this.relocationAgents.get(companyId).put(id, agent);
					counter++;
				}
			}
		}
	}

	public Map<String, List<RelocationZone>> getRelocationZones() {
		return this.relocationZones;
	}

	public List<RelocationZone> getRelocationZones(String companyId) {
		if (this.getRelocationZones().keySet().contains(companyId)) {
			return this.getRelocationZones().get(companyId);
		}

		return null;
	}

	public Map<String, List<Double>> getRelocationTimes() {
		return this.relocationTimes;
	}

	public List<Double> getRelocationTimes(String companyId) {
		if (this.getRelocationTimes().keySet().contains(companyId)) {
			return this.getRelocationTimes().get(companyId);
		}

		return null;
	}

	public Map<String, Map<Id<Person>, RelocationAgent>> getRelocationAgents() {
		return this.relocationAgents;
	}

	public Map<Id<Person>, RelocationAgent> getRelocationAgents(String companyId) {
		if (this.getRelocationAgents().keySet().contains(companyId)) {
			return this.getRelocationAgents().get(companyId);
		}

		return null;
	}

	public Map<String, List<RelocationInfo>> getRelocations() {
		return this.relocations;
	}

	public List<RelocationInfo> getRelocations(String companyId) {
		if (this.getRelocations().keySet().contains(companyId)) {
			return this.getRelocations().get(companyId);
		}

		return null;
	}

	public void addRelocation(String companyId, RelocationInfo info) {
		if (this.getRelocations(companyId) == null) {
			this.getRelocations().put(companyId, new ArrayList<RelocationInfo>());
		}

		this.getRelocations(companyId).add(info);
	}

	public Integer moduleEnableAfterIteration() {
		return this.moduleEnableAfterIteration;
	}

	public RelocationZone getRelocationZone(String companyId, Coord coord) {
		if (this.getRelocationZones().keySet().contains(companyId)) {
			SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
			Point point = (Point) pointFeature.getAttribute("the_geom");

			for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
				MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

				if (polygon.contains(point)) {
					return relocationZone;
				}
			}
		}

		return null;
	}

	public void addExpectedRequests(String companyId, Link link) {
		this.addExpectedRequests(companyId, link, 1);
	}

	public void addExpectedRequests(String companyId, Link link, int numberOfRequests) {
		RelocationZone relocationZone = this.getRelocationZone(companyId, link.getCoord());

		if (null != relocationZone) {
			relocationZone.addExpectedRequests(link, numberOfRequests);
		}
	}

	public void addExpectedReturns(String companyId, Link link) {
		this.addExpectedReturns(companyId, link, 1);
	}

	public void addExpectedReturns(String companyId, Link link, int numberOfReturns) {
		RelocationZone relocationZone = this.getRelocationZone(companyId, link.getCoord());

		if (null != relocationZone) {
			relocationZone.addExpectedReturns(link, numberOfReturns);
		}
	}

	public void addVehicles(String companyId, Link link, ArrayList<String> IDs) {
		RelocationZone relocationZone = this.getRelocationZone(companyId, link.getCoord());

		if (null != relocationZone) {
			relocationZone.addVehicles(link, IDs);
		}
	}

	public Map<String, Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>>> getStatus() {
		return this.status;
	}

	public void resetRelocationZones() {
		for (Entry<String, List<RelocationZone>> entry : this.getRelocationZones().entrySet()) {
			for (RelocationZone r : entry.getValue()) {
				r.reset();
			}
		}
	}

	public void resetRelocationZones(String companyId) {
		if (this.getRelocationZones().keySet().contains(companyId)) {
			for (RelocationZone r: this.getRelocationZones().get(companyId)) {
				r.reset();
			}
		}
	}

	public void resetRelocations() {
		this.relocations = new HashMap<String, List<RelocationInfo>>();
	}

	public void reset() {
		this.resetRelocationZones();
		this.resetRelocations();
	}

	public void storeStatus(String companyId, double now) {
		Map<Id<RelocationZone>, Map<String, Integer>> relocationZonesStatus = new HashMap<Id<RelocationZone>, Map<String, Integer>>();

		for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
			Map<String, Integer> zoneStatus = new HashMap<String, Integer>();
			zoneStatus.put("vehicles", relocationZone.getNumberOfVehicles());
			zoneStatus.put("requests", relocationZone.getNumberOfExpectedRequests());
			zoneStatus.put("returns", relocationZone.getNumberOfExpectedReturns());
			relocationZonesStatus.put(relocationZone.getId(), zoneStatus);
		}

		if (this.status.get(companyId) == null) {
			this.status.put(companyId, new HashMap<Double, Map<Id<RelocationZone>, Map<String, Integer>>>());
		}

		this.status.get(companyId).put(now, relocationZonesStatus);
	}

	protected Collection<RelocationZone> getAdjacentZones(String companyId, RelocationZone currentZone) {
		Collection<RelocationZone> relocationZones = new ArrayList<RelocationZone>(this.getRelocationZones().get(companyId));
		Collection<RelocationZone> adjacentZones = new ArrayList<RelocationZone>();
		relocationZones.remove(currentZone);
		MultiPolygon currentPolygon = (MultiPolygon) currentZone.getPolygon().getAttribute("the_geom");

		for (RelocationZone relocationZone : relocationZones) {
			MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

			if (polygon.touches(currentPolygon)) {
				adjacentZones.add(relocationZone);
			}
		}

		return adjacentZones;
	}
}
