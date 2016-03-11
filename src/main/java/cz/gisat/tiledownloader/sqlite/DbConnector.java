package cz.gisat.tiledownloader.sqlite;

import cz.gisat.tiledownloader.objects.Tile;

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

    public boolean addTileToPreparedStatement( PreparedStatement preparedStatement, Tile tile, byte[] bytes ) {
        if ( preparedStatement == null ) {
            return false;
        }

        int x = tile.getX();
        int y = tile.getY();
        int z = tile.getZoom();
        int mbtY = ( 1 << z ) - y - 1;

        try {
            preparedStatement.setInt( 1, z );
            preparedStatement.setInt( 2, x );
            preparedStatement.setInt( 3, mbtY );
            preparedStatement.setBytes( 4, bytes );
            preparedStatement.addBatch();
        }
        catch ( SQLException e ) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean executePreparedStatementBatch( PreparedStatement preparedStatement ) {
        try {
            this.connection.setAutoCommit( false );
            preparedStatement.executeBatch();
            this.connection.setAutoCommit( true );
            return true;
        }
        catch ( SQLException e ) {
        }
        return false;
    }

    @Deprecated
    public boolean insertTileToDb( Tile tile, byte[] bytes ) {
        if ( this.connection == null ) {
            return false;
        }

        int x = tile.getX();
        int y = tile.getY();
        int z = tile.getZoom();
        int mbtY = ( 1 << z ) - y - 1;

        try {
            PreparedStatement preparedStatement = this.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );
            preparedStatement.setInt( 1, z );
            preparedStatement.setInt( 2, x );
            preparedStatement.setInt( 3, mbtY );
            preparedStatement.setBytes( 4, bytes );
            preparedStatement.executeUpdate();
        } catch ( SQLException e ) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public long getDbSize() {
        return new File( this.dbFile ).length() / 1024;
    }
}
