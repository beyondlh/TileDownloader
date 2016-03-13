package cz.gisat.tiledownloader.sqlite;

import cz.gisat.tiledownloader.TileGetter;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DbCreator {

    public DbCreator() {
    }

    public DbConnector getOutputDb( TileGetter tileGetter ) {
        return this.getDbByType( tileGetter, "output" );
    }

    public DbConnector getStorageDb( TileGetter tileGetter ) {
        return this.getDbByType( tileGetter, "storage" );
    }

    private DbConnector getDbByType( TileGetter tileGetter, String type ) {
        SimpleDateFormat fileNameFormat = new SimpleDateFormat( "y_MM_dd_HH_mm_ss" );
        SimpleDateFormat createdFormat = new SimpleDateFormat( "y.MM.dd HH:mm:ss" );
        String fileName = fileNameFormat.format( new Date() ) + ".mbtiles";
        String created = createdFormat.format( new Date() );

        File outputFolder = null;
        File dbFile = null;
        if ( type.equals( "output" ) ) {
            outputFolder = new File( "out/" + tileGetter.getMapSource() );
            outputFolder.mkdirs();

            dbFile = new File( outputFolder, fileName );
        } else if ( type.equals( "storage" ) ) {
            outputFolder = new File( "maps/" + tileGetter.getMapSource() );
            outputFolder.mkdirs();

            dbFile = new File( outputFolder, tileGetter.getMapSource() + ".mbtiles" );
        }


        DbConnector dbConnector = new DbConnector( dbFile.getAbsolutePath() );
        dbConnector.open();

        TableCreator tableCreator = new TableCreator( dbConnector );
        if ( !tableCreator.exists( "metadata" ) ) {
            try {
                tableCreator.create( "CREATE TABLE metadata (name text, value text);" );
                dbConnector.executeSqlUp( "CREATE UNIQUE INDEX metadata_idx  ON metadata (name)" );
                PreparedStatement preparedStatement = dbConnector.createPreparedStatement( "INSERT INTO metadata VALUES (?, ?);" );
                preparedStatement.setString( 1, "type" );
                preparedStatement.setString( 2, "baselayer" );
                preparedStatement.addBatch();
                preparedStatement.setString( 1, "created" );
                preparedStatement.setString( 2, created );
                preparedStatement.addBatch();
                preparedStatement.setString( 1, "format" );
                preparedStatement.setString( 2, "png" );
                preparedStatement.executeBatch();
            } catch ( SQLException e ) {
                e.printStackTrace();
            }
        }
        if ( !tableCreator.exists( "tiles" ) ) {
            tableCreator.create( "CREATE TABLE tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob);" );
            dbConnector.executeSqlUp( "CREATE INDEX tiles_idx on tiles (zoom_level, tile_column, tile_row)" );
        }
        if ( !tableCreator.exists( "android_metadata" ) ) {
            tableCreator.create( "CREATE TABLE android_metadata (locale TEXT DEFAULT 'en_US');" );
            dbConnector.executeSqlUp( "INSERT INTO android_metadata VALUES ('en_US');" );
        }
        return dbConnector;
    }
}
