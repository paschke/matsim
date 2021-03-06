/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSparseVertexPool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.social.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialPerson;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraphFactory;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseVertex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.LinkedList;
import java.util.Set;

/**
 * @author jillenberger
 *
 */
public class SocialSparseVertexPool extends SocialSparseGraphFactory {

	private LinkedList<Person> persons;
	
	private GeometryFactory geoFactory;
	/**
	 * @param crs
	 */
	public SocialSparseVertexPool(Set<? extends Person> persons, CoordinateReferenceSystem crs) {
		super(crs);
		this.persons = new LinkedList<Person>(persons);
		geoFactory = new GeometryFactory();
	}

	@Override
	public SocialSparseVertex createVertex() {
		Person person = persons.poll();
		if(person == null)
			return null;
		else {
			Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			SocialPerson sPerson = new SocialPerson(person);
			return super.createVertex(sPerson, geoFactory.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY())));
		}
	}

}
