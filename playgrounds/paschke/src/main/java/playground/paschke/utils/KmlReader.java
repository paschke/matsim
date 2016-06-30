package playground.paschke.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class KmlReader extends MatsimXmlParser {
	protected CoordinateTransformation coordinateTransformation;

	protected String name = null;

	protected Map<String, Coord[]> relocationZones;

	protected String id = null;

	protected Coord[] coords;

	public KmlReader() {
		this.setValidating(false);
		this.coordinateTransformation = new WGS84toCH1903LV03();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("Document")) {
			// create a new collection of relocation zone containers
			this.relocationZones = new HashMap<String, Coord[]>(); 
		}

		if (name.equals("Placemark")) {
			// copy the id attribute
			this.id = atts.getValue("id");
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equals("Document")) {
			// pass collection to XML writer
		}

		if (name.equals("Placemark")) {
			// pass the relocation zone container to the collection
			this.relocationZones.put(this.id, this.coords);
		}

		if (name.equals("name")) {
			this.name = content;
		}

		if (name.equals("coordinates")) {
			// split the coordinates string be " ", then by ","
			String[] pairs = content.split(" ");
			this.coords = new Coord[pairs.length];

			for (int index = 0; index < pairs.length; index++) {
				String[] coordinates = pairs[index].split(",");
				Coord coord = new Coord(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
				Coord CH1903LV03Coord = coordinateTransformation.transform(coord);

				// add node elements to the relocation zone container
				this.coords[index] = CH1903LV03Coord;
			}
		}
	}

	public Map<String, Coord[]> getRelocationZones() {
		return this.relocationZones;
	}
}
