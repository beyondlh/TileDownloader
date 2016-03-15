package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import cz.gisat.tiledownloader.sqlite.DbCreator;
import cz.gisat.tiledownloader.storage.TileDbStorage;

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
                    /*Tile tile = new Tile( x, y, z );
                    tile = tileDbStorage.getFullTile( tile );
                    outDbConnector.addTileToPreparedStatement( outPreparedStatement, tile );

                    this.done++;

                    if ( batches % 250 == 0 ) {
                        outDbConnector.executePreparedStatementBatch( outPreparedStatement );
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
                    }*/
                }
            }
        }
        //outDbConnector.executePreparedStatementBatch( outPreparedStatement );
        outDbConnector.close();
        tileDbStorage.executeResidualPreparedStatementBatch();
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
}
