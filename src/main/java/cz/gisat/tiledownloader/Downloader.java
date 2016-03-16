package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import cz.gisat.tiledownloader.sqlite.DbCreator;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Downloader {
    private LatLon latLonMin;
    private LatLon latLonMax;
    private int zoom;
    private int size;
    private String mapSource;

    public Downloader( ArgsParser argsParser ) {
        this.zoom = argsParser.getZoom();
        this.latLonMin = argsParser.getLatLonMin();
        this.latLonMax = argsParser.getLatLonMax();
        this.size = argsParser.getSize();
        this.mapSource = argsParser.getMapSource();
        if ( this.size == 0 ) {
            this.size = 1024;
        }
    }

    public void download() {
        if ( this.zoom < 0 || this.latLonMin == null || this.latLonMax == null ) {
            System.out.println( "Some of parameters was missing or has set wrong value!" );
            return;
        }

        System.out.println( "Downloading and generating of output mbtiles file was started" );

        long sTime = System.currentTimeMillis();

        TileGetter tileGetter = new TileGetter( this.mapSource );
        MapZoom mapZoom = tileGetter.getMapZoom();

        DbCreator dbCreator = new DbCreator();

        DbConnector storageDbC = dbCreator.getDb( tileGetter, true );
        DbConnector outputDbC = dbCreator.getDb( tileGetter, false );

        outputDbC.executeUpdate( "ATTACH '" + storageDbC.getDbFile().getAbsolutePath() + "' AS storage;" );

        if ( this.zoom > mapZoom.getMaxZoom() ) {
            this.zoom = mapZoom.getMaxZoom();
        }

        int tilesCount = this.getTotalOfTiles();
        int batch = 0, file = 0, web = 0, error = 0, db = 0, done = 0;
        PreparedStatement storageStatement = outputDbC.createPreparedStatement(
                "INSERT OR IGNORE INTO storage.tiles VALUES ( ?, ?, ?, ? );"
        );
        PreparedStatement outputStatement = outputDbC.createPreparedStatement(
                "INSERT INTO tiles SELECT * FROM storage.tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?;"
        );
        for ( int z = mapZoom.getMinZoom() ; z <= this.zoom ; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX() ; x <= tileMax.getX() ; x++ ) {
                for ( int y = tileMin.getY() ; y >= tileMax.getY() ; y-- ) {
                    Tile tile = new Tile( x, y, z );
                    String tileUrl = tileGetter.getTileUrl( tile );

                    System.out.print( tileUrl );

                    ResultSet resultSet = outputDbC.executeQuery(
                            "SELECT 1 FROM storage.tiles WHERE zoom_level=" + tile.getZoom() + " AND tile_column=" + tile.getX() + " AND tile_row=" + tile.getMBTilesY() + ";"
                    );
                    try {
                        boolean err = false;
                        if ( resultSet == null || !resultSet.next() ) {
                            byte[] blob = this.getImageFromOldStorage( tileGetter, tile );
                            if ( blob != null && blob.length > 0 ) {
                                System.out.print( "     FILE " );
                                file++;
                            } else {
                                URL url = new URL( tileUrl );
                                blob = IOUtils.toByteArray( url.openStream() );
                                System.out.print( "     WEB  " );
                                web++;
                            }
                            if ( blob != null && blob.length > 0 ) {
                                storageStatement.setInt( 1, tile.getZoom() );
                                storageStatement.setInt( 2, tile.getX() );
                                storageStatement.setInt( 3, tile.getMBTilesY() );
                                storageStatement.setBytes( 4, blob );
                                storageStatement.addBatch();
                                batch++;
                                System.out.print( "     B:OK " );
                            } else {
                                System.out.print( "     B:ERR" );
                                err = true;
                                error++;
                            }
                        } else {
                            System.out.print( "     EXIST" );
                            db++;
                        }
                        resultSet.close();
                        if ( !err ) {
                            outputStatement.setInt( 1, tile.getZoom() );
                            outputStatement.setInt( 2, tile.getX() );
                            outputStatement.setInt( 3, tile.getMBTilesY() );
                            outputStatement.addBatch();
                            System.out.print( "     O:OK " );
                            batch++;
                        } else {
                            System.out.print( "     O:ERR" );
                            error++;
                        }
                    }
                    catch ( SQLException | IOException e ) {
                        e.printStackTrace();
                    }

                    if ( batch >= 50 ) {
                        outputDbC.executePreparedStatementBatch( storageStatement );
                        outputDbC.executePreparedStatementBatch( outputStatement );
                        batch = 0;
                    }

                    done++;

                    long outDbSize = outputDbC.getDbSize();

                    System.out.print( "     " + file );
                    System.out.print( " / " + web );
                    System.out.print( " / " + db );
                    System.out.print( " / " + error );
                    System.out.print( " / " + done );
                    System.out.print( " / " + ( tilesCount - done ) );
                    System.out.print( " / " + outDbSize );

                    System.out.print( "\n" );

                    if ( outDbSize > 1024 * 1024 * 10 ) {
                        try {
                            outputDbC.executePreparedStatementBatch( outputStatement );
                            outputStatement.clearBatch();
                            outputStatement = null;
                            outputDbC.executePreparedStatementBatch( storageStatement );
                            storageStatement.clearBatch();
                            storageStatement = null;
                            outputDbC.executeUpdate( "DETACH DATABASE storage;" );
                            outputDbC.close();
                            outputDbC = dbCreator.getDb( tileGetter, false );
                            outputDbC.executeUpdate( "ATTACH '" + storageDbC.getDbFile().getAbsolutePath() + "' AS storage;" );
                            storageStatement = outputDbC.createPreparedStatement(
                                    "INSERT OR IGNORE INTO storage.tiles VALUES ( ?, ?, ?, ? );"
                            );
                            outputStatement = outputDbC.createPreparedStatement(
                                    "INSERT INTO tiles SELECT * FROM storage.tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?;"
                            );
                        }
                        catch ( SQLException e ) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        try {
            outputDbC.executePreparedStatementBatch( storageStatement );
            storageStatement.clearBatch();
            storageDbC.close();

            outputDbC.executePreparedStatementBatch( outputStatement );
            outputStatement.clearBatch();
            outputDbC.close();
        }
        catch ( SQLException e ) {
            e.printStackTrace();
        }
        System.out.println( "XXXXXXXXXXXXXXXXXXX>   DONE    <XXXXXXXXXXXXXXXXXXX" );
        System.out.println( "TIME: " + ( System.currentTimeMillis() - sTime ) );
    }

    private int getTotalOfTiles() {
        int tot = 0;
        for ( int z = 0 ; z <= this.zoom ; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX() ; x <= tileMax.getX() ; x++ ) {
                for ( int y = tileMin.getY() ; y >= tileMax.getY() ; y-- ) {
                    tot++;
                }
            }
        }
        return tot;
    }

    private byte[] getImageFromOldStorage( TileGetter tileGetter, Tile tile ) {
        File oldImgFile = new File(
                "maps/" + tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png"
        );
        if ( oldImgFile.exists() ) {
            try {
                FileInputStream inputStream = new FileInputStream( oldImgFile );
                return IOUtils.toByteArray( inputStream );
            }
            catch ( IOException ignore ) {
            }
        }
        return null;
    }
}