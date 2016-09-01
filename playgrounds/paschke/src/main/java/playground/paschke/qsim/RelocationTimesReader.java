package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

public class RelocationTimesReader extends MatsimXmlParser {
	private ArrayList<Double> relocationTimes;

	private Counter counter;

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ( name.equals("relocationTimes" ) ) {
			this.relocationTimes = new ArrayList<Double>();

			counter = new Counter( "reading car sharing relocation time # " );
		}

		if ( name.equals( "relocationTime" ) ) {
			counter.incCounter();
			this.relocationTimes.add(Time.parseTime(atts.getValue( "start_time" )));
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ( name.equals( "relocationTimes" ) ) {
			counter.printCounter();
		}
	}

	public ArrayList<Double> getRelocationTimes() {
		return this.relocationTimes;
	}
}
