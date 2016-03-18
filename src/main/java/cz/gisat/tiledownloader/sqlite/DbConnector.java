package cz.gisat.tiledownloader.sqlite;

import java.io.File;
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
        }
        catch ( ClassNotFoundException | SQLException ignore ) {
            //e.printStackTrace();
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
        }
        catch ( SQLException ignore ) {
            //e.printStackTrace();
        }
    }

    public ResultSet executeQuery( String sql ) {
        if ( this.connection != null ) {
            try {
                Statement statement = this.connection.createStatement();
                return statement.executeQuery( sql );
            }
            catch ( SQLException ignore ) {
                //e.printStackTrace();
            }
        }
        return null;
    }

    public boolean rowExists( String sql ) {
        boolean exists = false;
        try {
            Statement statement = this.connection.createStatement();
            ResultSet resultSet = statement.executeQuery( sql );
            if ( resultSet.next() ) {
                exists = true;
            }
            resultSet.close();
            statement.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return exists;
    }

    public boolean executeUpdate( String sql ) {
        if ( this.connection != null ) {
            try {
                Statement statement = this.connection.createStatement();
                statement.executeUpdate( sql );
                statement.close();
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
            }
            catch ( SQLException ignore ) {
                //e.printStackTrace();
            }
        }
        return null;
    }

    public boolean executePreparedStatementBatch( PreparedStatement preparedStatement ) {
        if ( preparedStatement == null ) {
            return false;
        }
        try {
            this.connection.setAutoCommit( false );
            preparedStatement.executeBatch();
            this.connection.setAutoCommit( true );
            return true;
        }
        catch ( SQLException ignore ) {
            //e.printStackTrace();
        }
        return false;
    }

    public long getDbSize() {
        return new File( this.dbFile ).length();
    }

    public File getDbFile() {
        return new File( this.dbFile );
    }

    public Connection getConnection() {
        return this.connection;
    }
}
