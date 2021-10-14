package commands;

public class CommandLanguage {

    private final String title;
    private final String descShort;
    private final String descLong;
    private final String usage;
    private final String examples;

    public CommandLanguage(String title, String descShort, String descLong, String usage, String examples) {
        this.title = title;
        this.descShort = descShort;
        this.descLong = descLong;
        this.usage = usage;
        this.examples = examples;
    }

    public String getTitle() {
        return title;
    }

    public String getDescShort() {
        return descShort;
    }

    public String getDescLong() {
        return descLong;
    }

    public String getUsage() {
        return usage;
    }

    public String getExamples() {
        return examples;
    }

}
