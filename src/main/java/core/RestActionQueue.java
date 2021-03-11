package core;

import net.dv8tion.jda.api.requests.RestAction;

public class RestActionQueue {

    private RestAction<Void> currentRestAction;

    public RestActionQueue attach(RestAction<Void> restAction) {
        if (currentRestAction == null) {
            currentRestAction = restAction;
        } else {
            currentRestAction = currentRestAction.and(restAction);
        }
        return this;
    }

    public RestAction<Void> getCurrentRestAction() {
        return currentRestAction;
    }

}
