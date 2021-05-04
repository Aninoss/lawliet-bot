package commands.runnables.externalcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.Category;
import constants.Language;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "weaknesstype",
        emoji = "\uD83C\uDDF5",
        exclusiveUsers = { 397209883793162240L, 381156056660967426L, 272037078919938058L },
        executableWithoutArgs = false
)
public class WeaknessTypeCommand extends Command {

    public static final double[][] TABLE = new double[][] {
            { 1, 1, 1, 1, 1, 0.5, 1, 0, 0.5, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 2, 1, 0.5, 0.5, 1, 2, 0.5, 0, 2, 1, 1, 1, 1, 0.5, 2, 1, 2, 0.5 },
            { 1, 2, 1, 1, 1, 0.5, 2, 1, 0.5, 1, 1, 2, 0.5, 1, 1, 1, 1, 1 },
            { 1, 1, 1, 0.5, 0.5, 0.5, 1, 0.5, 0, 1, 1, 2, 1, 1, 1, 1, 1, 2 },
            { 1, 1, 0, 2, 1, 2, 0.5, 1, 2, 2, 1, 0.5, 2, 1, 1, 1, 1, 1 },
            { 1, 0.5, 2, 1, 0.5, 1, 2, 1, 0.5, 2, 1, 1, 1, 1, 2, 1, 1, 1 },
            { 1, 0.5, 0.5, 0.5, 1, 1, 1, 0.5, 0.5, 0.5, 1, 2, 1, 2, 1, 1, 2, 0.5 },
            { 0, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 0.5, 1 },
            { 1, 1, 1, 1, 1, 2, 1, 1, 0.5, 0.5, 0.5, 1, 0.5, 1, 2, 1, 1, 2 },
            { 1, 1, 1, 1, 1, 0.5, 2, 1, 2, 0.5, 0.5, 2, 1, 1, 2, 0.5, 1, 1 },
            { 1, 1, 1, 1, 2, 2, 1, 1, 1, 2, 0.5, 0.5, 1, 1, 1, 0.5, 1, 1 },
            { 1, 1, 0.5, 0.5, 2, 2, 0.5, 1, 0.5, 0.5, 2, 0.5, 1, 1, 1, 0.5, 1, 1 },
            { 1, 1, 2, 1, 0, 1, 1, 1, 1, 1, 2, 0.5, 0.5, 1, 1, 0.5, 1, 1 },
            { 1, 2, 1, 2, 1, 1, 1, 1, 0.5, 1, 1, 1, 1, 0.5, 1, 1, 0, 1 },
            { 1, 1, 2, 1, 2, 1, 1, 1, 0.5, 0.5, 0.5, 2, 1, 1, 0.5, 2, 1, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 0.5, 1, 1, 1, 1, 1, 1, 2, 1, 0 },
            { 1, 0.5, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 0.5, 0.5 },
            { 1, 2, 1, 0.5, 1, 1, 1, 1, 0.5, 0.5, 1, 1, 1, 1, 1, 2, 2, 1 },
    };

    private final double[] EFFECTIVENESS = new double[] { 0, 0.25, 0.5, 1, 2, 4 };

    public WeaknessTypeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        List<Integer> types = retrieveTypes(event, args);
        if (types.size() == 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getNoResultsString(getLocale(), args));
            event.getChannel().sendMessage(eb.build()).queue();
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(getString("title"))
                .setDescription(getContent(event, args, types));

        StringBuilder[] stringBuilders = new StringBuilder[EFFECTIVENESS.length];
        for (int i = 0; i < stringBuilders.length; i++) {
            stringBuilders[i] = new StringBuilder();
        }
        String[] typesString = TextManager.getString(getLocale(), Category.EXTERNAL, "weaknesstype_types").split("\n");

        for (int i = 0; i < TABLE.length; i++) {
            double value = 1;
            for (int type : types) {
                value *= TABLE[i][type];
            }
            for (int i1 = 0; i1 < EFFECTIVENESS.length; i1++) {
                if (EFFECTIVENESS[i1] == value) {
                    if (stringBuilders[i1].length() > 0) {
                        stringBuilders[i1].append(" ");
                    }
                    stringBuilders[i1].append("`").append(typesString[i]).append("`");
                    break;
                }
            }
        }

        for (int i = 0; i < stringBuilders.length; i++) {
            StringBuilder sb = stringBuilders[i];
            if (sb.length() > 0) {
                eb.addField(
                        TextManager.getString(getLocale(), Category.EXTERNAL, "weaknesstype_effectiveness", i),
                        sb.toString(),
                        false
                );
            }
        }

        EmbedUtil.setFooter(eb, this);
        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

    protected String getContent(GuildMessageReceivedEvent event, String args, List<Integer> types) {
        String[] typesString = TextManager.getString(getLocale(), Category.EXTERNAL, "weaknesstype_types").split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < typesString.length; i++) {
            if (types.contains(i)) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append("`").append(typesString[i]).append("`");
            }
        }
        return getString("desc", sb.toString());
    }


    protected List<Integer> retrieveTypes(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        ArrayList<Integer> types = new ArrayList<>();
        for (Language l : Language.values()) {
            String[] typesCheck = TextManager.getString(l.getLocale(), Category.EXTERNAL, "weaknesstype_types").split("\n");
            for (int i = 0; i < typesCheck.length; i++) {
                if (args.toLowerCase().contains(typesCheck[i].toLowerCase()) &&
                        !types.contains(i) &&
                        types.size() < 2
                ) {
                    types.add(i);
                }
            }
        }
        return types;
    }

}