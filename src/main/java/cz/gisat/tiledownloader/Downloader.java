package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.Tile;

import java.io.*;
import java.net.URL;

public class Downloader {
    private Tile tileMin;
    private Tile tileMax;
    private int zoom;

    public Downloader( Tile tileMin, Tile tileMax ) {
        this.tileMin = tileMin;
        this.tileMax = tileMax;
        this.zoom = this.tileMin.getZoom();
    }

    public Downloader( LatLon latLonMin, LatLon latLonMax, int zoom ) {
        new Downloader( latLonMin.getTile( zoom ), latLonMax.getTile( zoom ) );
    }

    public void executeDownload() {
        int tCount = 0;
        for ( int x = tileMin.getX() ; x <= tileMax.getX() ; x++ ) {
            for ( int y = tileMin.getY() ; y >= tileMax.getY() ; y-- ) {
                tCount++;
                String filePath = zoom + "/" + x + "/" + y + ".png";
                String url = "http://tile.openstreetmap.org/" + filePath;

                System.out.print( "Downloading... " + url );
                try {
                    this.saveImage( url, filePath );
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println( "TileCount:" + tCount );
    }

    private void saveImage( String imageUrl, String destinationFile ) throws IOException {
        File imgFile = new File( "map/" + destinationFile );
        if ( imgFile.exists() ) {
            System.out.println( "   -> EXISTS!" );
            return;
        }
        imgFile.getParentFile().mkdirs();
        imgFile.createNewFile();
        URL url = new URL( imageUrl );
        InputStream is = url.openStream();
        OutputStream os = new FileOutputStream( imgFile );

        byte[] b = new byte[ 2048 ];
        int length;

        while ( ( length = is.read( b ) ) != -1 ) {
            os.write( b, 0, length );
        }

        is.close();
        os.close();

        System.out.println( "   -> DONE!" );
    }
}
