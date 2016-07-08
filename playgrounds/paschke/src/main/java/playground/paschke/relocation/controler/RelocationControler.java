package playground.paschke.relocation.controler;

import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
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
import playground.paschke.events.MobismBeforeSimStepRelocationAgentsDispatcher;
import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.RelocationAgentsReader;
import playground.paschke.qsim.RelocationTimesReader;
import playground.paschke.qsim.RelocationZonesReader;
import playground.paschke.qsim.RelocationQsimFactory;
import playground.paschke.qsim.RelocationListener;


public class RelocationControler {
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
		new RelocationZonesReader(sc).parse(confGroup.getRelocationZones());

		// load relocation times
		new RelocationTimesReader(sc).parse(confGroup.getRelocationTimes());

		// load relocation agents
		new RelocationAgentsReader(sc).parse(confGroup.getRelocationAgents());

		final Controler controler = new Controler(sc);

		CarSharingVehicles carSharingVehicles = null;

		try {
			carSharingVehicles = new CarSharingVehicles(sc);
		} catch (IOException e) {
			e.printStackTrace();
		}

		CarSharingDemandTracker demandTracker = new CarSharingDemandTracker(controler);

		installCarSharing(controler, carSharingVehicles, demandTracker);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

	public static void installCarSharing(final Controler controler, final CarSharingVehicles carSharingVehicles, final CarSharingDemandTracker demandTracker) {
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
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( RelocationQsimFactory.class );

				this.addMobsimListenerBinding().to(MobismBeforeSimStepRelocationAgentsDispatcher.class);
				//setting up the scoring function factory, inside different scoring functions are set-up
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
			}
		});

		controler.addOverridingModule(CarsharingUtils.createModule());

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);

		controler.addControlerListener(new CarsharingListener(controler, csConfig.getStatsWriterFrequency()));

		controler.addControlerListener(demandTracker);
		controler.addControlerListener(new RelocationListener(controler, csConfig.getStatsWriterFrequency()));
	}
}
