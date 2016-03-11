package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;

public class ArgsParser {
    private LatLon latLonMin;
    private LatLon latLonMax;
    private int zoom;

    public ArgsParser( String[] args ) {
        this.parseArguments( args );
    }

    private void parseArguments( String[] args ) {
        for ( String arg : args ) {
            if ( arg.toLowerCase().startsWith( "zoom:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    this.zoom = Integer.parseInt( argAr[ 1 ] );
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "lmin:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    String[] latLon = argAr[ 1 ].split( "," );
                    latLonMin = new LatLon( Double.parseDouble( latLon[0] ), Double.parseDouble( latLon[1] ));
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "lmax:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    String[] latLon = argAr[ 1 ].split( "," );
                    latLonMax = new LatLon( Double.parseDouble( latLon[0] ), Double.parseDouble( latLon[1] ));
                } catch ( Exception e ) {
                }
            }
        }
    }

    public int getZoom() {
        return this.zoom;
    }

    public LatLon getLatLonMin(){
        return this.latLonMin;
    }

    public LatLon getLatLonMax(){
        return this.latLonMax;
    }
}
