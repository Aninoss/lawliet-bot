package General.Mention;

public class Mention {
    private String text;
    private boolean multiple;

    public Mention(String text, boolean multiple) {
        this.text = text;
        this.multiple = multiple;
    }

    public String getString() {
        return text;
    }

    public boolean isMultiple() {
        return multiple;
    }
}
