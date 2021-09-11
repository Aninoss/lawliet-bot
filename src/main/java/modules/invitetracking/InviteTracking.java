package modules.invitetracking;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import commands.runnables.utilitycategory.InviteTrackingCommand;
import core.CustomObservableMap;
import core.PermissionCheckRuntime;
import mysql.modules.guild.DBGuild;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.GuildInvite;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class InviteTracking {

    public static InviteMetrics generateInviteMetrics(long guildId, long inviterUserId) {
        int invites = 0;
        int activated = 0;
        int active = 0;
        for (InviteTrackingSlot inviteTrackingSlot : DBInviteTracking.getInstance().retrieve(guildId).getInviteTrackingSlots().values()) {
            if (inviteTrackingSlot.getInviterUserId() == inviterUserId) {
                invites++;
                if (inviteTrackingSlot.isActivated()) {
                    activated++;
                }
                if (inviteTrackingSlot.isActive()) {
                    active++;
                }
            }
        }

        return new InviteMetrics(invites, activated, active);
    }

    public static void memberActivity(Member member) {
        if (DBGuild.getInstance().retrieve(member.getGuild().getIdLong()).isInviteTracking()) {
            InviteTrackingSlot inviteTrackingSlot = DBInviteTracking.getInstance()
                    .retrieve(member.getGuild().getIdLong())
                    .getInviteTrackingSlots()
                    .get(member.getIdLong());

            if (inviteTrackingSlot != null) {
                inviteTrackingSlot.messageSent();
            }
        }
    }

    public static CompletableFuture<Long> registerMemberJoin(Member member) {
        CompletableFuture<Long> future = new CompletableFuture<>();

        try {
            Guild guild = member.getGuild();
            Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();

            if (PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, guild, Permission.MANAGE_SERVER)) {
                guild.retrieveInvites().queue(guildInvites -> {
                    CustomObservableMap<String, GuildInvite> databaseInvites = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getGuildInvites();
                    long inviterId = 0L;

                    for (Invite invite : guildInvites) {
                        int inviteUses = invite.getUses();
                        int databaseUses = 0;
                        GuildInvite guildInvite = databaseInvites.get(invite.getCode());
                        if (guildInvite != null) {
                            databaseUses = guildInvite.getUses();
                        }

                        if (inviteUses > databaseUses) {
                            if (inviterId == 0L && inviteUses == databaseUses + 1) {
                                inviterId = invite.getInviter().getIdLong();
                            } else {
                                inviterId = 0L;
                                break;
                            }
                        }
                    }

                    if (inviterId != 0L) {
                        CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots();
                        if (!inviteTrackingSlots.containsKey(member.getIdLong())) {
                            InviteTrackingSlot newSlot = new InviteTrackingSlot(guild.getIdLong(), member.getIdLong(), inviterId, LocalDate.now(), LocalDate.now());
                            inviteTrackingSlots.put(member.getIdLong(), newSlot);
                        }
                        future.complete(inviterId);
                    } else {
                        future.completeExceptionally(new NoSuchElementException("No inviter found"));
                    }
                    synchronizeGuildInvites(guild.getIdLong(), databaseInvites, guildInvites);
                }, future::completeExceptionally);
            } else {
                future.completeExceptionally(new PermissionException("Missing permissions"));
            }
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public static CompletableFuture<Void> synchronizeGuildInvites(Guild guild) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();
        if (PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, guild, Permission.MANAGE_SERVER)) {
            guild.retrieveInvites().queue(guildInvites -> {
                CustomObservableMap<String, GuildInvite> databaseInvites = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getGuildInvites();
                synchronizeGuildInvites(guild.getIdLong(), databaseInvites, guildInvites);
                future.complete(null);
            }, future::completeExceptionally);
        }

        return future;
    }

    private static void synchronizeGuildInvites(long guildId, CustomObservableMap<String, GuildInvite> databaseInvites, List<Invite> guildInvites) {
        /* add missing invites to database */
        HashSet<String> inviteCodes = new HashSet<>();
        for (Invite invite : guildInvites) {
            inviteCodes.add(invite.getCode());
            if (!databaseInvites.containsKey(invite.getCode()) || invite.getUses() != databaseInvites.get(invite.getCode()).getUses()) {
                databaseInvites.put(invite.getCode(), new GuildInvite(guildId, invite.getCode(), invite.getInviter().getIdLong(), invite.getUses()));
            }
        }

        /* remove invalid invites from database */
        for (GuildInvite guildInvite : new ArrayList<>(databaseInvites.values())) {
            if (!inviteCodes.contains(guildInvite.getCode())) {
                databaseInvites.remove(guildInvite.getCode());
            }
        }
    }

}
