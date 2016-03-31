package playground.paschke.freefloating.controler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.inject.TypeLiteral;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.paschke.events.MobismBeforeSimStepRelocationAgentsDispatcher;
import playground.paschke.events.RelocationAgentsInsertListener;
import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.CarSharingRelocationTimesReader;
import playground.paschke.qsim.CarSharingRelocationZonesReader;
import playground.paschke.qsim.RelocationAgent;
import playground.paschke.qsim.RelocationAgentsPlansWriterListener;
import playground.paschke.qsim.RelocationZoneKmlWriterListener;


public class FreeFloatingControler {
	public static void main(final String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		OneWayCarsharingConfigGroup configGroupOW = new OneWayCarsharingConfigGroup();
		config.addModule(configGroupOW);

		FreeFloatingConfigGroup configGroupFF = new FreeFloatingConfigGroup();
		config.addModule(configGroupFF);

		TwoWayCarsharingConfigGroup configGroupTW = new TwoWayCarsharingConfigGroup();
		config.addModule(configGroupTW);

		CarsharingConfigGroup configGroupAll = new CarsharingConfigGroup();
		config.addModule(configGroupAll);

		CarsharingVehicleRelocationConfigGroup configGroupCVR = new CarsharingVehicleRelocationConfigGroup();
		config.addModule(configGroupCVR);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		// load relocation zones
		final CarsharingVehicleRelocationConfigGroup confGroup = (CarsharingVehicleRelocationConfigGroup)
			config.getModule( CarsharingVehicleRelocationConfigGroup.GROUP_NAME );
		new CarSharingRelocationZonesReader(sc).parse(confGroup.getRelocationZones());

		// load relocation times
		new CarSharingRelocationTimesReader(sc).parse(confGroup.getRelocationTimes());

		final Controler controler = new Controler(sc);

		CarSharingVehicles carSharingVehicles = null;

		try {
			carSharingVehicles = new CarSharingVehicles(sc);
			carSharingVehicles.readVehicleLocations();
		} catch (IOException e) {
			e.printStackTrace();
		}

		CarSharingDemandTracker demandTracker = new CarSharingDemandTracker(controler);

		Map<Id<Person>, RelocationAgent> relocationAgents = new HashMap<Id<Person>, RelocationAgent>();

		// TODO: number of relocation agents should be configurable
		Id<Link> relocationAgentBaseLinkId = Id.createLinkId(150535); 
		int counter = 0;
		while (counter < 30) {
			Id<Person> id = Id.createPersonId("DemonAgent" + counter);
			RelocationAgent agent = new RelocationAgent(id, relocationAgentBaseLinkId, sc, carSharingVehicles);
			relocationAgents.put(id, agent);

			counter++;
		}

		installCarSharing(controler, carSharingVehicles, demandTracker, relocationAgents);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

	public static void installCarSharing(final Controler controler, final CarSharingVehicles carSharingVehicles, final CarSharingDemandTracker demandTracker, final Map<Id<Person>, RelocationAgent> relocationAgents) {
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
				bind(CarSharingDemandTracker.class).toInstance(demandTracker);
				bind(new TypeLiteral<Map<Id<Person>, RelocationAgent>>() {}).toInstance(relocationAgents);
				bindMobsim().toProvider( CarsharingQsimFactory.class );
				this.addMobsimListenerBinding().to(MobismBeforeSimStepRelocationAgentsDispatcher.class);
				this.addMobsimListenerBinding().to(RelocationAgentsInsertListener.class);

				// 2016-03-17 binding this class to avoid calling setScoringFunctionFactory because CarsharingScoringFunctionFactory cannot be instanciated. Will this work?
				bind(ScoringFunctionFactory.class).to(CarsharingScoringFunctionFactory.class);
			}
		});

		controler.addOverridingModule(CarsharingUtils.createModule());

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);

		controler.addControlerListener(new CarsharingListener(controler, csConfig.getStatsWriterFrequency()));

		controler.addControlerListener(demandTracker);
		controler.addControlerListener(new RelocationZoneKmlWriterListener(controler, csConfig.getStatsWriterFrequency()));
		controler.addControlerListener(new RelocationAgentsPlansWriterListener(controler, relocationAgents));
	}
}
