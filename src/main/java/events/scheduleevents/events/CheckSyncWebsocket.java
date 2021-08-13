package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import core.MainLogger;
import core.Program;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventFixedRate;
import websockets.syncserver.SendEvent;
import websockets.syncserver.SyncManager;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class CheckSyncWebsocket implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.productionMode() && !isConnected()) {
            MainLogger.get().error("Sync websocket disconnected, attempting reconnect");
            SyncManager.getInstance().reconnect();
        }
    }

    private boolean isConnected() {
        if (!SyncManager.getInstance().getClient().isConnected()) {
            return false;
        }

        try {
            SendEvent.sendEmpty("PING").get(1, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            //ignore
            return false;
        }
    }

}