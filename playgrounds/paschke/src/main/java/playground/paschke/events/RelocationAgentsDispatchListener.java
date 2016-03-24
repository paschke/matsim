package playground.paschke.events;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

// UNUSED EVER SINCE THE RELOCATION AGENTS ARE NOT PART OF THE POPULATION ANY MORE
// ALSO, RELOCATION AGENTS DO NOT HAVE PLANS NO MORE
public class RelocationAgentsDispatchListener implements BeforeMobsimListener {
	private static final Logger log = Logger.getLogger("dummy");

	Controler controler;

	public RelocationAgentsDispatchListener(Controler controler) {		
		this.controler = controler;		
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Scenario scenario = controler.getScenario();
		Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();

		Person dispatchAgent = persons.get(Id.createPersonId(1000));
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.setPerson(dispatchAgent);

//		Activity waitBeforeAtFacility = scenario.getPopulation().getFactory().createActivityFromCoord("h", new CoordImpl(-375, 0));
//		Activity waitBeforeAtFacility = scenario.getPopulation().getFactory().createActivityFromLinkId("h", Id.createLinkId(2));
		Activity waitBeforeAtFacility = new ActivityImpl("h", new Coord(-375, 0), Id.createLinkId(2));
		waitBeforeAtFacility.setStartTime(0);
		waitBeforeAtFacility.setEndTime(36000);
		plan.addActivity(waitBeforeAtFacility);

		Leg accessLeg = scenario.getPopulation().getFactory().createLeg("walk_ff");
		accessLeg.setDepartureTime(36000);
		accessLeg.setTravelTime(600);
		accessLeg.setRoute(new LinkNetworkRouteImpl(Id.createLinkId(2), Id.createLinkId(5)));
		plan.addLeg(accessLeg);

		Leg relocationLeg = scenario.getPopulation().getFactory().createLeg("freefloating");
		relocationLeg.setRoute(new LinkNetworkRouteImpl(Id.createLinkId(5), Id.createLinkId(1)));
		plan.addLeg(relocationLeg);

//		Activity dropVehicle = scenario.getPopulation().getFactory().createActivityFromCoord("w", new CoordImpl(1000, -250));
//		Activity dropVehicle = scenario.getPopulation().getFactory().createActivityFromLinkId("w", Id.createLinkId(1));
		Activity dropVehicle = new ActivityImpl("w", new Coord(1000, -250), Id.createLinkId(1));
		dropVehicle.setMaximumDuration(60);
		plan.addActivity(dropVehicle);

		Leg returnLeg = scenario.getPopulation().getFactory().createLeg("walk");
		returnLeg.setTravelTime(600);
		returnLeg.setRoute(new LinkNetworkRouteImpl(Id.createLinkId(1), Id.createLinkId(2)));
		plan.addLeg(returnLeg);

//		Activity waitAfterAtFacility = scenario.getPopulation().getFactory().createActivityFromCoord("h", new CoordImpl(-375, 0));
//		Activity waitAfterAtFacility = scenario.getPopulation().getFactory().createActivityFromLinkId("h", Id.createLinkId(2));
		Activity waitAfterAtFacility = new ActivityImpl("h", new Coord(-375, 0), Id.createLinkId(2));
		waitAfterAtFacility.setStartTime(40000);
		waitAfterAtFacility.setEndTime(86400);
		plan.addActivity(waitAfterAtFacility);

		dispatchAgent.addPlan(plan);
		dispatchAgent.setSelectedPlan(plan);

		log.info("dispatching Person 1000 to move one ff vehicle from link 0 to link 4, at 10:00");
	}
}
