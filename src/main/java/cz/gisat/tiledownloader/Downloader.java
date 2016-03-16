package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;
import cz.gisat.tiledownloader.objects.MapZoom;
import cz.gisat.tiledownloader.objects.Tile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class Downloader {
    private LatLon latLonMin;
    private LatLon latLonMax;
    private int zoom;
    private int size;
    private String mapSource;

    public Downloader( ArgsParser argsParser ) {
        this.zoom = argsParser.getZoom();
        this.latLonMin = argsParser.getLatLonMin();
        this.latLonMax = argsParser.getLatLonMax();
        this.size = argsParser.getSize();
        this.mapSource = argsParser.getMapSource();
        if ( this.size == 0 ) {
            this.size = 1024;
        }
    }

    public void download() {
        if ( this.zoom < 0 || this.latLonMin == null || this.latLonMax == null ) {
            System.out.println( "Some of parameters was missing or has set wrong value!" );
            return;
        }

        System.out.println( "Downloading and generating of output mbtiles file was started" );

        long sTime = System.currentTimeMillis();

        TileGetter tileGetter = new TileGetter( this.mapSource );
        MapZoom mapZoom = tileGetter.getMapZoom();

        //DbCreator dbCreator = new DbCreator();

        //DbConnector dbConnector = dbCreator.getOutputDb( tileGetter );

        if ( this.zoom > mapZoom.getMaxZoom() ) {
            this.zoom = mapZoom.getMaxZoom();
        }

        int tilesCount = this.getTotalOfTiles();
        long start = System.currentTimeMillis();
        for ( int z = mapZoom.getMinZoom(); z <= this.zoom; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX(); x <= tileMax.getX(); x++ ) {
                for ( int y = tileMin.getY(); y >= tileMax.getY(); y-- ) {
                    Tile tile = new Tile( x, y, z );
                    String tileFileBasicPath = tile.getZoom() + "_" + tile.getX() + "_" + tile.getY() + ".png";
                    String tileFileHash = DigestUtils.sha256Hex( tileFileBasicPath );
                    String tileFileHashPath = Arrays.toString( StringUtils.substring( tileFileHash, 0, 9 ).split( "(?<=\\G...)" ) );
                    tileFileHashPath = tileFileHashPath.replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" ) + "/" + StringUtils.substring( tileFileHash, 10 ) + ".png";
                    System.out.println( tileFileHashPath );

                    /*String tileOldFilePath = "maps/" + tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png";
                    File oldFile = new File( tileOldFilePath );

                    String tileNewFilePath = "storage/" + tileGetter.getMapSource() + "/" + Arrays.toString( tileFileName.split( "(?<=\\G...)" ) ).replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" ) + ".png";
                    File newFile = new File( tileNewFilePath );

                    if ( !oldFile.exists() && !newFile.exists() ) {
                        try {
                            oldFile.getParentFile().mkdirs();
                            URL url = new URL( tileGetter.getTileUrl( tile ) );
                            InputStream is = url.openStream();
                            OutputStream os = new FileOutputStream( oldFile );

                            byte[] b = new byte[ 2048 ];
                            int length;

                            while ( ( length = is.read( b ) ) != -1 ) {
                                os.write( b, 0, length );
                            }

                            is.close();
                            os.close();
                        }
                        catch ( IOException e ) {
                            e.printStackTrace();
                        }
                    }
                    if ( oldFile.exists() && !newFile.exists() ) {
                        try {
                            newFile.getParentFile().mkdirs();
                            FileUtils.copyFile( oldFile, newFile );
                        }
                        catch ( IOException e ) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println( tileOldFilePath + " -> " + tileNewFilePath );*/
                }
            }
        }
        //dbConnector.close();
        System.out.println( "XXXXXXXXXXXXXXXXXXX>   DONE    <XXXXXXXXXXXXXXXXXXX" );
        System.out.println( "TIME: " + ( System.currentTimeMillis() - start ) );
    }

    private int getTotalOfTiles() {
        int tot = 0;
        for ( int z = 0; z <= this.zoom; z++ ) {
            Tile tileMin = this.latLonMin.getTile( z );
            Tile tileMax = this.latLonMax.getTile( z );
            for ( int x = tileMin.getX(); x <= tileMax.getX(); x++ ) {
                for ( int y = tileMin.getY(); y >= tileMax.getY(); y-- ) {
                    tot++;
                }
            }
        }
        return tot;
    }
}
