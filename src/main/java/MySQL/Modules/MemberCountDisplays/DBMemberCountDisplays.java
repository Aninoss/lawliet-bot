package MySQL.Modules.MemberCountDisplays;

import MySQL.DBDataLoad;
import MySQL.DBBeanGenerator;
import MySQL.DBMain;
import MySQL.Modules.Server.DBServer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class DBMemberCountDisplays extends DBBeanGenerator<Long, MemberCountBean> {

    private static DBMemberCountDisplays ourInstance = new DBMemberCountDisplays();
    public static DBMemberCountDisplays getInstance() { return ourInstance; }
    private DBMemberCountDisplays() {}

    @Override
    protected MemberCountBean loadBean(Long serverId) throws Exception {
        MemberCountBean memberCountBean = new MemberCountBean(
                serverId,
                DBServer.getInstance().getBean(serverId),
                getMemberCountBeanSlot(serverId)
        );

        memberCountBean.getMemberCountBeanSlots()
                .addMapAddListener(slot -> addMemberCountBeanSlot(serverId, slot))
                .addMapRemoveListener(slot -> removeMemberCountBeanSlot(serverId, slot));

        return memberCountBean;
    }

    @Override
    protected void saveBean(MemberCountBean memberCountBean) {}

    private HashMap<Long, MemberCountDisplay> getMemberCountBeanSlot(long serverId) throws SQLException {
        return new DBDataLoad<MemberCountDisplay>("MemberCountDisplays", "vcId, name", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getHashMap(MemberCountDisplay::getVoiceChannelId, resultSet -> new MemberCountDisplay(serverId, resultSet.getLong(1), resultSet.getString(2)));
    }

    private void addMemberCountBeanSlot(long serverId, MemberCountDisplay memberCountDisplay) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO MemberCountDisplays (serverId, vcId, name) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, memberCountDisplay.getVoiceChannelId());
            preparedStatement.setString(3, memberCountDisplay.getMask());
        });
    }

    private void removeMemberCountBeanSlot(long serverId, MemberCountDisplay memberCountDisplay) {
        DBMain.getInstance().asyncUpdate("DELETE FROM MemberCountDisplays WHERE serverId = ? AND vcId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, memberCountDisplay.getVoiceChannelId());
        });
    }

}
