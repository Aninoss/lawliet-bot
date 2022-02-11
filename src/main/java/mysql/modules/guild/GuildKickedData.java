package mysql.modules.guild;

import java.time.LocalDate;
import mysql.DataWithGuild;

public class GuildKickedData extends DataWithGuild {

    private final LocalDate kicked;

    public GuildKickedData(long guildId, LocalDate kicked) {
        super(guildId);
        this.kicked = kicked;
    }

    public LocalDate getKicked() {
        return kicked;
    }

}
