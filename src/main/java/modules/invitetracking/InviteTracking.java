package modules.invitetracking;

import commands.runnables.invitetrackingcategory.InviteTrackingCommand;
import core.*;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InviteTracking {

    public static InviteMetrics generateInviteMetricsForInviterUser(Guild guild, long inviterUserId) {
        return generateInviteMetricsMap(guild).getOrDefault(inviterUserId, new InviteMetrics(guild.getIdLong(), inviterUserId));
    }

    public static HashMap<Long, InviteMetrics> generateInviteMetricsMap(Guild guild) {
        HashMap<Long, InviteMetrics> inviteMetricsMap = new HashMap<>();
        CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots();
        HashSet<Long> serverMemberIds = MemberCacheController.getInstance().loadMembers(guild, inviteTrackingSlots.keySet()).join().stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toCollection(HashSet::new));

        for (InviteTrackingSlot inviteTrackingSlot : inviteTrackingSlots.values()) {
            InviteMetrics inviteMetrics = inviteMetricsMap.computeIfAbsent(inviteTrackingSlot.getInviterUserId(), k -> new InviteMetrics(guild.getIdLong(), inviteTrackingSlot.getInviterUserId()));
            inviteMetrics.incrTotalInvites();
            if (serverMemberIds.contains(inviteTrackingSlot.getMemberId())) {
                inviteMetrics.incrOnServer();
                if (inviteTrackingSlot.isRetained()) {
                    inviteMetrics.incrRetained();
                    if (inviteTrackingSlot.isActive()) {
                        inviteMetrics.incrActive();
                    }
                }
            }
        }

        return inviteMetricsMap;
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

    public static CompletableFuture<TempInvite> registerMemberJoin(Member member, Locale locale) {
        CompletableFuture<TempInvite> future = new CompletableFuture<>();

        try {
            Guild guild = member.getGuild();
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
                                        MainLogger.get().error("Ambiguous invite (guild id: {}, invite 1: {}, invite 2: {})", member.getGuild().getId(), tempInvite.code, invite.code);
                                        tempInvite = null;
                                        ambiguousInvite = true;
                                        break;
                                    }
                                }
                            }

                            /* check temporary invites which no longer exist due to having limit uses and ignore expired ones */
                            if (!ambiguousInvite) {
                                for (String inviteCode : missingInviteCodes) {
                                    GuildInvite guildInvite = databaseInvites.get(inviteCode);
                                    if (guildInvite.getMaxAge() == null || guildInvite.getMaxAge().isAfter(Instant.now())) {
                                        if (tempInvite == null) {
                                            tempInvite = new TempInvite(guildInvite.getCode(), guildInvite.getUses() + 1, guildInvite.getMemberId(), guildInvite.getMaxAge());
                                        } else {
                                            MainLogger.get().error("Ambiguous invite (missing invite code) (guild id: {}, invite 1: {}, invite 2: {})", member.getGuild().getId(), tempInvite.code, guildInvite.getCode());
                                            tempInvite = null;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (tempInvite != null) {
                                CustomObservableMap<Long, InviteTrackingSlot> inviteTrackingSlots = DBInviteTracking.getInstance().retrieve(guild.getIdLong()).getInviteTrackingSlots();
                                if (!inviteTrackingSlots.containsKey(member.getIdLong())) {
                                    InviteTrackingSlot newSlot = new InviteTrackingSlot(guild.getIdLong(), member.getIdLong(), tempInvite.inviter, LocalDate.now(), LocalDate.now(), false);
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

    public static CompletableFuture<Void> synchronizeGuildInvites(Guild guild, Locale locale) {
        CompletableFuture<Void> future = new CompletableFuture<>();
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

    public static Instant calculateMaxAgeOfInvite(Invite invite) {
        if (invite.getMaxAge() > 0) {
            return invite.getTimeCreated().toInstant().plusSeconds(invite.getMaxAge());
        } else {
            return null;
        }
    }

    private static void synchronizeGuildInvites(long guildId, CustomObservableMap<String, GuildInvite> databaseInvites, List<TempInvite> guildInvites) {
        /* add missing invites to database */
        HashSet<String> inviteCodes = new HashSet<>();
        for (TempInvite invite : guildInvites) {
            inviteCodes.add(invite.code);
            if (!databaseInvites.containsKey(invite.code) ||
                    invite.uses != databaseInvites.get(invite.code).getUses() ||
                    (invite.maxAge != null && databaseInvites.get(invite.code).getMaxAge() == null)
            ) {
                databaseInvites.put(invite.code, new GuildInvite(guildId, invite.code, invite.inviter, invite.uses, invite.maxAge));
            }
        }

        /* remove invalid invites from database */
        Iterator<Map.Entry<String, GuildInvite>> iterator = databaseInvites.entrySet().iterator();
        while (iterator.hasNext()) {
            GuildInvite guildInvite = iterator.next().getValue();
            if (guildInvite != null && !inviteCodes.contains(guildInvite.getCode())) {
                iterator.remove();
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
                if (invite.getInviter() == null) {
                    MainLogger.get().error("Inviter user is null (guild id: {}, invite: {})", guild.getId(), invite.getCode());
                    continue;
                }
                inviteList.add(new TempInvite(
                        invite.getCode(),
                        invite.getUses(),
                        invite.getInviter().getIdLong(),
                        calculateMaxAgeOfInvite(invite)
                ));
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
