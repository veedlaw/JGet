import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class DownloaderUtilities
{
    public static void setBaseURL(String baseURL)
    {
        DownloaderUtilities.baseURL = baseURL;
    }

    private static String baseURL = null;
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String CONTENT_TYPE_HTML = "text/html";

    public static final HashMap<String, String> changeMap = new HashMap<>();

    /**
     * Takes an address string and returns that string with the HTTP(s) scheme removed.
     * This method is only called in the context that address is already prefixed with HTTP(s) scheme.
     * Example: getURLWithoutSchema("https://mff.cuni.cz") returns "mff.cuni.cz"
     * @param address   An URL address with HTTP(s) scheme.
     * @return  An URL address without the HTTP(s) scheme.
     */
    public static String getURLWithoutSchema(String address)
    {
        if (changeMap.containsKey(address))
        {
            //System.out.print("changing mapping: ->" + address);
            address = changeMap.get(address);
            //System.out.println("--> " + address);
        }

        if (address.startsWith(HTTP))
        {
            return address.substring(HTTP.length());
        }
        else if (address.startsWith(HTTPS))
        {
            return address.substring(HTTPS.length());
        }
        else
        {
            System.out.println("What the hell happened? FIX THIS.");
        }
        return null;
    }

    /**
     * A helper method to determine whether the content-type of a webpage is "html/text".
     * Opens a URLConnection to a URL from which it gets the url's content-type header
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
     * @param address An URL address from which to fetch the HTML document from.
     * @return A JSoup document from the located at address URL; or null.
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
     * The main goal of this method is to check the eligibility of adding some link to the download queue.
     * Essentially checks two conditions:
     * @param address URL address which we wish to check.
     * @return  True if the address may be added to the download queue.
     */
    private static boolean mayBeVisited(String address)
    {
        return isOnSameDomain(baseURL, address) && !Downloader.visited(address);
    }

    /**
     * Tests whether address is prefixed by baseURL.
     * @param baseURL The URL address string which the user initially provided to be downloaded.
     * @param address An URL address string which we wish to check.
     * @return  Returns true if address and baseURL are on the same domain.
     */
    public static boolean isOnSameDomain(String baseURL, String address)
    {
        //TODO figure out what I was doing here
        // If the url is shorter than the baseurl then it surely does have it as prefix
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
     *  The method is passed a JSoup document, a JSoup elements object, which contains Element objects in which we search
     *  for links, and an attribute key which specifies which part of the html element holds a URL address. Implicitly
     *  populates the queue with new addresses that are extracted from the elements in the process via a helper method.
     * @param htmlDocument A JSoup document from which the method tries to find links from.
     */
    public static void discoverURLs(Document htmlDocument)
    {
        // Elements that contain outgoing links that we are looking to follow
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
     * The method is passed a JSoup document, a JSoup elements object, which contains Element objects in which we search
     *  for links, and an attribute key which specifies which part of the html element holds an URL address. Populates the
     *  queue with new addresses that are extracted .
     * @param htmlDocument A JSoup document from which the method tries to find links from.
     * @param elements  A list of HTML Elements in which we looks for links.
     * @param attrKey   Different Elements have different attributeKeys for extracting links. Shall be specified via argument.
     */
    private static void discoverLinksFromHTMLElements(Document htmlDocument, Elements elements, String attrKey)
    {
        String address; // Will be reused and assigned addresses which are discovered when going through the elements.
        for (Element element : elements)
        {
            // An URL address is found some attribute of the html element. An appropriate attribute is selected via attrKey.
            address = element.attr(attrKey);
            if (changeMap.containsKey(address)) //TODO please don't forget to double check
            {
                fixLink(element, address); // changes the element in htmlDocument
            }

            if (mayBeVisited(address))
            {
                Downloader.enqueueURL(address);


                if (isHTML(address) && hasAmbiguousSuffix(address))
                {
                    System.out.println("fixing: " + address);
                    fixLink(element, address);
                }
            }
        }
    }

    /**
     * A method handling a tricky edge case is lies here:
     * Suppose we have some webpage "http://www.example.com/mypage" that serves html content.
     * If this webpage were to have the following structure: "http://www.example.com/mypage/pictures/picture1.png",
     * then we would first download and create a file "mypage", however this will leave us unable to later
     * create a directory "mypage/pictures/" as "mypage" is already a file.
     * To preserve website consistency, whenever such situation is detected, we modify the the underlying html
     * such that in our local copy, all links to "mypage" are directed to "mypage/index.html". The file is then
     * saved in the directory "mypage" with the filename "index.html" This allows us to preserve website
     * hierarchy when downloading a website.
     * @param address
     *
     * ALREADY ASSUMED THAT ADDRESS HAS CONTENT_TYPE: TEXT/HTML
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
    private static void fixLink(Element element, String address)
    {
        String currentHref = element.attr("href");
        System.out.println("Current HREF is " + currentHref);

        // read as: if (isRelativeUrl(url))
        if (getURLWithoutSchema(currentHref) == null) // means no http protocol prefix -> relative link
        {



            if (baseURL.endsWith("/"))
            {
                System.out.println("making an absolute url from a relative url --> " + baseURL + "/" + currentHref);
                //changeMap.put(element.attr("href"), baseURL + currentHref);
                //System.out.println("changemap entry added for: " + element.attr("href") + " --> " + baseURL+currentHref);
            }
            else
            {
                //System.out.println("making an absolute url from a relative url --> " + baseURL + currentHref);
                //changeMap.put(element.attr("href"), baseURL + currentHref);
                //changeMap.put(baseURL + currentHref, element.attr("href"));
                changeMap.put(element.attr("href"), baseURL + currentHref);
                System.out.println("changemap entry added for: " + element.attr("href") + " --> " + baseURL+currentHref);
            }
        }
        else {
            // 1) The URL in our current document must be changed.
            System.out.print("changing link element: " + element.attr("href"));
            element.attr("href", element.attr("href") + "/index.html");
            System.out.println(" to -> " + element.attr("href"));
//            changeMap.put(address, element.attr("href"));
            changeMap.put(element.attr("href"), baseURL + currentHref);

            System.out.println("changemap entry added for: " + address + " --> " + element.attr("href"));
        }
    }

    /**
     * This method is passed a URL address and from it it derives the appropriate file name for storing that URL
     * @param address
     * @return
     */
    public static String getFileName(String address)
    {
        String fileName = null;
        int lastSlashIndex = address.lastIndexOf('/');
        if (changeMap.containsKey(address))
        {
            System.out.print("(getFileName) changing address from: " + address);
            address = changeMap.get(address);
            System.out.println(" to --> " + address);
        }
        /*else if (changeMap.containsKey(address.substring(lastSlashIndex + 1)))
        {
            System.out.print("(getFileName) changing address from: " + address);
            address = changeMap.get(address.substring(lastSlashIndex + 1));
            System.out.println(" to --> " + address);
        }*/
        else
        {
            System.out.println(address.substring(lastSlashIndex));
            System.out.println("NO change for " + address);
        }


        if (lastSlashIndex == -1) return "ERROR " + address; // TODO DEBUG

        if (lastSlashIndex == address.length() - 1)
        {
            return "index.html";
        }
        System.out.println(address + "-->"  + address.substring(lastSlashIndex));
        return address.substring(lastSlashIndex);
    }
}
