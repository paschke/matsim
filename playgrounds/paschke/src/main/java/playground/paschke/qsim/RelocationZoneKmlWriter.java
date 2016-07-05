package playground.paschke.qsim;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.io.MatsimXmlWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class RelocationZoneKmlWriter extends MatsimXmlWriter {
	protected CoordinateTransformation coordinateTransformation;

	protected Map<Id<RelocationZone>, Coord[]> coords;

	public RelocationZoneKmlWriter() {
		this.coordinateTransformation = new CH1903LV03toWGS84();
	}

	public void setPolygons(Map<Id<RelocationZone>, MultiPolygon> polygons) {
		this.coords = new HashMap<Id<RelocationZone>, Coord[]>();
		Iterator<Entry<Id<RelocationZone>, MultiPolygon>> iterator = polygons.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<Id<RelocationZone>, MultiPolygon> entry = iterator.next();

			MultiPolygon polygon = entry.getValue();
			Coord[] coords = new Coord[polygon.getCoordinates().length];
			for (int index = 0; index < polygon.getCoordinates().length; index++) {
				Coordinate coordinate = polygon.getCoordinates()[index];
				Coord coord = new Coord(coordinate.getOrdinate(Coordinate.X), coordinate.getOrdinate(Coordinate.Y));
				Coord WGS84Coord = coordinateTransformation.transform(coord);
				coords[index] = WGS84Coord;
			}

			this.coords.put(entry.getKey(), coords);
		}
	}

	public Map<Id<RelocationZone>, Coord[]> getCoords()
	{
		return this.coords;
	}

	public void write(final int iteration, final String filename, Map<Id<RelocationZone>, Map<String, Integer>> status) {
		this.openFile(filename);
		this.writeStartTag("kml", Arrays.asList(createTuple("xmlns", "http://www.opengis.net/kml/2.2"), createTuple("xmlns:gx", "http://www.google.com/kml/ext/2.2"), createTuple("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"), createTuple("xsi:schemalocation", "http://www.opengis.net/kml/2.2 https://developers.google.com/kml/schema/kml22gx.xsd")));

		this.writeStartTag("Document", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("name", Collections.<Tuple<String, String>>emptyList());
		this.writeContent("Relocation Zones " + iteration, false);
		this.writeEndTag("name");

		Iterator<Entry<Id<RelocationZone>, Map<String, Integer>>> iterator = status.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Id<RelocationZone>, Map<String, Integer>> entry = iterator.next();

			Integer numVehicles = entry.getValue().get("vehicles");
			Integer numRequests = entry.getValue().get("requests");
			String lineColor = "ffff0000";
			String polyColor = "66ff0000";

			if (numVehicles - numRequests < 0) {
				lineColor = "ff0000ff";
				polyColor = "660000ff";
			} else if (numVehicles - numRequests > 0) {
				lineColor = "ff00ff00";
				polyColor = "6600ff00";
			}

			this.writeStartTag("Placemark", Arrays.asList(createTuple("id", "linepolygon_" + entry.getKey().toString())));
			this.writeStartTag("description", Collections.<Tuple<String, String>>emptyList());
			this.writeContent("vehicles: " + numVehicles.toString() + " requests: " + numRequests.toString(), true);
			this.writeEndTag("description");
			this.writeStartTag("Style", Collections.<Tuple<String, String>>emptyList());
			this.writeStartTag("LineStyle", Collections.<Tuple<String, String>>emptyList());
			this.writeStartTag("color", Collections.<Tuple<String, String>>emptyList());
			this.writeContent(lineColor, false);
			this.writeEndTag("color");
			this.writeStartTag("width", Collections.<Tuple<String, String>>emptyList());
			this.writeContent("3", false);
			this.writeEndTag("width");
			this.writeEndTag("LineStyle");
			this.writeStartTag("PolyStyle", Collections.<Tuple<String, String>>emptyList());
			this.writeStartTag("color", Collections.<Tuple<String, String>>emptyList());
			this.writeContent(polyColor, false);
			this.writeEndTag("color");
			this.writeEndTag("PolyStyle");
			this.writeEndTag("Style");
			this.writePolygon(this.getCoords().get(entry.getKey()));
			this.writeEndTag("Placemark");
		}
		

		this.writeEndTag("Document");
		this.writeEndTag("kml");
		this.close();
	}

	public void writeIconStyleTag(String style, String iconHref)
	{
		this.writeStartTag("Style", Arrays.asList(createTuple("id", style)));
		this.writeStartTag("IconStyle", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("Icon", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("href", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(iconHref, false);
		this.writeEndTag("href");
		this.writeEndTag("Icon");
		this.writeEndTag("IconStyle");
		this.writeEndTag("Style");
	}

	public void writePlacemark(double x, double y, String style, String description)
	{
		this.writeStartTag("Placemark", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("name", Collections.<Tuple<String, String>>emptyList());
		this.writeEndTag("name");
		this.writeStartTag("description", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(description, true);
		this.writeEndTag("description");
		this.writeStartTag("styleUrl", Collections.<Tuple<String, String>>emptyList());
		this.writeContent("#" + style, false);
		this.writeEndTag("styleUrl");
		this.writeStartTag("Point", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("coordinates", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(x + "," + y + ",0", true);
		this.writeEndTag("coordinates");
		this.writeEndTag("Point");
		this.writeEndTag("Placemark");
	}

	public void writePolygon(Coord[] coords)
	{
		String[] coordStrings = new String[coords.length];

		for (int index = 0; index < coords.length; index++) {
			Coord coord = coords[index];
			coordStrings[index] = coord.getX() + ", " + coord.getY();
		}

		this.writeStartTag("Polygon", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("outerBoundaryIs", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("LinearRing", Collections.<Tuple<String, String>>emptyList());
		this.writeStartTag("coordinates", Collections.<Tuple<String, String>>emptyList());
		this.writeContent(String.join(" ", coordStrings), true);
		this.writeEndTag("coordinates");
		this.writeEndTag("LinearRing");
		this.writeEndTag("outerBoundaryIs");
		this.writeEndTag("Polygon");
	}
}