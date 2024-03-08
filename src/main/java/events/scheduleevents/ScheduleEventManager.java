package events.scheduleevents;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.Startable;
import core.schedule.ScheduleAdapter;
import core.utils.TimeUtil;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ScheduleEventManager extends Startable {

    private final int DELAY = 1000;

    private final Reflections reflections = new Reflections("events/scheduleevents");
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory(() -> "Main", "ScheduleEvent", true));

    public ScheduleEventManager() {
    }

    @Override
    protected void run() {
        processAnnotations(ScheduleEventFixedRate.class, this::attachFixedRate);
        processAnnotations(ScheduleEventEveryMinute.class, this::attachEveryMinute);
        processAnnotations(ScheduleEventHourly.class, this::attachHourly);
        processAnnotations(ScheduleEventDaily.class, this::attachDaily);
    }

    private <A extends Annotation> void processAnnotations(Class<A> annotationClass, Consumer<ExceptionRunnable> action) {
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
                .filter(obj -> obj instanceof ExceptionRunnable)
                .map(obj -> (ExceptionRunnable) obj)
                .forEach(action);
    }

    private void attachFixedRate(ExceptionRunnable listener) {
        ScheduleEventFixedRate fixedRateAnnotation = listener.getClass().getAnnotation(ScheduleEventFixedRate.class);
        if (fixedRateAnnotation != null) {
            long millis = Duration.of(fixedRateAnnotation.rateValue(), fixedRateAnnotation.rateUnit()).toMillis();
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), ThreadLocalRandom.current().nextLong(millis) + DELAY, millis, TimeUnit.MILLISECONDS);
        }
    }

    private void attachEveryMinute(ExceptionRunnable listener) {
        ScheduleEventEveryMinute fixedRateEveryMinute = listener.getClass().getAnnotation(ScheduleEventEveryMinute.class);
        if (fixedRateEveryMinute != null) {
            long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), TimeUtil.instantToNextMinute(Instant.now()));
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), millis + DELAY, 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void attachHourly(ExceptionRunnable listener) {
        ScheduleEventHourly fixedRateHourly = listener.getClass().getAnnotation(ScheduleEventHourly.class);
        if (fixedRateHourly != null) {
            long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), TimeUtil.instantToNextHour(Instant.now()));
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), millis + DELAY, 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void attachDaily(ExceptionRunnable listener) {
        ScheduleEventDaily fixedRateDaily = listener.getClass().getAnnotation(ScheduleEventDaily.class);
        if (fixedRateDaily != null) {
            long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), TimeUtil.setInstantToNextDay(Instant.now()));
            scheduledExecutorService.scheduleAtFixedRate(new ScheduleAdapter(listener), millis + DELAY, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        }
    }

}
