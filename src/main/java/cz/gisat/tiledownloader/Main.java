package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.Tile;

public class Main {
    public static void main( String[] args ) {
        int zoom = 12;

        LatLon latLonMin = new LatLon( 48.916646, 12.216218 );
        LatLon latLonMax = new LatLon( 50.883366, 18.591652 );

        for( int z = 0 ; z <= zoom ; z++ ){
            Tile tileMin = latLonMin.getTile( z );
            Tile tileMax = latLonMax.getTile( z );

            Downloader downloader = new Downloader( tileMin, tileMax );
            downloader.executeDownload();
        }
    }
}
