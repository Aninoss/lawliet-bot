package core;

import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import websockets.syncserver.SendEvent;

public class CustomSessionController extends ConcurrentSessionController {

    private static final CustomSessionController ourInstance = new CustomSessionController();

    public static CustomSessionController getInstance() {
        return ourInstance;
    }

    private CustomSessionController() {
    }

    @Override
    public long getGlobalRatelimit() {
        return super.getGlobalRatelimit();
    }

    public void loadGlobalRatelimit(long ratelimit) {
        super.setGlobalRatelimit(ratelimit);
    }

    @Override
    public void setGlobalRatelimit(long ratelimit) {
        SendEvent.sendRequestSyncedRatelimit(ratelimit).exceptionally(ExceptionLogger.get());
        loadGlobalRatelimit(ratelimit);
    }

}
