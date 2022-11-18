package events.discordevents.guildusercontextinteraction;

import constants.AnicordVerificationIds;
import core.EmbedFactory;
import core.ModalMediator;
import core.ShardManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUserContextInteractionAbstract;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

@DiscordEvent
public class GuildUserContextInteractionVerification extends GuildUserContextInteractionAbstract {

    @Override
    public boolean onGuildUserContextInteraction(UserContextInteractionEvent event) throws Throwable {
        Guild guild = ShardManager.getLocalGuildById(AnicordVerificationIds.GUILD_ID).orElse(null);
        if (guild == null) {
            return true;
        }

        Role verificationRole = guild.getRoleById(AnicordVerificationIds.ROLE_ID);
        TextChannel verificationChannel = guild.getTextChannelById(AnicordVerificationIds.CHANNEL_ID);

        if (event.getTargetMember().getRoles().contains(verificationRole)) {
            sendErrorEmbed(event, String.format("%s ist bereits verifiziert!", event.getTargetMember().getAsMention()));
            return true;
        }

        if (!event.getMember().getRoles().contains(verificationRole)) {
            sendErrorEmbed(event, "Du musst selbst verifiziert sein, um andere Mitglieder verifizieren zu können!");
            return true;
        }

        TextInput textInput = TextInput.create("text", "ACHTUNG", TextInputStyle.PARAGRAPH)
                .setValue("Schalte andere Mitglieder nur frei, wenn du sie persönlich kennst. Sollte diese Person negativ auffallen, kannst du für die Folgen mitverantwortlich gemacht werden.")
                .build();

        Modal modal = ModalMediator.createModal("Verifizieren", e -> {
                    verificationRole.getGuild().addRoleToMember(event.getTargetMember(), verificationRole).queue();
                    verificationChannel.sendMessage(String.format("%s wurde von %s verifiziert!", event.getTargetMember().getAsMention(), event.getMember().getAsMention())).queue();

                    MessageEmbed messageEmbed = EmbedFactory.getEmbedDefault()
                            .setTitle("Verifiziert")
                            .setDescription(String.format("%s wurde verifiziert!", event.getTargetMember().getAsMention()))
                            .build();

                    e.replyEmbeds(messageEmbed)
                            .setEphemeral(true)
                            .queue();
                })
                .addActionRow(textInput)
                .build();
        event.replyModal(modal).queue();

        return true;
    }

    private void sendErrorEmbed(UserContextInteractionEvent event, String text) {
        MessageEmbed messageEmbed = EmbedFactory.getEmbedError()
                .setTitle("🚫 Abgelehnt")
                .setDescription(text)
                .build();

        event.replyEmbeds(messageEmbed)
                .setEphemeral(true)
                .queue();
    }

}
