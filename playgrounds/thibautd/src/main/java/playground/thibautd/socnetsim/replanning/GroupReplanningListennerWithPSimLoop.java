/* *********************************************************************** *
 * project: org.matsim.*
 * GroupReplanningListennerWithPSimLoop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;

import playground.thibautd.mobsim.PseudoSimConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.scoring.UniformlyInternalizingPlansScoring;

/**
 * @author thibautd
 */
public class GroupReplanningListennerWithPSimLoop implements ReplanningListener {
	private final GroupStrategyManager mainStrategyManager;
	private final GroupStrategyManager innovativeStrategyManager;
	private final ControllerRegistry registry;
	private final MobsimFactory pSimFactory;

	public GroupReplanningListennerWithPSimLoop(
			final ControllerRegistry registry,
			final GroupStrategyManager mainStrategyManager,
			final GroupStrategyManager innovativeStrategyManager,
			final MobsimFactory pSimFactory) {
		this.registry = registry;
		this.mainStrategyManager = mainStrategyManager;
		this.innovativeStrategyManager = innovativeStrategyManager;
		this.pSimFactory = pSimFactory;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		final PseudoSimConfigGroup config = getConfigGroup();

		if ( event.getIteration() % config.getPeriod() == 0 ) {
			doInnerLoop();
		}

		mainStrategyManager.run(
				event.getIteration(),
				registry );
	}

	private void doInnerLoop() {
		final int nIters = getConfigGroup().getNPSimIters();

		// XXX Uuuuuuuuuuuuuuuglyyyyyyyyyyyyyyyyyyyy
		// - impossible to configure which listenners are used
		// - if scoring listenner in controler changes, not automatically
		// retrofited here.
		final EventsManager events = EventsUtils.createEventsManager( registry.getScenario().getConfig() );
		final UniformlyInternalizingPlansScoring scoring =
				new UniformlyInternalizingPlansScoring(
					registry.getScenario(),
					events,
					registry.getScoringFunctionFactory());

		for ( int i=0; i < nIters; i++ ) {
			scoring.notifyIterationStarts( new IterationStartsEvent( null , i ) );

			innovativeStrategyManager.run(
					i, // what makes sense here???
					registry );

			pSimFactory.createMobsim(
					registry.getScenario(),
					events ).run();

			scoring.notifyScoring( new ScoringEvent( null , i ) );
			scoring.notifyIterationEnds( new IterationEndsEvent( null , i ) );
		}
	}

	private PseudoSimConfigGroup getConfigGroup() {
		return (PseudoSimConfigGroup)
			registry.getScenario().getConfig().getModule(
					PseudoSimConfigGroup.GROUP_NAME );
	}
}
