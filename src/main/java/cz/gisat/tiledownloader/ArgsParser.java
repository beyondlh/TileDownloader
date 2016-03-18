package cz.gisat.tiledownloader;

import cz.gisat.tiledownloader.objects.LatLon;

public class ArgsParser {
    private LatLon latLonMin;
    private LatLon latLonMax;
    private int zoom;
    private int mzoom;
    private int size;
    private String mapSource;
    private boolean onlystorage = false;

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
            } else if ( arg.toLowerCase().startsWith( "mzoom:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    this.mzoom = Integer.parseInt( argAr[ 1 ] );
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "lmin:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    String[] latLon = argAr[ 1 ].split( "," );
                    latLonMin = new LatLon( Double.parseDouble( latLon[ 0 ] ), Double.parseDouble( latLon[ 1 ] ) );
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "lmax:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    String[] latLon = argAr[ 1 ].split( "," );
                    latLonMax = new LatLon( Double.parseDouble( latLon[ 0 ] ), Double.parseDouble( latLon[ 1 ] ) );
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "s:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    this.size = Integer.parseInt( argAr[ 1 ] );
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "bb:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    if ( argAr[ 1 ].equalsIgnoreCase( "cze" ) ) {
                        this.latLonMin = new LatLon( 48.55, 12.09 );
                        this.latLonMax = new LatLon( 51.06, 18.87 );
                    }
                } catch ( Exception e ) {
                }
            } else if ( arg.toLowerCase().startsWith( "map:" ) ) {
                String[] argAr = arg.split( ":" );
                try {
                    this.mapSource = argAr[ 1 ].toLowerCase();
                } catch ( Exception e ) {
                }
            } else if ( arg.equalsIgnoreCase( "storageonly" ) ) {
                this.onlystorage = true;
            }
        }
    }

    public int getZoom() {
        return this.zoom;
    }

    public int getMzoom() {
        return this.mzoom;
    }

    public LatLon getLatLonMin() {
        return this.latLonMin;
    }

    public LatLon getLatLonMax() {
        return this.latLonMax;
    }

    public int getSize() {
        return this.size;
    }

    public String getMapSource() {
        return this.mapSource;
    }

    public boolean isOnlystorage() {
        return this.onlystorage;
    }
}
