package core.components;

import java.util.ArrayList;
import java.util.Collections;
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
        if (components == null) {
            return Collections.emptyList();
        }

        ArrayList<DataObject> rows = new ArrayList<>();
        ArrayList<Component> componentRemovable = new ArrayList<>(components);
        int rowIndex = 0;

        while(componentRemovable.size() > 0) {
            DataObject row;
            if (rowIndex >= rows.size()) {
                row = DataObject.empty()
                        .put("type", 1)
                        .put("components", DataArray.empty());
                rows.add(row);
            } else {
                row = rows.get(rowIndex);
            }

            DataArray dataArray = row.getArray("components");
            Component component = componentRemovable.get(0);
            if (countChildrenTypeInDataArray(dataArray, component.getType()) < component.getMaxPerRow() &&
                    dataArrayOnlyHasType(dataArray, component.getType())
            ) {
                dataArray.add(componentRemovable.remove(0).toData());
            } else {
                rowIndex++;
            }
        }

        return rows.stream()
                .map(ActionRow::fromData)
                .collect(Collectors.toList());
    }

    private static int countChildrenTypeInDataArray(DataArray dataArray, Component.Type type) {
        int n = 0;
        for (int i = 0; i < dataArray.length(); i++) {
            DataObject dataObject = dataArray.getObject(i);
            if (dataObject.getInt("type") == type.ordinal()) {
                n++;
            }
        }
        return n;
    }

    private static boolean dataArrayOnlyHasType(DataArray dataArray, Component.Type type) {
        for (int i = 0; i < dataArray.length(); i++) {
            DataObject dataObject = dataArray.getObject(i);
            if (dataObject.getInt("type") != type.ordinal()) {
                return false;
            }
        }
        return true;
    }

}
