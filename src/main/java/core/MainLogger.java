package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(MainLogger.class);

    public static Logger get() {
        return LOGGER;
    }

}
