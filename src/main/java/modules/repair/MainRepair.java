package modules.repair;

import org.javacord.api.DiscordApi;

public class MainRepair {

    public static void start(DiscordApi api, int hours) {
        if (api != null) {
            AutoChannelRepair.getInstance().start(api);
            RolesRepair.getInstance().start(api, hours);
        }
    }

}
