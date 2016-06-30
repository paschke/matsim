package playground.paschke.utils;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class GiveEveryoneFFCards {
	public static void main(String[] args) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(sc);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);

		final ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader( atts ).parse( args[2] );

		PopulationImpl population = (PopulationImpl) sc.getPopulation();
		@SuppressWarnings("unchecked")
		Map<Id<Person>, PersonImpl> persons = (Map<Id<Person>, PersonImpl>) population.getPersons();

		for (PersonImpl person : persons.values()) {
			//if (person.hasLicense()) {
			//	atts.putAttribute(person.getId().toString(), "FF_CARD", "true");
			//}
		}

		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(atts);
		betaWriter.writeFile("/Users/paschke/Projects/matsim_kurs/input/zurich_example/person_attributes.xml");
	}

}
