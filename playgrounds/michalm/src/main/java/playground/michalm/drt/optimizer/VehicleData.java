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

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.run.DrtConfigGroup;
import playground.michalm.drt.schedule.*;
import playground.michalm.drt.schedule.NDrtTask.NDrtTaskType;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry {
		public final Vehicle vehicle;
		public final LinkTimePair start;// TODO what if start is just at the beginning of the first stopTask???
		public int startOccupancy;
		public final List<Stop> stops = new ArrayList<>();

		public Entry(Vehicle vehicle, LinkTimePair start) {
			this.vehicle = vehicle;
			this.start = start;
		}
	}

	public static class Stop {
		public final NDrtStopTask task;
		public final double maxArrivalTime;// relating to max pass drive time (for dropoff requests)
		public final double maxDepartureTime;// relating to pass max wait time (for pickup requests)
		public final int occupancyChange;// diff in pickups and dropoffs
		public int outputOccupancy;

		public Stop(NDrtStopTask task, DrtConfigGroup drtCfg) {
			this.task = task;
			maxArrivalTime = calcMaxArrivalTime();
			maxDepartureTime = calcMaxDepartureTime(drtCfg.getMaxWaitTime());
			occupancyChange = task.getPickupRequests().size() - task.getDropoffRequests().size();
		}

		private double calcMaxArrivalTime() {
			double maxTime = Double.MAX_VALUE;
			for (NDrtRequest r : task.getDropoffRequests()) {
				double reqMaxArrivalTime = r.getEarliestStartTime() + 3600;// TODO temp
				if (reqMaxArrivalTime < maxTime) {
					maxTime = reqMaxArrivalTime;
				}
			}
			return maxTime;
		}

		private double calcMaxDepartureTime(double maxWaitTime) {
			double maxTime = Double.MAX_VALUE;
			for (NDrtRequest r : task.getPickupRequests()) {
				double reqMaxDepartureTime = r.getEarliestStartTime() + maxWaitTime;
				if (reqMaxDepartureTime < maxTime) {
					maxTime = reqMaxDepartureTime;
				}
			}
			return maxTime;
		}

	}

	private final DrtConfigGroup drtCfg;
	private final List<Entry> entries = new ArrayList<>();
	private final double currTime;

	public VehicleData(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg, Iterable<? extends Vehicle> vehicles) {
		this.drtCfg = drtCfg;
		currTime = optimContext.timer.getTimeOfDay();

		for (Vehicle v : vehicles) {
			Entry e = createVehicleData(v);
			if (e != null) {
				entries.add(e);
			}
		}
	}
	
	public void updateEntry(Entry vEntry) {
		int idx = entries.indexOf(vEntry);//TODO inefficient! ==> use map instead of list for storing entries...  
		entries.set(idx, createVehicleData(vEntry.vehicle));
	}

	private Entry createVehicleData(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		ScheduleStatus status = schedule.getStatus();
		if (currTime >= vehicle.getServiceEndTime() || status == ScheduleStatus.COMPLETED) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<NDrtTask> tasks = (List<NDrtTask>)schedule.getTasks();
		NDrtTask currentTask = (NDrtTask)schedule.getCurrentTask();

		LinkTimePair start;
		int nextTaskIdx;
		if (status == ScheduleStatus.STARTED) {
			switch (currentTask.getDrtTaskType()) {
				case DRIVE:
					NDrtDriveTask driveTask = (NDrtDriveTask)currentTask;
					start = ((OnlineDriveTaskTracker)driveTask.getTaskTracker()).getDiversionPoint();
					break;

				case STOP:
					NDrtStopTask stopTask = (NDrtStopTask)currentTask;
					start = new LinkTimePair(stopTask.getLink(), stopTask.getEndTime());
					break;

				case STAY:
					NDrtStayTask stayTask = (NDrtStayTask)currentTask;
					start = new LinkTimePair(stayTask.getLink(), currTime);
					break;

				default:
					throw new RuntimeException();
			}

			nextTaskIdx = currentTask.getTaskIdx() + 1;
		} else { // PLANNED
			start = new LinkTimePair(vehicle.getStartLink(), vehicle.getServiceBeginTime());
			nextTaskIdx = 0;
		}

		Entry data = new Entry(vehicle, start);
		for (int i = nextTaskIdx; i < tasks.size(); i++) {
			NDrtTask task = tasks.get(i);
			if (task.getDrtTaskType() == NDrtTaskType.STOP) {
				Stop stop = new Stop((NDrtStopTask)task, drtCfg);
				data.stops.add(stop);
			}
		}

		int outputOccupancy = 0;
		for (int i = data.stops.size() - 1; i >= 0; i--) {
			Stop s = data.stops.get(i);
			s.outputOccupancy = outputOccupancy;
			outputOccupancy -= s.occupancyChange;
		}
		data.startOccupancy = outputOccupancy;

		return data;
	}

	public int getSize() {
		return entries.size();
	}

	public Entry getEntry(int idx) {
		return entries.get(idx);
	}

	public List<Entry> getEntries() {
		return entries;
	}
}