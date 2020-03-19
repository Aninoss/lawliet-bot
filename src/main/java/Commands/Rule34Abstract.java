package Commands;

import CommandListeners.onRecievedListener;
import Commands.PornAbstractAbstract;

public abstract class Rule34Abstract extends PornAbstractAbstract {

    @Override
    protected String getSearchExtra() {
        return "";
    }

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img.rule34.xxx/images/%d/%f";
    }

}
