package core.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

public class ActionRows {

    public static List<ActionRow> of(Component... components) {
        return of(List.of(components));
    }

    public static List<ActionRow> of(List<? extends Component> components) {
        ArrayList<DataObject> rows = new ArrayList<>();
        ArrayList<Component> buttonsRemovable = new ArrayList<>(components);
        int rowIndex = 0;

        while(buttonsRemovable.size() > 0) {
            DataObject row;
            if (rowIndex >= rows.size()) {
                row = DataObject.empty()
                        .put("type", 1)
                        .put("components", DataArray.empty());
                rows.add(row);
            } else {
                row = rows.get(rowIndex);
            }

            DataArray componentsDataArray = row.getArray("components");
            if (componentsDataArray.length() < 5) {
                componentsDataArray.add(buttonsRemovable.remove(0).toData());
            } else {
                rowIndex++;
            }
        }

        return rows.stream()
                .map(ActionRow::fromData)
                .collect(Collectors.toList());
    }

}
