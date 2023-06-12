package hr.pickBox.servis;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMDbScraper {
    public static class MovieData {
        private String title;
        private String rating;
        private String description;
        private String genres;
        private String duration;

        public MovieData(String title, String rating, String description, String genres, String duration) {
            this.title = title;
            this.rating = rating;
            this.description = description;
            this.genres = genres;
            this.duration = duration;
        }

        public String getTitle() {
            return title;
        }

        public String getRating() {
            return rating;
        }

        public String getDescription() {
            return description;
        }

        public String getGenres() {
            return genres;
        }

        public String getDuration() {
            return duration;
        }
    }

    public static MovieData findMovieData(String filmTitle) {
        String imdbUrl = "https://www.imdb.com/";
        String searchUrl = imdbUrl + "find?q=" + filmTitle.replace(" ", "+") + "&s=tt&exact=true&ref_=fn_tt_ex";

        try {
            Document doc = Jsoup.connect(searchUrl).get();

            // Pronalazi prvi rezultat pretrage
            Element result = doc.select(".findList tr").first();

            // Dohvata link ka stranici filma
            Element link = result.select("a").first();
            String movieUrl = imdbUrl + link.attr("href");

            // Učitava stranicu filma
            doc = Jsoup.connect(movieUrl).get();

            // Dohvata naslov filma
            String title = doc.select("h1").first().text();

            // Dohvata ocenu filma
            String rating = doc.select(".ratingValue strong span").first().text();

            // Dohvata opis filma
            String description = doc.select(".summary_text").first().text().trim();

            // Dohvata žanrove filma
            Elements genres = doc.select(".subtext a[href^=\"/search/title?genres\"]");
            StringBuilder genresBuilder = new StringBuilder();
            for (Element genre : genres) {
                genresBuilder.append(genre.text()).append(", ");
            }
            String genreString = genresBuilder.toString().trim();

            // Dohvata trajanje filma
            String duration = doc.select(".subtext time").first().text().trim();

            return new MovieData(title, rating, description, genreString, duration);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String findMovieWithTitle(String filmTitle) throws IOException {
        String imdbUrl = "https://www.imdb.com/";
        String searchUrl = imdbUrl + "find?q=" + filmTitle.replace(" ", "+") + "&s=tt&exact=true&ref_=fn_tt_ex";

        Document document = Jsoup.connect(searchUrl).get();
        Elements articles = document.select("div.ipc-metadata-list-summary-item__tc");


        String extractedString = null;
        for (Element article : articles) {
            Elements articleItems = article.select("a.ipc-metadata-list-summary-item__t");
            for (Element articleItem : articleItems) {

                Element aElement = articleItem.selectFirst("a");
                // Dobijanje vrijednosti atributa href
                String href = aElement.attr("href");

                // Izdvajanje stringa tt1375666
                String regex = "/title/(.*?)/";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(href);

                // Provjera je li regex izraz pronašao podudaranje
                if (matcher.find()) {
                    // Izdvajanje podudaranog dela stringa
                    extractedString = matcher.group(1);
                    return extractedString; // vraća odmah prvi na koji naiđe
                }
            }
        }
        return extractedString;
    }

    public static void main(String[] args) throws IOException {
        String filmTitle = "inception"; // Unesite naslov filma za koji želite pronaći podatke
        String movieData = findMovieWithTitle(filmTitle);
        System.out.println(movieData);

    }
}

