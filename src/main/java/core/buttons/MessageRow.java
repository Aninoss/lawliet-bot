package core.buttons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

public class MessageRow implements MessageComponent {

    private final List<MessageButton> buttons;

    public MessageRow() {
        this.buttons = new ArrayList<>();
    }

    public MessageRow(MessageButton... buttons) {
        this.buttons = new ArrayList<>(List.of(buttons));
    }

    public MessageRow(Collection<MessageButton> buttons) {
        this.buttons = new ArrayList<>(buttons);
    }

    public void addButton(MessageButton button) {
        this.buttons.add(button);
    }

    public boolean removeButton(MessageButton button) {
        return this.buttons.remove(button);
    }

    public List<MessageButton> getButtons() {
        return Collections.unmodifiableList(this.buttons);
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public DataObject getJSON() {
        DataObject dataObject = DataObject.empty();
        dataObject.put("type", 1);

        List<DataObject> buttonDataObjects = this.buttons.stream().map(MessageComponent::getJSON).collect(Collectors.toList());
        DataArray buttons = DataArray.fromCollection(buttonDataObjects);
        dataObject.put("components", buttons);

        return dataObject;
    }

}
