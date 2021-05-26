package core.buttons;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.utils.data.DataObject;

public class MessageButton implements MessageComponent {

    private ButtonStyle buttonStyle;
    private String label;
    private DataObject emoji;
    private String value;
    private boolean disabled;

    public MessageButton(ButtonStyle buttonStyle, String label, String value) {
        this(buttonStyle, label, value, (String) null, false);
    }

    public MessageButton(ButtonStyle buttonStyle, String label, String value, String unicodeEmoji) {
        this(buttonStyle, label, value, unicodeEmoji, false);
    }

    public MessageButton(ButtonStyle buttonStyle, String label, String value, Emote emote) {
        this(buttonStyle, label, value, value, false);
    }

    public MessageButton(ButtonStyle buttonStyle, String label, String value, String unicodeEmoji, boolean disabled) {
        this.buttonStyle = buttonStyle;
        this.label = label;
        this.value = value;
        this.disabled = disabled;
        setEmoji(unicodeEmoji);
    }

    public MessageButton(ButtonStyle buttonStyle, String label, String value, Emote emote, boolean disabled) {
        this.buttonStyle = buttonStyle;
        this.label = label;
        this.value = value;
        this.disabled = disabled;
        setEmote(emote);
    }

    public void setButtonStyle(ButtonStyle buttonStyle) {
        this.buttonStyle = buttonStyle;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setEmoji(String unicodeEmoji) {
        this.emoji = DataObject.empty();
        this.emoji.put("id", null);
        this.emoji.put("name", unicodeEmoji.trim());
    }

    public void setEmote(Emote emote) {
        this.emoji = DataObject.empty();
        this.emoji.put("id", emote.getId());
        this.emoji.put("name", emote.getName());
        this.emoji.put("animated", emote.isAnimated());
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public int getType() {
        return 2;
    }

    @Override
    public DataObject getJSON() {
        DataObject dataObject = DataObject.empty();
        dataObject.put("type", 2);
        dataObject.put("style", buttonStyle.getValue());
        if (buttonStyle.usesLink()) {
            dataObject.put("url", value);
        } else {
            dataObject.put("custom_id", value);
        }
        if (label != null) {
            dataObject.put("label", label);
        }
        if (emoji != null) {
            dataObject.put("emoji", emoji);
        }
        dataObject.put("disabled", disabled);
        return dataObject;
    }

}
