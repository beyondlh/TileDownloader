package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import cz.gisat.tiledownloader.sqlite.DbCreator;
import org.apache.commons.io.IOUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

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

        DbConnector storageDbConnector = dbCreator.getStorageDb( tileGetter );
        DbConnector outDbConnector = dbCreator.getOutputDb( tileGetter );

        storageDbConnector.executeUpdate( "ATTACH '" + outDbConnector.getDbFile().getAbsolutePath() + "' AS 'out'" );

        if ( this.zoom > mapZoom.getMaxZoom() ) {
            this.zoom = mapZoom.getMaxZoom();
        }

        int batches = 0, storage = 0, remote = 0, old = 0;
        int tilesCount = this.getTotalOfTiles();
        PreparedStatement storagePS = null;
        PreparedStatement outPS = null;
        for ( int z = mapZoom.getMinZoom() ; z <= this.zoom ; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX() ; x <= tileMax.getX() ; x++ ) {
                for ( int y = tileMin.getY() ; y >= tileMax.getY() ; y-- ) {
                    Tile tile = new Tile( x, y, z );
                    String tileUrl = tileGetter.getTileUrl( tile );
                    System.out.print( tileUrl );
                    ResultSet resultSet = storageDbConnector.executeQuery(
                            "SELECT tile_data FROM tiles WHERE zoom_level=" + tile.getZoom() + " AND tile_column=" + tile.getX() + " AND tile_row=" + tile.getMBTilesY() + ";"
                    );
                    try {
                        if ( resultSet != null && resultSet.next() ) {
                            tile.setBlob( resultSet.getBytes( "tile_data" ) );
                            System.out.print( "     STORAGE" );
                            storage++;
                        }
                    }
                    catch ( SQLException e ) {
                        e.printStackTrace();
                    }
                    if ( tile.getBlob() == null ) {
                        try {
                            File oldImgFile = new File(
                                    "maps/" + tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png"
                            );
                            if ( oldImgFile.exists() ) {
                                FileInputStream fileInputStream = new FileInputStream( oldImgFile );
                                tile.setBlob( IOUtils.toByteArray( fileInputStream ) );
                                System.out.print( "     OLD    " );
                                old++;
                            } else {
                                URL url = new URL( tileUrl );
                                InputStream inputStream = url.openStream();
                                tile.setBlob( IOUtils.toByteArray( inputStream ) );
                                System.out.print( "     REMOTE " );
                                remote++;
                            }
                            if ( tile.getBlob() != null ) {
                                if ( storagePS == null ) {
                                    storagePS = storageDbConnector.createPreparedStatement( "INSERT INTO tiles VALUES ( ?, ?, ?, ? );" );
                                }
                                storagePS.setInt( 1, tile.getZoom() );
                                storagePS.setInt( 2, tile.getX() );
                                storagePS.setInt( 3, tile.getMBTilesY() );
                                storagePS.setBytes( 4, tile.getBlob() );
                                storagePS.addBatch();
                            }
                        }
                        catch ( IOException | SQLException e ) {
                            e.printStackTrace();
                        }
                    }
                    if ( tile.getBlob() != null ) {
                        if ( outPS == null ) {
                            outPS = storageDbConnector.createPreparedStatement( "INSERT INTO out.tiles VALUES (?, ?, ?, ?);" );
                        }
                        try {
                            outPS.setInt( 1, tile.getZoom() );
                            outPS.setInt( 2, tile.getX() );
                            outPS.setInt( 3, tile.getMBTilesY() );
                            outPS.setBytes( 4, tile.getBlob() );
                            outPS.addBatch();
                        }
                        catch ( SQLException e ) {
                            e.printStackTrace();
                        }
                    }

                    PrettyTime prettyTime = new PrettyTime();
                    String pTime = prettyTime.format( new Date( sTime ) );

                    long dbSize = outDbConnector.getDbSize();
                    int totDone = storage + remote + old;
                    int left = tilesCount - totDone;
                    long timePerItem = ( System.currentTimeMillis() - sTime ) / totDone;
                    long timeLeft = timePerItem * left;
                    String pTimeLeft = prettyTime.format( new Date( System.currentTimeMillis() + timeLeft ) );

                    System.out.println( "   " + totDone + "/" + storage + "/" + remote + "/" + old + "/" + left + "     " + pTime + "/" + pTimeLeft + "     " + ( dbSize / 1024 ) + "MB" );

                    if ( batches >= 150 ) {
                        if ( storagePS != null ) {
                            storageDbConnector.executePreparedStatementBatch( storagePS );
                        }
                        if ( outPS != null ) {
                            storageDbConnector.executePreparedStatementBatch( outPS );
                        }
                        batches = 0;
                    } else {
                        batches++;
                    }
                }
            }
        }
        if ( storagePS != null ) {
            storageDbConnector.executePreparedStatementBatch( storagePS );
        }
        if ( outPS != null ) {
            storageDbConnector.executePreparedStatementBatch( outPS );
        }
        outDbConnector.close();
        storageDbConnector.close();

        System.out.println( "XXXXXXXXXXXXXXXXXXX>   DONE    <XXXXXXXXXXXXXXXXXXX" );
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
}
