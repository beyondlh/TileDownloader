package cz.gisat.tiledownloader.objects;

public class MapZoom {
    private int minZoom;
    private int maxZoom;

    public MapZoom( int minZoom, int maxZoom ) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    public int getMinZoom() {
        return minZoom;
    }

    public int getMaxZoom() {
        return maxZoom;
    }
}
