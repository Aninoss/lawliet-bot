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
import core.modals.ModalMediator;
import core.utils.EmojiUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.fishery.FisheryGear;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.autowork.DBAutoWork;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
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
        aliases = {"working", "salary", "w"}
)
public class WorkCommand extends Command implements FisheryInterface, OnButtonListener, OnMessageInputListener {

    public static String BUTTON_ID_SET = "set";
    public static String BUTTON_ID_CANCEL = "cancel";

    private final String[] EMOJIS = new String[]{
            EmojiUtil.getEmojiFromOverride("🐟", "WORKCOUNT1"),
            EmojiUtil.getEmojiFromOverride("🐠", "WORKCOUNT2"),
            EmojiUtil.getEmojiFromOverride("🐡", "WORKCOUNT3"),
            Emojis.FULL_SPACE_UNICODE.getFormatted() + Emojis.FULL_SPACE_UNICODE.getFormatted()
    };

    private FisheryMemberData fisheryMemberData;
    private String[][] area;
    private int fishFocus;
    private int fishCounter;
    private boolean active = true;

    public WorkCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) {
        fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        Optional<Instant> nextWork = fisheryMemberData.checkNextWork(getGuildEntity().getFishery().getWorkIntervalMinutesEffectively());
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
        if (!StringUtil.stringIsInt(input)) {
            return null;
        }

        int number = Integer.parseInt(input);
        if (number < 0 || number > area.length * area[0].length) {
            return null;
        }

        return process(event.getMember(), number) ? MessageInputResponse.SUCCESS : MessageInputResponse.FAILED;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (event.getComponentId().equals(BUTTON_ID_SET)) {
            String id = "text";
            TextInput textInput = TextInput.create(id, getString("number"), TextInputStyle.SHORT)
                    .setRequiredRange(1, 3)
                    .build();

            Modal modal = ModalMediator.createDrawableCommandModal(this, getString("set"), e -> {
                        String input = e.getValue(id).getAsString();
                        if (!StringUtil.stringIsInt(input)) {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                            return null;
                        }

                        process(e.getMember(), Integer.parseInt(input));
                        return null;
                    })
                    .addActionRow(textInput)
                    .build();

            event.replyModal(modal).queue();
            return false;
        } else if (event.getComponentId().equals(BUTTON_ID_CANCEL)) {
            deregisterListenersWithComponents();
            active = false;
            fisheryMemberData.removeWork();
            setLog(null, getString("canceled"));
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
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
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_SET, getString("set"), Emoji.fromFormatted(EMOJIS[fishFocus])),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
        }

        return eb;
    }

    @Override
    protected void onListenerTimeOut() {
        active = false;
        fisheryMemberData.removeWork();
    }

    private boolean process(Member member, int number) {
        if (number == fishCounter) {
            deregisterListenersWithComponents();
            active = false;
            long coins = fisheryMemberData.getMemberGear(FisheryGear.WORK).getEffect();

            GuildEntity guildEntity = getGuildEntity();
            setAdditionalEmbeds(fisheryMemberData.changeValuesEmbed(member, 0, coins, guildEntity).build());
            setLog(LogStatus.SUCCESS, getString("right", TimeUtil.getDurationString(getLocale(), Duration.ofMinutes(guildEntity.getFishery().getWorkIntervalMinutesEffectively()))));
            return true;
        } else {
            setArea();
            setLog(LogStatus.FAILURE, getString("wrong"));
            return false;
        }
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
