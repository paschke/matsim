/* *********************************************************************** *
 * project: org.matsim.*
 * Shape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.gtfs.containers;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;

public class Shape {
	
	//Attributes
	/**
	 * The id
	 */
	private String id;

	/**
	 * The points of the shape
	 */
	private SortedMap<Integer,Coord> points;
	
	//Methods
	/**
	 * Constructs 
	 */
	public Shape(String id) {
		this.id = id;
		points = new TreeMap<>();
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the points
	 */
	public SortedMap<Integer,Coord> getPoints() {
		return points;
	}

	/**
	 * Adds a new point
	 * @param point
	 */
	public void addPoint(Coord point, int pos) {
		points.put(pos,point);
	}

}
