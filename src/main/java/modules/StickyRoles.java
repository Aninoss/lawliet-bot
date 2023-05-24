package modules;

import java.util.HashSet;
import java.util.stream.Collectors;
import mysql.modules.stickyroles.DBStickyRoles;
import mysql.modules.stickyroles.StickyRolesActionData;
import mysql.modules.stickyroles.StickyRolesData;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;

public class StickyRoles {

    public static void updateFromMemberRoles(Member member, boolean canAdd, boolean canRemove) {
        if (!member.isPending()) {
            synchronized (member) {
                StickyRolesData stickyRolesData = DBStickyRoles.getInstance().retrieve(member.getGuild().getIdLong());
                HashSet<Long> currentActionRoleIds = stickyRolesData.getActions().stream()
                        .filter(actionData -> actionData != null && actionData.getMemberId() == member.getIdLong())
                        .map(StickyRolesActionData::getRoleId)
                        .collect(Collectors.toCollection(HashSet::new));
                HashSet<Long> currentMemberRoleIds = member.getRoles().stream()
                        .map(ISnowflake::getIdLong)
                        .collect(Collectors.toCollection(HashSet::new));

                /* add */
                if (canAdd) {
                    stickyRolesData.getRoleIds().stream()
                            .filter(stickyRoleId -> !currentActionRoleIds.contains(stickyRoleId) && currentMemberRoleIds.contains(stickyRoleId))
                            .forEach(stickyRoleId -> {
                                stickyRolesData.getActions().add(new StickyRolesActionData(
                                        member.getGuild().getIdLong(),
                                        member.getIdLong(),
                                        stickyRoleId
                                ));
                            });
                }

                /* remove */
                if (canRemove) {
                    stickyRolesData.getActions()
                            .removeIf(actionData -> member.getIdLong() == actionData.getMemberId() && !currentMemberRoleIds.contains(actionData.getRoleId()));
                }
            }
        }
    }

}
