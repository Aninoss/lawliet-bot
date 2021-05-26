package core.buttons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl;

public class MessageActionAdvanced extends MessageActionImpl {

    private final List<MessageRow> rows = new ArrayList<>();

    public MessageActionAdvanced(MessageChannel channel) {
        super(
                channel.getJDA(),
                Route.Messages.SEND_MESSAGE.compile(channel.getId()),
                channel
        );
    }

    public MessageActionAdvanced appendButtons(MessageButton... buttons) {
        return appendButtons(List.of(buttons));
    }

    public MessageActionAdvanced appendButtons(Collection<MessageButton> buttons) {
        List<MessageButton> buttonList = new ArrayList<>(buttons);
        int rowIndex = 0;

        while(buttonList.size() > 0) {
            MessageRow row;
            if (rowIndex >= rows.size()) {
                row = new MessageRow();
                rows.add(row);
            } else {
                row = rows.get(rowIndex);
            }

            if (row.getButtons().size() < 5) {
                row.addButton(buttonList.remove(0));
            } else {
                rowIndex++;
            }
        }

        return this;
    }

    public MessageActionAdvanced addRow(MessageRow row) {
        this.rows.add(row);
        return this;
    }

    public MessageActionAdvanced removeRow(MessageRow row) {
        this.rows.remove(row);
        return this;
    }

    @Override
    protected DataObject getJSON() {
        DataObject dataObject = super.getJSON();
        if (rows.size() > 0) {
            List<DataObject> rowDataObjects = this.rows.stream().map(MessageComponent::getJSON).collect(Collectors.toList());
            DataArray rows = DataArray.fromCollection(rowDataObjects);
            dataObject.put("components", rows);
        }
        return dataObject;
    }

}
