package playground.paschke.freefloating.controler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.paschke.events.BeforeMobsimEventHandler;
import playground.paschke.events.RelocationAgentsDispatchListener;


public class FreeFloatingControler {
	public static void main(final String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(sc);

		installCarSharing(controler);

		controler.run();
	}

	public static void installCarSharing(final Controler controler) {
		Scenario sc = controler.getScenario() ;

		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( CarsharingQsimFactory.class );
			}
		});

		controler.setTripRouterFactory(CarsharingUtils.createTripRouterFactory(sc));

		//setting up the scoring function factory, inside different scoring functions are set-up
		controler.setScoringFunctionFactory(new CarsharingScoringFunctionFactory( sc.getConfig(), sc.getNetwork()));

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);
		controler.addControlerListener(new CarsharingListener(controler,
				csConfig.getStatsWriterFrequency() ) );

		controler.addControlerListener(new RelocationAgentsDispatchListener(controler));

		// adding an EventHandler to which the RelocationAgentsDispatchListener will register (but how??)
		controler.getEvents().addHandler(new BeforeMobsimEventHandler() {
			@Override
			public void handleEvent(BeforeMobsimEvent event) {
				// some;
			}

			@Override
			public void reset(int iteration) {

			}
		});
	}
}
