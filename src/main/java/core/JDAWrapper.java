package core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class JDAWrapper {

    private final JDA jda;
    private boolean alive = false;
    private boolean active = true;
    private int errors = 0;
    private String previousActivityText = "";

    public JDAWrapper(JDA jda) {
        this.jda = jda;
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onGenericEvent(@NotNull GenericEvent event) {
                alive = true;
            }
        });
    }

    public JDA getJDA() {
        return jda;
    }

    public boolean isActive() {
        return active;
    }

    public void checkConnection() {
        if (alive) {
            ShardManager.decreaseGlobalErrorCounter();
            errors = 0;
            alive = false;
            active = true;
        } else {
            MainLogger.get().debug("No data from shard {}", jda.getShardInfo().getShardId());
            if (++errors % 3 == 0) {
                active = false;
                ShardManager.increaseGlobalErrorCounter();
                MainLogger.get().warn("Shard {} temporarily offline", jda.getShardInfo().getShardId());
            }
        }
    }

    public void updateActivity() {
        String activityText = DiscordConnector.getActivityText();
        if (!activityText.equals(previousActivityText)) {
            previousActivityText = activityText;
            MainLogger.get().info("Updating activity");
            jda.getPresence().setActivity(Activity.watching(activityText));
        }
    }

}
