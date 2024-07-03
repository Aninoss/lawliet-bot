package modules.invitetracking;

import commands.runnables.invitetrackingcategory.InviteTrackingCommand;
import core.MemberCacheController;
import core.PermissionCheckRuntime;
import mysql.hibernate.entity.GuildInviteEntity;
import mysql.hibernate.entity.InviteTrackingSlotEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.InviteTrackingEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class InviteTracking {

    public static InviteMetrics generateInviteMetrics(InviteTrackingEntity inviteTracking, Guild guild, long inviterUserId) {
        int invites = 0;
        int onServer = 0;
        int retained = 0;
        int active = 0;

        HashSet<Long> serverMemberIds = MemberCacheController.getInstance().loadMembersFull(guild).join().stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toCollection(HashSet::new));
        for (Map.Entry<Long, InviteTrackingSlotEntity> entry : inviteTracking.getSlots().entrySet()) {
            if (entry.getValue().getInviterUserId() == inviterUserId) {
                invites++;
                if (serverMemberIds.contains(entry.getKey())) {
                    onServer++;
                    if (entry.getValue().isRetained()) {
                        retained++;
                        if (entry.getValue().isActive()) {
                            active++;
                        }
                    }
                }
            }
        }

        return new InviteMetrics(guild.getIdLong(), inviterUserId, invites, onServer, retained, active);
    }

    public static void memberActivity(InviteTrackingEntity inviteTracking, Member member) {
        if (inviteTracking.getActive()) {
            InviteTrackingSlotEntity slot = inviteTracking.getSlots().get(member.getIdLong());
            if (slot != null) {
                inviteTracking.beginTransaction();
                slot.setLastMessageDate(LocalDate.now());
                inviteTracking.commitTransaction();
            }
        }
    }

    public static TempInvite registerMemberJoin(GuildEntity guildEntity, Member member, Locale locale) throws ExecutionException, InterruptedException {
        Guild guild = member.getGuild();
        if (!PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, guild, Permission.MANAGE_SERVER)) {
            return null;
        }

        InviteTrackingEntity inviteTrackingEntity = guildEntity.getInviteTracking();
        List<TempInvite> guildInvites = collectInvites(guild).get();
        Map<String, GuildInviteEntity> databaseInvites = guildEntity.getGuildInvites();
        HashSet<String> missingInviteCodes = new HashSet<>(databaseInvites.keySet());
        TempInvite tempInvite = null;
        boolean ambiguousInvite = false;

        /* check invite uses */
        for (TempInvite invite : guildInvites) {
            missingInviteCodes.remove(invite.code);
            int inviteUses = invite.uses;
            int databaseUses = 0;
            GuildInviteEntity guildInvite = databaseInvites.get(invite.code);
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

        /* check temporary invites which no longer exist due to having limit uses and ignore expired ones */
        if (!ambiguousInvite) {
            for (String inviteCode : missingInviteCodes) {
                GuildInviteEntity guildInvite = databaseInvites.get(inviteCode);
                if (guildInvite.getMaxAge() == null || guildInvite.getMaxAge().isAfter(Instant.now())) {
                    if (tempInvite == null) {
                        tempInvite = new TempInvite(inviteCode, guildInvite.getUses() + 1, guildInvite.getUserId(), guildInvite.getMaxAge());
                    } else {
                        tempInvite = null;
                        break;
                    }
                }
            }
        }

        inviteTrackingEntity.beginTransaction();
        if (tempInvite != null) {
            Map<Long, InviteTrackingSlotEntity> inviteTrackingSlots = inviteTrackingEntity.getSlots();
            if (!inviteTrackingSlots.containsKey(member.getIdLong())) {
                InviteTrackingSlotEntity newSlot = new InviteTrackingSlotEntity(tempInvite.inviter, LocalDate.now(), LocalDate.now(), false);
                inviteTrackingSlots.put(member.getIdLong(), newSlot);
            }
        }
        synchronizeGuildInvites(guildEntity, guildInvites);
        inviteTrackingEntity.commitTransaction();
        return tempInvite;
    }

    public static void synchronizeGuildInvites(GuildEntity guildEntity, Guild guild) throws ExecutionException, InterruptedException {
        if (PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), InviteTrackingCommand.class, guild, Permission.MANAGE_SERVER)) {
            List<TempInvite> tempInvites = collectInvites(guild).get();
            synchronizeGuildInvites(guildEntity, tempInvites);
        }
    }

    public static Instant calculateMaxAgeOfInvite(Invite invite) {
        if (invite.getMaxAge() > 0) {
            return invite.getTimeCreated().toInstant().plusSeconds(invite.getMaxAge());
        } else {
            return null;
        }
    }

    private static void synchronizeGuildInvites(GuildEntity guildEntity, List<TempInvite> guildInvites) {
        /* add missing invites to database */
        Map<String, GuildInviteEntity> databaseInvites = guildEntity.getGuildInvites();
        HashSet<String> inviteCodes = new HashSet<>();
        for (TempInvite invite : guildInvites) {
            inviteCodes.add(invite.code);
            if (databaseInvites.containsKey(invite.code)) {
                GuildInviteEntity databaseInvite = databaseInvites.get(invite.code);
                if (invite.uses != databaseInvite.getUses() ||
                        (invite.maxAge != null && databaseInvite.getMaxAge() == null)
                ) {
                    databaseInvite.setUses(invite.uses);
                    databaseInvite.setMaxAge(invite.maxAge);
                }
            } else {
                databaseInvites.put(invite.code, new GuildInviteEntity(invite.inviter, invite.uses, invite.maxAge));
            }
        }

        /* remove invalid invites from database */
        databaseInvites.entrySet().removeIf(entry -> !inviteCodes.contains(entry.getKey()));
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
                        0L,
                        null
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
                            invite.getInviter().getIdLong(),
                            calculateMaxAgeOfInvite(invite)
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
        private final Instant maxAge;

        public TempInvite(String code, int uses, long inviter, Instant maxAge) {
            this.code = code;
            this.uses = uses;
            this.inviter = inviter;
            this.maxAge = maxAge;
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

        public Instant getMaxAge() {
            return maxAge;
        }

    }

}
