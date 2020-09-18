package core.mention;

import java.util.Optional;

public class Mention {

    private final String filteredOriginalText, mentionText;
    private final boolean multiple, containedBlockedUser;

    public Mention(String mentionText, String filteredOriginalText, boolean multiple, boolean containedBlockedUser) {
        this.mentionText = mentionText;
        this.filteredOriginalText = filteredOriginalText;
        this.multiple = multiple;
        this.containedBlockedUser = containedBlockedUser;
    }

    public String getMentionText() {
        return mentionText;
    }

    public Optional<String> getFilteredOriginalText() {
        return Optional.ofNullable(filteredOriginalText);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean containedBlockedUser() {
        return containedBlockedUser;
    }

}
