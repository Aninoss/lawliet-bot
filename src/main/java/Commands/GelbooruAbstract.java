package Commands;

import Commands.PornAbstractAbstract;

public abstract class GelbooruAbstract extends PornAbstractAbstract {

    @Override
    protected String getSearchExtra() {
        return " -japanese_(nationality) -asian";
    }

    @Override
    protected String getDomain() {
        return "gelbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "http://simg3.gelbooru.com/samples/%d/sample_%f";
    }

}
