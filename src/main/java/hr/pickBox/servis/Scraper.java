package hr.pickBox.servis;

/**
 * @author Vinícius Egidio (vegidio@gmail.com)
 * Feb 15th 2014
 * v1.3
 */


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper
{
    // Basic variables
    private String id;
    private String url;
    private boolean found;

    // Show information
    private String title, genre, description, director, cast, poster, recommended;
    private float rating = 0f;
    private short year = 0;
    private List<Map<String, String>> videos;

    /**
     * Constructor
     */
    public Scraper()
    {
        initialize();
    }

    /**
     * Initialize the constructor and search for a show with the id
     *
     * @param id String - IMDb show id
     */
    public Scraper(String id)
    {
        initialize();
        findById(id);
    }

    /**
     * Initialize all variables
     */
    private void initialize()
    {
        title = "";
        genre = "";
        description = "";
        director = "";
        cast = "";
        poster = "";
        recommended = "";
        rating = 0f;
        year = 0;
        videos = new ArrayList<>();
        found = false;
    }

    /**
     * Find a show information based on its Id
     *
     * @param id String
     * @return true if a show was found
     */
    public boolean findById(final String id)
    {
        this.id = id;
        this.url = "http://www.imdb.com/title/" + id;

        // Initialize the variables before we start
        initialize();

        ExecutorService executor = Executors.newFixedThreadPool(3);

        Thread t1 = new Thread(() -> {
            parseMain(id);
        });

        Thread t2 = new Thread(() -> {
            parseCredits(id);
        });

        Thread t3 = new Thread(() -> {
            parseVideos(id);
        });

        // Executando as threads
        executor.execute(t1);
        executor.execute(t2);
        executor.execute(t3);

        // While until all threads finish
        executor.shutdown();
        while(!executor.isTerminated());

        return found;
    }

    /**
     * Do a search using the default type ALL.
     *
     * @param query String - the name of the item you are looking for.
     * @return List with one or more Map objects; each Map has the keys "id" and "name" of the search results.
     */
    public List<Map<String, String>> search(String query)
    {
        return search(query, SearchType.ALL);
    }

    /**
     * Do a search.
     *
     * @param query String - the name of the item you are looking for.
     * @param searchType String - the type of search; options available in the SearchType class;
     * @return List with one or more Map objects; each Map has the keys "id" and "name" of the search results.
     */
    public List<Map<String, String>> search(String query, SearchType searchType)
    {
        // Regex
        final String SEARCH_ID_NAME = "\"result_text\"> <a href=\"/title/tt([0-9]*)/(.*?)\" >(.*?)</a>";

        // Variables
        Pattern pattern;
        Matcher matcher;
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map;

        // URL encode the query
        String search = null;
        try { search = URLEncoder.encode(query, "UTF-8"); }
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }

        // Do the search and get the HTML
        search = "http://www.imdb.com/find?q=" + search + "&s=" + searchType.getValue();
        String html = fetchHtml(search);

        // Extract the ID & Name
        pattern = Pattern.compile(SEARCH_ID_NAME);
        matcher = pattern.matcher(html);

        while(matcher.find())
        {
            map = new LinkedHashMap<>();
            map.put("id", "tt" + matcher.group(1));
            map.put("name", matcher.group(3));
            list.add(map);
        }

        return list;
    }

    /**
     * Save the poster locally
     *
     * @param posterFile File - the location where you want to save the poster file.
     */
    public void downloadPoster(File posterFile)
    {
        if(!this.poster.isEmpty())
        {
            String url = this.poster;
            downloadFile(posterFile, url);
        }
    }

    /**
     * Save the poster locally
     *
     * @param posterFile File - the location where you want to save the poster file.
     * @param bigVersion boolean - if true, it will download the big version of the poster.
     */
    public void downloadPoster(File posterFile, boolean bigVersion)
    {
        if(!this.poster.isEmpty())
        {
            String url = this.poster;
            if(bigVersion) url = url.replaceAll("\\._V1_(.*?)\\.jpg", "._V1_.jpg");
            downloadFile(posterFile, url);
        }
    }

    /**
     * Download the file
     */
    private void downloadFile(File posterFile, String url)
    {
        try
        {
            URL posterUrl = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(posterUrl.openStream());
            FileOutputStream fos = new FileOutputStream(posterFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getVideoUrl(String videoId)
    {
        String result = "";
        String url = "http://www.imdb.com/video/imdb/" + videoId + "/imdb/single?format=480p";
        String html = fetchHtml(url);

        if(!html.isEmpty())
        {
            final String VIDEO_URL = "\"url\":\"(.*?)\",";
            Pattern pattern = Pattern.compile(VIDEO_URL);
            Matcher matcher = pattern.matcher(html);
            if(matcher.find()) result = matcher.group(1);
        }

        return result;
    }

    /**
     * Retrive the HTML content of the page
     *
     * @param urlString
     * @return
     */
    private String fetchHtml(String urlString)
    {
        String line;
        StringBuffer html = new StringBuffer();

        try
        {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            // Fake the User-Agent
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/32.0.1700.107 Safari/537.36");

            // Check the HTTP response code
            if(conn.getResponseCode() == 200)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));

                // Reading the HTML
                while((line = in.readLine()) != null)
                    html.append(line.trim());

                in.close();
            }

            // Close the connection
            conn.disconnect();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        return html.toString();
    }

    /**
     * Parse the HTML from the main page
     */
    private void parseMain(String id)
    {
        String url = "http://www.imdb.com/title/" + id;
        String html = fetchHtml(url);

        if(!html.isEmpty())
        {
            // Regex
            final String IMDB_CAST      = "itemprop=\"actor\".*?<span class=\"itemprop\" itemprop=\"name\">(.*?)</span>";
            final String IMDB_GENRE     = "\"itemprop\" itemprop=\"genre\">(.*?)</span>";
            final String IMDB_DESC      = "itemprop=\"description\"><p>(.*?)\\s+<em";
            final String IMDB_POSTER    = "<div class=\"poster\">.*?<img.*?src=\"(.*?)\"itemprop=\"image\" />";
            final String IMDB_RATING    = "<span itemprop=\"ratingValue\">(.*?)</span>";
            final String IMDB_TITLE     = "property='og:title' content=\"(.*?)(\"| \\()";
            final String IMDB_YEAR      = "property='og:title' content=\".*?([0-9]{4}?).*?\\)\" />";
            final String IMDB_RECOMMEND = "<div class=\"rec_item\"(.*?)<a href=\"/title/(.*?)/\\?ref_=tt_rec_tti\"";

            // Variables
            Pattern pattern;
            Matcher matcher;

            // Get the cast
            pattern = Pattern.compile(IMDB_CAST);
            matcher = pattern.matcher(html);
            while(matcher.find()) cast = (cast.length() > 0)? cast + ", " + matcher.group(1): matcher.group(1);

            // Get the genre
            pattern = Pattern.compile(IMDB_GENRE);
            matcher = pattern.matcher(html);
            while(matcher.find()) genre = (genre.length() > 0)? genre + ", " + matcher.group(1): matcher.group(1);

            // Get the description
            pattern = Pattern.compile(IMDB_DESC);
            matcher = pattern.matcher(html);
            if(matcher.find()) description = matcher.group(1);

            // Get the poster
            pattern = Pattern.compile(IMDB_POSTER);
            matcher = pattern.matcher(html);
            if(matcher.find()) poster = matcher.group(1);

            // Get the rating
            pattern = Pattern.compile(IMDB_RATING);
            matcher = pattern.matcher(html);
            if(matcher.find()) rating = Float.parseFloat(matcher.group(1));

            // Get the recommended titles
            pattern = Pattern.compile(IMDB_RECOMMEND);
            matcher = pattern.matcher(html);
            while(matcher.find()) recommended = (recommended.length() > 0)? recommended + ", " + matcher.group(2):
                    matcher.group(2);

            // Get the title
            pattern = Pattern.compile(IMDB_TITLE);
            matcher = pattern.matcher(html);
            if(matcher.find()) title = matcher.group(1);

            // Get the year
            pattern = Pattern.compile(IMDB_YEAR);
            matcher = pattern.matcher(html);
            if(matcher.find()) year = Short.parseShort(matcher.group(1));

            found = true;
        }
    }

    /**
     * Parse the HTML from the credits page
     */
    private void parseCredits(String id)
    {
        String url = "http://www.imdb.com/title/" + id + "/fullcredits";
        String html = fetchHtml(url);

        // Regex
        if(!html.isEmpty())
        {
            final String IMDB_DIRECTOR = "Directed by(.*?)</tbody>";
            final String IMDB_NAME     = "<a href=(.*?)> (.*?)</a>";

            // Variables
            Pattern pattern;
            Matcher matcher;
            String tempHtml = "";

            // Get the diretor/name
            pattern = Pattern.compile(IMDB_DIRECTOR);
            matcher = pattern.matcher(html);
            if(matcher.find()) tempHtml = matcher.group(1);

            pattern = Pattern.compile(IMDB_NAME);
            matcher = pattern.matcher(tempHtml);
            while(matcher.find()) director = (director.length() > 0)? director + ", " + matcher.group(2):
                    matcher.group(2);
        }
    }

    private void parseVideos(String id)
    {
        List<Map<String, String>> list = new ArrayList<>();
        String url = "http://www.imdb.com/title/" + id + "/videogallery";
        String html = fetchHtml(url);

        // Regex
        if(!html.isEmpty())
        {
            final String IMDB_VID_ID   = "<h2><a href=\"/video/imdb/(.*?)\">";
            final String IMDB_VID_NAME = "<h2><a href=\"/video/imdb/(.*?)\">(.*?)</a>";

            // Variables
            Pattern pattern;
            Matcher matcher;

            // Get the video id
            pattern = Pattern.compile(IMDB_VID_ID);
            matcher = pattern.matcher(html);

            while(matcher.find())
            {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("id", matcher.group(1));
                list.add(map);
            }

            // Get the video name
            pattern = Pattern.compile(IMDB_VID_NAME);
            matcher = pattern.matcher(html);
            int i = 0;

            while(matcher.find())
            {
                Map<String, String> map = list.get(i);
                map.put("name", matcher.group(2));
                list.set(i++, map);
            }
        }

        videos = list;
    }

    /**
     * Returns true if we have found a show doing a search with findById
     *
     * @return
     */
    public boolean hasFound()
    {
        return found;
    }

    /**
     * @return the id
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @return the cast
     */
    public String getCast()
    {
        return cast;
    }

    /**
     * @return the director
     */
    public String getDirector()
    {
        return director;
    }

    /**
     * @return the genre
     */
    public String getGenre()
    {
        return genre;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return the rating
     */
    public float getRating()
    {
        return rating;
    }

    /**
     * @return the recommended
     */
    public String getRecommended()
    {
        return recommended;
    }

    /**
     * @return the title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @return the year
     */
    public short getYear()
    {
        return year;
    }

    public List<Map<String, String>> getVideos()
    {
        return videos;
    }

    public enum SearchType {
        ALL("all"), TITLE("tt"), TV_EPISODE("ep"), NAME("nm"), COMPANY("co"), KEYWORD("kw"), CHARACTER("ch"), QUOTE("ch");

        private final String value;

        SearchType(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
