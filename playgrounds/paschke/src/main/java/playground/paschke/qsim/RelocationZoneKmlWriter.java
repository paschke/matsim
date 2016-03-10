package playground.paschke.qsim;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class RelocationZoneKmlWriter extends MatsimXmlWriter {
	public void write(final String filename, Map<Coord, List<Integer>> status) {
		CoordinateTransformation coordinateTransformation = new CH1903LV03toWGS84();
		this.openFile(filename);
		this.writeXmlHead();
		this.writeStartTag("kml", Arrays.asList(createTuple("xmlns", "http://www.opengis.net/kml/2.2")));
		this.writeStartTag("Document", Collections.<Tuple<String, String>>emptyList());
		this.writeIconStyleTag("oversupplied", "http://paschke.ch/media/dot_green.png");
		this.writeIconStyleTag("balanced", "http://paschke.ch/media/dot_yellow.png");
		this.writeIconStyleTag("undersupplied", "http://paschke.ch/media/dot_red.png");

		Iterator<Entry<Coord, List<Integer>>> iterator = status.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Coord, List<Integer>> entry = iterator.next();
			Coord coord = entry.getKey();
			Coord WGS84Coord = coordinateTransformation.transform(coord);

			String supply = "balanced";

			if (entry.getValue().get(0) - entry.getValue().get(1) < 0) {
				supply = "undersupplied";
			} else if (entry.getValue().get(0) - entry.getValue().get(1) > 0) {
				supply = "oversupplied";
			}

			String description = "available: " + entry.getValue().get(0) + " requested: " + entry.getValue().get(1);

			this.writePlacemark(WGS84Coord.getX(), WGS84Coord.getY(), supply, description);
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
}
