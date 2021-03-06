package modules.mute;

import core.atomicassets.AtomicTextChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class MuteData {

    private final AtomicTextChannel atomicTextChannel;
    private final ArrayList<Member> members;
    private final Instant stopTime;

    public MuteData(TextChannel channel, ArrayList<Member> members, Instant stopTime) {
        this.atomicTextChannel = new AtomicTextChannel(channel);
        this.members = members;
        this.stopTime = stopTime;
    }

    public MuteData(TextChannel channel, ArrayList<Member> members) {
        this(channel, members, null);
    }

    public Optional<TextChannel> getTextChannel() {
        return atomicTextChannel.get();
    }

    public ArrayList<Member> getMembers() {
        return members;
    }

    public Optional<Instant> getStopTime() {
        return Optional.ofNullable(stopTime);
    }
}
