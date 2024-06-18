package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.FileStateProcessor;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.LocalFile;
import core.TextManager;
import core.components.ActionRows;
import core.modals.StringModalBuilder;
import core.utils.FileUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesDmEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesJoinEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesLeaveEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "welcome",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = {Permission.MANAGE_SERVER},
        emoji = "🙋",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class WelcomeCommand extends NavigationAbstract {

    public static int MAX_WELCOME_TITLE_LENGTH = 20;
    public static int MAX_TEXT_LENGTH = MessageEmbed.VALUE_MAX_LENGTH;

    public static final int STATE_SET_TEXT = 1,
            STATE_SET_CHANNEL = 2,
            STATE_SET_BANNER_BACKGROUND = 3,
            STATE_EXAMPLE = 4;


    private int category = 0;

    public WelcomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        getGuildEntity().getWelcomeMessages().getJoin().getChannel().get().ifPresent(this::checkWriteEmbedInChannelAndAttachFilesWithLog);
        getGuildEntity().getWelcomeMessages().getLeave().getChannel().get().ifPresent(this::checkWriteEmbedInChannelAndAttachFilesWithLog);

        registerNavigationListener(event.getMember(), List.of(
                new StringStateProcessor(this, STATE_SET_TEXT, DEFAULT_STATE, getString("state0_mdescription"))
                        .setDescription(getString("settext_description"))
                        .setClearButton(false)
                        .setMax(MAX_TEXT_LENGTH)
                        .enableHibernateTransaction()
                        .setGetter(() -> {
                            WelcomeMessagesEntity welcomeMessagesEntity = getGuildEntity().getWelcomeMessages();
                            switch (category) {
                                case 0 -> {
                                    return welcomeMessagesEntity.getJoin().getText();
                                }
                                case 1 -> {
                                    return welcomeMessagesEntity.getDm().getText();
                                }
                                case 2 -> {
                                    return welcomeMessagesEntity.getLeave().getText();
                                }
                                default -> {
                                    return null;
                                }
                            }
                        })
                        .setSetter(input -> {
                            WelcomeMessagesEntity welcomeMessagesEntity = getGuildEntity().getWelcomeMessages();
                            switch (category) {
                                case 0 -> {
                                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_TEXT, event.getMember(), welcomeMessagesEntity.getJoin().getText(), input);
                                    welcomeMessagesEntity.getJoin().setText(input);
                                }
                                case 1 -> {
                                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_DM_TEXT, event.getMember(), welcomeMessagesEntity.getDm().getText(), input);
                                    welcomeMessagesEntity.getDm().setText(input);
                                }
                                case 2 -> {
                                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_TEXT, event.getMember(), welcomeMessagesEntity.getLeave().getText(), input);
                                    welcomeMessagesEntity.getLeave().setText(input);
                                }
                            }
                        }),
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
                        .enableHibernateTransaction()
                        .setSingleGetter(() -> {
                            WelcomeMessagesEntity welcomeMessagesEntity = getGuildEntity().getWelcomeMessages();
                            return category == 0 ? welcomeMessagesEntity.getJoin().getChannelId() : welcomeMessagesEntity.getLeave().getChannelId();
                        })
                        .setSingleSetter(channelId -> {
                            WelcomeMessagesEntity welcomeMessagesEntity = getGuildEntity().getWelcomeMessages();
                            if (category == 0) {
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_CHANNEL, event.getMember(), welcomeMessagesEntity.getJoin().getChannelId(), channelId);
                                welcomeMessagesEntity.getJoin().setChannelId(channelId);
                            } else {
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_CHANNEL, event.getMember(), welcomeMessagesEntity.getLeave().getChannelId(), channelId);
                                welcomeMessagesEntity.getLeave().setChannelId(channelId);
                            }
                        }),
                new FileStateProcessor(this, STATE_SET_BANNER_BACKGROUND, DEFAULT_STATE, getString("dashboard_backgroundimage"))
                        .setClearButton(true)
                        .setAllowGifs(false)
                        .enableHibernateTransaction()
                        .setSetter(attachment -> {
                            LocalFile localFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", event.getGuild().getIdLong()));
                            if (attachment != null) {
                                FileUtil.downloadImageAttachment(attachment, localFile);
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_BANNER_BACKGROUND_SET, event.getMember());
                            } else {
                                localFile.delete();
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_BANNER_BACKGROUND_RESET, event.getMember());
                            }
                        })
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            deregisterListenersWithComponentMessage();
            return false;
        }
        return switch (category) {
            case 0 -> onButtonJoin(event, i);
            case 1 -> onButtonDM(event, i);
            case 2 -> onButtonLeave(event, i);
            default -> false;
        };
    }

    @ControllerButton(state = STATE_EXAMPLE)
    public boolean onButtonExample(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerStringSelectMenu(state = DEFAULT_STATE)
    public boolean onSelectMenuDefault(StringSelectInteractionEvent event, int i) {
        category = Integer.parseInt(event.getValues().get(0));
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        String[] options = getString("state0_options_" + category).split("\n");
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            Button button = Button.of(ButtonStyle.PRIMARY, String.valueOf(i), options[i]);
            buttons.add(button);
        }

        ArrayList<ActionRow> actionRows = new ArrayList<>(ActionRows.of(buttons));
        actionRows.add(generateSelectMenu());
        setActionRows(actionRows);

        GuildMessageChannel channel = getGuildMessageChannel().get();
        switch (category) {
            case 0 -> {
                WelcomeMessagesJoinEntity join = getGuildEntity().getWelcomeMessages().getJoin();
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"), getString("dashboard_join"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(channel, getLocale(), join.getActive()), true)
                        .addField(getString("state0_mdescription"), StringUtil.shortenString(stressVariables(join.getText()), 1024), true)
                        .addField(getString("state0_membed"), StringUtil.getOnOffForBoolean(channel, getLocale(), join.getEmbeds()), true)
                        .addField(getString("state0_mchannel"), join.getChannel().getPrefixedNameInField(getLocale()), true)
                        .addField(getString("state0_mbanner"), StringUtil.getOnOffForBoolean(channel, getLocale(), join.getImageMode() == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS), true)
                        .addField(getString("state0_mtitle"), StringUtil.escapeMarkdown(join.getBannerTitle()), true);
            }
            case 1 -> {
                WelcomeMessagesDmEntity dm = getGuildEntity().getWelcomeMessages().getDm();
                String text = StringUtil.shortenString(stressVariables(dm.getText()), 1024);
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"), getString("dashboard_dm"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(channel, getLocale(), dm.getActive()), true)
                        .addField(getString("state0_mdescription"), text.isEmpty() ? TextManager.getString(getLocale(), TextManager.GENERAL, "notset") : text, true)
                        .addField(getString("state0_membed"), StringUtil.getOnOffForBoolean(channel, getLocale(), dm.getEmbeds()), true);
            }
            case 2 -> {
                WelcomeMessagesLeaveEntity leave = getGuildEntity().getWelcomeMessages().getLeave();
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"), getString("dashboard_leave"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(channel, getLocale(), leave.getActive()), true)
                        .addField(getString("state0_mdescription"), StringUtil.shortenString(stressVariables(leave.getText()), 1024), true)
                        .addField(getString("state0_membed"), StringUtil.getOnOffForBoolean(channel, getLocale(), leave.getEmbeds()), true)
                        .addField(getString("state0_mchannel"), leave.getChannel().getPrefixedNameInField(getLocale()), true);
            }
            default -> throw new UnsupportedOperationException("Invalid category");
        }
    }

    @Draw(state = STATE_EXAMPLE)
    public EmbedBuilder drawExample(Member member) throws ExecutionException, InterruptedException, IOException {
        WelcomeMessagesJoinEntity join = getGuildEntity().getWelcomeMessages().getJoin();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(Welcome.resolveVariables(
                        join.getText(),
                        StringUtil.escapeMarkdown(member.getGuild().getName()),
                        member.getAsMention(),
                        StringUtil.escapeMarkdown(member.getUser().getName()),
                        StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                        StringUtil.numToString(member.getGuild().getMemberCount()),
                        StringUtil.escapeMarkdown(member.getUser().getEffectiveName())
                ));

        if (join.getImageMode() == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS) {
            eb.setImage(InternetUtil.getUrlFromInputStream(
                    WelcomeGraphics.createImageWelcome(member, join.getBannerTitle()).get(),
                    "png"
            ));
        }

        return eb;
    }

    private boolean onButtonJoin(ButtonInteractionEvent event, int i) {
        WelcomeMessagesJoinEntity join = getGuildEntity().getWelcomeMessages().getJoin();
        switch (i) {
            case 0 -> {
                getEntityManager().getTransaction().begin();
                join.setActive(!join.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_ACTIVE, event.getMember(), null, join.getActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", join.getActive(), getString("dashboard_join")));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_TEXT);
                return true;
            }
            case 2 -> {
                getEntityManager().getTransaction().begin();
                join.setEmbeds(!join.getEmbeds());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_EMBEDS, event.getMember(), null, join.getEmbeds());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("embedset", join.getEmbeds()));
                return true;
            }
            case 3 -> {
                setState(STATE_SET_CHANNEL);
                return true;
            }
            case 4 -> {
                getEntityManager().getTransaction().begin();
                join.setImageMode(join.getImageMode() == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS ? WelcomeMessagesJoinEntity.ImageMode.NONE : WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_BANNERS, event.getMember(), null, join.getImageMode() == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS);
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("bannerset", join.getImageMode() == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS));
                return true;
            }
            case 5 -> {
                Modal modal = new StringModalBuilder(this, getString("state0_mtitle"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, MAX_WELCOME_TITLE_LENGTH)
                        .setLogEvent(BotLogEntity.Event.WELCOME_BANNER_TITLE)
                        .setGetter(() -> getGuildEntity().getWelcomeMessages().getJoin().getBannerTitle())
                        .setSetter(s -> getGuildEntity().getWelcomeMessages().getJoin().setBannerTitle(s))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 6 -> {
                setState(STATE_SET_BANNER_BACKGROUND);
                return true;
            }
            case 7 -> {
                setState(STATE_EXAMPLE);
                return true;
            }
        }
        return false;
    }

    private boolean onButtonDM(ButtonInteractionEvent event, int i) {
        WelcomeMessagesDmEntity dm = getGuildEntity().getWelcomeMessages().getDm();
        switch (i) {
            case 0 -> {
                getEntityManager().getTransaction().begin();
                dm.setActive(!dm.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_DM_ACTIVE, event.getMember(), null, dm.getActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", dm.getActive(), getString("dashboard_dm")));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_TEXT);
                return true;
            }
            case 2 -> {
                getEntityManager().getTransaction().begin();
                dm.setEmbeds(!dm.getEmbeds());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_DM_EMBEDS, event.getMember(), null, dm.getEmbeds());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("embedset", dm.getEmbeds()));
                return true;
            }
        }
        return false;
    }

    private boolean onButtonLeave(ButtonInteractionEvent event, int i) {
        WelcomeMessagesLeaveEntity leave = getGuildEntity().getWelcomeMessages().getLeave();
        switch (i) {
            case 0 -> {
                getEntityManager().getTransaction().begin();
                leave.setActive(!leave.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_ACTIVE, event.getMember(), null, leave.getActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", leave.getActive(), getString("dashboard_leave")));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_TEXT);
                return true;
            }
            case 2 -> {
                getEntityManager().getTransaction().begin();
                leave.setEmbeds(!leave.getEmbeds());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_EMBEDS, event.getMember(), null, leave.getEmbeds());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("embedset", leave.getEmbeds()));
                return true;
            }
            case 3 -> {
                setState(STATE_SET_CHANNEL);
                return true;
            }
        }
        return false;
    }

    private String stressVariables(String text) {
        return Welcome.resolveVariables(
                StringUtil.escapeMarkdown(text),
                "`%SERVER`",
                "`%USER_MENTION`",
                "`%USERNAME`",
                "`%USER_DISCRIMINATED`",
                "`%MEMBERS`",
                "`%DISPLAY_NAME`"
        );
    }

    private ActionRow generateSelectMenu() {
        String[] selectMenuOptions = getString("state0_selectmenu").split("\n");
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("category")
                .setRequiredRange(1, 1);
        for (int i = 0; i < selectMenuOptions.length; i++) {
            selectMenuBuilder.addOption(selectMenuOptions[i], String.valueOf(i));
        }
        selectMenuBuilder.setDefaultOptions(SelectOption.of(selectMenuOptions[category], String.valueOf(category)));
        return ActionRow.of(selectMenuBuilder.build());
    }

}
