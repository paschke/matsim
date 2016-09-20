package playground.paschke.qsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class CarsharingVehicleRelocation {
	private Scenario scenario;

	private PointFeatureFactory pointFeatureFactory;

	private static final Logger log = Logger.getLogger("dummy");

	public static final String ELEMENT_NAME = "carSharingRelocationZones";

	private Map<String, List<RelocationZone>> relocationZones;

	private Map<String, List<Double>> relocationTimes;

	private Map<String, Map<String, Map<String, Double>>> relocationAgentBases;

	private ArrayList<RelocationInfo> relocations;

	private Map<String, Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>>> status = new HashMap<String, Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>>>();

	private boolean useRelocation = false;

	public CarsharingVehicleRelocation(Scenario sc) {
		this.scenario = sc;
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();

		this.relocations = new ArrayList<RelocationInfo>();

		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
				this.scenario.getConfig().getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );

		this.useRelocation = confGroup.useRelocation();
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
		this.relocationAgentBases = reader.getRelocationAgentBases();
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

	public Map<String, Map<String, Map<String, Double>>> getRelocationAgentBases() {
		return this.relocationAgentBases;
	}

	public Map<String, Map<String, Double>> getRelocationAgentBases(String companyId) {
		if (this.getRelocationAgentBases().keySet().contains(companyId)) {
			return this.getRelocationAgentBases().get(companyId);
		}

		return null;
	}

	public List<RelocationInfo> getRelocations() {
		return this.relocations;
	}

	public boolean useRelocation() {
		return this.useRelocation;
	}

	public void addExpectedRequests(String companyId, Link link) {
		this.addExpectedRequests(companyId, link, 1);
	}

	public void addExpectedRequests(String companyId, Link link, int numberOfRequests) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		if (this.getRelocationZones().keySet().contains(companyId)) {
			for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
				MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

				if (polygon.contains(point)) {
					relocationZone.addExpectedRequests(link, numberOfRequests);

					break;
				}
			}
		}
	}

	public void addExpectedReturns(String companyId, Link link) {
		this.addExpectedReturns(companyId, link, 1);
	}

	public void addExpectedReturns(String companyId, Link link, int numberOfReturns) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		if (this.getRelocationZones().keySet().contains(companyId)) {
			for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
				MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

				if (polygon.contains(point)) {
					relocationZone.addExpectedReturns(link, numberOfReturns);

					break;
				}
			}
		}
	}

	public void addVehicles(String companyId, Link link, ArrayList<String> IDs) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		if (this.getRelocationZones().keySet().contains(companyId)) {
			for (RelocationZone relocationZone : this.getRelocationZones().get(companyId)) {
				MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

				if (polygon.contains(point)) {
					relocationZone.addVehicles(link, IDs);

					break;
				}
			}
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

	public void reset() {
		this.resetRelocationZones();
		this.relocations = new ArrayList<RelocationInfo>();
	}

	public ArrayList<RelocationInfo> calculateRelocations(String companyId, String carsharingType, double now, double then) {
		List<RelocationZone> relocationZones = this.getRelocationZones(companyId);
		ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

		Collections.sort(relocationZones, new Comparator<RelocationZone>() {

			@Override
			public int compare(RelocationZone o1, RelocationZone o2) {
				if (o1.getNumberOfSurplusVehicles() < o2.getNumberOfSurplusVehicles()) {
					return -1;
				} else if (o1.getNumberOfSurplusVehicles() > o2.getNumberOfSurplusVehicles()) {
					return 1;
				} else {
					return o1.getId().toString().compareTo(o2.getId().toString());
				}
			}
		});

		int evenIndex = 0;
		for (ListIterator<RelocationZone> iterator = relocationZones.listIterator(); iterator.hasNext();) {
			RelocationZone nextZone = iterator.next();

			if (nextZone.getNumberOfSurplusVehicles(1.1) <= 0) {
				evenIndex = iterator.previousIndex();
			} else {
				break;
			}
		}

		List<RelocationZone> surplusZones = relocationZones.subList(evenIndex, (relocationZones.size() - 1));
		Collections.reverse(surplusZones);

		for (ListIterator<RelocationZone> iterator = relocationZones.listIterator(); iterator.hasNext();) {
			RelocationZone nextZone = (RelocationZone) iterator.next();

			if (nextZone.getNumberOfSurplusVehicles() < -1) {
				log.info("relocationZone " + nextZone.getId().toString() + " with " + nextZone.getNumberOfSurplusVehicles() + " surplus vehicles");

				for (int i = 0; i < Math.abs(nextZone.getNumberOfSurplusVehicles()); i++) {
					log.info("counting down surplus vehicles: " + i);
					Link fromLink = null;
					Link toLink = (Link) ((Set<Link>) nextZone.getExpectedRequests().keySet()).iterator().next();
					String surplusZoneId = null;
					String vehicleId = null;

					Iterator<RelocationZone> surplusZonesIterator = surplusZones.iterator();
					while (surplusZonesIterator.hasNext()) {
						RelocationZone surplusZone = surplusZonesIterator.next();

						if (surplusZone.getNumberOfSurplusVehicles(1.1) > 0) {
							surplusZoneId = surplusZone.getId().toString();
							Iterator<Link> links = surplusZone.getVehicles().keySet().iterator();
							if (links.hasNext()) {
								fromLink = links.next();
								ArrayList<String> vehicleIds = surplusZone.getVehicles().get(fromLink);
								vehicleId = vehicleIds.get(0);
								surplusZone.removeVehicles(fromLink, new ArrayList<String>(Arrays.asList(new String[]{vehicleId})));

								break;
							}
						}
					}

					if ((fromLink != null) && (vehicleId != null)) {
						relocations.add(new RelocationInfo(Time.writeTime(now) + " - " + Time.writeTime(then), companyId, vehicleId, carsharingType, fromLink.getId(), toLink.getId(), surplusZoneId, nextZone.getId().toString()));
					}
				}
			} else {
				break;
			}
		}

		this.relocations.addAll(relocations);

		return relocations;
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
