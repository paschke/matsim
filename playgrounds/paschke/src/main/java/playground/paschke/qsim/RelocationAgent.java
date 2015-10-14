package playground.paschke.qsim;

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

	private String vehicleId;
	private Id<Link> currentLinkId;
	private Id<Link> destinationLinkId;
	private MobsimVehicle vehicle;
	private State state;

	public RelocationAgent(Id<Person> id, Guidance guidance, MobsimTimer mobsimTimer, Scenario scenario, CarSharingVehicles carSharingVehicles) {
		this.id = id;
		this.guidance = guidance;
		this.mobsimTimer = mobsimTimer;
		this.scenario = scenario;
		this.carSharingVehicles = carSharingVehicles;
	}

	public void dispatch(String vehicleId, Id<Link> startLinkId, Id<Link> destinationLinkId) {
		this.vehicleId = vehicleId;
		this.currentLinkId = startLinkId;
		this.destinationLinkId = destinationLinkId;

		this.carSharingVehicles.getFreeFLoatingVehicles().removeVehicle(this.scenario.getNetwork().getLinks().get(startLinkId), vehicleId);
		this.state = State.LEG;
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
		return Double.POSITIVE_INFINITY ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		throw new UnsupportedOperationException() ;
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
		return this.guidance.getBestOutgoingLink( this.currentLinkId, this.destinationLinkId, this.mobsimTimer.getTimeOfDay()  ) ;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		log.info("agent " + this.id + " moving over link " + newLinkId);
		this.currentLinkId = newLinkId ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.currentLinkId.equals( this.destinationLinkId ) ) {
			deliverCarSharingVehicle();
			setStateToAbort(this.mobsimTimer.getTimeOfDay());
		}

		return false ;
	}

	private void deliverCarSharingVehicle() {
		this.carSharingVehicles.getFreeFLoatingVehicles().addVehicle(this.scenario.getNetwork().getLinks().get(this.getCurrentLinkId()), this.vehicleId);
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
		return Id.create(this.vehicleId, Vehicle.class);
	}
}
