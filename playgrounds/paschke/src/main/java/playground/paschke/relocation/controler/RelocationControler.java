package playground.paschke.relocation.controler;

import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseTheCompanyExample;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.ChooseVehicleTypeExample;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
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

import playground.paschke.events.MobismBeforeSimStepRelocationListener;
import playground.paschke.events.SetupListener;
import playground.paschke.events.handlers.DemandDistributionHandler;
import playground.paschke.qsim.AverageDemandRelocationListener;
import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.CarsharingVehicleRelocationContainer;
import playground.paschke.qsim.FFVehiclesRentalsWriterListener;
import playground.paschke.qsim.KmlWriterListener;
import playground.paschke.qsim.RelocationQsimFactory;
import playground.paschke.utils.ExampleCarsharingUtils;


public class RelocationControler {
	public static void main(final String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);

		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(sc);

		installCarSharing(controler);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

	public static void installCarSharing(final Controler controler) {
		final Scenario scenario = controler.getScenario();
		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(scenario.getNetwork());

		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup) scenario.getConfig().getModule( CarsharingConfigGroup.GROUP_NAME );

		// this is necessary to populate the companies list
		reader.readFile(configGroup.getvehiclelocations());
		Set<String> carsharingCompanies = reader.getCompanies().keySet();

		MembershipReader membershipReader = new MembershipReader();
		membershipReader.readFile(configGroup.getmembership());
		final MembershipContainer memberships = membershipReader.getMembershipContainer();

		final CostsCalculatorContainer costsCalculatorContainer = ExampleCarsharingUtils.createCompanyCostsStructure(carsharingCompanies);

		final CarsharingVehicleRelocationContainer carsharingVehicleRelocation = new CarsharingVehicleRelocationContainer(scenario);

		final SetupListener setupListener = new SetupListener();
		final CarSharingDemandTracker demandTracker = new CarSharingDemandTracker();
		final CarsharingListener carsharingListener = new CarsharingListener();
		final KmlWriterListener relocationListener = new KmlWriterListener(configGroup.getStatsWriterFrequency());
		final FFVehiclesRentalsWriterListener vehicleRentalsWriterListener = new FFVehiclesRentalsWriterListener(configGroup.getStatsWriterFrequency());
		final CarsharingSupplyContainer carsharingSupplyContainer = new CarsharingSupplyContainer(scenario);
		carsharingSupplyContainer.populateSupply();
		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyExample();
		final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemand(scenario.getNetwork());
		final CarsharingManagerInterface carsharingManager = new CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
		final AverageDemandRelocationListener averageDemandRelocationListener = new AverageDemandRelocationListener();

		//===adding carsharing objects on supply and demand infrastructure ===
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);
				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
			    bind(CarsharingSupplyInterface.class).toInstance(carsharingSupplyContainer);
			    bind(CarsharingManagerInterface.class).toInstance(carsharingManager);
				bind(CarsharingVehicleRelocationContainer.class).toInstance(carsharingVehicleRelocation);
				bind(CarSharingDemandTracker.class).toInstance(demandTracker);
				bind(DemandHandler.class).asEagerSingleton();
				bind(DemandDistributionHandler.class).asEagerSingleton();
			}
		});

		//=== carsharing specific replanning strategies ===
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			}
		});

		//=== adding qsimfactory, controller listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( RelocationQsimFactory.class );
				//bindMobsim().toProvider(CarsharingQsimFactoryNew.class);
				// this is now the only MobsimListenerBinding. Should it be handled differently?
				this.addMobsimListenerBinding().to(MobismBeforeSimStepRelocationListener.class);
				addControlerListenerBinding().toInstance(carsharingListener);
				addControlerListenerBinding().toInstance(setupListener);
				addControlerListenerBinding().toInstance(demandTracker);
				addControlerListenerBinding().toInstance(relocationListener);
				addControlerListenerBinding().toInstance(vehicleRentalsWriterListener);
				addControlerListenerBinding().to(CarsharingManagerNew.class);
				addControlerListenerBinding().toInstance(averageDemandRelocationListener);
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
				addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
				addEventHandlerBinding().to(DemandHandler.class);
				addEventHandlerBinding().to(DemandDistributionHandler.class);
				addEventHandlerBinding().toInstance(averageDemandRelocationListener);
			}
		});

		//=== routing moduels for carsharing trips ===
		controler.addOverridingModule(CarsharingUtils.createRoutingModule());
	}
}
