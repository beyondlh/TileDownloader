package cz.gisat.tiledownloader.storage;

import cz.gisat.tiledownloader.TileGetter;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TileDbStorage {
    private TileGetter tileGetter;
    private DbConnector storageDbConnector;
    private String tileUrl;

    public TileDbStorage( TileGetter tileGetter, DbConnector storageDbConnector ) {
        this.tileGetter = tileGetter;
        this.storageDbConnector = storageDbConnector;
    }

    public Tile getFullTile( Tile tile ) {
        this.tileUrl = tileGetter.getTileUrl( tile );
        System.out.print( this.tileUrl );
        tile = this.getTileFromStorage( tile );
        return tile;
    }

    private Tile getTileFromStorage( Tile tile ) {
        try {
            ResultSet resultSet = this.storageDbConnector.executeSqlQry(
                    "SELECT tile_data FROM tiles WHERE zoom_level=" + tile.getZoom() + " AND tile_column=" + tile.getX() + " AND tile_row=" + tile.getMBTilesY() + ";"
            );
            if ( resultSet != null ) {
                tile.setBlob( resultSet.getBytes( "tile_data" ) );
                System.out.print( "     EXIST!" );
            } else {
                tile = this.getTileFromRemoteStorage( tile );
                System.out.print( "     DONE! " );
            }
        }
        catch ( SQLException e ) {
            e.printStackTrace();
        }
        return tile;
    }

    private Tile getTileFromRemoteStorage( Tile tile ) {
        try {
            byte[] blob = this.getBlobBytesFromOldStorage( tile );
            if ( blob == new byte[ 0 ] ) {
                URL url = new URL( this.tileUrl );
                InputStream inputStream = url.openStream();
                blob = IOUtils.toByteArray( inputStream );
            }
            tile.setBlob( blob );
            PreparedStatement preparedStatement = this.storageDbConnector.createPreparedStatement( "REPLACE INTO tiles VALUES (?, ?, ?, ?);" );
            this.storageDbConnector.addTileToPreparedStatement( preparedStatement, tile );
            this.storageDbConnector.executePreparedStatementBatch( preparedStatement );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        return tile;
    }

    private byte[] getBlobBytesFromOldStorage( Tile tile ) {
        byte[] blob = new byte[ 0 ];
        File imageFile = new File( tileGetter.getMapSource() + "/" + tile.getZoom() + "/" + tile.getX() + "/" + tile.getY() + ".png" );
        if ( imageFile.exists() ) {
            try {
                blob = IOUtils.toByteArray( new FileInputStream( imageFile ) );
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        return blob;
    }
}
