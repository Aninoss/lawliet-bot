package modules;

import mysql.hibernate.entity.GuildEntity;
import mysql.hibernate.entity.StickyRolesEntity;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;

import java.util.Set;
import java.util.stream.Collectors;

public class StickyRoles {

    public static void updateFromMemberRoles(GuildEntity guildEntity, Member member) {
        if (!member.isPending()) {
            synchronized (member) {
                StickyRolesEntity stickyRolesEntity = guildEntity.getStickyRoles();

                Set<Long> activeRoleIds = stickyRolesEntity.getActiveRoleIdsForMember(member.getIdLong());
                Set<Long> currentMemberRoleIds = member.getRoles().stream()
                        .map(ISnowflake::getIdLong)
                        .collect(Collectors.toSet());

                /* add */
                Set<Long> addActiveRoleIds = stickyRolesEntity.getRoleIds().stream()
                        .filter(currentMemberRoleIds::contains)
                        .collect(Collectors.toSet());

                /* remove */
                Set<Long> removeActiveRoleIds = activeRoleIds.stream()
                        .filter(activeRoleId -> !currentMemberRoleIds.contains(activeRoleId))
                        .collect(Collectors.toSet());

                if (addActiveRoleIds.isEmpty() && removeActiveRoleIds.isEmpty()) {
                    return;
                }

                stickyRolesEntity.beginTransaction();
                stickyRolesEntity.addActiveRoleIdsForMember(member.getIdLong(), addActiveRoleIds);
                stickyRolesEntity.removeActiveRoleIdsForMember(member.getIdLong(), removeActiveRoleIds);
                stickyRolesEntity.commitTransaction();
            }
        }
    }

}
