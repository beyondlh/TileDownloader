package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.Tile;

public class Main {
    public static void main( String[] args ) {
        int zoom = 10;

        LatLon latLonMin = new LatLon( 48.916646, 12.216218 );
        LatLon latLonMax = new LatLon( 50.883366, 18.591652 );

        Tile tileMin = latLonMin.getTile( zoom );
        Tile tileMax = latLonMax.getTile( zoom );

        System.out.println( tileMin.getX() + " - " + tileMin.getY() );
        System.out.println( tileMax.getX() + " - " + tileMax.getY() );

        Downloader downloader = new Downloader( tileMin, tileMax );
        downloader.executeDownload();
    }
}
