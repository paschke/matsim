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

package playground.michalm.drt.optimizer.insertion;

import java.util.*;

import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.locationchoice.router.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.optimizer.*;
import playground.michalm.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import playground.michalm.drt.run.DrtConfigGroup;

/**
 * @author michalm
 */
public class InsertionDrtOptimizer extends AbstractDrtOptimizer implements MobsimBeforeCleanupListener {
	private final DrtConfigGroup drtCfg;

	private final ParallelMultiVehicleInsertionProblem insertionProblem;

	public InsertionDrtOptimizer(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg,
			InsertionDrtOptimizerParams params) {
		super(optimContext, params, new TreeSet<NDrtRequest>(Requests.ABSOLUTE_COMPARATOR));
		this.drtCfg = drtCfg;

		// TODO bug: cannot cast ImaginaryNode to RoutingNetworkNode
		// PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		// preProcessDijkstra.run(optimContext.network);
		PreProcessDijkstra preProcessDijkstra = null;
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		RoutingNetwork routingNetwork = new ArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);
		RoutingNetwork inverseRoutingNetwork = new InverseArrayRoutingNetworkFactory(preProcessDijkstra)
				.createRoutingNetwork(optimContext.network);

		SingleVehicleInsertionProblem[] singleVehicleInsertionProblems = new SingleVehicleInsertionProblem[drtCfg
				.getNumberOfThreads()];
		for (int i = 0; i < singleVehicleInsertionProblems.length; i++) {
			FastMultiNodeDijkstra router = new FastMultiNodeDijkstra(routingNetwork, optimContext.travelDisutility,
					optimContext.travelTime, preProcessDijkstra, fastRouterFactory, true);
			BackwardFastMultiNodeDijkstra backwardRouter = new BackwardFastMultiNodeDijkstra(inverseRoutingNetwork,
					optimContext.travelDisutility, optimContext.travelTime, preProcessDijkstra, fastRouterFactory,
					true);
			singleVehicleInsertionProblems[i] = new SingleVehicleInsertionProblem(router, backwardRouter,
					optimContext.scheduler.getParams().stopDuration, drtCfg.getMaxWaitTime());
		}

		insertionProblem = new ParallelMultiVehicleInsertionProblem(singleVehicleInsertionProblems);
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		insertionProblem.shutdown();
	}

	@Override
	protected void scheduleUnplannedRequests() {
		if (getUnplannedRequests().isEmpty()) {
			return;
		}

		VehicleData vData = new VehicleData(getOptimContext(), drtCfg, getOptimContext().fleet.getVehicles().values());

		Iterator<NDrtRequest> reqIter = getUnplannedRequests().iterator();
		while (reqIter.hasNext()) {
			NDrtRequest req = reqIter.next();
			BestInsertion best = insertionProblem.findBestInsertion(req, vData);
			if (best == null) {
				// throw new RuntimeException("No feasible solution");
			} else {
				getOptimContext().scheduler.insertRequest(best.vehicleEntry, req, best.insertion);
				vData.updateEntry(best.vehicleEntry);
			}
			reqIter.remove();
		}
	}
}
