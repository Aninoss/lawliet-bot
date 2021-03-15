package core.mention;

import java.util.Optional;

public class Mention {

    private final String filteredArgs, mentionText;
    private final boolean multiple, containedBlockedUser;

    public Mention(String mentionText, String filteredArgs, boolean multiple, boolean containedBlockedUser) {
        this.mentionText = mentionText;
        this.filteredArgs = filteredArgs;
        this.multiple = multiple;
        this.containedBlockedUser = containedBlockedUser;
    }

    public String getMentionText() {
        return mentionText;
    }

    public Optional<String> getFilteredArgs() {
        return Optional.ofNullable(filteredArgs).map(String::trim);
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean containedBlockedUser() {
        return containedBlockedUser;
    }

}
