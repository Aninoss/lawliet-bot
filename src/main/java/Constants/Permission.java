package Constants;

public interface Permission {

    int ADMINISTRATOR = 0x1,
            ATTACH_FILES = 0x2,
            KICK_MEMBERS = 0x4,
            BAN_MEMBERS = 0x8,
            CHANGE_OWN_NICKNAME = 0x10,
            READ_MESSAGES = 0x20,
            MANAGE_CHANNELS_ON_SERVER = 0x40,
            DEAFEN_MEMBERS = 0x80,
            MANAGE_EMOJIS = 0x100,
            EMBED_LINKS = 0x200,
            MANAGE_MESSAGES = 0x400,
            MANAGE_NICKNAMES = 0x800,
            MANAGE_ROLES = 0x1000,
            MANAGE_CHANNEL_PERMISSIONS = 0x2000,
            MANAGE_SERVER = 0x4000,
            MENTION_EVERYONE = 0x8000,
            MOVE_MEMBERS = 0x10000,
            MUTE_MEMBERS = 0x20000,
            READ_MESSAGE_HISTORY = 0x40000,
            SEND_MESSAGES = 0x80000,
            VIEW_AUDIT_LOG = 0x100000,
            SEND_TTS_MESSAGES = 0x200000,
            USE_EXTERNAL_EMOJIS = 0x400000,
            ADD_REACTIONS = 0x800000,
            MANAGE_CHANNEL = 0x1000000,
            MANAGE_WEBHOOKS = 0x2000000,
            CREATE_INSTANT_INVITE = 0x4000000,
            CONNECT = 0x8000000;

    int MAX = CONNECT;

}