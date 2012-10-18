/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Wraps a {@link TransitRouter}.
 *
 * @author thibautd
 */
public class TransitRouterWrapper implements RoutingModule {
	private static final StageActivityTypes CHECKER =
		new StageActivityTypesImpl( Arrays.asList( PtConstants.TRANSIT_ACTIVITY_TYPE ) );
	private final TransitRouter router;
	private final RoutingModule walkRouter;
	private final TransitSchedule transitSchedule;

	/**
	 * Initialises an instance
	 * @param toWrap the router to add
	 */
	public TransitRouterWrapper(
			final TransitRouter toWrap,
			final TransitSchedule transitSchedule,
			final RoutingModule walkRouter) {
		this.router = toWrap;
		this.transitSchedule = transitSchedule;
		this.walkRouter = walkRouter;
	}

	/**
	 * Just links to {@link TransitRouter#calcRoute(Coord, Coord, double, Person)}.
	 * @return the list of legs returned by the transit router.
	 */
	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		List<Leg> baseTrip = router.calcRoute(
				fromFacility.getCoord(),
				toFacility.getCoord(),
				departureTime,
				person);

		// the previous approach was to return null when no trip was found and
		// not to replace the trip if so.
		// However, this makes the output of routing modules more tricky to handle.
		// Thus, every module should return a valid trip. When available, the "main
		// mode" flag should be put to the mode of the routing module.
		return baseTrip != null ?
			fillWithActivities( baseTrip , fromFacility , toFacility ) :
			walkRouter.calcRoute( fromFacility , toFacility , departureTime , person );
	}

	private final List<PlanElement> fillWithActivities(
			final List<Leg> baseTrip,
			final Facility fromFacility,
			final Facility toFacility) {
		List<PlanElement> trip = new ArrayList<PlanElement>();

		// the following was executed in PlansCalcTransitRoute at plan insertion.
		Leg firstLeg = baseTrip.get(0);
		Id fromLinkId = fromFacility.getLinkId();
		Id toLinkId = null;
		if (baseTrip.size() > 1) { // at least one pt leg available
			toLinkId = (baseTrip.get(1).getRoute()).getStartLinkId();
		} else {
			toLinkId = toFacility.getLinkId();
		}

		//XXX: use ModeRouteFactory instead?
		Route route = new GenericRouteImpl(fromLinkId, toLinkId);
		route.setTravelTime( firstLeg.getTravelTime() );
		firstLeg.setRoute( route );

		Leg lastLeg = baseTrip.get(baseTrip.size() - 1);
		toLinkId = toFacility.getLinkId();
		if (baseTrip.size() > 1) { // at least one pt leg available
			fromLinkId = (baseTrip.get(baseTrip.size() - 2).getRoute()).getEndLinkId();
		}

		//XXX: use ModeRouteFactory instead?
		route = new GenericRouteImpl(fromLinkId, toLinkId);
		route.setTravelTime( lastLeg.getTravelTime() );
		lastLeg.setRoute( route );

		boolean isFirstLeg = true;
		Coord nextCoord = null;
		for (Leg leg2 : baseTrip) {
			if (isFirstLeg) {
				trip.add( leg2 );
				isFirstLeg = false;
			}
			else {
				if (leg2.getRoute() instanceof ExperimentalTransitRoute) {
					ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg2.getRoute();
					ActivityImpl act =
						new ActivityImpl(
								PtConstants.TRANSIT_ACTIVITY_TYPE, 
								this.transitSchedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), 
								tRoute.getStartLinkId());
					act.setMaximumDuration(0.0);
					trip.add(act);
					nextCoord = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
				}
				else { // walk legs don't have a coord, use the coord from the last egress point
					ActivityImpl act =
						new ActivityImpl(
								PtConstants.TRANSIT_ACTIVITY_TYPE,
								nextCoord, 
								leg2.getRoute().getStartLinkId());
					act.setMaximumDuration(0.0);
					trip.add(act);
				}

				trip.add( leg2 );
			}
		}

		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return CHECKER;
	}
} 
