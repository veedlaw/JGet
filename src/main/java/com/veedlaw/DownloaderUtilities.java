package com.veedlaw;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

public class DownloaderUtilities
{
    public static void setBaseURL(String baseURL)
    {
        DownloaderUtilities.baseURL = baseURL;
    }

    private static String baseURL = null;

    public static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String INDEX_HTML = "index.html";
    private static final String CONTENT_TYPE_HTML = "text/html";

    private static final HashSet<String> htmlAddresses = new HashSet<>();

    // Used as a mapping between URLs and local URLs (preserving directory structure)
    public static final HashMap<String, String> renameMap = new HashMap<>();

    /**
     * Takes an address string and returns that string with the HTTP(s) scheme removed.
     * This method is only called in the context that address is already prefixed with HTTP(s) scheme.
     * Example: getURLWithoutSchema("https://mff.cuni.cz") returns "mff.cuni.cz"
     * @param address   An URL address with HTTP(s) scheme.
     * @return  An URL address without the HTTP(s) scheme.
     */
    public static String getURLWithoutSchema(String address)
    {
        if (address.startsWith(HTTP))
        {
            return address.substring(HTTP.length());
        }
        else if (address.startsWith(HTTPS))
        {
            return address.substring(HTTPS.length());
        }

        return null;
    }

    /**
     * Checks whether the passed address is prefixed with either "http://" or "https://".
     * @param address   Address string
     * @return True if the address string is prefixed with either "http://" or "https://", false otherwise.
     */
    public static boolean hasHTTPsProtocol(String address)
    {
        return address.startsWith(HTTP) || address.startsWith(HTTPS);
    }

    /**
     * A helper method to determine whether the content-type of a webpage is "html/text".
     * Opens a URLConnection to a URL from which it gets the URL's content-type header
     * and compares it to the string "html/text", which specifies that the the kind of document
     * is of HyperText Markup Language.
     * @param address   An URL address string to which to connect to.
     * @return          True if the content-type of the webpage is "html/text", false otherwise.
     */
    public static boolean isHTML(String address)
    {
        if (htmlAddresses.contains(address)) // Checking within cached addresses
        {
            return true;
        }

        String receivedContentType;
        try
        {
            String contentTypeResponse = ((new URL(address)).openConnection()).getContentType();
            if (contentTypeResponse == null || contentTypeResponse.length() < CONTENT_TYPE_HTML.length())
            {
                return false;
            }

            receivedContentType = contentTypeResponse.substring(0, CONTENT_TYPE_HTML.length());
        }
        catch (IOException e)
        {
            return false;
        }
        boolean isHtml = CONTENT_TYPE_HTML.equals(receivedContentType);

        // Cache the true responses.
        if (isHtml)
        {
            htmlAddresses.add(address);
        }

        return isHtml;
    }

    /**
     * Fetches an HTML document from an URL using the JSoup library.
     * An IOException may be thrown if something goes wrong while connecting to the URL address.
     * If an IOException occurs, null is returned to signal inability to fetch the document.
     * @param address    An URL address from which to fetch the HTML document from.
     * @return           A JSoup document from the located at address URL; or null.
     */
    public static Document fetchDocument(String address)
    {
        try
        {
            return Jsoup.connect(address).get();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Checks the eligibility of adding some address string to the download queue.
     * @param address   URL address which we wish to check.
     * @return          True if the address may be added to the download queue.
     */
    private static boolean mayBeVisited(String address)
    {
        return isOnSameDomain(baseURL, address) && !Downloader.visited(address);
    }

    /**
     * Tests whether the address string is prefixed by baseURL.
     * @param baseURL   The URL address string which the user initially provided to be downloaded.
     * @param address   An URL address string which we wish to check.
     * @return          Returns true if address and baseURL are on the same domain.
     */
    public static boolean isOnSameDomain(String baseURL, String address)
    {
        if (address.length() < baseURL.length()) // accounts for differences such as "www.example.com/" and "www.example.com"
        {
            if ((address.length() - baseURL.length()) == -1) // -1 accounts for optional '/' suffix
            {
                return address.startsWith(baseURL.substring(0, baseURL.length() - 1));
            }
            return false;
        }
        return address.startsWith(baseURL);
    }

    /**
     *  The method is passed a JSoup Document, a JSoup Elements object, which contains Element objects in which we search
     *  for links, and an attribute key which specifies which part of the HTML element holds a URL address. Implicitly
     *  populates the queue with new addresses that are extracted from the elements in the process via a helper method.
     * @param htmlDocument  A JSoup document in which links are searched for.
     */
    public static void discoverURLs(Document htmlDocument)
    {
        // Elements that contain outgoing links that we are looking to follow:
        Elements[] outgoingElements = new Elements[] {
                htmlDocument.select("a[href]"),
                htmlDocument.select("img"),
                htmlDocument.select("link[href]")
        };

        for (Elements elements : outgoingElements)
        {
            String type = null;
            if (!elements.isEmpty())
            {
                type = elements.get(0).tag().toString();
            }
            // Comparison checks whether the destination is an image or a normal link and sets the attributeKey accordingly.
            discoverLinksFromHTMLElements(elements, "img".equals(type) ? "abs:src" : "abs:href");
        }
    }

    /**
     * A helper method for discoverURLs.
     * The method is passed a JSoup document, a JSoup Elements object, which contains Element objects in which we search
     * for links, and an attribute key which specifies which part of the html element holds an URL address. Populates the
     * queue with new addresses that are extracted.
     * @param elements      A list of HTML Elements in which we looks for links.
     * @param attrKey       Specifies attributeKey for extracting link from an element.
     */
    private static void discoverLinksFromHTMLElements(Elements elements, String attrKey)
    {
        String address; // Will be reused and assigned addresses which are discovered when going through the elements.
        for (Element element : elements)
        {
            // An URL address is found some attribute of the html element. An appropriate attribute is selected via attrKey.
            address = element.attr(attrKey);
            System.out.println("\t extracted address: " + address + " href=" + element.attr("href"));
            if (renameMap.containsKey(address))
            {
                localizeLink(element, address, attrKey); // changes the element in htmlDocument
            }

            if (mayBeVisited(address))
            {
                boolean isHashHref = element.attr("href").startsWith("#");
                // "hash href-s" are local href-s within the same page - they navigate to different HTML elements and
                // as such, they are not valid links to download.
                if (! isHashHref)
                {
                    Downloader.enqueueURL(address);
                    localizeLink(element, address, attrKey);

                }
            }
        }
    }

    /**
     * Is only called in the context that the passed address string is known to be of content-type "text/html"
     * Checks whether address has HTML file type describing suffix.
     * @param address   The address string which is checked
     * @return          True if address string ends with HTML file type describing suffix
     */
    private static boolean hasAmbiguousSuffix(String address)
    {
        return !address.endsWith(".html") && !address.endsWith(".htm") && !address.endsWith(".xhtml");
    }

    /**
     * Makes the links point relative to our local folder structure, making web pages browsable locally.
     * @param element   JSoup HTML element which has to be modified
     * @param address   The address where the HTML file is located, which element's we are modifying
     * @param attrKey   HTML element attribute key
     */
    private static void localizeLink(Element element, String address, String attrKey) {
        attrKey = attrKey.substring("abs:".length());

        String currentHref = element.attr(attrKey);
        //System.out.println("Current HREF is " + currentHref + " on address " + address);

        if (currentHref.startsWith("/")) // means that the href is relative to the root of the URL
        {
            //System.out.println("Absolute URL href: " + currentHref);
            element.attr(attrKey, currentHref.substring(1)); // exclude the slash for localization purposes
            currentHref = element.attr(attrKey);
        }

        if (currentHref.startsWith(baseURL)) // means that our href is absolute, we must localize it.
        {
            String newHref = currentHref.substring(baseURL.length());
            element.attr(attrKey, newHref);
        }

        if (!hasAmbiguousSuffix(currentHref))
        {
            return;
        }

        if (isHTML(address))
        {
            if (isDirectory(address))
            {
                if (!address.endsWith("/"))
                {
                    element.attr("href", currentHref + "/" + INDEX_HTML);
                    renameMap.put(address, address + "/" + INDEX_HTML);
                }
                else
                {
                    element.attr("href", currentHref + INDEX_HTML);
                    renameMap.put(address, address + INDEX_HTML);
                }
            }
            else if (getURLWithoutSchema(currentHref) == null)
            {
                // Already assumed that the address has an ambiguous suffix, so we are safe to append .html to it
                element.attr("href", currentHref + ".html");
                renameMap.put(address, address + ".html");
            }
        }
    }

    /**
     * Takes an URL address string and tests whether the address string is a directory that also happens to serve
     * HTML content.
     * @param address   URL address string
     * @return          True if the address string is a directory.
     */
    private static boolean isDirectory(String address)
    {
        // This is not great, but I could not figure out any other way to fix this ... probably spent around 16hrs thinking...

        if (address.endsWith("/"))
        {
            return true;
        }
        URL url;
        try
        {
            url = new URL(address + "/");
        }
        catch (MalformedURLException e)
        {
            return false;
        }

        try
        {
            url.getContent();
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    /**
     * Derives the appropriate file name for storing a file that originates from the address string.
     * All derivation is only based on the address string.
     * @param address URL address from which the file we are saving to disk originated from.
     * @return        Appropriate file name for storing the file on disk.
     */
    public static String getFileName(String address)
    {
        if (renameMap.containsKey(address))
        {
            address = renameMap.get(address);
        }
        int lastSlashIndex = address.lastIndexOf('/');
        if (lastSlashIndex == 6 || lastSlashIndex == 7 ) // means that the slash we discovered is in the http(s) part of the url
        {
            // We can "append an imaginary" slash at the end of the url
            return INDEX_HTML;
        }
        if (lastSlashIndex == address.length() - 1)
        {
            return INDEX_HTML;
        }
        return address.substring(lastSlashIndex);
    }

    /**
     * Derives the appropriate local path of storing a file that originates from the address string.
     * All derivation is based on the address string.
     * @param address   URL address from which we derive local path.
     * @return          String path relative to the root of our download directory.
     */
    public static String getPath(String address)
    {
        if (renameMap.containsKey(address))
        {
            address = renameMap.get(address);
        }

        int lastSlashIndex = address.lastIndexOf('/');

        if (lastSlashIndex == 6 || lastSlashIndex == 7 ) // means that the slash we discovered is in the http(s) part of the url
        {
            lastSlashIndex = address.length();  // We can "append an imaginary" slash at the end of the url
        }

        String stringPath = getURLWithoutSchema(address.substring(0, lastSlashIndex));
        if (stringPath == null)
        {
            return null;
        }

        // I forgot about Windows... good thing I remembered
        String path;
        if (!File.separator.equals("/"))
        {
            path = stringPath.replaceAll("/", File.separator);
        }
        else
        {
            path = stringPath;
        }

        return path;
    }

    /**
     * Checks whether we can connect to an address to decide whether it is reasonable to initiate downloading.
     * https://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-an-http-url-for-availability
     * @param address   Address of which we check the availability of.
     * @return          True if it is possible to connect to the webpage.
     */
    public static boolean canInitiateDownload(String address)
    {
        if (! DownloaderUtilities.hasHTTPsProtocol(address))
        {
            address = HTTP + address;
        }
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        }
        catch (IOException exception)
        {
            return false;
        }
    }

}
