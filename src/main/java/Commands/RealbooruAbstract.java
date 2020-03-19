package Commands;

import CommandListeners.onRecievedListener;
import Commands.PornAbstractAbstract;

public abstract class RealbooruAbstract extends PornAbstractAbstract {

    @Override
    protected String getSearchExtra() {
        return "";
    }

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

}
