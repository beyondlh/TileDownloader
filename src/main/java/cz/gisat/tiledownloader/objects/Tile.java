package cz.gisat.tiledownloader.objects;

public class Tile {
    int lat;
    int lon;
    int zoom;

    public Tile( int lon, int lat, int zoom ){
        this.lat = lat;
        this.lon = lon;
        this.zoom = zoom;
    }
}
