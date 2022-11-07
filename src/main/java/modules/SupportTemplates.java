package modules;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import constants.Language;
import core.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SupportTemplates {

    private static final String[] COMMANDS = {
            "Support wrong channel",
            "#faq",
            "#news",
            "English",
            "#gif-requests-info"
    };

    private static final String[] TEXT_WRONG_CHANNEL = {
            "This channel is not suited for bot support, please go to <#557955318722723891> instead or create a ticket in <#843836754271404062>",
            "Dieser Kanal ist nicht für Bot-Support geeignet, bitte gehe stattdessen zu <#601015368785330196> oder erstelle ein Ticket in <#843836754271404062>",
            "Este canal no es adecuado para el soporte de bots, por favor vaya a <#840725889113522206> en su lugar o cree un ticket en <#843836754271404062>",
            "Этот канал не подходит для поддержки ботов, пожалуйста, перейдите на <#840722552569462784> или создайте тикет в <#843836754271404062>"
    };

    private static final String[] TEXT_FAQ = {
            "Your question can be answered by looking at <#703949030673088533>",
            "Deine Frage kann durch einen Blick in <#703949030673088533> beantwortet werden",
            "Su pregunta puede ser respondida mirando <#703949030673088533>",
            "На ваш вопрос можно ответить, посмотрев на <#703949030673088533>"
    };

    private static final String[] TEXT_NEWS = {
            "Your question can be answered by looking at <#557960859792441357>",
            "Deine Frage kann durch einen Blick in <#557960859792441357> beantwortet werden",
            "Su pregunta puede ser respondida mirando <#557960859792441357>",
            "На ваш вопрос можно ответить, посмотрев на <#557960859792441357>"
    };

    private static final String TEXT_ENGLISH = "This is an English server, please check <#557961281034780704> for other languages";
    private static final String TEXT_GIF_REQUEST_INFO = "Your gif request is invalid, please take a look at <#708252207690154045> before requesting a new gif for role play commands";

    public static List<CommandData> generateSupportContextCommands() {
        return Arrays.stream(COMMANDS)
                .map(Commands::message)
                .collect(Collectors.toList());
    }

    public static void process(MessageContextInteractionEvent event) {
        Language language = findLanguage(event.getChannel().asTextChannel().getParentCategory());
        String text = switch (findCommandIndex(event.getName())) {
            case 0 -> TEXT_WRONG_CHANNEL[language.ordinal()];
            case 1 -> TEXT_FAQ[language.ordinal()];
            case 2 -> TEXT_NEWS[language.ordinal()];
            case 3 -> TEXT_ENGLISH;
            case 4 -> TEXT_GIF_REQUEST_INFO;
            default -> "Unknown error";
        };

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor("Lawliet Support")
                .setDescription(text);

        event.reply(event.getTarget().getAuthor().getAsMention())
                .addEmbeds(eb.build())
                .queue();
    }

    private static Language findLanguage(Category parentCategory) {
        return switch (parentCategory.getId()) {
            case "630524285689004032" -> Language.DE;
            case "840725763815899146" -> Language.ES;
            case "840721663687131147" -> Language.RU;
            default -> Language.EN;
        };
    }

    private static int findCommandIndex(String name) {
        for (int i = 0; i < COMMANDS.length; i++) {
            if (name.equals(COMMANDS[i])) {
                return i;
            }
        }
        return -1;
    }

}
