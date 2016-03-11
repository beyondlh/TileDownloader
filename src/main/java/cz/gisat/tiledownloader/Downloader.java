package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import cz.gisat.tiledownloader.sqlite.TableCreator;
import org.apache.commons.io.IOUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.*;
import java.net.URL;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Downloader {
    int srv = 0, done = 0, err = 0, skip = 0;
    private LatLon latLonMin;
    private LatLon latLonMax;
    private int zoom;
    private List< Tile > tiles;
    private long sTime;

    public Downloader( ArgsParser argsParser ) {
        this.zoom = argsParser.getZoom();
        this.latLonMin = argsParser.getLatLonMin();
        this.latLonMax = argsParser.getLatLonMax();
    }

    private boolean prepareDownload() {
        if ( this.zoom < 0 || this.latLonMin == null || this.latLonMax == null ) {
            System.out.println( "Some of parameters was missing or has set wrong value!" );
            return false;
        }
        sTime = System.currentTimeMillis();
        tiles = new ArrayList< Tile >();
        for ( int z = 0 ; z <= this.zoom ; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX() ; x <= tileMax.getX() ; x++ ) {
                for ( int y = tileMin.getY() ; y >= tileMax.getY() ; y-- ) {
                    tiles.add( new Tile( x, y, z ) );
                }
            }
        }
        if ( tiles.size() == 0 ) {
            System.out.println( "No tiles to download. Wrong locations?" );
            return false;
        }
        System.out.println( this.tiles.size() + " tiles are ready to download...." );
        return true;
    }

    public void executeDownload() {
        if ( !this.prepareDownload() ) {
            return;
        }

        DbConnector dbConnector = initNewDb();
        PreparedStatement preparedStatement = dbConnector.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );

        int total = this.tiles.size();
        int batches = 0;
        for ( Tile tile : this.tiles ) {
            String url = "http://mt" + srv + ".google.com/vt/lyrs=m@110&hl=cs&x=" + tile.getX() + "&y=" + tile.getY() + "&z=" + tile.getZoom();
            String filePath = tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png";

            System.out.print( "Downloading... " + url );
            try {
                this.saveImage( url, filePath, tile, dbConnector, preparedStatement );
                batches++;
            }
            catch ( Exception e ) {
                e.printStackTrace();
                err++;
            }
            PrettyTime prettyTime = new PrettyTime();
            String pTime = prettyTime.format( new Date( this.sTime ) );
            srv++;
            if ( srv > 3 ) {
                srv = 0;
            }
            if ( batches % 100 == 0 ) {
                dbConnector.executePreparedStatementBatch( preparedStatement );
            }
            long dbSize = dbConnector.getDbSize();
            if ( dbSize % ( 1024 * 1024 ) == 1024 ) {
                dbConnector.close();
                dbConnector = initNewDb();
                preparedStatement = dbConnector.createPreparedStatement( "INSERT INTO tiles VALUES(?, ?, ?, ?)" );
            }
            System.out.println( "   " + this.done + "/" + this.skip + "/" + this.err + "/" + ( total - ( this.done + this.skip + this.err ) ) + "     " + pTime + "     " + dbSize );
        }
        dbConnector.close();
    }

    private void saveImage( String imageUrl, String destinationFile, Tile tile, DbConnector dbConnector, PreparedStatement preparedStatement ) throws IOException {
        File imgFile = new File( "map/" + destinationFile );
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

            System.out.print( "     -> DONE!" );

            is.close();
            os.close();
            done++;
        } else {
            System.out.print( "     -> EXIST!" );
            FileInputStream fileInputStream = new FileInputStream( imgFile );
            if ( dbConnector.addTileToPreparedStatement( preparedStatement, tile, IOUtils.toByteArray( fileInputStream ) ) ) {
                System.out.print( "     DB-IN-OK" );
            } else {
                System.out.print( "     DB-IN-ER" );
            }
            fileInputStream.close();
            skip++;
        }
    }

    private DbConnector initNewDb() {
        SimpleDateFormat fileNameFormat = new SimpleDateFormat( "y_MM_dd_HH_mm_ss" );
        SimpleDateFormat createdFormat = new SimpleDateFormat( "y.MM.dd HH:mm:ss" );
        String fileName = fileNameFormat.format( new Date() ) + ".mbtiles";
        String created = createdFormat.format( new Date() );

        File dbFile = new File( fileName );

        DbConnector dbConnector = new DbConnector( dbFile.getAbsolutePath() );
        dbConnector.open();

        TableCreator tableCreator = new TableCreator( dbConnector );
        if ( !tableCreator.exists( "metadata" ) ) {
            tableCreator.create( "CREATE TABLE metadata (name text, value text)" );
            dbConnector.executeSqlIns( "INSERT INTO metadata VALUES ('created', '" + created + "')" );
        }
        if ( !tableCreator.exists( "tiles" ) ) {
            tableCreator.create( "CREATE TABLE tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob)" );
        }

        return dbConnector;
    }
}
