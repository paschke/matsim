package org.matsim.contrib.carsharing.config;

import java.util.ArrayList;
import java.util.Stack;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.qsim.CarsharingAreas;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;

public class CarsharingAreasReader extends MatsimXmlParser {
	private PolygonFeatureFactory polygonFeatureFactory;

	private final Scenario scenario;

	private CarsharingAreas carsharingAreas;

	private Counter counter;

	private String idString = null;

	private ArrayList<Coord> coords;

	private String groupName;

	public CarsharingAreasReader(Scenario scenario) {
		this.polygonFeatureFactory = new PolygonFeatureFactory.Builder()
				.setName("carsharing_area")
				.setCrs(DefaultGeographicCRS.WGS84)
				.addAttribute("id", String.class)
				.create();

		this.scenario = scenario;
	}

	public void parse(String areasInputFile, String groupName) {
		this.groupName = groupName;

		this.parse(areasInputFile);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("polygons")) {
			this.carsharingAreas = new CarsharingAreas();

			counter = new Counter("reading freefloating area # ");
		}

		if (name.equals("polygon")) {
			counter.incCounter();
			this.idString = atts.getValue("id");
			this.coords = new ArrayList<Coord>();
		}

		if (name.equals("node")) {
			final String x = atts.getValue("x");
			final String y = atts.getValue("y");

			Coord coord = new Coord(
					Double.parseDouble(x),
					Double.parseDouble(y)
			);

			this.coords.add(coord);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (name.equals("polygon")) {
			Coord[] coordsArray = this.coords.toArray(new Coord[this.coords.size()]);
			SimpleFeature carsharingArea = this.polygonFeatureFactory.createPolygon(coordsArray);
			carsharingArea.setAttribute("id", this.idString);

			// create union of local and instance variable carsharingArea
			this.carsharingAreas.add(carsharingArea);
		}

		if (name.equals("polygons")) {
			counter.printCounter();
			scenario.addScenarioElement(CarsharingAreas.ELEMENT_NAME + this.groupName, this.carsharingAreas);
		}
	}
}
