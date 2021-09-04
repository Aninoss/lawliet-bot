package events.scheduleevents;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import core.MainLogger;
import core.Startable;
import core.schedule.ScheduleAdapter;
import core.schedule.ScheduleInterface;
import core.utils.TimeUtil;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;
import org.reflections.Reflections;

public class ScheduleEventManager extends Startable {

    private final int DELAY = 1000;

    private final Reflections reflections = new Reflections("events/scheduleevents");
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3, new CountingThreadFactory(() -> "Main", "ScheduleEvent", true));

    public ScheduleEventManager() {
    }

    @Override
    protected void run() {
        processAnnotations(ScheduleEventFixedRate.class, this::attachFixedRate);
        processAnnotations(ScheduleEventHourly.class, this::attachHourly);
        processAnnotations(ScheduleEventDaily.class, this::attachDaily);
    }

    private <A extends Annotation> void processAnnotations(Class<A> annotationClass, Consumer<ScheduleInterface> action) {
        Set<Class<?>> annotations = reflections.getTypesAnnotatedWith(annotationClass);
        annotations.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        MainLogger.get().error("Error when creating listener class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof ScheduleInterface)
                .map(obj -> (ScheduleInterface) obj)
                .forEach(action);
    }

    private void attachFixedRate(ScheduleInterface listener) {
        ScheduleEventFixedRate fixedRateAnnotation = listener.getClass().getAnnotation(ScheduleEventFixedRate.class);
        if (fixedRateAnnotation != null) {
            long millis = Duration.of(fixedRateAnnotation.rateValue(), fixedRateAnnotation.rateUnit()).toMillis();
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), millis + DELAY, millis, TimeUnit.MILLISECONDS);
        }
    }

    private void attachHourly(ScheduleInterface listener) {
        ScheduleEventHourly fixedRateHourly = listener.getClass().getAnnotation(ScheduleEventHourly.class);
        if (fixedRateHourly != null) {
            long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), TimeUtil.instantToNextHour(Instant.now()));
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), millis + DELAY, 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void attachDaily(ScheduleInterface listener) {
        ScheduleEventDaily fixedRateDaily = listener.getClass().getAnnotation(ScheduleEventDaily.class);
        if (fixedRateDaily != null) {
            long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), TimeUtil.setInstantToNextDay(Instant.now()));
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), millis + DELAY, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }

}
