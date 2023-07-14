package commands.runnables.fisherycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmojiUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import modules.fishery.FisheryGear;
import mysql.modules.autowork.DBAutoWork;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

@CommandProperties(
        trigger = "work",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "💼",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "working", "salary", "w" }
)
public class WorkCommand extends Command implements FisheryInterface, OnButtonListener, OnMessageInputListener {

    private final String[] EMOJIS = new String[] {
            EmojiUtil.getEmojiFromOverride("🐟", "WORKCOUNT1"),
            EmojiUtil.getEmojiFromOverride("🐠", "WORKCOUNT2"),
            EmojiUtil.getEmojiFromOverride("🐡", "WORKCOUNT3"),
            Emojis.FULL_SPACE_UNICODE.getFormatted() + Emojis.FULL_SPACE_UNICODE.getFormatted()
    };

    private FisheryMemberData fisheryMemberBean;
    private String[][] area;
    private int fishFocus;
    private int fishCounter;
    private boolean active = true;

    public WorkCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) {
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        Optional<Instant> nextWork = fisheryMemberBean.checkNextWork();
        if (nextWork.isEmpty()) {
            setArea();
            registerButtonListener(event.getMember());
            registerMessageInputListener(event.getMember(), false);
            return true;
        } else {
            EmbedBuilder eb;
            if (DBAutoWork.getInstance().retrieve().isActive(event.getMember().getIdLong())) {
                eb = EmbedFactory.getEmbedDefault(this, getString("autowork"));
            } else {
                eb = EmbedFactory.getEmbedDefault(this, getString("cannot_work"));
                eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);
            }

            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("next", TimeFormat.RELATIVE.atInstant(nextWork.get()).toString()), false);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @Override
    public MessageInputResponse onMessageInput(@NotNull MessageReceivedEvent event, @NotNull String input) {
        if (StringUtil.stringIsInt(input)) {
            int number = Integer.parseInt(input);
            if (number >= 0 && number <= area.length * area[0].length) {
                if (number == fishCounter) {
                    deregisterListenersWithComponents();
                    active = false;
                    long coins = fisheryMemberBean.getMemberGear(FisheryGear.WORK).getEffect();
                    setAdditionalEmbeds(fisheryMemberBean.changeValuesEmbed(event.getMember(), 0, coins, getGuildEntity()).build());
                    fisheryMemberBean.completeWork();
                    setLog(LogStatus.SUCCESS, getString("right"));
                    return MessageInputResponse.SUCCESS;
                } else {
                    setArea();
                    setLog(LogStatus.FAILURE, getString("wrong"));
                    return MessageInputResponse.FAILED;
                }
            }
        }

        return null;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        deregisterListenersWithComponents();
        active = false;
        fisheryMemberBean.removeWork();
        setLog(null, getString("canceled"));
        return true;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        StringBuilder areaBuilder = new StringBuilder();

        for (int y = 0; y < area.length; y++) {
            for (int x = 0; x < area[0].length; x++) {
                areaBuilder.append(area[y][x]);
            }

            areaBuilder.append("\n");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, areaBuilder.toString());
        if (active) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("instructions", StringUtil.escapeMarkdown(member.getEffectiveName()), EMOJIS[fishFocus]), false);
            setComponents(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
        }

        return eb;
    }

    @Override
    protected void onListenerTimeOut() {
        active = false;
        fisheryMemberBean.removeWork();
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
