package cz.gisat.tiledownloader;

public class Main {
    public static void main( String[] args ) {
        ArgsParser argsParser = new ArgsParser( args );
        Downloader downloader = new Downloader( argsParser );
        downloader.download();
    }
}
