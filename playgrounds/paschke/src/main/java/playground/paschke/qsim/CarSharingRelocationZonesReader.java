package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

public class CarSharingRelocationZonesReader extends MatsimXmlParser {
	private final Scenario scenario;

	private CarSharingRelocationZones relocationZones;

	private ArrayList<RelocationZone> relocationZonesList;

	private Counter counter;

	public CarSharingRelocationZonesReader(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ( name.equals("relocationZones" ) ) {
			this.relocationZones = new CarSharingRelocationZones();

			this.relocationZonesList = new ArrayList<RelocationZone>();

			counter = new Counter( "reading car sharing relocation zone # " );
		}

		if ( name.equals( "relocationZone" ) ) {
			counter.incCounter();
			final String idString = atts.getValue( "id" );
			final String x = atts.getValue( "x" );
			final String y = atts.getValue( "y" );

			Coord coord = new Coord(
					Double.parseDouble( x ),
					Double.parseDouble( y )
			);
			RelocationZone relocationZone = new RelocationZone(coord);
	    	this.relocationZonesList.add(relocationZone);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ( name.equals( "relocationZones" ) ) {
			counter.printCounter();
			this.relocationZones.setRelocationZoneLocations(this.relocationZonesList);
			scenario.addScenarioElement( CarSharingRelocationZones.ELEMENT_NAME, this.relocationZones );
		}
	}
}
