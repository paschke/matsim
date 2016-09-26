package playground.paschke.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class GiveEveryoneFFCards {
	public static void main(String[] args) {
		Map<Id<Person>, Map<String, List<String>>> memberships = new HashMap<Id<Person>, Map<String, List<String>>>();

		final Config config = ConfigUtils.loadConfig(args[0]);
		final Scenario sc = ScenarioUtils.loadScenario(config);

		Population population = (Population) sc.getPopulation();
		@SuppressWarnings("unchecked")
		Map<Id<Person>, Person> persons = (Map<Id<Person>, Person>) population.getPersons();

		for (Person person : persons.values()) {
			Map<String, List<String>> personMemberships = new HashMap<String, List<String>>();
			List<String> types = new ArrayList<String>();
			types.add("freefloating");
			personMemberships.put("Catchacar", types);
			memberships.put(person.getId(), personMemberships);
		}

		MembershipWriter writer = new MembershipWriter(memberships);
		writer.writeFile("/Users/paschke/Documents/workspace/matsim/input/zurich_example/membership.xml");
	}

}
