package hr.pickBox.servis;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class PickboxSqLiteScraper {
    private static final String URL = "https://www.pickbox.hr/tv/raspored/";
    private static final String DATABASE_NAME = "pickbox.db";
    private static Connection connection;

    public static void UpisiProgram(){
        try {
            connectToDatabase();
            connection.setAutoCommit(false); // Disable auto-commit mode
            dropTableIfExists("pickbox");
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
                    saveDataToDatabase(formattedDate, dayOfWeekText, category, time, originalTitle, localTitle);
                }
            }

            connection.commit(); // Commit the transaction
        } catch (IOException | SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback(); // Rollback the transaction in case of an exception
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true); // Enable auto-commit mode again
                    disconnectFromDatabase();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void connectToDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_NAME);
    }

    private static void disconnectFromDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private static void dropTableIfExists(String tableName) throws SQLException {
        Statement statement = connection.createStatement();
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, tableName);
            if (preparedStatement.executeQuery().next()) {
                statement.executeUpdate("delete from " + tableName);
                System.out.println("The table '" + tableName + "' has been dropped.");
            } else {
                System.out.println("The table '" + tableName + "' does not exist.");
            }
        }
    }

    private static void createTableIfNotExists() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS pickbox (" +
                "id INTEGER PRIMARY KEY, " +
                "datum TEXT, " +
                "dan TEXT, " +
                "vrsta TEXT, " +
                "vrijeme TEXT, " +
                "naziv TEXT, " +
                "prijevod TEXT" +
                ")";
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
    }

    private static void saveDataToDatabase(String datum, String dan, String vrsta, String vrijeme, String naziv, String prijevod) throws SQLException {
        String query = "INSERT INTO pickbox (datum, dan, vrsta, vrijeme, naziv, prijevod) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, datum);
            preparedStatement.setString(2, dan);
            preparedStatement.setString(3, vrsta);
            preparedStatement.setString(4, vrijeme);
            preparedStatement.setString(5, naziv);
            preparedStatement.setString(6, prijevod);
            preparedStatement.executeUpdate();
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

    public static void main(String[] args) {
        UpisiProgram();
    }
}

