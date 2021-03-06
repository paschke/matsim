/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonsTask;

import java.util.Collection;

/**
 * @author johannes
 *
 */
public class ApplySampleProbas implements PersonsTask {

	private final int N;
	
	public ApplySampleProbas(int N) {
		this.N = N;
	}
	
	@Override
	public void apply(Collection<? extends Person> persons) {
		double p = 1/(double)N;
		double wsum = 0;
		for(Person person : persons) {
			wsum += Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
		}
		
		for(Person person : persons) {
			double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
			person.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(p * w * persons.size()/wsum));
		}

	}

}
