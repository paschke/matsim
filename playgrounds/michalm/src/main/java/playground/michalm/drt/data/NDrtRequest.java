/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.drt.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import playground.michalm.drt.schedule.NDrtStopTask;

/**
 * @author michalm
 */
public class NDrtRequest extends RequestImpl implements PassengerRequest {

	public enum DrtRequestStatus {
		UNPLANNED, // submitted by the CUSTOMER and received by the DISPATCHER
		PLANNED, // planned - included into one of the routes
		PICKUP, // being picked up
		RIDE, // on board
		DROPOFF, // being dropped off
		PERFORMED, // completed
		REJECTED; // rejected by the DISPATCHER
	}

	private final MobsimPassengerAgent passenger;
	private final Link fromLink;
	private final Link toLink;
	private NDrtStopTask pickupTask = null;
	private NDrtStopTask dropoffTask = null;

	public NDrtRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink, double t0,
			double submissionTime) {
		super(id, 1, t0, t0, submissionTime);
		this.passenger = passenger;
		this.fromLink = fromLink;
		this.toLink = toLink;
	}

	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public Link getToLink() {
		return toLink;
	}

	@Override
	public MobsimPassengerAgent getPassenger() {
		return passenger;
	}

	public NDrtStopTask getPickupTask() {
		return pickupTask;
	}

	public void setPickupTask(NDrtStopTask pickupTask) {
		this.pickupTask = pickupTask;
	}

	public NDrtStopTask getDropoffTask() {
		return dropoffTask;
	}

	public void setDropoffTask(NDrtStopTask dropoffTask) {
		this.dropoffTask = dropoffTask;
	}

	public DrtRequestStatus getStatus() {
		if (pickupTask == null) {
			return DrtRequestStatus.UNPLANNED;
		}

		switch (pickupTask.getStatus()) {
			case PLANNED:
				return DrtRequestStatus.PLANNED;

			case STARTED:
				return DrtRequestStatus.PICKUP;

			case PERFORMED:// continue
		}

		switch (dropoffTask.getStatus()) {
			case PLANNED:
				return DrtRequestStatus.RIDE;

			case STARTED:
				return DrtRequestStatus.DROPOFF;

			case PERFORMED:
				return DrtRequestStatus.PERFORMED;

		}

		throw new IllegalStateException("Unreachable code");
	}
}
