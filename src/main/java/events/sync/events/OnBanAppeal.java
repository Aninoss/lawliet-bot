package events.sync.events;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.ModSettingsCommand;
import core.EmbedFactory;
import core.ShardManager;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.BanAppealEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.ModerationEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

@SyncServerEvent(event = "BAN_APPEAL")
public class OnBanAppeal implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        String username = jsonObject.getString("username");
        String avatar = jsonObject.getString("avatar");
        long guildId = jsonObject.getLong("guild_id");
        String message = jsonObject.getString("message");

        Guild guild = ShardManager.getLocalGuildById(guildId).orElse(null);
        OnBanAppealInit.Response response = OnBanAppealInit.getResponse(guild, userId);

        JSONObject responseJson = new JSONObject();
        boolean ok = response == OnBanAppealInit.Response.OK && submit(guild, userId, username, avatar, message);
        responseJson.put("ok", ok);
        if (ok) {
            FeatureLogger.inc(PremiumFeature.BAN_APPEALS, guildId);
        }

        return responseJson;
    }

    private boolean submit(Guild guild, long userId, String username, String avatar, String appealMessage) {
        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guild.getIdLong(), OnBanAppeal.class)) {
            ModerationEntity moderationEntity = guildEntity.getModeration();

            GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, moderationEntity.getBanAppealLogChannelIdEffectively());
            if (channel == null) {
                return false;
            }
            EmbedBuilder eb = generateEmbed(guildEntity.getLocale(), userId, username, avatar, appealMessage);
            Message message = channel.sendMessageEmbeds(eb.build())
                    .setComponents(generateActionRow(guildEntity.getLocale()))
                    .complete();

            StaticReactionMessageData staticReactionMessageData = new StaticReactionMessageData(message,
                    Command.getCommandProperties(ModSettingsCommand.class).trigger(), String.valueOf(userId)
            );
            DBStaticReactionMessages.getInstance()
                    .retrieve(guild.getIdLong())
                    .put(message.getIdLong(), staticReactionMessageData);

            moderationEntity.beginTransaction();
            moderationEntity.getBanAppeals().put(userId, new BanAppealEntity(appealMessage, true));
            moderationEntity.commitTransaction();
        }

        return true;
    }

    private EmbedBuilder generateEmbed(Locale locale, long userId, String username, String avatar, String message) {
        return EmbedFactory.getEmbedDefault()
                .setDescription(TextManager.getString(locale, Category.MODERATION, "mod_banappeals_permissions"))
                .setAuthor(username + " (" + userId + ")", null, avatar)
                .setTitle(TextManager.getString(locale, Category.MODERATION, "mod_banappeals_title"))
                .addField(TextManager.getString(locale, Category.MODERATION, "mod_banappeals_message"), message, false);
    }

    private ActionRow generateActionRow(Locale locale) {
        ButtonStyle[] buttonStyles = new ButtonStyle[]{ButtonStyle.PRIMARY, ButtonStyle.DANGER, ButtonStyle.DANGER};
        String[] buttonIds = new String[]{ModSettingsCommand.BUTTON_ID_UNBAN, ModSettingsCommand.BUTTON_ID_DECLINE, ModSettingsCommand.BUTTON_ID_DECLINE_PERMANENTLY};
        String[] buttonLabels = TextManager.getString(locale, Category.MODERATION, "mod_banappeals_buttons").split("\n");

        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < buttonLabels.length; i++) {
            Button button = Button.of(buttonStyles[i], buttonIds[i], buttonLabels[i]);
            buttons.add(button);
        }

        return ActionRow.of(buttons);
    }

}
