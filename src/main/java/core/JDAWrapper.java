package core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class JDAWrapper {

    private final JDA jda;
    private boolean alive = false;
    private boolean active = true;
    private int errors = 0;

    public JDAWrapper(JDA jda) {
        this.jda = jda;
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
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
            if (++errors % 4 == 3) {
                active = false;
                ShardManager.increaseGlobalErrorCounter();
                MainLogger.get().warn("Shard {} temporarily offline", jda.getShardInfo().getShardId());
            }
        }
    }

}
