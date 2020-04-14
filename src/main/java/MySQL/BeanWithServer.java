package MySQL;

import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;
import java.util.Observable;
import java.util.Optional;

public abstract class BeanWithServer extends Observable {

    private final ServerBean serverBean;

    public BeanWithServer(ServerBean serverBean) {
        this.serverBean = serverBean;
    }

    public ServerBean getServerBean() { return serverBean; }
    public long getServerId() {
        return serverBean.getServerId();
    }
    public Optional<Server> getServer() {
        return serverBean.getServer();
    }

}
