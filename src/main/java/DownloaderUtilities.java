import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class DownloaderUtilities
{
    public static void setBaseURL(String baseURL)
    {
        DownloaderUtilities.baseURL = baseURL;
    }

    private static String urlRootDir = null;

    private static String baseURL = null;
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String CONTENT_TYPE_HTML = "text/html";

    public static final HashMap<String, String> changeMap = new HashMap<>();
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
     * A helper method to determine whether the content-type of a webpage is "html/text".
     * Opens a URLConnection to a URL from which it gets the URL's content-type header
     * and compares it to the string "html/text", which specifies that the the kind of document
     * is of HyperText Markup Language.
     * @param address   An URL address string to which to connect to.
     * @return          True if the content-type of the webpage is "html/text", false otherwise.
     */
    public static boolean isHTML(String address)
    {
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

        return CONTENT_TYPE_HTML.equals(receivedContentType);
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
            System.out.println("(fetchDocument) IO exception: " + address);
            return null;
        }
    }

    /**
     * Checks the eligibility of adding some link to the download queue.
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
        //TODO figure out what I was doing here
        // If the url is shorter than the baseurl then it surely does not have it as prefix
        if (address.length() < baseURL.length())
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
     *  for links, and an attribute key which specifies which part of the html element holds a URL address. Implicitly
     *  populates the queue with new addresses that are extracted from the elements in the process via a helper method.
     * @param htmlDocument  A JSoup document in which links are searched for.
     */
    public static void discoverURLs(Document htmlDocument)
    {
        System.out.println("Starting link discovery from HTML document ... ");
        // Elements that contain outgoing links that we are looking to follow:
        Elements[] outgoingElements = new Elements[] {
                htmlDocument.select("a[href]"),
                htmlDocument.select("[src]"),
                htmlDocument.select("link[href]")
        };

        for (Elements elements : outgoingElements)
        {
            String type = null;
            if (!elements.isEmpty())
                type = elements.get(0).tag().toString();
            // Comparison checks whether the destination is an image or a normal link and sets the attributeKey accordingly.
            discoverLinksFromHTMLElements(htmlDocument, elements, "img".equals(type) ? "abs:src" : "abs:href");
        }
    }

    /**
     * A helper method for discoverURLs.
     * The method is passed a JSoup document, a JSoup Elements object, which contains Element objects in which we search
     *  for links, and an attribute key which specifies which part of the html element holds an URL address. Populates the
     *  queue with new addresses that are extracted.
     * @param htmlDocument  A JSoup document from which the method tries to find links from.
     * @param elements      A list of HTML Elements in which we looks for links.
     * @param attrKey       Specifies attributeKey for extracting link from an element.
     */
    private static void discoverLinksFromHTMLElements(Document htmlDocument, Elements elements, String attrKey)
    {
        String address; // Will be reused and assigned addresses which are discovered when going through the elements.
        for (Element element : elements)
        {
            // An URL address is found some attribute of the html element. An appropriate attribute is selected via attrKey.
            address = element.attr(attrKey);
            //System.out.println("\t extracted address: " + address);
            if (changeMap.containsKey(address)) //TODO please don't forget to double check
            {
                fixLink(element, address); // changes the element in htmlDocument
            }

            if (mayBeVisited(address))
            {
                Downloader.enqueueURL(address);

                // An annoying edge case is addressed here:
                // Suppose we have some webpage "http://www.example.com/mypage" that serves html content.
                // If this webpage were to have the following structure: "http://www.example.com/mypage/pictures/picture1.png",
                // then we would first download and create a file "mypage", however this will leave us unable to later
                // create a directory "mypage/pictures/" as "mypage" is already a file.
                // To preserve website consistency, whenever such situation is detected, we modify the the underlying HTML
                // such that in our local copy, all links to "mypage" are directed to "mypage/index.html". The file is then
                // saved in the directory "mypage" with the filename "index.html" This allows us to preserve website
                // hierarchy locally when downloading a website.
                if (isHTML(address) && hasAmbiguousSuffix(address))
                {
                    System.out.println("Entering fixLinks for: " + address);
                    fixLink(element, address);
                }
            }
        }
    }

    /**
     *
     * @param address
     * @return
     */
    private static boolean hasAmbiguousSuffix(String address)
    {
        if (address.endsWith(".html") || address.endsWith(".htm") || address.endsWith(".xhtml"))
        {
            return false;
        }
        return true;
    }

    /**
     * Key challenges to address:
     * 1) The url in the current document must be redirected
     * 2) The url in future documents must be redirected --> each document we store to disk must be checked for urls?
     * 3) These changes must be propagated to directory structure
     * @param address
     */
    private static void fixLink(Element element, String address) {
        String currentHref = element.attr("href");
        System.out.println("Current HREF is " + currentHref + " on address " + address);

        // Check if appending "/" to address still gives us a valid link
        // If so, then this address is a directory that also serves HTML content and
        // The HTML content will get saved inside that directory as "index.html"
        // Otherwise: append .html to link

        if (isDirectory(address)) {
            System.out.println("The following address is deemed a directory: " + address);
            System.out.println("\t The href is: " + currentHref);
            if (!address.endsWith("/"))
            {
                element.attr("href", currentHref + "/index.html");
                renameMap.put(address, address + "/index.html");
                changeMap.put(address, address + "/index.html");
            }
            else
            {
                element.attr("href", currentHref + "index.html");
                renameMap.put(address, address + "index.html");
                changeMap.put(address, address + ".index.html");
            }
            System.out.println("\t The href has been changed to: " + element.attr("href"));
            System.out.println("\t The address has been renamed from: " + address  + " to -> (see below)");
            System.out.println("\t\t\t\t " + renameMap.get(address));
        }
        else if (getURLWithoutSchema(currentHref) == null) // means no http protocol prefix, so we can infer relative link
        {
            // Already assumed that the address has an ambiguous suffix, so we are safe to append .html to it
            element.attr("href", currentHref + ".html");
            renameMap.put(address, address + ".html");
            changeMap.put(address, address + ".html");
        }
        else
        {
            System.out.println("(fixLinks) GOT INTO THE ELSE BRANCH WITH ADDRESS: " + address);
            // Some weird link with absolute address, the difference with the above cases is converting the relative url to absolute url
        }
    }

    // This is not great, but I could not figure out any other way to fix this ... probably spent around 16hrs thinking...
    private static boolean isDirectory(String address)
    {
        if (address.endsWith("/"))
        {
            return true;
        }
        URL url = null;
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

    //TODO PROBABLY DELETE
    private static String relativeURLToAbsolute(String relativeURL, String currentURL)
    {
        String absoluteURL = null;
        if (urlRootDir == null)
        {
            try
            {
                URL url = new URL(baseURL);
                urlRootDir = HTTP + url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        // We should consider two cases
        // 1. The address is relative to the root of the site
        // 2. The address is relative to the current path
        if (relativeURL.startsWith("/")) // case 1.
        {
            // Get the root directory of the website and append the relative URL
            if (urlRootDir.endsWith("/"))
            {
                System.out.println("(relativeURLToAbsolute) Returning abs: " + urlRootDir.substring(0, urlRootDir.length()-2) + relativeURL);
                return urlRootDir.substring(0, urlRootDir.length()-2) + relativeURL;
            }
            else
            {
                System.out.println("(relativeURLToAbsolute) Returning abs: " + urlRootDir + relativeURL);
                return urlRootDir + relativeURL;
            }
        }
        else // case 2.
        {
        }
        return absoluteURL;
    }

    /**
     * This method is passed a URL address and from it it derives the appropriate file name for storing that URL
     * @param address URL address from which the file we are saving to disk originated from.
     * @return        Appropriate file name for storing the file on disk.
     */
    public static String getFileName(String address)
    {
        // What are we missing?
        // A mapping from absolute addresses of urls we modified to the changed url's

        String fileName = null;
        if (renameMap.containsKey(address))
        {
            System.out.print("(getFileName) changing address from: " + address);
            address = renameMap.get(address);
            System.out.println(" to --> " + address);
        }
        else
        {
            //System.out.println(address.substring(lastSlashIndex));
            System.out.println("NO change for " + address);
        }

        int lastSlashIndex = address.lastIndexOf('/');

        if (lastSlashIndex == -1) return "ERROR " + address; // TODO DEBUG

        if (lastSlashIndex == address.length() - 1)
        {
            return "index.html";
        }
        System.out.println(address + "-->"  + address.substring(lastSlashIndex));
        return address.substring(lastSlashIndex);
    }

    public static String getPath(String address)
    {
        String path = null;

        if (renameMap.containsKey(address))
        {
            address = renameMap.get(address);
        }

        int lastSlashIndex = address.lastIndexOf('/');
        System.out.println(address.substring(0, lastSlashIndex));
        path = address.substring(0, lastSlashIndex);
        return getURLWithoutSchema(path);
    }
}
