package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;

import java.util.Random;

public class TileGetter {
    private String mapSource;
    private MapZoom googleZoom = new MapZoom( 0, 20 );
    private String[] google = {
            "http://mt0.google.com/vt/lyrs=m@110&hl=cs&x={$x}&y={$y}&z={$z}",
            "http://mt1.google.com/vt/lyrs=m@110&hl=cs&x={$x}&y={$y}&z={$z}",
            "http://mt2.google.com/vt/lyrs=m@110&hl=cs&x={$x}&y={$y}&z={$z}",
            "http://mt3.google.com/vt/lyrs=m@110&hl=cs&x={$x}&y={$y}&z={$z}"
    };
    private MapZoom googleSatZoom = new MapZoom( 0, 20 );
    private String[] googleSat = {
            "http://mt0.google.com/vt/lyrs=s&src=app&x={$x}&y={$y}&z={$z}&s=Galileo",
            "http://mt1.google.com/vt/lyrs=s&src=app&x={$x}&y={$y}&z={$z}&s=Galileo",
            "http://mt2.google.com/vt/lyrs=s&src=app&x={$x}&y={$y}&z={$z}&s=Galileo",
            "http://mt3.google.com/vt/lyrs=s&src=app&x={$x}&y={$y}&z={$z}&s=Galileo"
    };
    private MapZoom osmZoom = new MapZoom( 0, 19 );
    private String[] osm = {
            "http://a.tile.openstreetmap.org/{$z}/{$x}/{$y}.png",
            "http://b.tile.openstreetmap.org/{$z}/{$x}/{$y}.png",
            "http://c.tile.openstreetmap.org/{$z}/{$x}/{$y}.png"
    };
    private MapZoom mapquestZoom = new MapZoom( 0, 18 );
    private String[] mapquest = {
            "http://otile1.mqcdn.com/tiles/1.0.0/osm/{$z}/{$x}/{$y}.png",
            "http://otile2.mqcdn.com/tiles/1.0.0/osm/{$z}/{$x}/{$y}.png",
            "http://otile3.mqcdn.com/tiles/1.0.0/osm/{$z}/{$x}/{$y}.png",
            "http://otile4.mqcdn.com/tiles/1.0.0/osm/{$z}/{$x}/{$y}.png"
    };

    public TileGetter( String mapSource ) {
        this.mapSource = mapSource;
    }

    public String getTileUrl( Tile tile ) {
        String tileUrl = null;
        switch ( this.getMapSource() ) {
            case "google":
                tileUrl = google[ new Random().nextInt( google.length ) ];
                break;
            case "googlesat":
                tileUrl = googleSat[ new Random().nextInt( googleSat.length ) ];
                break;
            case "osm":
                tileUrl = osm[ new Random().nextInt( osm.length ) ];
                break;
            case "mapquest":
                tileUrl = mapquest[ new Random().nextInt( mapquest.length ) ];
                break;
            default:
                tileUrl = google[ new Random().nextInt( google.length ) ];
        }
        tileUrl = tileUrl.replace( "{$x}", String.valueOf( tile.getX() ) );
        tileUrl = tileUrl.replace( "{$y}", String.valueOf( tile.getY() ) );
        tileUrl = tileUrl.replace( "{$z}", String.valueOf( tile.getZoom() ) );
        return tileUrl;
    }

    public MapZoom getMapZoom() {
        MapZoom mapZoom;
        switch ( this.getMapSource() ) {
            case "google":
                mapZoom = this.googleZoom;
                break;
            case "googlesat":
                mapZoom = this.googleSatZoom;
                break;
            case "osm":
                mapZoom = this.osmZoom;
                break;
            case "mapquest":
                mapZoom = this.mapquestZoom;
                break;
            default:
                mapZoom = this.googleZoom;
        }
        return mapZoom;
    }

    public String getMapSource() {
        if ( this.mapSource == null ) {
            return "google";
        } else {
            return this.mapSource;
        }
    }

}
