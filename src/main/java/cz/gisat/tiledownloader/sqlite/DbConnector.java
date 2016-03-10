package cz.gisat.tiledownloader.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnector {
    private String dbFile;
    private Connection connection;

    public DbConnector( String dbFile ) {
        this.dbFile = dbFile;
    }

    public Connection open() {
        try {
            Class.forName( "org.sqlite.JDBC" );
            connection = DriverManager.getConnection( "jdbc:sqlite:" + dbFile );
            System.out.println( "Connection to database was established..." );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
        return connection;
    }

    public void close() {
        try {
            this.connection.close();
            System.out.println( "Connection to database was closed..." );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }
}
