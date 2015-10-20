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

/**
 * Mostly copied from tutorial.programming.ownMobsimAgentUsingRouter.MyGuidance
 * 
 * Guidance. applied TripRouter.
 */

public class Guidance {
    private final TripRouter router;

    public Guidance(TripRouter router) {
        this.router = router;
    }

    public Id<Link> getBestOutgoingLink(Link startLink, Link destinationLink, double now) {
        Person person = null; // does this work?
        double departureTime = now;
        String mainMode = TransportMode.car;
        Facility<ActivityFacility> startFacility = new LinkWrapperFacility(startLink);
        Facility<ActivityFacility> destinationFacility = new LinkWrapperFacility(destinationLink);
        List<? extends PlanElement> trip = router.calcRoute(mainMode, startFacility, destinationFacility, departureTime, person);

        Leg leg = (Leg) trip.get(0);  // test: either plan element 0 or 1 will be a car leg

        NetworkRoute route = (NetworkRoute) leg.getRoute();

        if (route.getLinkIds().isEmpty()) {
        	return route.getEndLinkId();
        }

        return route.getLinkIds().get(0); // entry number 0 should be link connected to next intersection (?)
    }
}
