package playground.paschke.freefloating.controler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.paschke.events.MobismBeforeSimStepRelocationAgentsDispatcher;
import playground.paschke.events.RelocationAgentsDispatchListener;
import playground.paschke.events.RelocationAgentsInsertListener;
import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.CarSharingRelocationZones;
import playground.paschke.qsim.Guidance;
import playground.paschke.qsim.RelocationAgent;


public class FreeFloatingControler {
	public static void main(final String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(sc);

		CarSharingVehicles carSharingVehicles = null;

		try {
			carSharingVehicles = new CarSharingVehicles(sc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CarSharingRelocationZones relocationZones = null;

		try {
			relocationZones = new CarSharingRelocationZones(sc);
		} catch (IOException e) {
			e.printStackTrace();
		}

		CarSharingDemandTracker demandTracker = new CarSharingDemandTracker(controler);

		Map<Id<Person>, RelocationAgent> relocationAgents = new HashMap<Id<Person>, RelocationAgent>();

		// TODO: number of relocation agents should be configurable
		Id<Link> relocationAgentBaseLinkId = Id.createLinkId(1); 
		int counter = 0;
		while (counter < 5) {
			Id<Person> id = Id.createPersonId("DemonAgent" + counter);
			RelocationAgent agent = new RelocationAgent(id, relocationAgentBaseLinkId, sc, carSharingVehicles);
			relocationAgents.put(id, agent);

			counter++;
		}

		installCarSharing(controler, carSharingVehicles, relocationZones, demandTracker, relocationAgents);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

	public static void installCarSharing(final Controler controler, final CarSharingVehicles carSharingVehicles, final CarSharingRelocationZones relocationZones, final CarSharingDemandTracker demandTracker, final Map<Id<Person>, RelocationAgent> relocationAgents) {
		Scenario sc = controler.getScenario();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			     /*
			      * This tells Guice that whenever it sees a dependency on a CarSharingVehicles instance,
			      * it should satisfy the dependency using this static CarSharingVehicles.
			      */
				bind(CarSharingVehicles.class).toInstance(carSharingVehicles);
				bind(CarSharingRelocationZones.class).toInstance(relocationZones);
				bind(CarSharingDemandTracker.class).toInstance(demandTracker);
				bind(new TypeLiteral<Map<Id<Person>, RelocationAgent>>() {}).toInstance(relocationAgents);
				bindMobsim().toProvider( CarsharingQsimFactory.class );
				this.addMobsimListenerBinding().to(MobismBeforeSimStepRelocationAgentsDispatcher.class);
				this.addMobsimListenerBinding().to(RelocationAgentsInsertListener.class);
			}
		});

		controler.setTripRouterFactory(CarsharingUtils.createTripRouterFactory(sc));

		//setting up the scoring function factory, inside different scoring functions are set-up
		controler.setScoringFunctionFactory(new CarsharingScoringFunctionFactory( sc.getConfig(), sc.getNetwork()));

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);

		controler.addControlerListener(new CarsharingListener(controler, csConfig.getStatsWriterFrequency()));

		controler.addControlerListener(demandTracker);
	}
}
