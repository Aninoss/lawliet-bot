package modules;

import constants.Language;
import core.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SupportTemplates {

    private static final String[] COMMANDS = {
            "Support wrong channel",
            "#faq",
            "#announcements/#technical-issues",
            "English",
            "#gif-requests-info"
    };

    private static final String[] TEXT_WRONG_CHANNEL = {
            "This channel is not suited for bot support, please go to <#557955318722723891> instead or create a ticket in <#843836754271404062>",
            "Dieser Kanal ist nicht für Bot-Support geeignet, bitte gehe stattdessen zu <#601015368785330196> oder erstelle ein Ticket in <#843836754271404062>",
            "Este canal no es adecuado para el soporte de bots, por favor vaya a <#840725889113522206> en su lugar o cree un ticket en <#843836754271404062>",
            "Этот канал не подходит для поддержки ботов, пожалуйста, перейдите на <#840722552569462784> или создайте тикет в <#843836754271404062>",
            "Ce canal n'est pas adapté au support des bots, merci de vous rendre dans <#557955318722723891> ou de créer un ticket dans <#843836754271404062>",
            "Este canal não é adequado para suporte de bot, por favor, vá para <#557955318722723891> ou crie um ticket em <#843836754271404062>",
            "Bu kanal bot desteği için uygun değil, lütfen <#557955318722723891> kanalına gidin veya <#843836754271404062> üzerinde bir bilet oluşturun"
    };

    private static final String[] TEXT_FAQ = {
            "Your question can be answered by looking at <#703949030673088533>",
            "Deine Frage kann durch einen Blick in <#703949030673088533> beantwortet werden",
            "Su pregunta puede ser respondida mirando <#703949030673088533>",
            "На ваш вопрос можно ответить, посмотрев на <#703949030673088533>",
            "Ta question peut être répondue en regardant <#703949030673088533>",
            "Sua dúvida pode ser respondida olhando para <#703949030673088533>",
            "Sorunun <#703949030673088533> bakılarak yanıtlanabilir"
    };

    private static final String[] TEXT_ANNOUNCEMENTS_OR_TECHNICAL_ISSUES = {
            "Your question can be answered by looking at <#557960859792441357> or <#1067095641475522722>",
            "Deine Frage kann durch einen Blick in <#557960859792441357> oder <#1067095641475522722> beantwortet werden",
            "Su pregunta puede ser respondida mirando <#557960859792441357> o <#1067095641475522722>",
            "На ваш вопрос можно ответить, посмотрев <#557960859792441357> или <#1067095641475522722>",
            "Ta question peut être répondue en regardant <#557960859792441357> ou <#1067095641475522722>",
            "Sua dúvida pode ser respondida olhando para <#557960859792441357> ou <#1067095641475522722>",
            "Sorunun <#557960859792441357> veya <#1067095641475522722> bakılarak yanıtlanabilir"
    };

    private static final String TEXT_ENGLISH = "This is primarily an English server, please check <#557961281034780704> for other languages";
    private static final String TEXT_GIF_REQUEST_INFO = "Your gif request is invalid, please take a look at <#708252207690154045> before requesting a new gif for role play commands";

    public static List<CommandData> generateSupportContextCommands() {
        return Arrays.stream(COMMANDS)
                .map(Commands::message)
                .collect(Collectors.toList());
    }

    public static void process(MessageContextInteractionEvent event) {
        Language language = findLanguage(event.getChannel() instanceof ICategorizableChannel ? ((ICategorizableChannel) event.getChannel()).getParentCategory() : null);
        String text;
        switch (findCommandIndex(event.getName())) {
            case 0 -> text = TEXT_WRONG_CHANNEL[language.ordinal()];
            case 1 -> text = TEXT_FAQ[language.ordinal()];
            case 2 -> text = TEXT_ANNOUNCEMENTS_OR_TECHNICAL_ISSUES[language.ordinal()];
            case 3 -> {
                text = TEXT_ENGLISH;
                event.getTarget().delete().queue();
            }
            case 4 -> text = TEXT_GIF_REQUEST_INFO;
            default -> {
                return;
            }
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor("Lawliet Support")
                .setDescription(text);

        event.reply(event.getTarget().getAuthor().getAsMention())
                .addEmbeds(eb.build())
                .queue();
    }

    private static Language findLanguage(Category parentCategory) {
        if (parentCategory == null) {
            return Language.EN;
        }

        return switch (parentCategory.getId()) {
            case "630524285689004032" -> Language.DE;
            case "840725763815899146" -> Language.ES;
            case "840721663687131147" -> Language.RU;
            case "1309163867338707035" -> Language.FR;
            case "1309164170972758118" -> Language.PT;
            case "1309164274043588669" -> Language.TR;
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
