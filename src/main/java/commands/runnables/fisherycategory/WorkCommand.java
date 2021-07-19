package commands.runnables.fisherycategory;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.FisheryGear;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import mysql.modules.autowork.DBAutoWork;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "work",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "💼",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "working", "salary", "w" }
)
public class WorkCommand extends Command implements FisheryInterface, OnButtonListener, OnMessageInputListener {

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
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        Optional<Instant> nextWork = fisheryMemberBean.checkNextWork();
        if (nextWork.isEmpty()) {
            setArea();
            setButtons(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
            registerButtonListener();
            registerMessageInputListener(false);
            return true;
        } else {
            EmbedBuilder eb;
            if (DBAutoWork.getInstance().retrieve().isActive(event.getMember().getIdLong())) {
                eb = EmbedFactory.getEmbedDefault(this, getString("autowork"));
            } else {
                eb = EmbedFactory.getEmbedDefault(this, getString("cannot_work"));
                eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);
            }

            eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("next", TimeFormat.RELATIVE.atInstant(nextWork.get()).toString()), false);
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
            return false;
        }
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) {
        if (StringUtil.stringIsInt(input)) {
            int number = Integer.parseInt(input);
            if (number >= 0 && number <= area.length * area[0].length) {
                if (number == fishCounter) {
                    deregisterListenersWithButtons();
                    active = false;
                    long coins = fisheryMemberBean.getMemberGear(FisheryGear.WORK).getEffect();
                    setAdditionalEmbeds(fisheryMemberBean.changeValuesEmbed(0, coins).build());
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
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithButtons();
        active = false;
        fisheryMemberBean.setWorkCanceled();
        setLog(null, getString("canceled"));
        return true;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        StringBuilder areaBuilder = new StringBuilder();

        for (int y = 0; y < area.length; y++) {
            for (int x = 0; x < area[0].length; x++) {
                areaBuilder.append(area[y][x]);
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
            area = new String[15][10];
            fishFocus = new Random().nextInt(EMOJIS.length - 1);
            fishCounter = 0;

            for (int y = 0; y < area.length; y++) {
                for (int x = 0; x < area[0].length; x++) {
                    int i = RandomUtil.pickWithProbabilities(1.0 / 16, 1.0 / 20, 1.0 / 24);
                    if (i == fishFocus) {
                        fishCounter++;
                    }
                    area[y][x] = EMOJIS[i];
                }
            }
        } while (fishCounter <= 0);
    }

}
