package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.modals.ModalMediator;
import core.utils.BotPermissionUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.FisheryEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Locale;

@CommandProperties(
        trigger = "voterewards",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        botGuildPermissions = Permission.MANAGE_SERVER,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🗳️",
        executableWithoutArgs = true,
        patreonRequired = true,
        usesExtEmotes = true,
        aliases = {"topgg", "topggvoterewards"}
)
public class VoteRewardsCommand extends NavigationAbstract {

    private static final int STATE_ADJUST_CHANNEL = 1;

    public VoteRewardsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                FisheryEntity fisheryEntity = getGuildEntity().getFishery();
                fisheryEntity.beginTransaction();
                fisheryEntity.setVoteRewardsActive(!fisheryEntity.getVoteRewardsActive());
                fisheryEntity.commitTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_VOTE_REWARDS_ACTIVE, event.getMember(), null, fisheryEntity.getVoteRewardsActive());
                setLog(LogStatus.SUCCESS, getString("log_active", fisheryEntity.getVoteRewardsActive()));
                return true;
            }
            case 1 -> {
                setState(STATE_ADJUST_CHANNEL);
                return true;
            }
            case 2 -> {
                String textId = "portion";
                TextInput message = TextInput.create(textId, getString("main_dailyportion_textinput"), TextInputStyle.SHORT)
                        .setValue(String.valueOf(getGuildEntity().getFishery().getVoteRewardsDailyPortionInPercent()))
                        .setMinLength(1)
                        .setMaxLength(4)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("main_dailyportion"), e -> {
                            String newDailyPortion = e.getValue(textId).getAsString();
                            if (newDailyPortion.matches(".*[^0-9%].*") || newDailyPortion.replace("%", "").isEmpty()) {
                                setLog(LogStatus.FAILURE, getString("error_dailyportion_invalid"));
                                return null;
                            }
                            int newRewardsDailyPortion = Integer.parseInt(newDailyPortion.replace("%", ""));

                            FisheryEntity fisheryEntity = getGuildEntity().getFishery();
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_VOTE_REWARDS_PORTION_OF_DAILY, event.getMember(), fisheryEntity.getVoteRewardsDailyPortionInPercent(), newRewardsDailyPortion);
                            fisheryEntity.beginTransaction();
                            fisheryEntity.setVoteRewardsDailyPortionInPercent(newRewardsDailyPortion);
                            fisheryEntity.commitTransaction();

                            setLog(LogStatus.SUCCESS, getString("log_dailyportion"));
                            return null;
                        }).addActionRows(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 3 -> {
                String auth = RandomUtil.generateRandomString(20);
                FisheryEntity fisheryEntity = getGuildEntity().getFishery();
                fisheryEntity.beginTransaction();
                fisheryEntity.setVoteRewardsAuthorization(auth);
                fisheryEntity.commitTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_VOTE_REWARDS_GENERATE_AUTH, event.getMember());

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setDescription(getString("main_auth", auth));

                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();

                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_ADJUST_CHANNEL)
    public boolean onButtonAdjustChannel(ButtonInteractionEvent event, int i) {
        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerEntitySelectMenu
    public boolean onEntitySelectMenuAdjustChannel(EntitySelectInteractionEvent event) {
        TextChannel channel = event.getMentions().getChannels(TextChannel.class).get(0);
        if (!BotPermissionUtil.canWriteEmbed(channel)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", new AtomicTextChannel(channel).getPrefixedName(getLocale()));
            setLog(LogStatus.FAILURE, error);
            return true;
        }

        FisheryEntity fishery = getGuildEntity().getFishery();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_VOTE_REWARDS_LOG_CHANNEL, event.getMember(), fishery.getVoteRewardsChannelId(), channel.getIdLong());
        fishery.beginTransaction();
        fishery.setVoteRewardsChannelId(channel.getIdLong());
        fishery.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("log_channel"));
        setState(DEFAULT_STATE);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        FisheryEntity fisheryEntity = getGuildEntity().getFishery();
        setComponents(getString("main_options").split("\n"));

        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        String webhookSettingsUrl = MessageFormat.format("https://top.gg/servers/{0}/webhooks", member.getGuild().getId());
        return EmbedFactory.getEmbedDefault(this, getString("main_desc", webhookSettingsUrl))
                .addField(getString("main_active"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), fisheryEntity.getVoteRewardsActive()), true)
                .addField(getString("main_logchannel"), fisheryEntity.getVoteRewardsChannelId() != null ? fisheryEntity.getVoteRewardsChannel().getPrefixedNameInField(getLocale()) : notSet, true)
                .addField(getString("main_dailyportion"), getString("main_dailyportion_value", StringUtil.numToString(fisheryEntity.getVoteRewardsDailyPortionInPercent())), true);
    }

    @Draw(state = STATE_ADJUST_CHANNEL)
    public EmbedBuilder onDrawAdjustChannel(Member member) {
        EntitySelectMenu selectMenu = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT)
                .build();
        setComponents(selectMenu);

        return EmbedFactory.getEmbedDefault(this, getString("channel_desc"));
    }

}
