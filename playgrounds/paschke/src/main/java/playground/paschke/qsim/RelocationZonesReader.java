package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Stack;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;

public class RelocationZonesReader extends MatsimXmlParser {
	private PolygonFeatureFactory polygonFeatureFactory;

	private final Scenario scenario;

	private RelocationZones relocationZones;

	private Counter counter;

	private String idString = null;

	private ArrayList<Coord> coords;

	public RelocationZonesReader(Scenario scenario) {
		this.polygonFeatureFactory = new PolygonFeatureFactory.Builder()
				.setName("carsharing_relocation_zone")
				.setCrs(DefaultGeographicCRS.WGS84)
				.addAttribute("id", String.class)
				.create();

		this.scenario = scenario;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (name.equals("relocationZones")) {
			this.relocationZones = new RelocationZones();

			counter = new Counter("reading car sharing relocation zone # ");
		}

		if (name.equals("relocationZone")) {
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
		if (name.equals("relocationZone")) {
			Coord[] coordsArray = this.coords.toArray(new Coord[this.coords.size()]);
			SimpleFeature relocationZonePolygon = this.polygonFeatureFactory.createPolygon(coordsArray);
			RelocationZone relocationZone = new RelocationZone(Id.create(this.idString, RelocationZone.class), relocationZonePolygon);
			this.relocationZones.add(relocationZone);
		}

		if (name.equals("relocationZones")) {
			counter.printCounter();
			scenario.addScenarioElement(RelocationZones.ELEMENT_NAME, this.relocationZones);
		}
	}
}
