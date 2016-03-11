package cz.gisat.tiledownloader.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TableCreator {
    private DbConnector dbConnector;

    public TableCreator( DbConnector dbConnector ) {
        this.dbConnector = dbConnector;
    }

    public boolean exists( String tableName ) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=" + tableName;
        ResultSet resultSet = this.dbConnector.executeSqlQry( sql );
        if ( resultSet != null ) {
            try {
                if ( resultSet.getString( "name" ).equals( tableName ) ) {
                    return true;
                }
            } catch ( SQLException e ) {
            }
        }
        return false;
    }

    public boolean create( String sql ) {
        if ( this.dbConnector.executeSqlQry( sql ) != null ) {
            return true;
        }
        return false;
    }
}
