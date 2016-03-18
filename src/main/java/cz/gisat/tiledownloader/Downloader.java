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
    private int mzoom;
    private int size;
    private String mapSource;
    private boolean onlystorage;

    public Downloader( ArgsParser argsParser ) {
        this.zoom = argsParser.getZoom();
        this.mzoom = argsParser.getMzoom();
        this.latLonMin = argsParser.getLatLonMin();
        this.latLonMax = argsParser.getLatLonMax();
        this.size = argsParser.getSize();
        this.mapSource = argsParser.getMapSource();
        this.onlystorage = argsParser.isOnlystorage();
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

        TileGetter tileGetter = new TileGetter( this.mapSource );
        MapZoom mapZoom = tileGetter.getMapZoom();

        DbCreator dbCreator = new DbCreator();

        DbConnector storageDbC = dbCreator.getDb( tileGetter, true );
        DbConnector outputDbC = null;
        if ( !this.onlystorage ) {
            outputDbC = dbCreator.getDb( tileGetter, false );
            outputDbC.executeUpdate( "ATTACH '" + storageDbC.getDbFile().getAbsolutePath() + "' AS storage" );
        }

        if ( this.zoom > mapZoom.getMaxZoom() ) {
            this.zoom = mapZoom.getMaxZoom();
        }

        long sTime = System.currentTimeMillis();
        int tilesCount = this.getTotalOfTiles( mapZoom );
        int batch = 0, file = 0, web = 0, error = 0, db = 0, done = 0;
        Tile tile;
        String tileUrl;
        PreparedStatement storageStatement = null;
        PreparedStatement outputStatement = null;
        for ( int z = ( this.mzoom > mapZoom.getMinZoom() ? this.mzoom : mapZoom.getMinZoom() ); z <= this.zoom; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX(); x <= tileMax.getX(); x++ ) {
                for ( int y = tileMin.getY(); y >= tileMax.getY(); y-- ) {
                    tile = new Tile( x, y, z );
                    tileUrl = tileGetter.getTileUrl( tile );

                    System.out.print( tileUrl );

                    boolean inStorage = false;

                    try {
                        PreparedStatement preparedStatement = storageDbC.getConnection().prepareStatement(
                                "SELECT 1 FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=? LIMIT 1;"
                        );
                        preparedStatement.setInt( 1, tile.getZoom() );
                        preparedStatement.setInt( 2, tile.getX() );
                        preparedStatement.setInt( 3, tile.getMBTilesY() );
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if ( resultSet.next() ) {
                            inStorage = true;
                            db++;
                        }
                        resultSet.close();
                        preparedStatement.close();
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }

                    if ( !inStorage ) {
                        String oldImgFilePath = "maps/" + tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png";
                        File oldImgFile = new File( oldImgFilePath );
                        if ( oldImgFile.exists() ) {
                            try {
                                FileInputStream stream = new FileInputStream( oldImgFile );
                                byte[] blob = IOUtils.toByteArray( stream );
                                if ( blob != null && blob.length > 0 ) {
                                    if ( storageStatement == null ) {
                                        storageStatement = storageDbC.getConnection().prepareStatement(
                                                "INSERT INTO tiles VALUES ( ?, ?, ?, ? );"
                                        );
                                    }
                                    storageStatement.setInt( 1, tile.getZoom() );
                                    storageStatement.setInt( 2, tile.getX() );
                                    storageStatement.setInt( 3, tile.getMBTilesY() );
                                    storageStatement.setBytes( 4, blob );
                                    storageStatement.addBatch();
                                    inStorage = true;
                                    batch++;
                                    file++;
                                }
                                stream.close();
                            } catch ( IOException | SQLException e ) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if ( !inStorage ) {
                        try {
                            URL url = new URL( tileUrl );
                            InputStream stream = url.openStream();
                            byte[] blob = IOUtils.toByteArray( stream );
                            if ( blob != null && blob.length > 0 ) {
                                if ( storageStatement == null ) {
                                    storageStatement = storageDbC.getConnection().prepareStatement(
                                            "INSERT INTO tiles VALUES ( ?, ?, ?, ? );"
                                    );
                                }
                                storageStatement.setInt( 1, tile.getZoom() );
                                storageStatement.setInt( 2, tile.getX() );
                                storageStatement.setInt( 3, tile.getMBTilesY() );
                                storageStatement.setBytes( 4, blob );
                                storageStatement.addBatch();
                                inStorage = true;
                                batch++;
                                web++;
                            }
                            stream.close();
                        } catch ( IOException | SQLException e ) {
                            e.printStackTrace();
                        }
                    }

                    if ( inStorage ) {
                        try {
                            if ( outputDbC != null ) {
                                if ( outputStatement == null ) {
                                    outputStatement = outputDbC.getConnection().prepareStatement(
                                            "INSERT INTO tiles SELECT * FROM storage.tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?;"
                                    );
                                }
                                outputStatement.setInt( 1, tile.getZoom() );
                                outputStatement.setInt( 2, tile.getX() );
                                outputStatement.setInt( 3, tile.getMBTilesY() );
                                outputStatement.addBatch();
                                batch++;
                            }
                        } catch ( SQLException e ) {
                            e.printStackTrace();
                        }
                    } else {
                        error++;
                    }

                    if ( batch >= 100 ) {
                        try {
                            if ( storageStatement != null ) {
                                storageStatement.executeBatch();
                                storageStatement.clearBatch();
                                storageStatement.close();
                                storageStatement = null;
                            }
                            if ( outputStatement != null ) {
                                outputStatement.executeBatch();
                                outputStatement.clearBatch();
                                outputStatement.close();
                                outputStatement = null;
                            }
                            batch = 0;
                        } catch ( SQLException e ) {
                            e.printStackTrace();
                        }
                    }

                    if ( outputDbC != null && outputDbC.getDbSize() >= 1024 * 1024 * this.size ) {
                        try {
                            if ( outputStatement != null ) {
                                outputStatement.executeBatch();
                                outputStatement.clearBatch();
                                outputStatement.close();
                                outputStatement = null;
                            }
                            outputDbC.close();
                            outputDbC = dbCreator.getDb( tileGetter, false );
                            outputDbC.executeUpdate( "ATTACH '" + storageDbC.getDbFile().getAbsolutePath() + "' AS storage" );
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }

                    }

                    done++;

                    long eTime = System.currentTimeMillis() - sTime;
                    long tpt = eTime / done;
                    long eta = tpt * ( tilesCount - done );
                    PrettyTime prettyTime = new PrettyTime();
                    String pEta = prettyTime.format( new Date( System.currentTimeMillis() + eta ) );

                    System.out.print( "     DB:" + db );
                    System.out.print( " F:" + file );
                    System.out.print( " W:" + web );
                    System.out.print( " ER:" + error );
                    System.out.print( " L:" + ( tilesCount - done ) );
                    System.out.print( " TPT:" + tpt );
                    System.out.print( " ETA:" + pEta );
                    System.out.print( " DSS:" + storageDbC.getDbSize() );
                    if ( outputDbC != null ) {
                        System.out.print( " DOS:" + outputDbC.getDbSize() );
                    }
                    System.out.println();
                }
            }
        }

        try {
            if ( storageStatement != null ) {
                storageStatement.executeBatch();
                storageStatement.clearBatch();
                storageStatement.close();
            }
            if ( outputStatement != null ) {
                outputStatement.executeBatch();
                outputStatement.clearBatch();
                outputStatement.close();
            }
            storageDbC.close();
            if ( outputDbC != null ) {
                outputDbC.close();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        System.out.println( "XXXXXXXXXXXXXXXXXXX>   DONE    <XXXXXXXXXXXXXXXXXXX" );
        System.out.println( "TIME: " + ( System.currentTimeMillis() - sTime ) );
    }

    private int getTotalOfTiles( MapZoom mapZoom ) {
        int tot = 0;
        for ( int z = ( this.mzoom > mapZoom.getMinZoom() ? this.mzoom : mapZoom.getMinZoom() ); z <= this.zoom; z++ ) {
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