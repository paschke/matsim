package playground.paschke.qsim;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

public class RelocationAgentsReader extends MatsimXmlParser {
	private final Scenario scenario;

	private HashMap<String, Map<String, Double>> relocationAgentBasesList;

	private Counter counter;

	public RelocationAgentsReader(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ( name.equals("relocationAgentBases" ) ) {
			this.relocationAgentBasesList = new HashMap<String, Map<String, Double>>();

			counter = new Counter( "reading car sharing relocation agent base # " );
		}

		if ( name.equals( "relocationAgentBase" ) ) {
			counter.incCounter();
			HashMap<String, Double> agentBaseData = new HashMap<String, Double>();
			agentBaseData.put("x", Double.parseDouble(atts.getValue("x")));
			agentBaseData.put("y", Double.parseDouble(atts.getValue("y")));
			agentBaseData.put("number", Double.parseDouble(atts.getValue("number")));

			this.relocationAgentBasesList.put(atts.getValue("id"), agentBaseData);
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ( name.equals( "relocationAgentBases" ) ) {
			counter.printCounter();
			scenario.addScenarioElement( "CarSharingRelocationAgents", this.relocationAgentBasesList );
		}
	}
}
