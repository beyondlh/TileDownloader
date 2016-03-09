package cz.gisat.tiledownloader.objects;

public class LatLon {
    private double lat;
    private double lon;

    public LatLon( double lat, double lon ) {
        this.lat = lat;
        this.lon = lon;
    }

    public Tile getTile( int zoom ) {
        int tileX = ( int ) Math.floor( ( lon + 180 ) / 360 * ( 1 << zoom ) );
        int tileY = ( int ) Math.floor( ( 1 - Math.log( Math.tan( Math.toRadians( lat ) ) + 1 / Math.cos( Math.toRadians( lat ) ) ) / Math.PI ) / 2 * ( 1 << zoom ) );
        if ( tileX < 0 ) {
            tileX = 0;
        }
        if ( tileX >= ( 1 << zoom ) ) {
            tileX = ( ( 1 << zoom ) - 1 );
        }
        if ( tileY < 0 ) {
            tileY = 0;
        }
        if ( tileY >= ( 1 << zoom ) ) {
            tileY = ( ( 1 << zoom ) - 1 );
        }
        return new Tile( tileX, tileY, zoom );
    }
}
