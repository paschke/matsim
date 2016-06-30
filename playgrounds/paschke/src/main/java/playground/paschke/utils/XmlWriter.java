package playground.paschke.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class XmlWriter extends MatsimXmlWriter {
	public void write(final String filename, Map<String, Coord[]> coords) {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeDoctype("relocationZones", "src/main/ressources/dtd/relocation_zones_v1.dtd");
		this.writeStartTag("relocationZones", Collections.<Tuple<String, String>>emptyList());

		Iterator<Entry<String, Coord[]>> iterator = coords.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Coord[]> entry = iterator.next();

			this.writeStartTag("relocationZone", Arrays.asList(createTuple("id", entry.getKey().toString())));

			for (Coord coord : entry.getValue()) {
				this.writeStartTag("node", Arrays.asList(createTuple("x", coord.getX()), createTuple("y", coord.getY())), true);
			}

			this.writeEndTag("relocationZone");
		}

		this.writeEndTag("relocationZones");
		this.close();
	}
}
