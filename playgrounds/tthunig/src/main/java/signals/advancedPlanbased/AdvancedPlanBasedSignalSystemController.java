package signals.advancedPlanbased;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.*;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.Analyzable;
import signals.sensor.LinkSensorManager;

/**
 * Created by nkuehnel on 05.04.2017.
 */
public class AdvancedPlanBasedSignalSystemController implements SignalController, Analyzable {

    private final LinkSensorManager sensorManager;
    private final DefaultPlanbasedSignalSystemController delegate = new DefaultPlanbasedSignalSystemController();
    protected SignalSystem system;
    private final TtTotalDelay delayCalculator;
    public static final String IDENTIFIER = "AdvancedPlanBasedSignalSystemController";


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LinkSensorManager sensorManager;
        private final TtTotalDelay delayCalculator;

        public SignalControlProvider(LinkSensorManager sensorManager, TtTotalDelay delayCalculator) {
            this.sensorManager = sensorManager;
            this.delayCalculator = delayCalculator;
        }

        @Override
        public SignalController get() {
            return new AdvancedPlanBasedSignalSystemController(sensorManager, delayCalculator);
        }
    }


    public AdvancedPlanBasedSignalSystemController(LinkSensorManager sensorManager, TtTotalDelay delayCalculator) {
        this.sensorManager = sensorManager;
        this.delayCalculator = delayCalculator;
    }

    @Override
    public void updateState(double timeSeconds) {
        delegate.updateState(timeSeconds);
    }

    @Override
    public void addPlan(SignalPlan plan) {
        delegate.addPlan(plan);
    }

    @Override
    public void reset(Integer iterationNumber) {
        delegate.reset(iterationNumber);
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        delegate.simulationInitialized(simStartTimeSeconds);
        for (Signal signal : system.getSignals().values()) {
            Id<Link> linkId = signal.getLinkId();
            this.sensorManager.registerNumberOfCarsInDistanceMonitoring(linkId, 0.);
        }
    }

    @Override
    public void setSignalSystem(SignalSystem signalSystem) {
        delegate.setSignalSystem(signalSystem);
        this.system = signalSystem;
    }

    @Override
    public String getStatFields() {
        StringBuilder builder = new StringBuilder();
        builder.append("total_delay;");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            builder.append("state_group_" + group.getId() + ";");
        }
        for (Signal signal : system.getSignals().values()) {
            builder.append("n_signal_" + signal.getId() + ";");
        }
        return builder.toString();
    }

    @Override
    public String getStepStats(double now) {
        StringBuilder builder = new StringBuilder();
        builder.append(delayCalculator.getTotalDelay() + ";");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            if(group.getState() != null) {
                builder.append(group.getState().name() + ";");
            } else {
                builder.append("null;");
            }
        }
        for (Signal signal : system.getSignals().values()) {
            builder.append(sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), 0., now) + ";");
        }
        return builder.toString();
    }

    @Override
    public boolean analysisEnabled() {
        return true;
    }
}
