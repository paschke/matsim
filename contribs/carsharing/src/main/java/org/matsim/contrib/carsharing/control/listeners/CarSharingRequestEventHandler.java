package org.matsim.contrib.carsharing.control.listeners;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.events.CarSharingRequestEvent;
import org.matsim.contrib.carsharing.events.handlers.CarSharingRequestEventHandlerInterface;

public class CarSharingRequestEventHandler implements CarSharingRequestEventHandlerInterface{

	ArrayList<CarSharingRequestInfo> carSharingRequest = new ArrayList<CarSharingRequestInfo>();

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		carSharingRequest = new ArrayList<CarSharingRequestInfo>();
	}

	@Override
	public void handleEvent(CarSharingRequestEvent event) {
		CarSharingRequestInfo info = new CarSharingRequestInfo();
		info.time = event.getTime();
		info.linkId = event.getLinkId();
		info.type = event.getCarsharingType();
		carSharingRequest.add(info);
	}

	public ArrayList<CarSharingRequestInfo> info() {
		return this.carSharingRequest;
	}

	public class CarSharingRequestInfo {
		double time;
		Id<Link> linkId = null;
		String type = null;

		public String toString() {
			return time + " " + linkId.toString() + " " + type;
		}
	}
}
