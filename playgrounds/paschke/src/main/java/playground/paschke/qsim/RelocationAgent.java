package playground.paschke.qsim;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
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
	private Guidance guidance;
	private MobsimTimer mobsimTimer;
	private Scenario scenario;
	private CarSharingVehicles carSharingVehicles;

	private Id<Link> homeLinkId;
	private Id<Link> currentLinkId;
	private Id<Link> destinationLinkId;
	private String transportMode;
	private MobsimVehicle vehicle;
	private State state;

	private ArrayList<RelocationInfo> relocations = new ArrayList<RelocationInfo>();
	// TODO: is this needed or could we just use the first position of relocations?
	private RelocationInfo currentRelocation;

	public RelocationAgent(Id<Person> id, Id<Link> homeLinkId, Scenario scenario, CarSharingVehicles carSharingVehicles) {
		this.id = id;
		this.currentLinkId = this.homeLinkId = homeLinkId;
		this.scenario = scenario;
		this.carSharingVehicles = carSharingVehicles;

		this.state = State.ACTIVITY;
	}

	public void setGuidance(Guidance guidance) {
		this.guidance = guidance;
	}

    /**
     * - add RelocationInfo to relocations
     * - reserve car sharing vehicle, aka remove it from storage structure
     */
	public void dispatchRelocation(RelocationInfo info) {
		this.relocations.add(info);
		this.carSharingVehicles.getFreeFLoatingVehicles().removeVehicle(this.scenario.getNetwork().getLinks().get(info.getStartLinkId()), info.getVehicleId());
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
		this.currentLinkId = this.homeLinkId;
		this.currentRelocation = null;
		this.state = State.ACTIVITY;
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
		if (this.relocations.isEmpty() == false) {
			return this.getTimeOfDay();
		}

		return Double.POSITIVE_INFINITY ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		try {
			RelocationInfo relocationInfo = this.relocations.remove(0);
			this.currentRelocation = relocationInfo;
			this.prepareRelocation(relocationInfo);
		} catch (IndexOutOfBoundsException e) {
			// do nothing
		}
	}

	private void prepareRelocation(RelocationInfo relocationInfo) {
		this.destinationLinkId = relocationInfo.getDestinationLinkId();
		this.transportMode = TransportMode.bike;
		this.state = State.LEG;
	}

	private void executeRelocation(RelocationInfo relocationInfo) {
		this.destinationLinkId = relocationInfo.getDestinationLinkId();
		this.transportMode = TransportMode.car;
		// TODO: set vehicle
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		if (this.currentRelocation == null) {
			this.state = State.ACTIVITY;
		} else {
			if (this.getCurrentLinkId().equals(this.currentRelocation.getStartLinkId())) {
				this.executeRelocation(this.currentRelocation);
			} else if (this.getCurrentLinkId().equals(this.currentRelocation.getDestinationLinkId())) {
				this.deliverCarSharingVehicle();

				try {
					RelocationInfo relocationInfo = this.relocations.remove(0); 
					this.currentRelocation = relocationInfo;
					this.prepareRelocation(relocationInfo);
				} catch (IndexOutOfBoundsException e) {
					this.currentRelocation = null;
					this.destinationLinkId = this.homeLinkId;
					this.transportMode = TransportMode.bike;
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
		return null ;
	}

    @Override
    public Double getExpectedTravelDistance() {
        return null;
    }

    @Override
	public String getMode() {
		return TransportMode.car ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.guidance.getBestOutgoingLink(this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()), this.scenario.getNetwork().getLinks().get(this.getDestinationLinkId()), this.getTimeOfDay()  ) ;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		log.info("agent " + this.id + " moving over link " + newLinkId);
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
		this.carSharingVehicles.getFreeFLoatingVehicles().addVehicle(this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()), this.currentRelocation.getVehicleId());
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
		return (this.currentRelocation != null) ? Id.create("FF_" + this.currentRelocation.getVehicleId(), Vehicle.class) : null;
	}
}
