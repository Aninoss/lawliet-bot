package commands.runnables.birthdaycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ListAbstract;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MemberCacheController;
import core.atomicassets.AtomicMember;
import javafx.util.Pair;
import mysql.hibernate.entity.guild.BirthdayUserEntryEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "birthdaylist",
        emoji = "📅",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        releaseDate = { 2024, 10, 23 }
)
public class BirthdayListCommand extends ListAbstract {

    private List<Entry> entries;

    public BirthdayListCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (getGuildEntity().getBirthday().getActive()) {
            registerList(event.getMember(), args);
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(
                    this,
                    getString("disabled")
            );
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @Override
    protected int configure(Member member, int orderBy) throws Throwable {
        Map<Long, BirthdayUserEntryEntity> birthdayEntries = getGuildEntity().getBirthday().getUserEntries();
        Set<Long> memberIdSet = MemberCacheController.getInstance().loadMembers(member.getGuild(), birthdayEntries.keySet()).get().stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toSet());

        entries = birthdayEntries.entrySet().stream()
                .filter(entry -> memberIdSet.contains(entry.getKey()))
                .map(entry -> new Entry(entry.getKey(), entry.getValue().getNextBirthday()))
                .filter(entry -> entry.nextBirthday != null)
                .sorted(Comparator.comparing(entry -> entry.nextBirthday))
                .collect(Collectors.toList());
        return entries.size();
    }

    @Override
    protected Pair<String, String> getEntry(Member member, int i, int orderBy) {
        Entry entry = entries.get(i);
        String prefix = Instant.now().isAfter(entry.nextBirthday) ? "🎂 " : "";
        return new Pair<>(
                prefix + new AtomicMember(member.getGuild().getIdLong(), entry.userId).getName(getLocale()),
                TimeFormat.DATE_TIME_SHORT.atInstant(entry.nextBirthday).toString()
        );
    }


    private static class Entry {

        private final long userId;
        private final Instant nextBirthday;

        public Entry(long userId, Instant nextBirthday) {
            this.userId = userId;
            this.nextBirthday = nextBirthday;
        }

    }

}