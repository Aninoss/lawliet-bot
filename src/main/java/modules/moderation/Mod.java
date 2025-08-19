package modules.moderation;

import commands.Category;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.ModSettingsCommand;
import constants.ExternalLinks;
import core.*;
import core.atomicassets.AtomicRole;
import core.components.ActionRows;
import core.utils.BotPermissionUtil;
import core.utils.FutureUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import modules.Jail;
import modules.schedulers.TempBanScheduler;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.ModerationEntity;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningSlot;
import mysql.modules.warning.ServerWarningsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Mod {

    private static final String EMOJI_AUTOMOD = "👷";

    public static void insertWarning(GuildEntity guildEntity, Member target, Member requester, String reason,
                                     boolean withAutoActions
    ) {
        insertWarning(guildEntity, requester.getGuild(), target.getUser(), requester, reason, withAutoActions);
    }

    public static void insertWarning(GuildEntity guildEntity, Guild guild, User target, Member requester, String reason,
                                     boolean withAutoActions
    ) {
        ServerWarningsData serverWarningsData = DBServerWarnings.getInstance().retrieve(new Pair<>(guild.getIdLong(), target.getIdLong()));
        serverWarningsData.getWarnings().add(new ServerWarningSlot(
                        guild.getIdLong(),
                        target.getIdLong(),
                        Instant.now(),
                        requester.getIdLong(),
                        reason == null || reason.isEmpty() ? null : reason
                )
        );

        if (withAutoActions) {
            String prefix = guildEntity.getPrefix();
            Locale locale = guildEntity.getLocale();

            ModerationEntity moderationEntity = guildEntity.getModeration();
            Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();

            Integer autoKickInfractions = moderationEntity.getAutoKick().getInfractions();
            Integer autoBanInfractions = moderationEntity.getAutoBan().getInfractions();
            Integer autoMuteInfractions = moderationEntity.getAutoMute().getInfractions();
            Integer autoJailInfractions = moderationEntity.getAutoJail().getInfractions();

            Integer autoKickInfractionDays = moderationEntity.getAutoKick().getInfractionDays();
            Integer autoBanInfractionDays = moderationEntity.getAutoBan().getInfractionDays();
            Integer autoMuteInfractionDays = moderationEntity.getAutoMute().getInfractionDays();
            Integer autoJailInfractionDays = moderationEntity.getAutoJail().getInfractionDays();

            boolean autoKick =  autoKickInfractions != null && (autoKickInfractionDays != null ? serverWarningsData.getAmountLatest(autoKickInfractionDays, ChronoUnit.DAYS).size() : serverWarningsData.getWarnings().size()) >= autoKickInfractions;
            boolean autoBan = autoBanInfractions != null && (autoBanInfractionDays != null ? serverWarningsData.getAmountLatest(autoBanInfractionDays, ChronoUnit.DAYS).size() : serverWarningsData.getWarnings().size()) >= autoBanInfractions;
            boolean autoMute = autoMuteInfractions != null && (autoMuteInfractionDays != null ? serverWarningsData.getAmountLatest(autoMuteInfractionDays, ChronoUnit.DAYS).size() : serverWarningsData.getWarnings().size()) >= autoMuteInfractions;
            boolean autoJail = autoJailInfractions != null && (autoJailInfractionDays != null ? serverWarningsData.getAmountLatest(autoJailInfractionDays, ChronoUnit.DAYS).size() : serverWarningsData.getWarnings().size()) >= autoJailInfractions;

            if (autoBan &&
                    PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guild, Permission.BAN_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target)
            ) {
                List<Guild.Ban> banList = guild.retrieveBanList().complete();
                if (banList.stream().noneMatch(ban -> ban.getUser().getIdLong() == target.getIdLong())) {
                    Integer durationMinutes = moderationEntity.getAutoBan().getDurationMinutes();
                    String durationString = durationMinutes != null ? TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(durationMinutes)).toString() : "";

                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                            .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autoban_template", durationMinutes != null, StringUtil.escapeMarkdown(target.getName()), durationString));

                    postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationEntity, target, true).join();

                    guild.ban(target, 0, TimeUnit.DAYS)
                            .reason(TextManager.getString(locale, Category.MODERATION, "mod_autoban"))
                            .queue();

                    if (durationMinutes != null) {
                        TempBanData tempBanData = new TempBanData(guild.getIdLong(), target.getIdLong(), Instant.now().plus(Duration.ofMinutes(durationMinutes)));
                        DBTempBan.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), tempBanData);
                        TempBanScheduler.loadTempBan(tempBanData);
                    }
                }
            } else if (autoKick &&
                    PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guild, Permission.KICK_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target) &&
                    guild.isMember(target)
            ) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autokick_template", StringUtil.escapeMarkdown(target.getName())));

                postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationEntity, target, false).join();
                guild.kick(target)
                        .reason(TextManager.getString(locale, Category.MODERATION, "mod_autokick"))
                        .queue();
            }

            if (autoJail &&
                    member != null &&
                    !BotPermissionUtil.can(member, Permission.ADMINISTRATOR)
            ) {
                List<Role> jailRoles = AtomicRole.to(guildEntity.getModeration().getJailRoles());
                if (PermissionCheckRuntime.botCanManageRoles(locale, ModSettingsCommand.class, jailRoles)) {
                    Integer durationMinutes = moderationEntity.getAutoJail().getDurationMinutes();
                    String durationString = durationMinutes != null ? TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(durationMinutes)).toString() : "";
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_autojail"))
                            .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_autojail_template", durationMinutes != null, StringUtil.escapeMarkdown(target.getName()), durationString));

                    postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationEntity, target, false).join();
                    Jail.jail(guild, member, durationMinutes, TextManager.getString(locale, Category.MODERATION, "mod_autojail"), guildEntity);
                }
            }

            if (autoMute &&
                    PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guild, Permission.MODERATE_MEMBERS) &&
                    BotPermissionUtil.canInteract(guild, target)
            ) {
                Integer durationMinutes = moderationEntity.getAutoMute().getDurationMinutes();
                String durationString = durationMinutes != null ? TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(durationMinutes)).toString() : "";
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, Category.MODERATION, "mod_automute"))
                        .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_automute_template", durationMinutes != null, StringUtil.escapeMarkdown(target.getName()), durationString));

                postLogUsers(CommandManager.createCommandByClass(ModSettingsCommand.class, locale, prefix), eb, guild, moderationEntity, target, false).join();
                Mute.mute(guild, target, durationMinutes, TextManager.getString(locale, Category.MODERATION, "mod_automute"));
            }
        }
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, ModerationEntity moderationEntity, Member member, boolean includeBanAppealButton) {
        return postLogMembers(command, eb, moderationEntity, Collections.singletonList(member), includeBanAppealButton);
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, ModerationEntity moderationEntity, User user, boolean includeBanAppealButton) {
        return postLogUsers(command, eb, guild, moderationEntity, Collections.singletonList(user), includeBanAppealButton);
    }

    public static CompletableFuture<Void> postLogMembers(Command command, EmbedBuilder eb, ModerationEntity moderationEntity, List<Member> members, boolean includeBanAppealButton) {
        eb.setFooter("");

        CompletableFuture<Void> future = FutureUtil.supplyAsync(() -> {
            members.forEach(member -> {
                if (!member.getUser().isBot()) {
                    try {
                        JDAUtil.openPrivateChannel(member)
                                .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()).setComponents(getBanAppealActionRow(moderationEntity, includeBanAppealButton)))
                                .complete();
                    } catch (Throwable e) {
                        MainLogger.get().error("Exception", e);
                    }
                }
            });
            return null;
        });

        sendAnnouncement(command, eb, moderationEntity);
        return future;
    }

    public static CompletableFuture<Void> postLogUsers(Command command, EmbedBuilder eb, Guild guild, ModerationEntity moderationEntity, List<User> users, boolean includeBanAppealButton) {
        eb.setFooter("");

        CompletableFuture<Void> future = FutureUtil.supplyAsync(() -> {
            MemberCacheController.getInstance().loadMembersWithUsers(guild, users).join()
                    .forEach(member -> {
                        if (!member.getUser().isBot()) {
                            try {
                                JDAUtil.openPrivateChannel(member)
                                        .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()).setComponents(getBanAppealActionRow(moderationEntity, includeBanAppealButton)))
                                        .complete();
                            } catch (Throwable e) {
                                MainLogger.get().error("Exception", e);
                            }
                        }
                    });
            return null;
        });

        sendAnnouncement(command, eb, moderationEntity);
        return future;
    }

    public static void sendAnnouncement(Command command, EmbedBuilder eb, ModerationEntity moderationEntity) {
        eb.setFooter("");

        moderationEntity.getLogChannel().get().ifPresent(channel -> {
            if (PermissionCheckRuntime.botHasPermission(command.getLocale(), command.getClass(), channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        });
    }

    private static List<ActionRow> getBanAppealActionRow(ModerationEntity moderationEntity, boolean includeBanAppealButton) {
        if (includeBanAppealButton && moderationEntity.getBanAppealLogChannelIdEffectively() != null) {
            GuildEntity guildEntity = moderationEntity.getHibernateEntity();
            String banAppealButtonLabel = TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "moderation_appealban");
            Button banAppealButton = Button.of(ButtonStyle.LINK, ExternalLinks.BAN_APPEAL_URL + guildEntity.getGuildId(), banAppealButtonLabel);
            return ActionRows.of(banAppealButton);
        } else {
            return Collections.emptyList();
        }
    }

}
