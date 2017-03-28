package playground.balac.utils.personattributes;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class InducedDemandPersonAttributes {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		ObjectAttributes bla = new ObjectAttributes();
		
		//new ObjectAttributesXmlReader(bla).readFile(args[0]);
		new ObjectAttributesXmlReader(bla);
		
		
		for(Person p : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = p.getSelectedPlan();
			Map<String, Integer> count = new HashMap<>();
			Activity firstHomeActivity = null;
			for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					double duration = ((Activity) pe).getEndTime() - ((Activity) pe).getStartTime();
					String type = ((Activity) pe).getType();
					
					if (plan.getPlanElements().size() == 1) {
						bla.putAttribute(p.getId().toString(), "typicalDuration_" + type + "_1", 86400.0);
						bla.putAttribute(p.getId().toString(), "minimalDuration_" + type + "_1", 900.0);
						bla.putAttribute(p.getId().toString(), "latestStartTime_" + type + "_1", 86400.0);
						bla.putAttribute(p.getId().toString(), "earliestEndTime_" + type + "_1", 0.0);
						((Activity) pe).setType(type + "_1");
						continue;
					}
						
					if (plan.getPlanElements().get(plan.getPlanElements().size() - 1) == pe)
						duration = firstHomeActivity.getEndTime() + 24 * 3600.0 - ((Activity) pe).getStartTime();
					else if (plan.getPlanElements().get(0) == pe) {
						firstHomeActivity = (Activity) pe;
						((Activity) pe).setType(type + "_" + count.get(type));
						continue;
					}
					if (count.containsKey(type))
						count.put(type, count.get(type) + 1);
					else
						count.put(type, 1);
					((Activity) pe).setType(type + "_" + count.get(type));

					bla.putAttribute(p.getId().toString(), "typicalDuration_" + type + "_" + count.get(type), duration);
					bla.putAttribute(p.getId().toString(), "minimalDuration_" + type + "_" + count.get(type), 900.0);
					bla.putAttribute(p.getId().toString(), "latestStartTime_" + type + "_" + count.get(type), 86400.0);
					bla.putAttribute(p.getId().toString(), "earliestEndTime_" + type + "_" + count.get(type), 0.0);

				}			
				
			}		
			
		}
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.writeV5(args[4]);
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile(args[3]);		
		
		
	}

}
