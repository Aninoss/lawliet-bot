package MySQL.Modules.InviteTypeUsages;

import Constants.InviteTypes;
import MySQL.DBMain;

public class DBInviteTypeUsages {

    private static final DBInviteTypeUsages ourInstance = new DBInviteTypeUsages();
    public static DBInviteTypeUsages getInstance() { return ourInstance; }
    private DBInviteTypeUsages() {}

    public void insertInvite(InviteTypes inviteType) {
        DBMain.getInstance().asyncUpdate("INSERT INTO invitetypeusages (`type`, `usages`) VALUES (?, 1) ON DUPLICATE KEY UPDATE `usages` = `usages` + 1;",
                preparedStatement -> {
                    preparedStatement.setString(1, inviteType.name());
                });
    }

}