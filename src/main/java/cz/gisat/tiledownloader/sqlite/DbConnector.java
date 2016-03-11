package cz.gisat.tiledownloader.sqlite;

import java.sql.*;

public class DbConnector {
    private String dbFile;
    private Connection connection;

    public DbConnector( String dbFile ) {
        this.dbFile = dbFile;
    }

    public Connection open() {
        try {
            Class.forName( "org.sqlite.JDBC" );
            this.connection = DriverManager.getConnection( "jdbc:sqlite:" + dbFile );
            System.out.println( "Connection to database was established..." );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
        return this.connection;
    }

    public void close() {
        if ( this.connection == null ) {
            return;
        }
        try {
            this.connection.close();
            System.out.println( "Connection to database was closed..." );
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    public ResultSet executeSqlQry( String sql ) {
        if ( this.connection != null ) {
            try {
                Statement statement = this.connection.createStatement();
                ResultSet resultSet = statement.executeQuery( sql );
                if ( resultSet.next() ) {
                    return resultSet;
                }
            } catch ( SQLException e ) {
            }
        }
        return null;
    }

    public boolean executeSqlIns( String sql ) {
        if ( this.connection != null ) {
            try {
                Statement statement = this.connection.createStatement();
                statement.executeUpdate( sql );
                return true;
            } catch ( SQLException e ) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public PreparedStatement createPreparedStatement( String sql ) {
        if ( this.connection != null ) {
            try {
                return this.connection.prepareStatement( sql );
            } catch ( SQLException e ) {
            }
        }
        return null;
    }
}
