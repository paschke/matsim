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
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class RelocationZones {
	private PointFeatureFactory pointFeatureFactory;

	private static final Logger log = Logger.getLogger("dummy");

	public static final String ELEMENT_NAME = "carSharingRelocationZones";

	private ArrayList<RelocationZone> relocationZones = new ArrayList<RelocationZone>();

	private Map<Double, Map<Id<RelocationZone>, Map<String, Integer>>> status = new HashMap<Double, Map<Id<RelocationZone>, Map<String, Integer>>>();

	public RelocationZones() {
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();
	}

	public void add(RelocationZone relocationZone) {
		// TODO: add checks to avoid overlapping zones
		this.relocationZones.add(relocationZone);
	}

	public List<RelocationZone> getRelocationZones() {
		return this.relocationZones;
	}

	public void addRequests(Link link, int numberOfRequests) {
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(link.getCoord(), new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		for (RelocationZone relocationZone : this.getRelocationZones()) {
			MultiPolygon polygon = (MultiPolygon) relocationZone.getPolygon().getAttribute("the_geom");

			if (polygon.contains(point)) {
				relocationZone.addRequests(link, numberOfRequests);

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

	public void reset() {
		for (RelocationZone r : this.getRelocationZones()) {
			r.reset();
		}
	}

	public ArrayList<RelocationInfo> getRelocations() {
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

		Iterator<RelocationZone> iterator = this.getRelocationZones().iterator();
		while (iterator.hasNext()) {
			RelocationZone nextZone = (RelocationZone) iterator.next();

			if (nextZone.getNumberOfSurplusVehicles() < 0) {
				log.info("relocationZone " + nextZone.getId().toString() + " with " + nextZone.getNumberOfSurplusVehicles() + " surplus vehicles");
				Collection<RelocationZone> adjacentZones = this.getAdjacentZones(nextZone);

				for (int i = 0; i < Math.abs(nextZone.getNumberOfSurplusVehicles()); i++) {
					log.info("counting down surplus vehicles: " + i);
					Link fromLink = null;
					Link toLink = (Link) ((Set<Link>) nextZone.getRequests().keySet()).iterator().next();
					String vehicleId = null;

					Iterator<RelocationZone> adjacentIterator = adjacentZones.iterator();
					while (adjacentIterator.hasNext()) {
						RelocationZone adjacentZone = (RelocationZone) adjacentIterator.next();
						log.info("adjacentZone " + adjacentZone.getId().toString() + " has " + adjacentZone.getNumberOfSurplusVehicles() + " surplus vehicles");

						if (adjacentZone.getNumberOfSurplusVehicles() > 0) {
							Iterator<Link> links = adjacentZone.getVehicles().keySet().iterator();
							fromLink = links.next();
							CopyOnWriteArrayList<String> vehicleIds = adjacentZone.getVehicles().get(fromLink);

							if (vehicleIds.size() == 1) {
								log.info("found one vehicle at link " + fromLink.getId());
								vehicleId = vehicleIds.get(0);
								adjacentZone.getVehicles().remove(fromLink);
							} else {
								log.info("found multiple vehicles at link " + fromLink.getId());
								vehicleId = vehicleIds.remove(0);
								adjacentZone.getVehicles().put(fromLink, vehicleIds);
							}

							break;
						}
					}

					if ((fromLink != null) && (vehicleId != null)) {
						relocations.add(new RelocationInfo(vehicleId, fromLink.getId(), toLink.getId()));
					}
				}
			} else {
				break;
			}
		}

		return relocations;
	}

	public void storeStatus(double now) {
		Map<Id<RelocationZone>, Map<String, Integer>> relocationZonesStatus = new HashMap<Id<RelocationZone>, Map<String, Integer>>();

		for (RelocationZone relocationZone : this.getRelocationZones()) {
			Map<String, Integer> zoneStatus = new HashMap<String, Integer>();
			zoneStatus.put("vehicles", relocationZone.getNumberOfVehicles());
			zoneStatus.put("requests", relocationZone.getNumberOfRequests());
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
