package modules.invitetracking;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import commands.runnables.invitetrackingcategory.InviteTrackingCommand;
import core.*;
import mysql.modules.guild.DBGuild;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.GuildInvite;
import mysql.modules.invitetracking.InviteTrackingData;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class InviteTracking {

    public static InviteMetrics generateInviteMetrics(Guild guild, long inviterUserId) {
        int invites = 0;
        int onServer = 0;
        int retained = 0;
        int active = 0;

        HashSet<Long> serverMemberIds = MemberCacheController.getInstance().loadMembersFull(guild).join().stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toCollection(HashSet::new));
        for (InviteTrackingSlot inviteTrackingSlot : DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots().values()) {
            if (inviteTrackingSlot.getInviterUserId() == inviterUserId) {
                invites++;
                if (serverMemberIds.contains(inviteTrackingSlot.getMemberId())) {
                    onServer++;
                    if (inviteTrackingSlot.isRetained()) {
                        retained++;
                        if (inviteTrackingSlot.isActive()) {
                            active++;
                        }
                    }
                }
            }
        }

        return new InviteMetrics(guild.getIdLong(), inviterUserId, invites, onServer, retained, active);
    }

    public static void memberActivity(Member member) {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(member.getGuild().getIdLong());
        if (inviteTrackingData.isActive()) {
            InviteTrackingSlot inviteTrackingSlot = inviteTrackingData
                    .getInviteTrackingSlots()
                    .get(member.getIdLong());

            if (inviteTrackingSlot != null) {
                inviteTrackingSlot.messageSent();
            }
        }
    }

    public static CompletableFuture<TempInvite> registerMemberJoin(Member member) {
        CompletableFuture<TempInvite> future = new CompletableFuture<>();

        try {
            Guild guild = member.getGuild();
            Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();
            if (PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, guild, Permission.MANAGE_SERVER)) {
                collectInvites(guild)
                        .thenAccept(guildInvites -> {
                            CustomObservableMap<String, GuildInvite> databaseInvites = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getGuildInvites();
                            HashSet<String> missingInviteCodes = databaseInvites.values().stream()
                                    .map(GuildInvite::getCode)
                                    .collect(Collectors.toCollection(HashSet::new));
                            TempInvite tempInvite = null;
                            boolean ambiguousInvite = false;

                            /* check invite uses */
                            for (TempInvite invite : guildInvites) {
                                missingInviteCodes.remove(invite.code);
                                int inviteUses = invite.uses;
                                int databaseUses = 0;
                                GuildInvite guildInvite = databaseInvites.get(invite.code);
                                if (guildInvite != null) {
                                    databaseUses = guildInvite.getUses();
                                }

                                if (inviteUses > databaseUses) {
                                    if (tempInvite == null) {
                                        tempInvite = invite;
                                    } else {
                                        tempInvite = null;
                                        ambiguousInvite = true;
                                        break;
                                    }
                                }
                            }

                            /* check temporary invites which no longer exist due to having limit uses */
                            if (!ambiguousInvite) {
                                for (String inviteCode : missingInviteCodes) {
                                    if (tempInvite == null) {
                                        GuildInvite guildInvite = databaseInvites.get(inviteCode);
                                        tempInvite = new TempInvite(guildInvite.getCode(), guildInvite.getUses() + 1, guildInvite.getMemberId());
                                    } else {
                                        tempInvite = null;
                                        break;
                                    }
                                }
                            }

                            if (tempInvite != null) {
                                CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots();
                                if (!inviteTrackingSlots.containsKey(member.getIdLong())) {
                                    InviteTrackingSlot newSlot = new InviteTrackingSlot(guild.getIdLong(), member.getIdLong(), tempInvite.inviter, LocalDate.now(), LocalDate.now());
                                    inviteTrackingSlots.put(member.getIdLong(), newSlot);
                                }
                                future.complete(tempInvite);
                            } else {
                                future.completeExceptionally(new NoSuchElementException("No inviter found"));
                            }
                            synchronizeGuildInvites(guild.getIdLong(), databaseInvites, guildInvites);
                        }).exceptionally(ExceptionLogger.get());
            } else {
                future.completeExceptionally(new PermissionException("Missing permissions"));
            }
        } catch (Throwable e) {
            MainLogger.get().error("Invite error", e);
            future.completeExceptionally(e);
        }

        return future;
    }

    public static CompletableFuture<Void> synchronizeGuildInvites(Guild guild) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();
        if (PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, guild, Permission.MANAGE_SERVER)) {
            collectInvites(guild)
                    .thenAccept(guildInvites -> {
                        CustomObservableMap<String, GuildInvite> databaseInvites = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getGuildInvites();
                        synchronizeGuildInvites(guild.getIdLong(), databaseInvites, guildInvites);
                        future.complete(null);
                    }).exceptionally(ExceptionLogger.get());
        }

        return future;
    }

    private static void synchronizeGuildInvites(long guildId, CustomObservableMap<String, GuildInvite> databaseInvites, List<TempInvite> guildInvites) {
        /* add missing invites to database */
        HashSet<String> inviteCodes = new HashSet<>();
        for (TempInvite invite : guildInvites) {
            inviteCodes.add(invite.code);
            if (!databaseInvites.containsKey(invite.code) || invite.uses != databaseInvites.get(invite.code).getUses()) {
                databaseInvites.put(invite.code, new GuildInvite(guildId, invite.code, invite.inviter, invite.uses));
            }
        }

        /* remove invalid invites from database */
        GuildInvite[] invites = databaseInvites.values().toArray(new GuildInvite[0]);
        for (GuildInvite guildInvite : invites) {
            if (!inviteCodes.contains(guildInvite.getCode())) {
                databaseInvites.remove(guildInvite.getCode());
            }
        }
    }

    private static CompletableFuture<List<TempInvite>> collectInvites(Guild guild) {
        CompletableFuture<List<TempInvite>> future = new CompletableFuture<>();
        ArrayList<TempInvite> inviteList = new ArrayList<>();
        boolean[] completed = new boolean[2];

        if (guild.getVanityCode() != null) {
            guild.retrieveVanityInvite().queue(vanityInvite -> {
                TempInvite tempInvite = new TempInvite(
                        vanityInvite.getCode(),
                        vanityInvite.getUses(),
                        0L
                );
                inviteList.add(tempInvite);
                completed[0] = true;
                if (completed[1]) {
                    future.complete(inviteList);
                }
            }, future::completeExceptionally);
        } else {
            completed[0] = true;
        }

        guild.retrieveInvites().queue(invites -> {
            for (Invite invite : invites) {
                if (invite.getInviter() != null) {
                    inviteList.add(new TempInvite(
                            invite.getCode(),
                            invite.getUses(),
                            invite.getInviter().getIdLong()
                    ));
                }
            }
            completed[1] = true;
            if (completed[0]) {
                future.complete(inviteList);
            }
        }, future::completeExceptionally);

        return future;
    }


    public static class TempInvite {

        private final String code;
        private final int uses;
        private final long inviter;

        public TempInvite(String code, int uses, long inviter) {
            this.code = code;
            this.uses = uses;
            this.inviter = inviter;
        }

        public String getCode() {
            return code;
        }

        public int getUses() {
            return uses;
        }

        public long getInviter() {
            return inviter;
        }

    }

}
