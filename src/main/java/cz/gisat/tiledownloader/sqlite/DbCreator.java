package cz.gisat.tiledownloader.sqlite;

import cz.gisat.tiledownloader.TileGetter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DbCreator {

    public DbCreator() {
    }

    public DbConnector getOutputDb( TileGetter tileGetter ) {
        SimpleDateFormat fileNameFormat = new SimpleDateFormat( "y_MM_dd_HH_mm_ss" );
        SimpleDateFormat createdFormat = new SimpleDateFormat( "y.MM.dd HH:mm:ss" );
        String fileName = fileNameFormat.format( new Date() ) + ".mbtiles";
        String created = createdFormat.format( new Date() );

        File outputFolder;
        File dbFile = null;
        outputFolder = new File( "out/" + tileGetter.getMapSource() );
        outputFolder.mkdirs();

        dbFile = new File( outputFolder, fileName );

        DbConnector dbConnector = new DbConnector( dbFile.getAbsolutePath() );
        dbConnector.open();

        dbConnector.executeUpdate( "BEGIN TRANSACTION;" );

        TableCreator tableCreator = new TableCreator( dbConnector );
        if ( !tableCreator.exists( "metadata" ) ) {
            dbConnector.executeUpdate( "CREATE TABLE metadata (name text, value text);" );
            dbConnector.executeUpdate( "CREATE UNIQUE INDEX metadata_idx ON metadata (name)" );
            dbConnector.executeUpdate( "INSERT INTO metadata VALUES ('type', 'baselayer');" );
            dbConnector.executeUpdate( "INSERT INTO metadata VALUES ('created', '" + created + "');" );
            dbConnector.executeUpdate( "INSERT INTO metadata VALUES ('format', 'png');" );
        }
        if ( !tableCreator.exists( "tiles" ) ) {
            dbConnector.executeUpdate( "CREATE TABLE tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob);" );
            dbConnector.executeUpdate( "CREATE UNIQUE INDEX tiles_idx on tiles (zoom_level, tile_column, tile_row)" );
        }
        if ( !tableCreator.exists( "android_metadata" ) ) {
            dbConnector.executeUpdate( "CREATE TABLE android_metadata (locale TEXT DEFAULT 'en_US');" );
            dbConnector.executeUpdate( "INSERT INTO android_metadata VALUES ('en_US');" );
        }
        dbConnector.executeUpdate( "COMMIT;" );
        return dbConnector;
    }
}
