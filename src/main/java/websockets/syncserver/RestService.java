package websockets.syncserver;

import java.time.Duration;
import core.AsyncTimer;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("")
@Singleton
public class RestService {

    private final static Logger LOGGER = LoggerFactory.getLogger(RestService.class);

    @POST
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String event(@PathParam("name") String name, String json) {
        try (AsyncTimer timer = new AsyncTimer(Duration.ofSeconds(5))) {
            JSONObject requestJson = new JSONObject(json);
            JSONObject responseJson = null;
            try {
                responseJson = EventManager.getEvent(name).apply(requestJson);
            } catch (Throwable e) {
                LOGGER.error("Error in event \"{}\"", name, e);
            }
            if (responseJson == null) {
                responseJson = new JSONObject();
            }
            return responseJson.toString();
        } catch (Throwable e) {
            LOGGER.error("Error in event \"{}\"", name, e);
            return new JSONObject().toString();
        }
    }

}
