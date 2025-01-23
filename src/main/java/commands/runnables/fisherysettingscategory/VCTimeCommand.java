package commands.runnables.fisherysettingscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.FisheryEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "vctime",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = {"voicechanneltime", "vccap", "voicechannelcap", "vccaps", "vclimit", "vclimits", "vctimeout"}
)
public class VCTimeCommand extends Command implements OnButtonListener {

    private static final String BUTTON_ID_SET_LIMIT = "set";
    private static final String BUTTON_ID_UNLIMITED = "unlimited";
    private static final String BUTTON_ID_CANCEL = "cancel";

    private EmbedBuilder eb;
    private boolean completed = false;

    public VCTimeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        int voiceHoursLimitEffectively = getGuildEntity().getFishery().getVoiceHoursLimitEffectively();
        this.eb = EmbedFactory.getEmbedDefault(
                this,
                getString(
                        "status",
                        voiceHoursLimitEffectively != 24,
                        voiceHoursLimitEffectively != 24
                                ? StringUtil.numToString(voiceHoursLimitEffectively)
                                : getString("unlimited")
                )
        );

        registerButtonListener(event.getMember());
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        switch (event.getComponentId()) {
            case BUTTON_ID_UNLIMITED -> {
                deregisterListenersWithComponents();

                FisheryEntity fishery = getGuildEntity().getFishery();
                fishery.beginTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_VOICE_HOURS_LIMIT, event.getMember(), fishery.getVoiceHoursLimit(), 24);
                fishery.setVoiceHoursLimit(24);
                fishery.commitTransaction();

                completed = true;
                this.eb = EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(null), getString("unlimited")));
                return true;
            }
            case BUTTON_ID_SET_LIMIT -> {
                String ID = "text";
                int currentLimit = getGuildEntity().getFishery().getVoiceHoursLimitEffectively();
                TextInput textInput = TextInput.create(ID, getString("hoursperday"), TextInputStyle.SHORT)
                        .setRequiredRange(1, 2)
                        .setValue(currentLimit < 24 ? String.valueOf(currentLimit) : null)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("setlimit"), e -> {
                            String input = e.getValue(ID).getAsString();

                            if (!StringUtil.stringIsInt(input)) {
                                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                                return null;
                            }

                            int value = Integer.parseInt(input);
                            if (value < 1 || value > 23) {
                                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "23"));
                                return null;
                            }

                            FisheryEntity fishery = getGuildEntity().getFishery();
                            fishery.beginTransaction();
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_VOICE_HOURS_LIMIT, e.getMember(), fishery.getVoiceHoursLimit(), value);
                            fishery.setVoiceHoursLimit(value);
                            fishery.commitTransaction();

                            completed = true;
                            return EmbedFactory.getEmbedDefault(this, getString("success", getNumberSlot(value), StringUtil.numToString(value)));
                        })
                        .addActionRow(textInput)
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case BUTTON_ID_CANCEL -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        if (!completed) {
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_SET_LIMIT, getString("setlimit")),
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_UNLIMITED, getString("setunlimited")),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
        }
        return new EmbedBuilder(this.eb);
    }

    private int getNumberSlot(Integer i) {
        if (i == null) {
            return 0;
        } else if (i == 1) return 1;
        return 2;
    }

}
