package playground.paschke.qsim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

public class Guidance {
    private final TripRouter router;
    private final Scenario scenario;

    public Guidance(TripRouter router, Scenario scenario) {
        this.router = router;
        this.scenario = scenario;
    }

    public Id<Link> getBestOutgoingLink(Id<Link> linkId, Id<Link> destinationLinkId, double now) {
        Person person = null; // does this work?
        double departureTime = now;
        String mainMode = TransportMode.car;
        Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(this.scenario.getNetwork().getLinks().get(linkId));
        Facility<ActivityFacility> toFacility = new LinkWrapperFacility(this.scenario.getNetwork().getLinks().get(destinationLinkId));
        List<? extends PlanElement> trip = router.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

        Leg leg = (Leg) trip.get(0);  // test: either plan element 0 or 1 will be a car leg

        NetworkRoute route = (NetworkRoute) leg.getRoute();

        return route.getLinkIds().get(0); // entry number 0 should be link connected to next intersection (?)
    }
}
