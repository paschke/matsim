package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.CompanyContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * Mostly copied from tutorial.programming.ownMobsimAgentUsingRouter.MyMobsimAgent
 * 
 * MobsimDriverAgent that
 * - gets a person Id, start and destination link Id
 * - computes best path to it at every turn, using a Guidance instance
 */

public class RelocationAgent implements MobsimDriverAgent {
	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private Id<Person> id;
	private String companyId;
	private Guidance guidance;
	private MobsimTimer mobsimTimer;
	private Scenario scenario;

	private CarsharingSupplyInterface carsharingSupply;
	// RelocationAgent needs CompanyContainer and Company

	private Id<Link> homeLinkId;
	private Id<Link> currentLinkId;
	private Id<Link> destinationLinkId;
	private String transportMode;
	private MobsimVehicle vehicle;
	private State state;

	private ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();

	private ArrayList<PlanElement> planElements = new ArrayList<PlanElement>();

	public RelocationAgent(Id<Person> id, String companyId, Id<Link> homeLinkId, Scenario scenario) {
		this.id = id;
		this.companyId = companyId;
		this.currentLinkId = this.homeLinkId = homeLinkId;
		this.scenario = scenario;

		this.state = State.ACTIVITY;
	}

	public void setCarsharingSupplyContainer(CarsharingSupplyInterface carsharingSupply) {
		this.carsharingSupply = carsharingSupply;
	}

	public void setGuidance(Guidance guidance) {
		this.guidance = guidance;
	}

	public ArrayList<RelocationInfo> getRelocations() {
		return this.relocations;
	}

	public ArrayList<PlanElement> getPlanElements()
	{
		return this.planElements;
	}

    /**
     * - add RelocationInfo to relocations
     * - reserve car sharing vehicle, aka remove it from storage structure
     */
	public void dispatchRelocation(RelocationInfo info) {
		CompanyContainer companyContainer = this.carsharingSupply.getCompany(this.companyId);		
		CSVehicle vehicle = this.carsharingSupply.getVehicleWithId(info.getVehicleId());
		if (true == companyContainer.reserveVehicle(vehicle)) {
			this.relocations.add(info);

			log.info("relocationAgent " + this.id + " removed vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId());
		}

		log.info("relocationAgent " + this.id + " could not remove vehicle " + info.getVehicleId() + " from link " + info.getStartLinkId());
	}

	public void setMobsimTimer(MobsimTimer mobsimTimer) {
		this.mobsimTimer = mobsimTimer;
	}

	public MobsimTimer getMobsimTimer() {
		return this.mobsimTimer;
	}

	public double getTimeOfDay() {
		if (this.getMobsimTimer() != null) {
			return this.getMobsimTimer().getTimeOfDay();
		}

		return 0;
	}

	public void reset() {
		this.relocations.clear();
		this.planElements.clear();
		this.currentLinkId = this.homeLinkId;
		this.state = State.ACTIVITY;

		log.info("resetting agent " + this.getId());
	}

	protected void startLeg(String transportMode) {
		Leg leg = PopulationUtils.createLeg(transportMode);
		leg.setDepartureTime(this.getTimeOfDay());
		leg.setRoute(new LinkNetworkRouteImpl(this.getCurrentLinkId(), this.getDestinationLinkId()));
		this.planElements.add(leg);
	}

	protected void endLeg() {
		try {
			Leg leg = (Leg) this.getCurrentPlanElement();
			leg.setTravelTime(this.getTimeOfDay() - leg.getDepartureTime());
		} catch (Exception e) {
			// do nothing
		}
	}

	protected void addLinkId(Id<Link> linkId) {
		try {
			Leg leg = (Leg) this.getCurrentPlanElement();
			LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

			List<Id<Link>> linkIds = new ArrayList<Id<Link>>(route.getLinkIds());
			linkIds.add(linkId);
			LinkNetworkRouteImpl newRoute = new LinkNetworkRouteImpl(route.getStartLinkId(), linkIds, route.getEndLinkId());

			leg.setRoute(newRoute);
		} catch (Exception e) {
			// do nothing
		}
	}

	protected void startActivity() {
		Activity activity = PopulationUtils. createActivityFromLinkId("work", this.getCurrentLinkId());
		activity.setStartTime(this.getTimeOfDay());
		this.planElements.add(activity);
	}

	protected void endActivity() {
		try {
			Activity activity = (Activity) this.getCurrentPlanElement();
			activity.setEndTime(this.getTimeOfDay());
		} catch (Exception e) {
			// do nothing
		}
	}

	protected PlanElement getCurrentPlanElement()
	{
		return this.planElements.get(this.planElements.size() - 1);
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId ;
	}

	@Override
	public Id<Person> getId() {
		return this.id ;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public double getActivityEndTime() {
		double now = this.getTimeOfDay();

		if (this.relocations.isEmpty() == false) {
			return now;
		} else if (now < (21600 + 1)) {
			return (21600 + 1);
		} else if (now < (64800 + 1)) {
			// TODO: "before 6pm, check back in 3 hours", hard coded. Make this configurable.
			double endTime = (now + 10800 - (now % 10800));
			return endTime + 1;
		} else {
			return Double.POSITIVE_INFINITY;
		}
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		try {
			this.endActivity();
			this.prepareRelocation(this.relocations.get(0));
		} catch (IndexOutOfBoundsException e) {
			// do nothing, assuming that the only activity that this user can have is idling at his home link
		}
	}

	private void prepareRelocation(RelocationInfo relocationInfo) {
		this.destinationLinkId = relocationInfo.getStartLinkId();
		this.transportMode = TransportMode.bike;
		this.state = State.LEG;
		this.startLeg(TransportMode.bike);
	}

	private void executeRelocation(RelocationInfo relocationInfo) {
		this.destinationLinkId = relocationInfo.getEndLinkId();
		this.transportMode = TransportMode.car;
		// TODO: set vehicle
		this.startLeg(TransportMode.car);
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		this.endLeg();

		if (this.relocations.isEmpty()) {
			this.state = State.ACTIVITY;
			this.startActivity();
		} else {
			if (this.getDestinationLinkId().equals(this.relocations.get(0).getStartLinkId())) {
				RelocationInfo relocationInfo = this.relocations.get(0);
				relocationInfo.setStartTime(now);
				this.executeRelocation(relocationInfo);
			} else if (this.getDestinationLinkId().equals(this.relocations.get(0).getEndLinkId())) {
				this.deliverCarSharingVehicle();

				try {
					RelocationInfo relocationInfo = this.relocations.get(0);
					relocationInfo.setEndTime(now);
					this.relocations.remove(0); 
					this.prepareRelocation(this.relocations.get(0));
				} catch (IndexOutOfBoundsException e) {
					this.destinationLinkId = this.homeLinkId;
					this.transportMode = TransportMode.bike;
					this.startLeg(TransportMode.bike);
				}
			}
		}
	}

	@Override
	public void setStateToAbort(double now) {
		this.state = State.ABORT;
	}

	@Override
	public Double getExpectedTravelTime() {
		return this.guidance.getExpectedTravelTime(
				this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()),
				this.scenario.getNetwork().getLinks().get(this.getDestinationLinkId()),
				this.getTimeOfDay(),
				this.transportMode,
				null
		);
	}

    @Override
    public Double getExpectedTravelDistance() {
		return this.guidance.getExpectedTravelDistance(
				this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()),
				this.scenario.getNetwork().getLinks().get(this.getDestinationLinkId()),
				this.getTimeOfDay(),
				this.transportMode,
				null
		);
    }

    @Override
	public String getMode() {
		return this.transportMode;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		this.currentLinkId = linkId;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.guidance.getBestOutgoingLink(
				this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()),
				this.scenario.getNetwork().getLinks().get(this.getDestinationLinkId()),
				this.getTimeOfDay()
		);
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		this.addLinkId(newLinkId);
		this.currentLinkId = newLinkId ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.getCurrentLinkId().equals( this.getDestinationLinkId() ) ) {
			return true;
		}

		return false ;
	}

	private void deliverCarSharingVehicle() {
		CompanyContainer companyContainer = this.carsharingSupply.getCompany(this.companyId);		
		CSVehicle vehicle = this.carsharingSupply.getVehicleWithId(this.relocations.get(0).getVehicleId());
		companyContainer.parkVehicle(vehicle, this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()));
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return (this.relocations.get(0) != null) ? Id.create(this.relocations.get(0).getVehicleId(), Vehicle.class) : null;
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		return null;
	}
}
