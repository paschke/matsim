package org.matsim.contrib.carsharing.qsim;

import java.util.ArrayList;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class CarsharingAreas {
	private PointFeatureFactory pointFeatureFactory;

	public static final String ELEMENT_NAME = "carSharingAreas";

	private SimpleFeature carsharingAreas = null;

	private ArrayList<String> ids = new ArrayList<String>();

	public CarsharingAreas() {
		this.pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("point")
				.setCrs(DefaultGeographicCRS.WGS84)
				.create();
	}

	public void add(SimpleFeature carsharingArea) {
		if (carsharingArea.getName().getLocalPart() == "carsharing_area") {
			String id = (String) carsharingArea.getAttribute("id");

			if (this.ids.contains(id) == false) {
				this.ids.add(id);

				if (this.carsharingAreas == null) {
					this.carsharingAreas = carsharingArea;
				} else {
					// merge geometries!
					MultiPolygon oldArea = (MultiPolygon) this.carsharingAreas.getAttribute("the_geom");
					MultiPolygon newArea = (MultiPolygon) carsharingArea.getAttribute("the_geom");
					MultiPolygon unitedArea = (MultiPolygon) oldArea.union(newArea);
					this.carsharingAreas.setAttribute("the_geom", unitedArea);
				}
			}
		}
	}

	public boolean contains(Coord coord) {
		MultiPolygon area = (MultiPolygon) this.carsharingAreas.getAttribute("the_geom");
		SimpleFeature pointFeature = this.pointFeatureFactory.createPoint(coord, new Object[0], null);
		Point point = (Point) pointFeature.getAttribute("the_geom");

		return area.contains(point);
	}
}
