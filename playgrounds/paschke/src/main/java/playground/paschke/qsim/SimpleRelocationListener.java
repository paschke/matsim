package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

import playground.paschke.events.DispatchRelocationsEvent;
import playground.paschke.events.handlers.DispatchRelocationsEventHandler;

public class SimpleRelocationListener implements DispatchRelocationsEventHandler {
	@Inject private CarsharingVehicleRelocationContainer carsharingVehicleRelocation;

	private int iteration;

	public static final Logger log = Logger.getLogger("dummy");

	@Override
	public void handleEvent(DispatchRelocationsEvent event) {
		String timeSlot = Time.writeTime(event.getStart()) + " - " + Time.writeTime(event.getEnd());
		String companyId = event.getCompanyId();

		for (RelocationInfo info : this.calculateRelocations(timeSlot, companyId, this.carsharingVehicleRelocation.getRelocationZones(companyId))) {
			this.carsharingVehicleRelocation.addRelocation(companyId, info);
			log.info("SimpleRelocationListener suggests we move vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId() + " to " + info.getEndLinkId());

			if (this.iteration > this.carsharingVehicleRelocation.moduleEnableAfterIteration()) {
				RelocationAgent agent = this.getRelocationAgent(companyId);

				if (agent != null) {
					info.setAgentId(agent.getId());
					agent.dispatchRelocation(info);
				}
			}
		}
	}

	private RelocationAgent getRelocationAgent(String companyId) {
		Map<Id<Person>, RelocationAgent> relocationAgents = this.carsharingVehicleRelocation.getRelocationAgents(companyId);
		Iterator<Entry<Id<Person>, RelocationAgent>> agentIterator = relocationAgents.entrySet().iterator();

		while (agentIterator.hasNext()) {
			Entry<Id<Person>, RelocationAgent> agentEntry = agentIterator.next();
			RelocationAgent agent = agentEntry.getValue();

			if (agent.getRelocations().isEmpty()) {
				return agent;
			}
		}

		return null;
	}

	@Override
	public void reset(int iteration) {
		this.iteration = iteration;
	}

	public ArrayList<RelocationInfo> calculateRelocations(String timeSlot, String companyId, List<RelocationZone> relocationZones) {
		ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

		Collections.sort(relocationZones, new Comparator<RelocationZone>() {

			@Override
			public int compare(RelocationZone o1, RelocationZone o2) {
				if (o1.getNumberOfSurplusVehicles() < o2.getNumberOfSurplusVehicles()) {
					return -1;
				} else if (o1.getNumberOfSurplusVehicles() > o2.getNumberOfSurplusVehicles()) {
					return 1;
				} else {
					return o1.getId().toString().compareTo(o2.getId().toString());
				}
			}
		});

		int evenIndex = 0;
		for (ListIterator<RelocationZone> iterator = relocationZones.listIterator(); iterator.hasNext();) {
			RelocationZone nextZone = iterator.next();

			if (nextZone.getNumberOfSurplusVehicles() <= 0) {
				evenIndex = iterator.previousIndex();
			} else {
				break;
			}
		}

		List<RelocationZone> surplusZones = relocationZones.subList(evenIndex, (relocationZones.size() - 1));
		Collections.reverse(surplusZones);

		for (ListIterator<RelocationZone> iterator = relocationZones.listIterator(); iterator.hasNext();) {
			RelocationZone nextZone = (RelocationZone) iterator.next();

			if (nextZone.getNumberOfSurplusVehicles() < -1) {
				log.info("relocationZone " + nextZone.getId().toString() + " with " + nextZone.getNumberOfSurplusVehicles() + " surplus vehicles");

				for (int i = 0; i < Math.abs(nextZone.getNumberOfSurplusVehicles()); i++) {
/*					log.info("counting down surplus vehicles: " + i);
					Link fromLink = null;
					Link toLink = (Link) ((Set<Link>) nextZone.getNumberOfExpectedRequests().keySet()).iterator().next();
					String surplusZoneId = null;
					String vehicleId = null;

					Iterator<RelocationZone> surplusZonesIterator = surplusZones.iterator();
					while (surplusZonesIterator.hasNext()) {
						RelocationZone surplusZone = surplusZonesIterator.next();

						if (surplusZone.getNumberOfSurplusVehicles(1.1) > 0) {
							surplusZoneId = surplusZone.getId().toString();
							Iterator<Link> links = surplusZone.getVehicles().keySet().iterator();
							if (links.hasNext()) {
								fromLink = links.next();
								ArrayList<String> vehicleIds = surplusZone.getVehicles().get(fromLink);
								vehicleId = vehicleIds.get(0);
								surplusZone.removeVehicles(fromLink, new ArrayList<String>(Arrays.asList(new String[]{vehicleId})));

								break;
							}
						}
					}

					if ((fromLink != null) && (vehicleId != null)) {
						relocations.add(new RelocationInfo(timeSlot, companyId, vehicleId, fromLink.getId(), toLink.getId(), surplusZoneId, nextZone.getId().toString()));
					}*/
				}
			} else {
				break;
			}
		}

		return relocations;
	}
}
