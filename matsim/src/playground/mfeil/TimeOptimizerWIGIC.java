/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizerWIGIC.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.mfeil;


import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.scoring.PlanScorer;




/**
 * @author Matthias Feil
 * TimeOptimizerWIGIC: "What I Get Is Correct"
 * This is an extension of the standard TimeOptimizer that saves runtime by
 * assuming that the plans to be optimized are "correct" (i.e. times of the legs
 * and acts are meaningful and correct, no overlaps or similar). It basically saves
 * the initial clean-up loop compared to the standard TimeOptimizer.
 * The TimeOptimizerWIGIC is particularly designed to serve as FinalTimer in the 
 * PlanomatX.
 */

public class TimeOptimizerWIGIC extends TimeOptimizer implements PlanAlgorithm { 
		
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
	
	public TimeOptimizerWIGIC (LegTravelTimeEstimator estimator, PlanScorer scorer){
		
		super(estimator, scorer);
	
		this.OFFSET					= 900;
		this.MAX_ITERATIONS 		= 30;
		this.STOP_CRITERION			= 10;
		this.NEIGHBOURHOOD_SIZE		= 10;
		
		//TODO @MF: constants to be configured externally
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	public void run (Plan basePlan){
		
		if (basePlan.getPlanElements().size()==1) return;		
		this.processPlan(basePlan);
	}
}
	

	
