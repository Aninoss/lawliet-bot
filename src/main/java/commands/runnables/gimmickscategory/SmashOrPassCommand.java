package commands.runnables.gimmickscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.anilist.AnilistCharacter;
import modules.anilist.AnilistCharacterFile;
import mysql.hibernate.entity.SmashOrPassCharacterEntity;
import mysql.hibernate.entity.user.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "smashorpass",
        emoji = "🔥",
        executableWithoutArgs = true
)
public class SmashOrPassCommand extends Command implements OnButtonListener {

    private static final int MAX_CHARACTERS_PER_CYCLE = 30;
    private static final String BUTTON_ID_SMASH = "smash";
    private static final String BUTTON_ID_PASS = "pass";
    private static final UnicodeEmoji EMOJI_SMASH = Emoji.fromUnicode("🔥");
    private static final UnicodeEmoji EMOJI_PASS_DARK = Emoji.fromUnicode("✖️");
    private static final UnicodeEmoji EMOJI_PASS_LIGHT = Emoji.fromUnicode("❌");

    private int index = 0;
    private boolean previewResults = false;

    public SmashOrPassCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        index = getIndex();
        registerButtonListener(event.getMember());
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (previewResults) {
            index++;
            previewResults = false;
            return true;
        }

        UserEntity userEntity = getUserEntity();
        if (index >= MAX_CHARACTERS_PER_CYCLE) {
            userEntity.beginTransaction();
            userEntity.setSmashOrPassIndex(0);
            userEntity.commitTransaction();
            index = 0;
            previewResults = false;
            return true;
        }

        SmashOrPassCharacterEntity character = getCharacter();
        getEntityManager().getTransaction().begin();
        if (event.getComponentId().equals(BUTTON_ID_SMASH)) {
            character.getSmashUserIds().add(event.getUser().getIdLong());
            character.getPassUserIds().remove(event.getUser().getIdLong());
        } else if (event.getComponentId().equals(BUTTON_ID_PASS)) {
            character.getSmashUserIds().remove(event.getUser().getIdLong());
            character.getPassUserIds().add(event.getUser().getIdLong());
        }
        userEntity.setSmashOrPassIndex(index + 1);
        getEntityManager().getTransaction().commit();

        previewResults = true;
        return true;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        if (index >= MAX_CHARACTERS_PER_CYCLE) {
            setComponents(getString("complete_button"));
            Instant instantNext = TimeUtil.setInstantToNextWeek(Instant.now());
            return EmbedFactory.getEmbedDefault(this, getString("complete", TimeFormat.RELATIVE.atInstant(instantNext).toString()));
        }

        if (previewResults) {
            setComponents(TextManager.getString(getLocale(), TextManager.GENERAL, "continue"));
        } else {
            setComponents(
                    Button.of(ButtonStyle.SUCCESS, BUTTON_ID_SMASH, getString("smash")).withEmoji(EMOJI_SMASH),
                    Button.of(ButtonStyle.DANGER, BUTTON_ID_PASS, getString("pass")).withEmoji(EMOJI_PASS_DARK)
            );
        }
        SmashOrPassCharacterEntity character = getCharacter();

        String desc = getString(character.getGender() != null ? "desc" : "desc_nogender", character.getAge(), character.getGender() != null ? translateGender(character.getGender()) : "");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc)
                .setTitle(character.getName())
                .setUrl(character.getCharacterUrl())
                .setImage(character.getImageUrl());
        EmbedUtil.setFooter(eb, this, getString("index", StringUtil.numToString(index + 1), StringUtil.numToString(MAX_CHARACTERS_PER_CYCLE)));
        if (character.getMediaName() != null) {
            eb.setAuthor(character.getMediaName(), character.getMediaUrl());
        }
        if (previewResults) {
            int votesSmash = character.getSmashUserIds().size();
            int votesPass = character.getPassUserIds().size();
            int votesTotal = votesSmash + votesPass;
            String votesDesc = getString("vote", EMOJI_SMASH.getFormatted(), StringUtil.getBar((double) votesSmash / votesTotal, 12), StringUtil.numToString(votesSmash)) + "\n" +
                    getString("vote", EMOJI_PASS_LIGHT.getFormatted(), StringUtil.getBar((double) votesPass / votesTotal, 12), StringUtil.numToString(votesPass));
            eb.addField(getString("results", votesTotal != 1, StringUtil.numToString(votesSmash + votesPass)), votesDesc, false);
        }
        return eb;
    }

    private SmashOrPassCharacterEntity getCharacter() throws IOException {
        Map<Integer, SmashOrPassCharacterEntity> characters = SmashOrPassCharacterEntity.findAll(getEntityManager());
        if (characters.containsKey(index)) {
            return characters.get(index);
        } else {
            boolean preferFemale = ThreadLocalRandom.current().nextInt(3) < 2;
            AnilistCharacter randomCharacter = getRandomCharacter(characters.values(), preferFemale, 5);

            SmashOrPassCharacterEntity smashOrPassCharacterEntity = new SmashOrPassCharacterEntity(randomCharacter, index);
            getEntityManager().getTransaction().begin();
            getEntityManager().persist(smashOrPassCharacterEntity);
            getEntityManager().getTransaction().commit();
            return smashOrPassCharacterEntity;
        }
    }

    private int getIndex() {
        UserEntity userEntity = getUserEntity();
        int year = TimeUtil.getCurrentYear();
        int week = TimeUtil.getCurrentWeekOfYear();

        if (year != userEntity.getSmashOrPassYear() || week != userEntity.getSmashOrPassWeek()) {
            userEntity.beginTransaction();
            userEntity.setSmashOrPassIndex(0);
            userEntity.setSmashOrPassYear(year);
            userEntity.setSmashOrPassWeek(week);
            userEntity.commitTransaction();
            return 0;
        }
        return userEntity.getSmashOrPassIndex();
    }

    private AnilistCharacter getRandomCharacter(Collection<SmashOrPassCharacterEntity> previousCharacters, boolean preferFemale, int tries) throws IOException {
        List<AnilistCharacter> newCharacters = AnilistCharacterFile.getCharacters();
        Set<String> previousCharacterUrls = previousCharacters.stream().map(SmashOrPassCharacterEntity::getCharacterUrl).collect(Collectors.toSet());
        List<AnilistCharacter> filteredCharacters = newCharacters.stream()
                .filter(c -> c.getGender() == null ||
                        (!c.getGender().equals("Male") && preferFemale) ||
                        (!c.getGender().equals("Female") && !preferFemale)
                )
                .filter(c -> !previousCharacterUrls.contains(c.getCharacterUrl()))
                .collect(Collectors.toList());

        if (filteredCharacters.isEmpty()) {
            if (tries >= 1) {
                return getRandomCharacter(previousCharacters, preferFemale, tries - 1);
            } else {
                throw new RuntimeException("Cannot get random character");
            }
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(filteredCharacters.size());
        return filteredCharacters.get(randomIndex);
    }

    private String translateGender(String gender) {
        return switch (gender) {
            case "Male" -> getString("male");
            case "Female" -> getString("female");
            default -> gender;
        };
    }

}
