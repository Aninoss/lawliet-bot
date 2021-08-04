package modules.repair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import commands.Command;
import commands.runnables.fisherysettingscategory.FisheryRolesCommand;
import commands.runnables.utilitycategory.AutoRolesCommand;
import constants.FisheryStatus;
import core.MainLogger;
import core.MemberCacheController;
import core.PermissionCheckRuntime;
import mysql.modules.autoroles.AutoRolesData;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class RolesRepair {

    private static final RolesRepair ourInstance = new RolesRepair();

    public static RolesRepair getInstance() {
        return ourInstance;
    }

    private RolesRepair() {
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new CountingThreadFactory(() -> "Main", "RoleRepair", false));

    public void start(JDA jda, int minutes) {
        executorService.submit(() -> run(jda, minutes));
    }

    public void run(JDA jda, int minutes) {
        for (Guild guild : jda.getGuilds()) {
            processAutoRoles(guild, minutes);
            processFisheryRoles(guild, minutes);
        }
    }

    private void processFisheryRoles(Guild guild, int minutes) {
        FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guild.getIdLong());
        Locale locale = fisheryGuildData.getGuildData().getLocale();
        if (fisheryGuildData.getGuildData().getFisheryStatus() == FisheryStatus.ACTIVE && fisheryGuildData.getRoleIds().size() > 0) {
            MemberCacheController.getInstance().loadMembersFull(guild).join();
            guild.getMembers().stream()
                    .filter(member -> !member.getUser().isBot() && userJoinedRecently(member, minutes))
                    .forEach(member -> checkRoles(
                            locale,
                            Command.getCommandLanguage(FisheryRolesCommand.class, locale).getTitle(),
                            member,
                            fisheryGuildData.getMemberData(member.getIdLong()).getRoles()
                    ));
        }
    }

    private void processAutoRoles(Guild guild, int minutes) {
        AutoRolesData autoRolesData = DBAutoRoles.getInstance().retrieve(guild.getIdLong());
        Locale locale = autoRolesData.getGuildData().getLocale();
        List<Role> roles = autoRolesData.getRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong);
        if (roles.size() > 0) {
            MemberCacheController.getInstance().loadMembersFull(guild).join();
            guild.getMembers().stream()
                    .filter(member -> userJoinedRecently(member, minutes) && !member.isPending())
                    .forEach(member -> checkRoles(
                            locale,
                            Command.getCommandLanguage(AutoRolesCommand.class, locale).getTitle(),
                            member,
                            roles
                    ));
        }
    }

    private void checkRoles(Locale locale, String reason, Member member, List<Role> roles) {
        HashSet<Role> rolesToAdd = new HashSet<>();
        roles.stream()
                .filter(role -> !member.getRoles().contains(role) && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role))
                .forEach(role -> {
                    MainLogger.get().info("Giving role \"{}\" to user \"{}\" on server \"{}\"", role.getName(), member.getUser().getAsTag(), role.getGuild().getName());
                    rolesToAdd.add(role);
                });

        if (rolesToAdd.size() > 0) {
            member.getGuild().modifyMemberRoles(member, rolesToAdd, Collections.emptySet())
                    .reason(reason)
                    .complete();
        }
    }

    private boolean userJoinedRecently(Member member, int minutes) {
        if (member.hasTimeJoined()) {
            return member.getTimeJoined().toInstant().isAfter(Instant.now().minus(minutes, ChronoUnit.MINUTES));
        }
        return false;
    }

}
