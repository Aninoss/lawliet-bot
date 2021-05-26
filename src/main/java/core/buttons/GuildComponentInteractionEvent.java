package core.buttons;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class GuildComponentInteractionEvent extends GuildMessageReceivedEvent implements ISnowflake {

    private final Member member;
    private final int componentType;
    private final String customId;
    private final long id;
    private final String token;

    public GuildComponentInteractionEvent(@NotNull JDA api, long responseNumber, @NonNull Member member, @Nonnull Message message, int componentType, String customId, long id, String token) {
        super(api, responseNumber, message);
        this.member = member;
        this.componentType = componentType;
        this.customId = customId;
        this.id = id;
        this.token = token;
    }

    @Override
    public Member getMember() {
        return member;
    }

    public boolean isButton() {
        return componentType == 2;
    }

    public String getCustomId() {
        return customId;
    }

    @Override
    public long getIdLong() {
        return id;
    }

    public String getToken() {
        return token;
    }

}
