package cz.gisat.tiledownloader.objects;

public class Tile {
    private int x;
    private int y;
    private int z;

    public Tile( int x, int y, int z ){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZoom() {
        return this.z;
    }
}
