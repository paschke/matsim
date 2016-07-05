package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Stack;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

public class RelocationTimesReader extends MatsimXmlParser {
	private final Scenario scenario;

	private ArrayList<Double> relocationTimesList;

	private Counter counter;

	public RelocationTimesReader(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ( name.equals("relocationTimes" ) ) {
			this.relocationTimesList = new ArrayList<Double>();

			counter = new Counter( "reading car sharing relocation time # " );
		}

		if ( name.equals( "relocationTime" ) ) {
			counter.incCounter();
			this.relocationTimesList.add(Time.parseTime(atts.getValue( "start_time" )));
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ( name.equals( "relocationTimes" ) ) {
			counter.printCounter();
			scenario.addScenarioElement( "CarSharingRelocationTimes", this.relocationTimesList );
		}
	}
}
