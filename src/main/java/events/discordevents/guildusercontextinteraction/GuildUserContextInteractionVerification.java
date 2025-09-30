package events.discordevents.guildusercontextinteraction;

import constants.AnicordVerificationIds;
import core.EmbedFactory;
import core.ShardManager;
import core.modals.ModalMediator;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUserContextInteractionAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

@DiscordEvent
public class GuildUserContextInteractionVerification extends GuildUserContextInteractionAbstract {

    @Override
    public boolean onGuildUserContextInteraction(UserContextInteractionEvent event, EntityManagerWrapper entityManager) {
        if (event.isGlobalCommand()) {
            return true;
        }

        Guild guild = ShardManager.getLocalGuildById(AnicordVerificationIds.GUILD_ID).orElse(null);
        if (guild == null) {
            return true;
        }

        Role verificationRole = guild.getRoleById(AnicordVerificationIds.ROLE_ID);
        GuildMessageChannel verificationChannel = guild.getChannelById(GuildMessageChannel.class, AnicordVerificationIds.CHANNEL_ID);

        if (event.getTargetMember().getRoles().contains(verificationRole)) {
            sendErrorEmbed(event, String.format("%s ist bereits verifiziert!", event.getTargetMember().getAsMention()));
            return true;
        }

        if (!event.getMember().getRoles().contains(verificationRole)) {
            sendErrorEmbed(event, "Du musst selbst verifiziert sein, um andere Mitglieder verifizieren zu können!");
            return true;
        }

        TextInput textInput = TextInput.create("text", TextInputStyle.PARAGRAPH)
                .setValue("Schalte andere Mitglieder nur frei, wenn du sie persönlich kennst. Sollte diese Person negativ auffallen, kannst du für die Folgen mitverantwortlich gemacht werden.")
                .build();

        Modal modal = ModalMediator.createModal(event.getMember().getIdLong(), "Verifizieren", (e, em) -> {
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
                .addComponents(Label.of("ACHTUNG", textInput))
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
