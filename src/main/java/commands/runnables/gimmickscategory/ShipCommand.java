package commands.runnables.gimmickscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.Language;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.utils.MentionUtil;
import modules.graphics.ShipGraphics;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "ship",
        botChannelPermissions = Permission.MESSAGE_ATTACH_FILES,
        emoji = "\uD83D\uDC6B",
        requiresFullMemberCache = true,
        executableWithoutArgs = false
)
public class ShipCommand extends Command {

    private final CustomShipValues[] customShipValues = new CustomShipValues[] {
            new CustomShipValues(272037078919938058L, 368521195940741122L, 100),
            new CustomShipValues(397209883793162240L, 710120672499728426L, 100),
            new CustomShipValues(368521195940741122L, 844717861132959809L, 100),
            new CustomShipValues(272037078919938058L, 530085770698948608L, 300)
    };

    public ShipCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws IOException, ExecutionException, InterruptedException {
        ArrayList<Member> list = new ArrayList<>(MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember()).getList());
        if (list.size() == 1 && list.get(0).getIdLong() != event.getMember().getIdLong()) {
            list.add(event.getMember());
        }

        if (list.size() != 2) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("not_2")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        list.sort(Comparator.comparingLong(ISnowflake::getIdLong));
        String idString = String.valueOf(list.get(0).getIdLong() + list.get(1).getIdLong() + getDateValue());
        int randomNum = String.valueOf(idString.hashCode()).hashCode();
        int percentage = new Random(randomNum).nextInt(101);
        for (CustomShipValues customShipValue : customShipValues) {
            Integer percentageNew = customShipValue.getPercentage(list.get(0), list.get(1));
            if (percentageNew != null) {
                percentage = percentageNew;
            }
        }

        int n = RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), 8).get();

        deferReply();
        InputStream is = ShipGraphics.createImageShip(list.get(0).getUser(), list.get(1).getUser(), n, percentage);
        if (is == null) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("noavatar")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setImage("attachment://ship.png");

        addFileAttachment(is, "ship.png");
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private int getDateValue() {
        LocalDate localDate = LocalDate.now();
        return localDate.get(WeekFields.of(Language.EN.getLocale()).weekOfYear()) +
                localDate.getYear() * 100;
    }


    private static final class CustomShipValues {

        private final long userId0;
        private final long userId1;
        private final int percentage;

        public CustomShipValues(long userId0, long userId1, int percentage) {
            this.userId0 = userId0;
            this.userId1 = userId1;
            this.percentage = percentage;
        }

        public CustomShipValues(long userId0, int percentage) {
            this.userId0 = userId0;
            this.userId1 = 0L;
            this.percentage = percentage;
        }

        public Integer getPercentage(Member member0, Member member1) {
            if ((member0.getIdLong() == userId0 && member1.getIdLong() == userId1) ||
                    (member0.getIdLong() == userId1 && member1.getIdLong() == userId0) ||
                    (member0.getIdLong() == userId0 && userId1 == 0) ||
                    (member1.getIdLong() == userId0 && userId1 == 0)
            ) {
                return percentage;
            } else {
                return null;
            }
        }

    }

}
