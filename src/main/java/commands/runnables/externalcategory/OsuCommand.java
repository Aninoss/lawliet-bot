package commands.runnables.externalcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.EmbedUtil;
import core.utils.EncryptionUtil;
import core.utils.StringUtil;
import modules.OsuGame;
import modules.osu.OsuAccount;
import modules.osu.OsuAccountDownloader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "osu",
        emoji = "✍️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        releaseDate = { 2020, 11, 28 },
        aliases = { "osu!" }
)
public class OsuCommand extends MemberAccountAbstract implements OnButtonListener {

    private final static String BUTTON_ID_CONNECT = "connect";

    private boolean memberIsAuthor;

    public OsuCommand(Locale locale, String prefix) {
        super(locale, prefix, false, false, false);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        this.memberIsAuthor = memberIsAuthor;

        boolean userExists = false;
        OsuGame gameMode = extractGameMode(args);

        EmbedBuilder eb;
        Long osuId = getUserEntityReadOnly().getOsuId();
        OsuAccount osuAccount = osuId != null ? OsuAccountDownloader.download(String.valueOf(osuId), gameMode).get().orElse(null) : null;
        if (osuAccount != null) {
            eb = generateAccountEmbed(member, osuAccount, gameMode);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("noacc", StringUtil.escapeMarkdown(member.getEffectiveName())));
            if (memberIsAuthor) {
                setComponents(Button.of(ButtonStyle.PRIMARY, BUTTON_ID_CONNECT, getString("connect", userExists)));
            }
        }

        return eb;
    }

    private OsuGame extractGameMode(String args) {
        if (args.toLowerCase().contains("taiko")) {
            setFound();
            return OsuGame.TAIKO;
        } else if (args.toLowerCase().contains("fruits") || args.toLowerCase().contains("catch") || args.toLowerCase().contains("ctb")) {
            setFound();
            return OsuGame.CATCH;
        } else if (args.toLowerCase().contains("mania")) {
            setFound();
            return OsuGame.MANIA;
        } else {
            if (args.toLowerCase().contains("osu")) {
                setFound();
            }
            return OsuGame.OSU;
        }
    }

    @Override
    protected void sendMessage(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        drawMessage(eb).exceptionally(ExceptionLogger.get());
        if (memberIsAuthor) {
            registerButtonListener(member, false);
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        String state = EncryptionUtil.encrypt(event.getMember().getId());
        String url = "https://osu.ppy.sh/oauth/authorize?client_id=" + System.getenv("OSU_CLIENT_ID") + "&redirect_uri=" + System.getenv("OSU_REDIRECT_URI") + "&state=" + state + "&response_type=code";
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("connect_message"));
        event.replyEmbeds(eb.build())
                .setEphemeral(true)
                .setComponents(ActionRow.of(Button.of(ButtonStyle.LINK, url, getString("connect_message_button"))))
                .queue();
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) {
        return null;
    }

    private EmbedBuilder generateAccountEmbed(Member member, OsuAccount acc, OsuGame gameMode) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle(getString("embedtitle", StringUtil.escapeMarkdown(acc.getUsername()), acc.getCountryEmoji(), getString(gameMode.getId())))
                .setDescription(getString(
                        "main",
                        StringUtil.numToString(acc.getPp()),
                        acc.getGlobalRank().map(StringUtil::numToString).orElse("?"),
                        acc.getCountryRank().map(StringUtil::numToString).orElse("?"),
                        String.valueOf(acc.getAccuracy()),
                        String.valueOf(acc.getLevel()),
                        String.valueOf(acc.getLevelProgress())
                ))
                .setThumbnail(acc.getAvatarUrl());
        return EmbedUtil.setMemberAuthor(eb, member);
    }

}