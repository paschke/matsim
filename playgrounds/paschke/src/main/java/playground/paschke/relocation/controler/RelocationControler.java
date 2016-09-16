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

import com.google.common.collect.ImmutableSet;

import playground.paschke.events.MobismBeforeSimStepRelocationListener;
import playground.paschke.events.SetupListener;
import playground.paschke.qsim.CarSharingDemandTracker;
import playground.paschke.qsim.CarsharingVehicleRelocation;
import playground.paschke.qsim.RelocationQsimFactory;
import playground.paschke.qsim.RelocationListener;


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
		Set<String> carsharingCompanies = ImmutableSet.of("Catchacar");

		MembershipReader membershipReader = new MembershipReader();
		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup)
				controler.getScenario().getConfig().getModule( CarsharingConfigGroup.GROUP_NAME );
		membershipReader.readFile(configGroup.getmembership());

		final MembershipContainer memberships = membershipReader.getMembershipContainer();

		final CostsCalculatorContainer costsCalculatorContainer = CarsharingUtils.createCompanyCostsStructure(carsharingCompanies);

		//CarSharingVehicles carSharingVehicles = new CarSharingVehicles(controler.getScenario());
		CarsharingVehicleRelocation carsharingVehicleRelocation = new CarsharingVehicleRelocation(controler.getScenario());

		final SetupListener setupListener = new SetupListener();
		final CarSharingDemandTracker demandTracker = new CarSharingDemandTracker();
		final DemandHandler demandHandler = new DemandHandler();
		final CarsharingListener carsharingListener = new CarsharingListener();
		// this probably replaces CarSharingVehicles?
		final CarsharingSupplyContainer carsharingSupplyContainer = new CarsharingSupplyContainer(controler.getScenario());
		carsharingSupplyContainer.populateSupply();
		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyExample();
		final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemand(controler.getScenario().getNetwork());
		final CarsharingManagerInterface carsharingManager = new CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);
				bind(RouteCarsharingTripImpl.class).asEagerSingleton();
				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
			    bind(CarsharingSupplyInterface.class).toInstance(carsharingSupplyContainer);
			    bind(CarsharingManagerInterface.class).toInstance(carsharingManager);
				bind(CarsharingVehicleRelocation.class).toInstance(carsharingVehicleRelocation);
				bind(CarSharingDemandTracker.class).toInstance(demandTracker);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( RelocationQsimFactory.class );
				//bindMobsim().toProvider(CarsharingQsimFactoryNew.class);
				// this is now the only MobsimListenerBinding. Should it be handled differently?
				this.addMobsimListenerBinding().to(MobismBeforeSimStepRelocationListener.class);
		        addControlerListenerBinding().toInstance(carsharingListener);
		        addControlerListenerBinding().to(CarsharingManagerNew.class);
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
		        bind(DemandHandler.class).toInstance(demandHandler);
		        addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
		        addEventHandlerBinding().toInstance(demandHandler);
			}
		});

		controler.addOverridingModule(CarsharingUtils.createRoutingModule());

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);

		controler.addControlerListener(setupListener);
		controler.addControlerListener(demandTracker);
		controler.addControlerListener(new RelocationListener(csConfig.getStatsWriterFrequency()));
	}
}
