package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.Tile;

public class Downloader {
    private Tile tileMin;
    private Tile tileMax;
    private int zoom;

    public Downloader( Tile tileMin, Tile tileMax ){
        this.tileMin = tileMin;
        this.tileMax = tileMax;
        this.zoom = this.tileMin.getZoom();
    }

    public Downloader( LatLon latLonMin, LatLon latLonMax, int zoom ){
        new Downloader( latLonMin.getTile( zoom ), latLonMax.getTile( zoom )  );
    }

    public void executeDownload(){
        int tCount = 0;
        for( int x = tileMin.getX() ; x < tileMax.getX() ; x++  ){
            for( int y = tileMin.getY() ; y > tileMax.getY() ; y--  ){
                tCount++;
                System.out.println( "http://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png" );
            }
        }
        System.out.println( "TileCount:" +tCount );
    }
}
