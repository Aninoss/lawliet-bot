package modules.repair;

import net.dv8tion.jda.api.JDA;

public class MainRepair {

    private final static AutoChannelRepair autoChannelRepair = new AutoChannelRepair();
    private final static RolesRepair rolesRepair = new RolesRepair();

    public static void start(JDA jda, int minutes) {
        if (jda != null) {
            autoChannelRepair.start(jda);
            rolesRepair.start(jda, minutes);
        }
    }

}
