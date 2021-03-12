package core;

import net.dv8tion.jda.api.requests.RestAction;

public class RestActionQueue {

    private RestAction<?> currentRestAction;

    public RestActionQueue attach(RestAction<?> restAction) {
        if (currentRestAction == null) {
            currentRestAction = restAction;
        } else {
            currentRestAction = currentRestAction.and(restAction);
        }
        return this;
    }

    public RestAction<?> getCurrentRestAction() {
        return currentRestAction;
    }

}
