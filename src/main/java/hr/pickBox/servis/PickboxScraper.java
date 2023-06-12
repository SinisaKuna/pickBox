package hr.pickBox.servis;


import hr.antikvarijat.model.Pickbox;
import hr.antikvarijat.service.PickboxService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class PickboxScraper {

    private final PickboxService pickboxService;
    private static final String URL = "https://www.pickbox.hr/tv/raspored/";

    public PickboxScraper(PickboxService pickboxService) {
        this.pickboxService = pickboxService;
    }


    public static void UpisiProgram(PickboxService pickboxService) throws IOException {

        Document document = Jsoup.connect(URL).get();
        LocalDate currentDate = LocalDate.now().minusDays(1);

        Elements articles = document.select("article");

        for (Element article : articles) {

            currentDate = currentDate.plusDays(1);
            String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            String dayOfWeekText = getDayOfWeekText(dayOfWeek);

            System.out.println(dayOfWeekText + " " + formattedDate);
            System.out.println("***********************************");

            Elements articleItems = article.select("div.epg-article-item-holder");

            for (Element articleItem : articleItems) {
                String time = articleItem.select("div.time-epg-holder span").text();
                String category = articleItem.select("div.epg-category-holder span").text();
                String localTitle = articleItem.select("div.titles-epg-holder p.epg-local-title").text();
                String originalTitle = articleItem.select("div.titles-epg-holder p.epg-original-title").text();

                System.out.println(time + " " + category + " " + localTitle + " " + originalTitle);
                Pickbox pickbox = new Pickbox();
                pickbox.setDatum(formattedDate);
                pickbox.setDan(dayOfWeekText);
                pickbox.setVrsta(category);
                pickbox.setVrijeme(time);
                pickbox.setNaziv(originalTitle);
                pickbox.setPrijevod(localTitle);
                pickboxService.savePickBox(pickbox);
            }
        }
    }

    private static String getDayOfWeekText(DayOfWeek dayOfWeek) {
        HashMap<DayOfWeek, String> dayOfWeekMap = new HashMap<>();
        dayOfWeekMap.put(DayOfWeek.SUNDAY, "nedjelja");
        dayOfWeekMap.put(DayOfWeek.MONDAY, "ponedjeljak");
        dayOfWeekMap.put(DayOfWeek.TUESDAY, "utorak");
        dayOfWeekMap.put(DayOfWeek.WEDNESDAY, "srijeda");
        dayOfWeekMap.put(DayOfWeek.THURSDAY, "četvrtak");
        dayOfWeekMap.put(DayOfWeek.FRIDAY, "petak");
        dayOfWeekMap.put(DayOfWeek.SATURDAY, "subota");
        return dayOfWeekMap.get(dayOfWeek);
    }
}

