package mysql.interfaces;

import java.sql.SQLException;
import java.util.ArrayList;

public interface CompleteLoadOnStartup<T> {

    ArrayList<T> getKeySet() throws SQLException;

}
