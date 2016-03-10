package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.Tile;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Downloader {
    private LatLon latLonMin;
    private LatLon latLonMax;
    private int zoom;
    private List< Tile > tiles;
    private long sTime;
    int srv = 0, done = 0, err = 0, skip = 0;

    public Downloader( ArgsParser argsParser ) {
        this.zoom = argsParser.getZoom();
        this.latLonMin = argsParser.getLatLonMin();
        this.latLonMax = argsParser.getLatLonMax();
    }

    private boolean prepareDownload() {
        if ( this.zoom < 0 || this.latLonMin == null || this.latLonMax == null ) {
            System.out.println( "Some of parameters was missing or has set wrong value!" );
            return false;
        }
        sTime = System.currentTimeMillis();
        tiles = new ArrayList< Tile >();
        for ( int z = 0 ; z <= this.zoom ; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX() ; x <= tileMax.getX() ; x++ ) {
                for ( int y = tileMin.getY() ; y >= tileMax.getY() ; y-- ) {
                    tiles.add( new Tile( x, y, z ) );
                }
            }
        }
        if ( tiles.size() == 0 ) {
            System.out.println( "No tiles to download. Wrong locations?" );
            return false;
        }
        System.out.println( this.tiles.size() + " tiles are ready to download...." );
        return true;
    }

    public void executeDownload() {
        if ( !this.prepareDownload() ) {
            return;
        }

        int total = this.tiles.size();
        for ( Tile tile : this.tiles ) {
            String url = "http://mt" + srv + ".google.com/vt/lyrs=m@110&hl=cs&x=" + tile.getX() + "&y=" + tile.getY() + "&z=" + tile.getZoom();
            String filePath = tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png";
            long sqKey = ( ( ( tile.getZoom() << tile.getZoom() ) + tile.getX() ) << tile.getZoom() ) + tile.getY();

            System.out.print( "Downloading... " + url );
            try {
                this.saveImage( url, filePath );
            }
            catch ( Exception e ) {
                System.out.print( "     -> ERROR!" );
                err++;
            }
            PrettyTime prettyTime = new PrettyTime();
            String pTime = prettyTime.format( new Date( this.sTime ) );
            System.out.println( "   " + this.done + "/" + this.skip + "/" + this.err + "/" + ( total - ( this.done + this.skip + this.err ) ) + "     " + pTime + "     " + sqKey );
            srv++;
            if ( srv > 3 ) {
                srv = 0;
            }
        }
    }

    private void saveImage( String imageUrl, String destinationFile ) throws IOException {
        File imgFile = new File( "map/" + destinationFile );
        if ( imgFile.exists() ) {
            System.out.print( "       -> EXISTS!" );
            skip++;
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

        System.out.print( "     -> DONE!" );
        done++;
    }
}
