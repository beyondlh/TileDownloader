package cz.gisat.tiledownloader.storage;

import cz.gisat.tiledownloader.TileGetter;
import cz.gisat.tiledownloader.objects.Tile;
import cz.gisat.tiledownloader.sqlite.DbConnector;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TileDbStorage {
    private TileGetter tileGetter;
    private DbConnector storageDbConnector;

    public TileDbStorage( TileGetter tileGetter, DbConnector storageDbConnector ) {
        this.tileGetter = tileGetter;
        this.storageDbConnector = storageDbConnector;
    }

    public Tile getFullTile( Tile tile ) {
        byte[] blob = this.getTileBlobFromStorage( tile );
        tile = new Tile( tile.getX(), tile.getY(), tile.getZoom(), blob );
        return tile;
    }

    private byte[] getTileBlobFromStorage( Tile tile ) {
        byte[] blob = null;
        try {
            ResultSet resultSet = this.storageDbConnector.executeSqlQry( "SELECT tile_data FROM tiles WHERE zoom_level=" + tile.getZoom() + " AND tile_column=" + tile.getX() + " AND tile_row=" + tile.getY() + ";" );
            if ( resultSet != null && resultSet.next() ) {
                blob = IOUtils.toByteArray( resultSet.getBlob( "tile_data" ).getBinaryStream() );
            } else {
                blob = this.getTileBlobFromRemoteStorage( tile );
            }
        } catch ( SQLException | IOException e ) {
            e.printStackTrace();
        }
        return blob;
    }

    private byte[] getTileBlobFromRemoteStorage( Tile tile ) {
        byte[] blob = null;
        String tileUrl = tileGetter.getTileUrl( tile );
        try {
            URL url = new URL( tileUrl );
            InputStream inputStream = url.openStream();
            blob = IOUtils.toByteArray( inputStream );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return blob;
    }
}
