package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class RelocationZones {
	private PointFeatureFactory pointFeatureFactory;

	private static final Logger log = Logger.getLogger("dummy");

	public static final String ELEMENT_NAME = "carSharingRelocationZones";

	private ArrayList<RelocationZone> relocationZones;

	private ArrayList<RelocationInfo> relocations;

	private Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>> status = new HashMap<Double, Map<Id<RelocationZone>, Map<String, Integer>>>();

	public RelocationZones() {
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();

		this.relocationZones = new ArrayList<RelocationZone>();
		this.relocations = new ArrayList<RelocationInfo>();
	}

	public void add(RelocationZone relocationZone) {
		// TODO: add checks to avoid overlapping zones
		this.relocationZones.add(relocationZone);
	}

	public List<RelocationZone> getRelocationZones() {
		return this.relocationZones;
	}

	public List<RelocationInfo> getRelocations() {
		return this.relocations;
	}

	public void addExpectedRequests(Link link, int numberOfRequests) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		for (RelocationZone relocationZone : this.getRelocationZones()) {
			MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

			if (polygon.contains(point)) {
				relocationZone.addExpectedRequests(link, numberOfRequests);

				break;
			}
		}
	}

	public void addExpectedReturns(Link link, int numberOfReturns) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		for (RelocationZone relocationZone : this.getRelocationZones()) {
			MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

			if (polygon.contains(point)) {
				relocationZone.addExpectedReturns(link, numberOfReturns);

				break;
			}
		}
	}

	public void addVehicles(Link link, ArrayList<String> IDs) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		for (RelocationZone relocationZone : this.getRelocationZones()) {
			MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

			if (polygon.contains(point)) {
				relocationZone.addVehicles(link, IDs);

				break;
			}
		}
	}

	public Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>> getStatus() {
		return this.status;
	}

	public void resetRelocationZones() {
		for (RelocationZone r : this.getRelocationZones()) {
			r.reset();
		}
	}

	public void reset() {
		this.resetRelocationZones();
		this.relocations = new ArrayList<RelocationInfo>();
	}

	public ArrayList<RelocationInfo> calculateRelocations(double now, double then) {
		ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

		Collections.sort(this.getRelocationZones(), new Comparator<RelocationZone>() {

			@Override
			public int compare(RelocationZone o1, RelocationZone o2) {
				if (o1.getNumberOfSurplusVehicles() < o2.getNumberOfSurplusVehicles()) {
					return -1;
				} else if (o1.getNumberOfSurplusVehicles() > o2.getNumberOfSurplusVehicles()) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		int evenIndex = 0;
		Iterator<RelocationZone> iterator = this.getRelocationZones().iterator();
		while (iterator.hasNext()) {
			RelocationZone nextZone = (RelocationZone) iterator.next();

			if (nextZone.getNumberOfSurplusVehicles(1.1) <= 0) {
				evenIndex++;
			} else {
				break;
			}
		}

		List<RelocationZone> surplusZones = this.getRelocationZones().subList(evenIndex, (this.getRelocationZones().size() - 1));
		Collections.reverse(surplusZones);

		iterator = this.getRelocationZones().iterator();
		while (iterator.hasNext()) {
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
								CopyOnWriteArrayList<String> vehicleIds = surplusZone.getVehicles().get(fromLink);

								if (vehicleIds.size() == 1) {
									log.info("found one vehicle at link " + fromLink.getId());
									vehicleId = vehicleIds.get(0);
									surplusZone.getVehicles().remove(fromLink);
								} else {
									log.info("found multiple vehicles at link " + fromLink.getId());
									vehicleId = vehicleIds.remove(0);
									surplusZone.getVehicles().put(fromLink, vehicleIds);
								}

								break;
							}
						}
					}

					if ((fromLink != null) && (vehicleId != null)) {
						relocations.add(new RelocationInfo(Time.writeTime(now) + " - " + Time.writeTime(then), surplusZoneId, nextZone.getId().toString(), vehicleId, fromLink.getId(), toLink.getId()));
					}
				}
			} else {
				break;
			}
		}

		this.relocations.addAll(relocations);

		return relocations;
	}

	public void storeStatus(double now) {
		Map<Id<RelocationZone>, Map<String, Integer>> relocationZonesStatus = new HashMap<Id<RelocationZone>, Map<String, Integer>>();

		for (RelocationZone relocationZone : this.getRelocationZones()) {
			Map<String, Integer> zoneStatus = new HashMap<String, Integer>();
			zoneStatus.put("vehicles", relocationZone.getNumberOfVehicles());
			zoneStatus.put("requests", relocationZone.getNumberOfExpectedRequests());
			zoneStatus.put("returns", relocationZone.getNumberOfExpectedReturns());
			relocationZonesStatus.put(relocationZone.getId(), zoneStatus);
		}

		this.status.put(now, relocationZonesStatus);
	}

	protected Collection<RelocationZone> getAdjacentZones(RelocationZone currentZone) {
		Collection<RelocationZone> relocationZones = new ArrayList<RelocationZone>(this.getRelocationZones());
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
