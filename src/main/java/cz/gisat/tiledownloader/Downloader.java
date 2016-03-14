package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import cz.gisat.tiledownloader.sqlite.DbCreator;
import cz.gisat.tiledownloader.storage.TileDbStorage;
import org.apache.commons.io.IOUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.*;
import java.net.URL;
import java.sql.PreparedStatement;
import java.util.Date;

public class Downloader {
    int done = 0, err = 0, skip = 0;
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

        DbConnector outDbConnector = dbCreator.getOutputDb( tileGetter );
        PreparedStatement outPreparedStatement = outDbConnector.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );

        DbConnector storageDbConnector = dbCreator.getStorageDb( tileGetter );

        TileDbStorage tileDbStorage = new TileDbStorage( tileGetter, storageDbConnector );

        if ( this.zoom > mapZoom.getMaxZoom() ) {
            this.zoom = mapZoom.getMaxZoom();
        }

        int batches = 0;
        int tilesCount = this.getTotalOfTiles();
        for ( int z = mapZoom.getMinZoom(); z <= this.zoom; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX(); x <= tileMax.getX(); x++ ) {
                for ( int y = tileMin.getY(); y >= tileMax.getY(); y-- ) {
                    Tile tile = new Tile( x, y, z );
                    tile = tileDbStorage.getFullTile( tile );

                    outDbConnector.addTileToPreparedStatement( outPreparedStatement, tile );

                    this.done++;


                    /*String url = tileGetter.getTileUrl();
                    url = url.replace( "{$x}", String.valueOf( x ) );
                    url = url.replace( "{$y}", String.valueOf( y ) );
                    url = url.replace( "{$z}", String.valueOf( z ) );

                    String filePath = "maps/" + tileGetter.getMapSource() + "/" + z + "/" + x + "/" + y + ".png";

                    System.out.print( url );

                    try {
                        this.saveImage( url, filePath, new Tile( x, y, z ), outDbConnector, outPreparedStatement );
                        batches++;
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        err++;
                    }*/

                    if ( batches % 250 == 0 ) {
                        outDbConnector.executePreparedStatementBatch( outPreparedStatement );
                        //storageDbConnector.executePreparedStatementBatch( storagePreparedStatement );
                    }

                    PrettyTime prettyTime = new PrettyTime();
                    String pTime = prettyTime.format( new Date( sTime ) );

                    long dbSize = outDbConnector.getDbSize();
                    int totDone = this.done + this.skip + this.err;
                    int left = tilesCount - totDone;
                    long timePerItem = ( System.currentTimeMillis() - sTime ) / totDone;
                    long timeLeft = timePerItem * left;
                    String pTimeLeft = prettyTime.format( new Date( System.currentTimeMillis() + timeLeft ) );

                    System.out.println( "   " + this.done + "/" + this.skip + "/" + this.err + "/" + left + "     " + pTime + "/" + pTimeLeft + "     " + ( dbSize / 1024 ) + "MB" );

                    if ( dbSize / 1024 >= this.size ) {
                        outDbConnector.executePreparedStatementBatch( outPreparedStatement );
                        outDbConnector.close();
                        outDbConnector = dbCreator.getOutputDb( tileGetter );
                        outPreparedStatement = outDbConnector.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );
                    }
                }
            }
        }
        outDbConnector.executePreparedStatementBatch( outPreparedStatement );
        outDbConnector.close();
        //storageDbConnector.executePreparedStatementBatch( storagePreparedStatement );
        storageDbConnector.close();
        System.out.println( "!|! - DONE  !|!" );
    }

    private int getTotalOfTiles() {
        int tot = 0;
        for ( int z = 0; z <= this.zoom; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX(); x <= tileMax.getX(); x++ ) {
                for ( int y = tileMin.getY(); y >= tileMax.getY(); y-- ) {
                    tot++;
                }
            }
        }
        return tot;
    }

    private void saveImage( String imageUrl, String destinationFile, Tile tile, DbConnector dbConnector, PreparedStatement outPreparedStatement ) throws IOException {
        File imgFile = new File( destinationFile );
        if ( !imgFile.exists() || imgFile.length() == 0 ) {
            if ( !imgFile.exists() ) {
                imgFile.getParentFile().mkdirs();
                imgFile.createNewFile();
            }

            URL url = new URL( imageUrl );
            InputStream is = url.openStream();
            OutputStream os = new FileOutputStream( imgFile );

            byte[] b = new byte[ 2048 ];
            int length;

            while ( ( length = is.read( b ) ) != -1 ) {
                os.write( b, 0, length );
            }

            System.out.print( "     -> DONE! " );

            is.close();
            os.close();
            done++;
        } else {
            System.out.print( "     -> EXIST!" );
            skip++;
        }
        FileInputStream fileInputStream = new FileInputStream( imgFile );
        if ( dbConnector.addTileToPreparedStatement( outPreparedStatement, tile, IOUtils.toByteArray( fileInputStream ) ) ) {
            System.out.print( "     DB-IN-OK" );
        } else {
            System.out.print( "     DB-IN-ER" );
        }
        fileInputStream.close();
    }
}
