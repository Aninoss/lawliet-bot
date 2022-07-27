package events.sync;

import java.time.Duration;
import core.AsyncTimer;
import core.MainLogger;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.json.JSONObject;

@Path("")
@Singleton
public class RestService {

    @POST
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String event(@PathParam("name") String name, String json) {
        try (AsyncTimer timer = new AsyncTimer(Duration.ofSeconds(5))) {
            JSONObject requestJson = new JSONObject(json);
            JSONObject responseJson = null;
            boolean completedSuccessfully = false;
            try {
                responseJson = EventManager.getEvent(name).apply(requestJson);
                completedSuccessfully = true;
            } catch (Throwable e) {
                MainLogger.get().error("Error in event \"{}\"", name, e);
            }
            if (responseJson == null) {
                responseJson = new JSONObject();
            }
            if (!responseJson.has("completed_successfully")) {
                responseJson.put("completed_successfully", completedSuccessfully);
            }
            return responseJson.toString();
        } catch (Throwable e) {
            MainLogger.get().error("Error in event \"{}\"", name, e);
            return new JSONObject().toString();
        }
    }

}
