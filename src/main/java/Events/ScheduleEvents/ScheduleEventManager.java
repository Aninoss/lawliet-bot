package Events.ScheduleEvents;

import Core.Utils.TimeUtil;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.function.Consumer;

public class ScheduleEventManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleEventManager.class);

    private boolean started = false;
    private final Reflections reflections = new Reflections("Events/ScheduleEvents");
    private final Timer timer = new Timer();

    public ScheduleEventManager() {}

    public void start() {
        if (started) return;
        started = true;

        processAnnotations(ScheduleEventFixedRate.class, this::attachFixedRate);
        processAnnotations(ScheduleEventHourly.class, this::attachHourly);
        processAnnotations(ScheduleEventDaily.class, this::attachDaily);
    }

    private <A extends Annotation> void processAnnotations(Class<A> annotationClass, Consumer<ScheduleEventInterface> action) {
        Set<Class<?>> annotations = reflections.getTypesAnnotatedWith(annotationClass);
        annotations.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOGGER.error("Error when creating listener class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof ScheduleEventInterface)
                .map(obj -> (ScheduleEventInterface) obj)
                .forEach(action);
    }

    private void attachFixedRate(ScheduleEventInterface listener) {
        ScheduleEventFixedRate fixedRateAnnotation = listener.getClass().getAnnotation(ScheduleEventFixedRate.class);
        if (fixedRateAnnotation != null) {
            long millis = Duration.of(fixedRateAnnotation.rateValue(), fixedRateAnnotation.rateUnit()).toMillis();
            timer.scheduleAtFixedRate(new ScheduleEventAdapter(listener), millis, millis);
        }
    }

    private void attachHourly(ScheduleEventInterface listener) {
        ScheduleEventHourly fixedRateHourly = listener.getClass().getAnnotation(ScheduleEventHourly.class);
        if (fixedRateHourly != null) {
            long millis = TimeUtil.getMilisBetweenInstants(Instant.now(), TimeUtil.setInstantToNextHour(Instant.now()));
            timer.scheduleAtFixedRate(new ScheduleEventAdapter(listener), millis, 60 * 60 * 1000);
        }
    }

    private void attachDaily(ScheduleEventInterface listener) {
        ScheduleEventDaily fixedRateDaily = listener.getClass().getAnnotation(ScheduleEventDaily.class);
        if (fixedRateDaily != null) {
            long millis = TimeUtil.getMilisBetweenInstants(Instant.now(), TimeUtil.setInstantToNextDay(Instant.now()));
            timer.scheduleAtFixedRate(new ScheduleEventAdapter(listener), millis, 24 * 60 * 60 * 1000);
        }
    }

}
