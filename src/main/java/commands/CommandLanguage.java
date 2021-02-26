package commands;

public class CommandLanguage {

    private final String title, descLong, usage, examples;

    public CommandLanguage(String title, String descLong, String usage, String examples) {
        this.title = title;
        this.descLong = descLong;
        this.usage = usage;
        this.examples = examples;
    }

    public String getTitle() {
        return title;
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
