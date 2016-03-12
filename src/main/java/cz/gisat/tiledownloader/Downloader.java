package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import cz.gisat.tiledownloader.sqlite.TableCreator;
import org.apache.commons.io.IOUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.*;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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

        DbConnector dbConnector = initNewDb( tileGetter );
        PreparedStatement preparedStatement = dbConnector.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );

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
                    String url = tileGetter.getTileUrl();
                    url = url.replace( "{$x}", String.valueOf( x ) );
                    url = url.replace( "{$y}", String.valueOf( y ) );
                    url = url.replace( "{$z}", String.valueOf( z ) );

                    String filePath = "maps/" + tileGetter.getMapSource() + "/" + z + "/" + x + "/" + y + ".png";

                    System.out.print( url );

                    try {
                        this.saveImage( url, filePath, new Tile( x, y, z ), dbConnector, preparedStatement );
                        batches++;
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        err++;
                    }

                    if ( batches % 250 == 0 ) {
                        dbConnector.executePreparedStatementBatch( preparedStatement );
                    }

                    PrettyTime prettyTime = new PrettyTime();
                    String pTime = prettyTime.format( new Date( sTime ) );

                    long dbSize = dbConnector.getDbSize();
                    int totDone = this.done + this.skip + this.err;
                    int left = tilesCount - totDone;
                    long timePerItem = ( System.currentTimeMillis() - sTime ) / totDone;
                    long timeLeft = timePerItem * left;
                    String pTimeLeft = prettyTime.format( new Date( System.currentTimeMillis() + timeLeft ) );

                    System.out.println( "   " + this.done + "/" + this.skip + "/" + this.err + "/" + left + "     " + pTime + "/" + pTimeLeft + "     " + ( dbSize / 1024 ) + "MB" );

                    if ( dbSize / 1024 >= this.size ) {
                        dbConnector.executePreparedStatementBatch( preparedStatement );
                        dbConnector.close();
                        dbConnector = initNewDb( tileGetter );
                        preparedStatement = dbConnector.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );
                    }
                }
            }
        }
        dbConnector.executePreparedStatementBatch( preparedStatement );
        dbConnector.close();
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

    private void saveImage( String imageUrl, String destinationFile, Tile tile, DbConnector dbConnector, PreparedStatement preparedStatement ) throws IOException {
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
        if ( dbConnector.addTileToPreparedStatement( preparedStatement, tile, IOUtils.toByteArray( fileInputStream ) ) ) {
            System.out.print( "     DB-IN-OK" );
        } else {
            System.out.print( "     DB-IN-ER" );
        }
        fileInputStream.close();
    }

    private DbConnector initNewDb( TileGetter tileGetter ) {
        SimpleDateFormat fileNameFormat = new SimpleDateFormat( "y_MM_dd_HH_mm_ss" );
        SimpleDateFormat createdFormat = new SimpleDateFormat( "y.MM.dd HH:mm:ss" );
        String fileName = fileNameFormat.format( new Date() ) + ".mbtiles";
        String created = createdFormat.format( new Date() );

        File outputFolder = new File( "out/" + tileGetter.getMapSource() );
        outputFolder.mkdirs();
        File dbFile = new File( outputFolder, fileName );

        DbConnector dbConnector = new DbConnector( dbFile.getAbsolutePath() );
        dbConnector.open();

        TableCreator tableCreator = new TableCreator( dbConnector );
        if ( !tableCreator.exists( "metadata" ) ) {
            try {
                tableCreator.create( "CREATE TABLE metadata (name text, value text);" );
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
        }
        if ( !tableCreator.exists( "android_metadata" ) ) {
            tableCreator.create( "CREATE TABLE android_metadata (locale TEXT DEFAULT 'en_US');" );
            dbConnector.executeSqlUp( "INSERT INTO android_metadata VALUES ('en_US');" );
        }
        dbConnector.executeSqlUp( "CREATE UNIQUE INDEX metadata_idx  ON metadata (name)" );
        dbConnector.executeSqlUp( "CREATE INDEX tiles_idx on tiles (zoom_level, tile_column, tile_row)" );
        return dbConnector;
    }
}
