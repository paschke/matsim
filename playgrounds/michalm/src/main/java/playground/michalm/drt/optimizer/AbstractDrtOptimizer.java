/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.michalm.drt.optimizer;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.michalm.drt.data.NDrtRequest;

/**
 * @author michalm
 */
public abstract class AbstractDrtOptimizer implements DrtOptimizer {
	private final DrtOptimizerContext optimContext;
	private final Collection<NDrtRequest> unplannedRequests;

	private boolean requiresReoptimization = false;

	public AbstractDrtOptimizer(DrtOptimizerContext optimContext, AbstractDrtOptimizerParams params,
			Collection<NDrtRequest> unplannedRequests) {
		this.optimContext = optimContext;
		this.unplannedRequests = unplannedRequests;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (requiresReoptimization) {
			for (Vehicle v : optimContext.fleet.getVehicles().values()) {
				optimContext.scheduler.updateTimeline(v);
			}

			scheduleUnplannedRequests();
			requiresReoptimization = false;
		}
	}

	protected abstract void scheduleUnplannedRequests();

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.add((NDrtRequest)request);
		requiresReoptimization = true;
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		optimContext.scheduler.updateBeforeNextTask(vehicle);

		Task newCurrentTask = vehicle.getSchedule().nextTask();

		if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
			requiresReoptimization = doReoptimizeAfterNextTask((TaxiTask)newCurrentTask);
		}
	}

	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return false;
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {
		// optimContext.scheduler.updateTimeline(vehicle);

		// TODO we may here possibly decide whether or not to reoptimize
		// if (delays/speedups encountered) {requiresReoptimization = true;}
	}
}
