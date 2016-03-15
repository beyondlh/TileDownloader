package cz.gisat.tiledownloader.sqlite;

import java.sql.ResultSet;

public class TableCreator {
    private DbConnector dbConnector;

    public TableCreator( DbConnector dbConnector ) {
        this.dbConnector = dbConnector;
    }

    public boolean exists( String tableName ) {
        String sql = "SELECT COUNT(*) FROM " + tableName + ";";
        ResultSet resultSet = this.dbConnector.executeQuery( sql );
        if ( resultSet != null ) {
            return true;
        }
        return false;
    }

    public boolean create( String sql ) {
        if ( this.dbConnector.executeUpdate( sql ) ) {
            return true;
        }
        return false;
    }
}
