/* *********************************************************************** *
 * project: org.matsim.*
 * QueryAgentActivityStatus.java
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

package org.matsim.utils.vis.otfvis.opengl.queries;

import java.util.Collection;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;

public class QueryAgentActivityStatus implements OTFQuery {

	private static final long serialVersionUID = -8532403277319196797L;

	private Id agentId = null;

	private double now;
	//out
	int activityNr = -1;
	double finished = 0;

	public void query(QueueNetwork net, Population plans, Events events, OTFServerQuad quad) {
		Person person = plans.getPerson(this.agentId);
		if (person == null) return;

		Plan plan = person.getSelectedPlan();

		// find the actual activity by searching all activity links
		// for a vehicle with this agent id

		for (int i=0;i< plan.getPlanElements().size(); i+=2) {
			Act act = (Act)plan.getPlanElements().get(i);
			QueueLink link = net.getQueueLink(act.getLinkId());
			Collection<QueueVehicle> vehs = link.getAllVehicles();
			for (QueueVehicle info : vehs) {
				if (info.getDriver().getPerson().getId().compareTo(this.agentId) == 0) {
					// we found the little nutty, now lets reason about the length of 1st activity
					double departure = info.getDepartureTime_s();
					double diff =  departure - info.getLastMovedTime();
					this.finished = (this.now - info.getLastMovedTime()) / diff;
					this.activityNr = i/2;
				}
			}
		}

	}

	public void remove() {
	}

	public void draw(OTFDrawer drawer) {
	}

	public boolean isAlive() {
		return false;
	}

	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	public void setId(String id) {
		this.agentId = new IdImpl(id);
	}

	public void setNow(double now) {
		this.now = now;
	}
}
