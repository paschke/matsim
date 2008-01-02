/* *********************************************************************** *
 * project: org.matsim.*
 * BestPlanSelectorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.replanning.selectors;

import org.matsim.basic.v01.Id;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * Test for {@link KeepSelected}
 *
 * @author mrieser
 */
public class KeepSelectedTest extends AbstractPlanSelectorTest {

	@Override
	protected PlanSelectorI getPlanSelector() {
		return new KeepSelected();
	}

	/**
	 * Test that really the already selected plan is returned.
	 *
	 * @author mrieser
	 */
	public void testSelected() {
		Person person = new Person(new Id(1), "m", 40, null, null, null);
		Plan plan1 = person.createPlan(null, "no");
		Plan plan2 = person.createPlan("10.0", "yes");
		Plan plan3 = person.createPlan("-50.0", "no");

		KeepSelected selector = new KeepSelected();

		// test default selected plan
		assertEquals(plan2, selector.selectPlan(person));

		// test selected plan with negative score
		person.setSelectedPlan(plan3);
		assertEquals(plan3, selector.selectPlan(person));

		// test selected plan with undefined score
		person.setSelectedPlan(plan1);
		assertEquals(plan1, selector.selectPlan(person));
	}

}
