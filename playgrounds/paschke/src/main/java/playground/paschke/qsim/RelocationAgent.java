package playground.paschke.qsim;

import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.vehicles.Vehicle;

public class RelocationAgent implements MobsimDriverAgent {
	private Id<Person> id;
	private Guidance guidance;
	private Id<Link> currentLinkId;
	private MobsimVehicle vehicle;
	private MobsimTimer mobsimTimer;
	private Id<Link> destinationLinkId;
	private Scenario scenario;
	private Random rnd = new Random(4711) ;

	public RelocationAgent(Id<Person> id, Id<Link> startLinkId, Id<Link> destinationLinkId, Guidance guidance, MobsimTimer mobsimTimer, Scenario scenario) {
		this.id = id;
		this.currentLinkId = startLinkId ;
		this.destinationLinkId = destinationLinkId ;
		this.guidance = guidance ;
		this.mobsimTimer = mobsimTimer ;
		this.scenario = scenario ;
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
		return MobsimAgent.State.LEG ;
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
		throw new UnsupportedOperationException() ;
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
		this.currentLinkId = newLinkId ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.currentLinkId.equals( this.destinationLinkId ) ) {
			getRandomLink();
		}
		return false ;
	}

	private Id<Link> getRandomLink() {
		// if we are at the final destination, select a random new destination:
		Map<Id<Link>, ? extends Link> links = this.scenario.getNetwork().getLinks() ;
		int idx = rnd.nextInt(links.size()) ;
		int cnt = 0 ;
		for ( Link link : links.values() ) {
			if ( cnt== idx ) {
				return link.getId() ;
			}
		}
		throw new RuntimeException("should not happen");
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
		return null ;
	}
}
