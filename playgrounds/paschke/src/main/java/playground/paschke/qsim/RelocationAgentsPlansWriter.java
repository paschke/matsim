package playground.paschke.qsim;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;

public class RelocationAgentsPlansWriter extends MatsimXmlWriter {

	public void write(String filename, Map<Id<Person>, RelocationAgent> relocationAgents) {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeStartTag("population", Collections.<Tuple<String, String>>emptyList());

		Iterator<Entry<Id<Person>, RelocationAgent>> agentIterator = relocationAgents.entrySet().iterator();
		while (agentIterator.hasNext()) {
			Entry<Id<Person>, RelocationAgent> entry = agentIterator.next();

			this.writePersonPlans(entry.getValue());
		}

		this.writeEndTag("population");
		this.close();
	}

	public void writePersonPlans(RelocationAgent relocationAgent) {
		this.writeStartTag("person", Arrays.asList(createTuple("id", relocationAgent.getId().toString())));
		this.writeStartTag("plan", Arrays.asList(createTuple("selected", "yes")));

		Iterator<PlanElement> planElementsIterator = relocationAgent.getPlanElements().iterator();
		while (planElementsIterator.hasNext()) {
			PlanElement planElement = planElementsIterator.next();
			if (planElement.getClass() == ActivityImpl.class) {
				this.writeStartTag("act", Arrays.asList(
						createTuple("type", ((ActivityImpl) planElement).getType()),
						createTuple("link", ((ActivityImpl) planElement).getLinkId().toString()),
						createTuple("start_time", Time.writeTime(((ActivityImpl) planElement).getStartTime())),
						createTuple("end_time", Time.writeTime(((ActivityImpl) planElement).getEndTime()))
						), true);
			} else if (planElement.getClass() == LegImpl.class) {
				if (((LegImpl) planElement).getMode() == "car") {
					this.writeStartTag("leg", Arrays.asList(
							createTuple("mode", ((LegImpl) planElement).getMode()),
							createTuple("dep_time", Time.writeTime(((LegImpl) planElement).getDepartureTime())),
							createTuple("arr_time", Time.writeTime(((LegImpl) planElement).getArrivalTime()))
							));
					this.writeStartTag("route", Arrays.asList(
							createTuple("type", "links")
							));

					String linkIdString = "";

					for (Id<Link> linkId : ((LinkNetworkRouteImpl) ((LegImpl) planElement).getRoute()).getLinkIds()) {
						linkIdString = linkIdString + linkId.toString() + " ";
					}

					this.writeContent(linkIdString, true);
					this.writeEndTag("route");
					this.writeEndTag("leg");
				} else {
					this.writeStartTag("leg", Arrays.asList(
							createTuple("mode", ((LegImpl) planElement).getMode()),
							createTuple("dep_time", Time.writeTime(((LegImpl) planElement).getDepartureTime())),
							createTuple("arr_time", Time.writeTime(((LegImpl) planElement).getArrivalTime()))
							));
					this.writeStartTag("route", Arrays.asList(
							createTuple("type", "generic"),
							createTuple("start_link", ((LegImpl) planElement).getRoute().getStartLinkId().toString()),
							createTuple("end_link", ((LegImpl) planElement).getRoute().getEndLinkId().toString())
							), true);
					this.writeEndTag("leg");
				}
			}
		}

		this.writeEndTag("plan");
		this.writeEndTag("person");
	}
}
