package modules.mute;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelUpdater;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.user.User;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class MuteManager {

    private static final MuteManager instance = new MuteManager();
    private MuteManager() {}
    public static MuteManager getInstance() {
        return instance;
    }

    public boolean executeMute(MuteData muteData, boolean mute) throws ExecutionException, InterruptedException {
        return updatePermissions(muteData, mute);
    }

    private boolean updatePermissions(MuteData muteData, boolean mute) throws ExecutionException, InterruptedException {
        boolean doneSomething = false;

        ServerTextChannel channel = muteData.getChannel();
        ServerTextChannelUpdater updateChannel = channel.createUpdater();
        ArrayList<User> users = muteData.getUsers();
        //Map<Long, Permissions> userPermissions = channel.getOverwrittenUserPermissions();
        Map<User, Permissions> userPermissions = channel.getOverwrittenUserPermissions();

        for(User user: users) {
            PermissionsBuilder permissions = null;

            boolean cont = true;
            //if (userPermissions.containsKey(user.getId()))
                //permissions = userPermissions.get(user.getId()).toBuilder();
            if (userPermissions.containsKey(user))
                permissions = userPermissions.get(user).toBuilder();
            else {
                if (mute) permissions = new PermissionsBuilder().setAllUnset();
                else cont = false;
            }

            if (cont) {
                if (mute && permissions.getState(PermissionType.SEND_MESSAGES) != PermissionState.DENIED)
                    doneSomething = true;
                else if (!mute && permissions.getState(PermissionType.SEND_MESSAGES) == PermissionState.DENIED)
                    doneSomething = true;

                permissions.setState(PermissionType.SEND_MESSAGES, mute ? PermissionState.DENIED : PermissionState.UNSET);
                PermissionsBuilder finalPermissions = permissions;
                if (!mute && Stream.of(PermissionType.values()).allMatch(permissionType -> finalPermissions.getState(permissionType) == PermissionState.UNSET))
                    updateChannel.removePermissionOverwrite(user);
                else
                    updateChannel.addPermissionOverwrite(user, permissions.build());
            }
        }

        if (doneSomething) updateChannel.update().get();

        return doneSomething;
    }

}
