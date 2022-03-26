package events.sync;

import core.Program;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class AuthFilter implements ContainerRequestFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String auth = requestContext.getHeaderString("Authorization");
        if (Program.productionMode() &&
                (auth == null || !auth.equals(System.getenv("SYNC_AUTH")))
        ) {
            LOGGER.warn("Invalid authorization: {}", auth);
            Response response = Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing authorization").build();
            requestContext.abortWith(response);
        }
    }

}
