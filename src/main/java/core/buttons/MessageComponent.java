package core.buttons;

import net.dv8tion.jda.api.utils.data.DataObject;

public interface MessageComponent {

    int getType();

    DataObject getJSON();

}
