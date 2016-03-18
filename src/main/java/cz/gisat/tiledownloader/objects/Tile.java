package cz.gisat.tiledownloader.objects;

public class Tile {
    private int x;
    private int y;
    private int z;
    private String sX;
    private String sY;
    private String sZ;
    private byte[] blob;

    public Tile( int x, int y, int z ){
        this.x = x;
        this.y = y;
        this.z = z;
        this.sX = String.valueOf( this.x );
        this.sY = String.valueOf( this.y );
        this.sZ = String.valueOf( this.z );
    }

    public Tile( int x, int y, int z, byte[] blob ) {
        this( x, y, z );
        this.blob = blob;
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

    public byte[] getBlob() {
        return blob;
    }

    public void setBlob( byte[] blob ) {
        this.blob = blob;
    }

    public int getMBTilesY() {
        return ( ( 1 << this.getZoom() ) - this.getY() - 1 );
    }

    public String getsX() {
        return sX;
    }

    public String getsY() {
        return sY;
    }

    public String getsZ() {
        return sZ;
    }
}
