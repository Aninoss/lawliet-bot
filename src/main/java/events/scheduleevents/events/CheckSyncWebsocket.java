package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import core.MainLogger;
import core.Program;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventFixedRate;
import websockets.syncserver.SyncManager;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class CheckSyncWebsocket implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.productionMode() && !SyncManager.getInstance().getClient().isConnected()) {
            MainLogger.get().error("Sync websocket disconnected, attempting reconnect");
            SyncManager.getInstance().reconnect();
        }
    }

}