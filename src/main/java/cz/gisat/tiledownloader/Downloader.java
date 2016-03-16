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

        if ( this.zoom > mapZoom.getMaxZoom() ) {
            this.zoom = mapZoom.getMaxZoom();
        }

        int tilesCount = this.getTotalOfTiles();
        int batch = 0;
        int exists = 0, notfound = 0;
        PreparedStatement preparedStatement = storageDbC.createPreparedStatement( "INSERT OR IGNORE INTO tiles VALUES( ?, ?, ?, ? );" );
        for ( int z = mapZoom.getMinZoom(); z <= this.zoom; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX(); x <= tileMax.getX(); x++ ) {
                for ( int y = tileMin.getY(); y >= tileMax.getY(); y-- ) {
                    Tile tile = new Tile( x, y, z );
                    String tileUrl = tileGetter.getTileUrl( tile );

                    System.out.print( tileUrl );

                    File oldImgFile = new File(
                            "maps/" + tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png"
                    );
                    if ( oldImgFile.exists() ) {
                        try {
                            FileInputStream inputStream = new FileInputStream( oldImgFile );
                            tile.setBlob( IOUtils.toByteArray( inputStream ) );
                            if ( tile.getBlob() != null && tile.getBlob().length != 0 ) {
                                preparedStatement.setInt( 1, tile.getZoom() );
                                preparedStatement.setInt( 2, tile.getX() );
                                preparedStatement.setInt( 3, tile.getMBTilesY() );
                                preparedStatement.setBytes( 4, tile.getBlob() );
                                preparedStatement.addBatch();
                            } else {

                            }
                            inputStream.close();
                        } catch ( IOException | SQLException e ) {
                            e.printStackTrace();
                        }
                        exists++;
                    } else {
                        notfound++;
                    }

                    ResultSet resultSet = storageDbC.executeQuery(
                            "SELECT 1 FROM tiles WHERE zoom_level=" + tile.getZoom() + " AND tile_column=" + tile.getX() + " AND tile_row=" + tile.getMBTilesY() + ";"
                    );
                    try {
                        if ( resultSet == null || !resultSet.next() ) {

                        } else {
                            System.out.print( "     EXIST" );
                        }
                    } catch ( SQLException ignored ) {
                    }

                    if ( batch >= 150 ) {
                        storageDbC.executePreparedStatementBatch( preparedStatement );
                        batch = 0;
                    } else {
                        batch++;
                    }

                    /*String tileOldFilePath = "maps/" + tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png";
                    File oldFile = new File( tileOldFilePath );

                    String tileNewFilePath = "storage/" + tileGetter.getMapSource() + "/" + Arrays.toString( tileFileName.split( "(?<=\\G...)" ) ).replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" ) + ".png";
                    File newFile = new File( tileNewFilePath );

                    if ( !oldFile.exists() && !newFile.exists() ) {
                        try {
                            oldFile.getParentFile().mkdirs();
                            URL url = new URL( tileGetter.getTileUrl( tile ) );
                            InputStream is = url.openStream();
                            OutputStream os = new FileOutputStream( oldFile );

                            byte[] b = new byte[ 2048 ];
                            int length;

                            while ( ( length = is.read( b ) ) != -1 ) {
                                os.write( b, 0, length );
                            }

                            is.close();
                            os.close();
                        }
                        catch ( IOException e ) {
                            e.printStackTrace();
                        }
                    }
                    if ( oldFile.exists() && !newFile.exists() ) {
                        try {
                            newFile.getParentFile().mkdirs();
                            FileUtils.copyFile( oldFile, newFile );
                        }
                        catch ( IOException e ) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println( tileOldFilePath + " -> " + tileNewFilePath );*/
                    System.out.print( "\n" );
                }
            }
        }
        storageDbC.executePreparedStatementBatch( preparedStatement );
        storageDbC.close();
        outputDbC.close();
        System.out.println( "XXXXXXXXXXXXXXXXXXX>   DONE    <XXXXXXXXXXXXXXXXXXX" );
        System.out.println( "TIME: " + ( System.currentTimeMillis() - sTime ) );
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
}
