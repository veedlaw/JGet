import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.jsoup.nodes.Document;

public class Downloader
{
    private static String baseURL; // The url that is initially passed by the user
    private static String rootDir; // Directory in which files are downloaded to

    private static final Queue<String> discoveredURLs = new ArrayDeque<>();
    private static Set<String> visitedURLs = new HashSet<>();

    private static int maxDepth = -1;
    private static final long maxFileSize = 0;

    /**
     * Runs the main downloading loop. Dequeues addresses from the URL queue and downloads them until there are no more addresses to download.
     * There are no more addresses to download once the queue is empty.
     * All URLs that are passed to this method have been checked to be valid URLs with HTTP(s) protocol.
     * @param url           An URL address to download and on which the search for additional links will be expanded on.
     * @param dir Specifies in which directory the files will be saved.
     */
    private static void runDownload(String url, String dir)
    {
        DownloaderUtilities.setBaseURL(url);
        baseURL = url;
        rootDir = dir;

        discoveredURLs.add(url);
        String address;
        while (! discoveredURLs.isEmpty())
        {
            address = discoveredURLs.remove();
            if (! visitedURLs.contains(address))
                download(address);
        }
    }

    private static void download(String address)
    {
        visitedURLs.add(address);
        if (DownloaderUtilities.isHTML(address))
        {
            Document htmlDocument = DownloaderUtilities.fetchDocument(address);
            if (htmlDocument != null)
            {
                DownloaderUtilities.discoverURLs(htmlDocument);
                downloadHTML(htmlDocument, address);
            }
            else
            {
                //downloadNonHTML(address)
            }
        }
        else
        {
            //downloadNonHTML(address);
        }

    }

    /**
     * Saves the JSoup object htmlDocument to disk.
     * Additional processing may be necessary when dealing with tricky urls.
     * @param htmlDocument A JSoup Document object which we will be saving to the disk
     */
    private static void downloadHTML(Document htmlDocument, String address)
    {
        System.out.println("downloading: " + address);
        // Save document to disk
        // The folder we are saving to is the name of the url
        String fileName = DownloaderUtilities.getFileName(address);

        try
        {
            String p = null;
            p = DownloaderUtilities.getURLWithoutSchema(address);
            String path = null;

            System.out.println("WITHOUT SCHEMA: " + p);
            if (p == null)
            {
                System.out.println("p == null --> " + DownloaderUtilities.changeMap.get(address));
                p = DownloaderUtilities.getURLWithoutSchema(DownloaderUtilities.changeMap.get(address));
            }

                path = (p.substring(0, p.lastIndexOf('/')));
            System.out.println("path var = " + path);
            //System.out.println(Paths.get(rootDir, path, fileName).toString() + " created directory");
            Files.createDirectories(Paths.get(rootDir, path));
            Files.writeString(Paths.get(rootDir,path, fileName), htmlDocument.html(), htmlDocument.charset());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }




    private static void downloadNonHTML(String address)
    {
        System.out.println("DUMMY downloading address: " + address);
    }


    public static void main(String[] args)
    {
        //create and show GUI todo later swingworker and all shebang
        // validate url and do checking whether it has a scheme and such
        String url = "https://kam.mff.cuni.cz/~fiala/";
        if (! (url.startsWith("http://") || url.startsWith("https://")))
        {
            System.out.println("Please prefix the url with scheme \"http://\" and try again");
            return;
        }
        //runDownload(url, "/Users/kasutaja/Desktop/jsoup/");
    runDownload("https://d3s.mff.cuni.cz/teaching/nprg013/", "/Users/kasutaja/Desktop/jsoup");
    }

    /**
     * Allows DownloaderUtilities class to query the contents of visitedURLs collection.
     * @param address String for which we wish to check the existence of in visitedURLs set.
     * @return True if "address" is present in visitedURLs.
     */
    public static boolean visited(String address)
    {
        return visitedURLs.contains(address);
    }

    /**
     * Allows to enqueue an address from the DownloaderUtilities class.
     * @param address An URL address which we wish to enqueue.
     */
    public static void enqueueURL(String address)
    {
        discoveredURLs.add(address);
    }
}
