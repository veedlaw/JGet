import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.jsoup.nodes.Document;

import javax.swing.*;

public class Downloader
{
    private static String baseURL; // The url that is initially passed by the user
    private static String rootDir; // Directory to which files are downloaded to

    private static final Queue<String> discoveredURLs = new ArrayDeque<>(); // URLs which are yet to be downloaded
    private static final Set<String> visitedURLs = new HashSet<>();

    private static int numFilesDownloaded = 0;
    private static String currentDownload = ""; // Is accessed via a getter method by the GUI for information displaying purposes.

    /**
     * Runs the main downloading loop. Dequeues addresses from the URL queue and downloads them until there are no more addresses to download.
     * There are no more addresses to download once the queue is empty.
     * All URLs that are passed to this method have been checked to be valid URLs with HTTP(s) protocol.
     * @param url   An URL address to download and on which the search for additional links will be expanded on.
     * @param dir   Specifies in which directory the files will be saved.
     */
    public static void runDownload(String url, String dir)
    {
        baseURL = url;
        rootDir = dir;
        DownloaderUtilities.setBaseURL(baseURL);
        DownloaderUtilities.setRootDir(rootDir);

        discoveredURLs.add(url);
        String address;
        while (! discoveredURLs.isEmpty())
        {
            address = discoveredURLs.remove();
            if (! visitedURLs.contains(address))
                download(address);
        }
    }

    /**
     * Downloads a single file from an address. The file is saved to disk.
     * If the file is an HTML document, it is then searched for links.
     * @param address   An URL address from which we wish to download from.
     */
    private static void download(String address)
    {
        System.out.println("DOWNLOADING: " + address);
        currentDownload = address;
        visitedURLs.add(address);
        if (DownloaderUtilities.isHTML(address))
        {
            Document htmlDocument = DownloaderUtilities.fetchDocument(address);
            if (htmlDocument != null)
            {
                DownloaderUtilities.discoverURLs(htmlDocument);
                downloadHTML(htmlDocument, address);
            }
            else // We were unable to fetch the document
            {
                // Since we were unable to fetch the HTML document, we will download the file manually
                System.out.println("downloading manually");
                downloadNonHTML(address);
            }
        }
        else
        {
            System.out.println("(Non-HTML) DOWNLOADING: " + address);
            downloadNonHTML(address);
        }
    }

    /**
     * Saves the JSoup object htmlDocument to disk.
     * Additional processing may be necessary when dealing with tricky urls.
     * @param htmlDocument A JSoup Document object which we will be saving to the disk.
     * @param address The URL address from which the HTML document originates, used for file name derivation purposes.
     */
    private static void downloadHTML(Document htmlDocument, String address)
    {
        System.out.println("downloading: " + address);
        // Save document to disk
        // The folder we are saving to is the name of the url
        String fileName = DownloaderUtilities.getFileName(address);
        String path = DownloaderUtilities.getPath(address);

        try
        {
            Files.createDirectories(Paths.get(rootDir, path));
            Files.writeString(Paths.get(rootDir, path, fileName), htmlDocument.html(), htmlDocument.charset());
            numFilesDownloaded++;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * Downloads a non-HTML file.
     * @param address Address string from where we wish to download.
     */
    private static void downloadNonHTML(String address)
    {
        HttpURLConnection connection = null; // self-note: not AutoCloseable
        try
        {
            connection = (HttpURLConnection) (new URL(address)).openConnection();
            /*if (connection.getContentLengthLong() > maxFileSize)
            {
                System.out.println("not downloading from " + address + " because it is over maxFileSize");
                return;
            }*/
        }
        catch (IOException e)
        {
            e.printStackTrace(); // TODO
            return;
        }

        try (InputStream urlConnectionInputStream = new BufferedInputStream(connection.getInputStream()))
        {
            // transfer file directly to disk
            String fileName = DownloaderUtilities.getFileName(address);
            String path = DownloaderUtilities.getPath(address);
            Files.createDirectories(Paths.get(rootDir, path));
            Files.copy(urlConnectionInputStream, Paths.get(rootDir, path, fileName));

            numFilesDownloaded++;
        }
        catch (IOException e)
        {
            System.out.println("(NonHTML) " + e); // TODO
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main entry point of the program.
     * Runs the GUI creation.
     * Downloading functionality is accessed via the GUI.
     * @param args command line args.
     */
    public static void main(String[] args)
    {
        // validate url and do checking whether it has a scheme and such
        String url = "https://kam.mff.cuni.cz/~fiala/";
        if (! (url.startsWith("http://") || url.startsWith("https://")))
        {
            System.out.println("Please prefix the url with scheme \"http://\" and try again");
            return;
        }
        //runDownload(url, "/Users/kasutaja/Desktop/jsoup/");
        //runDownload("http://www.koopiatehas.ee", "/Users/kasutaja/Desktop/jsoup");
        //runDownload("https://iuuk.mff.cuni.cz/~ipenev/", "/Users/kasutaja/Desktop/jsoup");

        SwingUtilities.invokeLater(View::createAndShowGUI);
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

    /**
     * Allows the View class to get the number of downloaded files.
     * @return Integer value of number of files saved to disk.
     */
    public static int getNumFilesDownloaded() {
        return numFilesDownloaded;
    }

    /**
     * Allows the View class to read the size of the current download queue.
     * @return Size of discoveredURLs queue.
     */
    public static int getNumFilesToBeDownloaded()
    {
        return discoveredURLs.size();
    }

    /**
     * Allows the View class to read which file is currently being downloaded.
     * @return Address string of currently downloaded page.
     */
    public static String getCurrentDownload() {
        return currentDownload;
    }
}
