package mysql.modules.membercountdisplays;

import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

import java.util.Map;

public class DBMemberCountDisplays extends DBObserverMapCache<Long, MemberCountData> {

    private static final DBMemberCountDisplays ourInstance = new DBMemberCountDisplays();

    public static DBMemberCountDisplays getInstance() {
        return ourInstance;
    }

    private DBMemberCountDisplays() {
    }

    @Override
    protected MemberCountData load(Long serverId) throws Exception {
        MemberCountData memberCountData = new MemberCountData(
                serverId,
                getMemberCountBeanSlot(serverId)
        );

        memberCountData.getMemberCountDisplaySlots()
                .addMapAddListener(slot -> addMemberCountBeanSlot(serverId, slot))
                .addMapRemoveListener(slot -> removeMemberCountBeanSlot(serverId, slot));

        return memberCountData;
    }

    @Override
    protected void save(MemberCountData memberCountBean) {
    }

    private Map<Long, MemberCountDisplaySlot> getMemberCountBeanSlot(long serverId) {
        return new DBDataLoad<MemberCountDisplaySlot>("MemberCountDisplays", "vcId, name", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getMap(MemberCountDisplaySlot::getVoiceChannelId, resultSet -> new MemberCountDisplaySlot(serverId, resultSet.getLong(1), resultSet.getString(2)));
    }

    private void addMemberCountBeanSlot(long serverId, MemberCountDisplaySlot memberCountDisplaySlot) {
        MySQLManager.asyncUpdate("REPLACE INTO MemberCountDisplays (serverId, vcId, name) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, memberCountDisplaySlot.getVoiceChannelId());
            preparedStatement.setString(3, memberCountDisplaySlot.getMask());
        });
    }

    private void removeMemberCountBeanSlot(long serverId, MemberCountDisplaySlot memberCountDisplaySlot) {
        MySQLManager.asyncUpdate("DELETE FROM MemberCountDisplays WHERE serverId = ? AND vcId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, memberCountDisplaySlot.getVoiceChannelId());
        });
    }

}
