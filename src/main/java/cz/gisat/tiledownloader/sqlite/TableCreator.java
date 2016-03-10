package cz.gisat.tiledownloader.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TableCreator {
    private Connection connection;

    public TableCreator( Connection connection ) {
        this.connection = connection;
    }

    public boolean exists( String tableName ) {
        if ( this.connection != null ) {
            try {
                Statement statement = this.connection.createStatement();
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=" + tableName;
                ResultSet resultSet = statement.executeQuery( sql );
                if ( resultSet.next() ) {
                    if ( resultSet.getString( "name" ).equals( tableName ) ) {
                        return true;
                    }
                }
            } catch ( SQLException e ) {
            }
        }

        return false;
    }

    public boolean create( String sql ) {
        return false;
    }
}
