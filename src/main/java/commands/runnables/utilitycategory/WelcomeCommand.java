package commands.runnables.utilitycategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.Emojis;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.FileUtil;
import core.utils.InternetUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
    trigger = "welcome",
    botPermissions = PermissionDeprecated.ATTACH_FILES,
    userPermissions = PermissionDeprecated.MANAGE_SERVER,
    emoji = "🙋",
    executableWithoutArgs = true
)
public class WelcomeCommand extends Command implements OnNavigationListenerOld {
    
    private WelcomeMessageBean welcomeMessageBean;
    private User author;

    public WelcomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(event.getGuild().getIdLong());
        author = event.getMessage().getUserAuthor().get();
        welcomeMessageBean.getWelcomeChannel().ifPresent(this::checkWriteInChannelWithLog);
        welcomeMessageBean.getGoodbyeChannel().ifPresent(this::checkWriteInChannelWithLog);
        return true;    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                if (inputString.length() > 0) {
                    if (inputString.length() <= 20) {
                        welcomeMessageBean.setWelcomeTitle(inputString);
                        setLog(LogStatus.SUCCESS, getString("titleset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("titletoolarge", "20"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            case 2:
                if (inputString.length() > 0) {
                    if (inputString.length() <= 1000) {
                        welcomeMessageBean.setWelcomeText(inputString);
                        setLog(LogStatus.SUCCESS, getString("descriptionset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("descriptiontoolarge", "1000"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            case 3:
                ArrayList<ServerTextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                    return Response.FALSE;
                } else {
                    if (checkWriteInChannelWithLog(channelList.get(0))) {
                        welcomeMessageBean.setWelcomeChannelId(channelList.get(0).getId());
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else return Response.FALSE;
                }

            case 4:
                List<MessageAttachment> attachmentList = event.getMessage().getAttachments();
                if (attachmentList.size() > 0 && attachmentList.get(0).isImage()) {
                    String downloadFileName = String.format("data/welcome_backgrounds/%d.png", event.getGuild().getIdLong());
                    if (FileUtil.downloadMessageAttachment(attachmentList.get(0), downloadFileName).isPresent()) {
                        setLog(LogStatus.SUCCESS, getString("backgroundset"));
                        setState(0);
                        return Response.TRUE;
                    }
                }

                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
                return Response.FALSE;

            case 6:
                if (inputString.length() > 0) {
                    if (inputString.length() <= 1000) {
                        welcomeMessageBean.setGoodbyeText(inputString);
                        setLog(LogStatus.SUCCESS, getString("goodbyetextset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("goodbyetoolarge", "1000"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            case 7:
                channelList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                    return Response.FALSE;
                } else {
                    if (checkWriteInChannelWithLog(channelList.get(0))) {
                        welcomeMessageBean.setGoodbyeChannelId(channelList.get(0).getId());
                        setLog(LogStatus.SUCCESS, getString("farechannelset"));
                        setState(0);
                        return Response.TRUE;
                    } else return Response.FALSE;
                }

            case 8:
                if (inputString.length() > 0) {
                    if (inputString.length() <= 1000) {
                        welcomeMessageBean.setDmText(inputString);
                        setLog(LogStatus.SUCCESS, getString("dmtextset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("dmtoolarge", "1000"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            default:
                return null;
        }
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        welcomeMessageBean.toggleWelcomeActive();
                        setLog(LogStatus.SUCCESS, getString("activateset", !welcomeMessageBean.isWelcomeActive()));
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        setState(3);
                        return true;

                    case 4:
                        setState(4);
                        return true;

                    case 5:
                        welcomeMessageBean.toggleDmActive();
                        setLog(LogStatus.SUCCESS, getString("dmset", !welcomeMessageBean.isDmActive()));
                        return true;

                    case 6:
                        setState(8);
                        return true;

                    case 7:
                        welcomeMessageBean.toggleGoodbyeActive();
                        setLog(LogStatus.SUCCESS, getString("goodbyeset", !welcomeMessageBean.isGoodbyeActive()));
                        return true;

                    case 8:
                        setState(6);
                        return true;

                    case 9:
                        setState(7);
                        return true;

                    case 10:
                        setState(5);
                        return true;

                    default:
                        return false;
                }

            default:
                if (i == -1) {
                    setState(0);
                    return true;
                } return false;
        }
    }

    @Override
    public EmbedBuilder draw(int state) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI, false)
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getLocale(), welcomeMessageBean.isWelcomeActive()), true)
                        .addField(getString("state0_mtitle"), StringUtil.escapeMarkdown(welcomeMessageBean.getWelcomeTitle()), true)
                        .addField(getString("state0_mdescription"), stressVariables(welcomeMessageBean.getWelcomeText()),
                               true)
                        .addField(getString("state0_mchannel"), welcomeMessageBean.getWelcomeChannel().map(Mentionable::getMentionTag).orElse(notSet), true)
                        .addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI, false)
                        .addField(getString("state0_mdm"), StringUtil.getOnOffForBoolean(getLocale(), welcomeMessageBean.isDmActive()), true)
                        .addField(getString("state0_mdmText"), stressVariables(welcomeMessageBean.getDmText().isEmpty() ? notSet : welcomeMessageBean.getDmText()),
                                true)
                        .addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI, false)
                        .addField(getString("state0_mgoodbye"), StringUtil.getOnOffForBoolean(getLocale(), welcomeMessageBean.isGoodbyeActive()), true)
                        .addField(getString("state0_mgoodbyeText"), stressVariables(welcomeMessageBean.getGoodbyeText()), true)
                        .addField(getString("state0_mfarewellchannel"), welcomeMessageBean.getGoodbyeChannel().map(Mentionable::getMentionTag).orElse(notSet), true);

            default:
                if (state == 5) {
                    return getWelcomeMessageTest(author);
                }
                return EmbedFactory.getEmbedDefault(this, getString("state"+state+"_description"), getString("state"+state+"_title"));
        }
    }

    private String stressVariables(String text) {
        return Welcome.resolveVariables(StringUtil.escapeMarkdown(text),
                "`%SERVER`",
                "`%USER_MENTION`",
                "`%USER_NAME`",
                "`%USER_DISCRIMINATED`",
                "`%MEMBERS`");
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable { }

    @Override
    public int getMaxReactionNumber() {
        return 11;
    }

    public EmbedBuilder getWelcomeMessageTest(User user) throws ExecutionException, InterruptedException {
        Server server = welcomeMessageBean.getGuild().get();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(Welcome.resolveVariables(welcomeMessageBean.getWelcomeText(),
                        StringUtil.escapeMarkdown(server.getName()),
                        user.getMentionTag(),
                        StringUtil.escapeMarkdown(user.getName()),
                        StringUtil.escapeMarkdown(user.getDiscriminatedName()),
                        StringUtil.numToString(server.getMemberCount())));

        eb.setImage(InternetUtil.getURLFromInputStream(WelcomeGraphics.createImageWelcome(user, server, welcomeMessageBean.getWelcomeTitle())).toString());
        return eb;
    }

}
