package commands.runnables.fisherycategory;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.FisheryGear;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "work",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "💼",
        executableWithoutArgs = true,
        aliases = { "working", "salary", "w" }
)
public class WorkCommand extends Command implements FisheryInterface, OnReactionListener, OnMessageInputListener {

    private final String[] EMOJIS = new String[] { "🐟", "🐠", "🐡", Emojis.EMPTY_EMOJI + Emojis.EMPTY_EMOJI };

    private FisheryMemberData fisheryMemberBean;
    private String[][] area;
    private int fishFocus;
    private int fishCounter;
    private boolean active = true;

    public WorkCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
        Optional<Instant> nextWork = fisheryMemberBean.checkNextWork();
        if (nextWork.isEmpty()) {
            setArea();
            registerReactionListener(Emojis.X);
            registerMessageInputListener(false);
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("cannot_work"));
            eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);

            EmbedUtil.addRemainingTime(eb, nextWork.get());
            EmbedUtil.addLog(eb, LogStatus.TIME, TextManager.getString(getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), nextWork.get(), false)));
            event.getChannel().sendMessage(eb.build()).queue();
            return false;
        }
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) {
        if (StringUtil.stringIsInt(input)) {
            int number = Integer.parseInt(input);
            if (number >= 0 && number <= area.length * area[0].length) {
                if (number == fishCounter) {
                    deregisterListenersWithReactions();
                    active = false;
                    long coins = fisheryMemberBean.getMemberGear(FisheryGear.WORK).getEffect();
                    event.getChannel().sendMessage(fisheryMemberBean.changeValuesEmbed(0, coins).build()).queue();
                    fisheryMemberBean.setWorkDone();
                    setLog(LogStatus.SUCCESS, getString("right"));
                    return Response.TRUE;
                } else {
                    setArea();
                    setLog(LogStatus.FAILURE, getString("wrong"));
                    return Response.FALSE;
                }
            }
        }

        return null;
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) {
        deregisterListenersWithReactions();
        active = false;
        fisheryMemberBean.setWorkCanceled();
        setLog(null, getString("canceled"));
        return true;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        int width = area[0].length + 2;
        int height = area.length + 2;

        StringBuilder areaBuilder = new StringBuilder();

        for (int y = 1; y < height - 1; y++) { //TODO: border removed temporarily
            for (int x = 1; x < width - 1; x++) {
                if (y == 0) {
                    if (x == 0) {
                        areaBuilder.append(Emojis.SLOT_DR);
                    } else if (x == width - 1) {
                        areaBuilder.append(Emojis.SLOT_DL);
                    } else {
                        areaBuilder.append(Emojis.SLOT_LR);
                    }
                } else if (y == height - 1) {
                    if (x == 0) {
                        areaBuilder.append(Emojis.SLOT_UR);
                    } else if (x == width - 1) {
                        areaBuilder.append(Emojis.SLOT_UL);
                    } else {
                        areaBuilder.append(Emojis.SLOT_LR);
                    }
                } else {
                    if (x == 0 || x == width - 1) {
                        areaBuilder.append(Emojis.SLOT_UD);
                    } else {
                        areaBuilder.append(area[y - 1][x - 1]);
                    }
                }
            }

            areaBuilder.append("\n");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, areaBuilder.toString());
        if (active) {
            String unknownMember = TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getMemberId().get()));
            eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("instructions", StringUtil.escapeMarkdown(getMember().map(Member::getEffectiveName).orElse(unknownMember)), EMOJIS[fishFocus]), false);
        }

        return eb;
    }

    @Override
    protected void onListenerTimeOut() {
        active = false;
        fisheryMemberBean.setWorkCanceled();
    }

    private void setArea() {
        do {
            area = new String[7][7];
            fishFocus = new Random().nextInt(EMOJIS.length - 1);
            fishCounter = 0;

            for (int y = 0; y < area.length; y++) {
                for (int x = 0; x < area[0].length; x++) {
                    int i = RandomUtil.pickWithProbabilities(1.0 / 8, 1.0 / 10, 1.0 / 12);
                    if (i == fishFocus) {
                        fishCounter++;
                    }
                    area[y][x] = EMOJIS[i];
                }
            }
        } while (fishCounter <= 0);
    }

}
