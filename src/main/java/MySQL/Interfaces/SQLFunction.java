package MySQL.Interfaces;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public interface SQLFunction<T, R> {

    R apply(T t) throws SQLException;

}
