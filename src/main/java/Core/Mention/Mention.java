package Core.Mention;

import java.util.Optional;

public class Mention {
    private final String string, filteredOriginalText;
    private final boolean multiple;

    public Mention(String string, String filteredOriginalText, boolean multiple) {
        this.string = string;
        this.filteredOriginalText = filteredOriginalText;
        this.multiple = multiple;
    }

    @Override
    public String toString() {
        return string;
    }

    public Optional<String> getFilteredOriginalText() {
        return Optional.ofNullable(filteredOriginalText);
    }

    public boolean isMultiple() {
        return multiple;
    }
}
