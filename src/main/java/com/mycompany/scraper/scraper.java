/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.scraper;

import com.google.gson.JsonObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Giacomo
 */
public class scraper {

    private static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36";
    private static String baseUrl = "https://www.scorespro.com/soccer/italy/serie-a/";
    private static String baseFilePath = "c:\\tmp\\";
    private static DesiredCapabilities desiredCaps;
    private static WebDriver driver;

    public static void initPhantomJS() {
        desiredCaps = new DesiredCapabilities();
        desiredCaps.setJavascriptEnabled(true);
        desiredCaps.setCapability("takesScreenshot", false);
        desiredCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "C:\\Users\\Giacomo\\Desktop\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe");
        desiredCaps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "User-Agent", USER_AGENT);

        ArrayList<String> cliArgsCap = new ArrayList();
        cliArgsCap.add("--web-security=false");
        cliArgsCap.add("--ssl-protocol=any");
        cliArgsCap.add("--ignore-ssl-errors=true");
        cliArgsCap.add("--webdriver-loglevel=ERROR");

        desiredCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
        driver = new PhantomJSDriver(desiredCaps);
        driver.manage().window().setSize(new Dimension(1920, 1080));
        System.setProperty("phantomjs.page.settings.userAgent", USER_AGENT);
    }

    public static void waitForAjaxToFinish() {
        WebDriverWait wait = new WebDriverWait(driver, 5000);
        wait.until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver wdriver) {
                return ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
            }
        });
    }

    public static void scrapeSerieA(String year) throws InterruptedException, IOException {
        System.out.println("--------------------------");
        System.out.println("Scraping at\t" + baseUrl + year + "/");
        driver.get(baseUrl + year + "/");
        int c = 0;
        try {
            while (true) {
                driver.findElement(By.cssSelector("a.show_more")).click();
                //System.out.println("Click #" + c);
                c++;
                waitForAjaxToFinish();
            }
        } catch (NoSuchElementException e) {
            System.out.println("Done with clicking showmore.\t" + c);
            //File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            // Now you can do whatever you need to do with it, for example copy somewhere
            //FileUtils.copyFile(scrFile, new File("c:\\tmp\\screenshot" + year + ".png"));
        }

        //Must use Jsoup -> much faster than selenium parser
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements list = doc.getElementsByClass("blocks");
        List<WebElement> listSelenium = driver.findElements(By.cssSelector("a.score_link"));
        System.out.println(listSelenium.size());
        System.out.println("# matches:\t" + list.size());
        PrintWriter writer = new PrintWriter(baseFilePath + year + "matches.txt", "UTF-8");

        int index = 0;
        for (Element e : list) {
            JsonObject match = new JsonObject();
            Elements eList = e.getElementsByTag("td");
            Elements dateTime = eList.get(0).getElementsByTag("span");

            match.addProperty("date", dateTime.get(0).text());
            match.addProperty("time", dateTime.get(1).text());
            match.addProperty("homeTeam", eList.get(2).text());
            match.addProperty("result", eList.get(3).text());
            match.addProperty("awayTeam", eList.get(4).text());
            match.addProperty("halfResult", eList.get(5).text());

            //------------------
            //****Switch to new window
            // Store the current window handle
            String winHandleBefore = driver.getWindowHandle();
            listSelenium.get(index).click();

            // Switch to new window opened
            for (String winHandle : driver.getWindowHandles()) {
                if (!winHandle.equals(winHandleBefore)) {
                    driver.switchTo().window(winHandle);
                    break;
                }
            }
            //NEW WINDOW ACTIVATED

            Document newWindow = Jsoup.parse(driver.getPageSource());
            Elements rows = newWindow.getElementsByClass("score_row");
            int i = 0;
            for (Element row : rows) {
                if (i > 1) {
                    Elements tds = row.getElementsByTag("td");
                    if (!tds.get(0).text().equals("")) {
                        match.addProperty("homeEvent" + (i - 2), tds.get(0).text());
                    }
                    if (!tds.get(1).text().equals("")) {
                        match.addProperty("scoreTmp" + (i - 2), tds.get(1).text());
                    }
                    if (!tds.get(2).text().equals("")) {
                        match.addProperty("awayEvent" + (i - 2), tds.get(2).text());
                    }
                }
                i++;
            }
            //NEW WINDOW SHUTTING DOWN
            // SCREENSHOT
            //FileUtils.copyFile(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE), new File("c:\\tmp\\screenshot22" + year + ".png"));           

            //****Done with new window
            // Close the new window, if that window no more required
            driver.close();
            // Switch back to original browser (first window)
            driver.switchTo().window(winHandleBefore);
            //Screenshot
            //FileUtils.copyFile(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE), new File("c:\\tmp\\screenshot44" + year + ".png"));            
            //------------------

            //to avoid flushing once per loop (caused by println)
            writer.print(match.toString());
            if (index != list.size() - 1) {
                writer.print(",\n");
            }
            index++;
        }
        writer.flush();
        writer.close();

        System.out.println("--------------------------");
    }

    public static void scrapeClassifica(String year) throws IOException {
        System.out.println("--------------------------");
        System.out.println("Scraping classifica at\t" + baseUrl + year + "/");
        PrintWriter writer = new PrintWriter(baseFilePath + year + "standings.txt", "UTF-8");
        driver.get(baseUrl + year + "/standings/");
        //Must use Jsoup -> much faster than selenium parser
        Document doc = Jsoup.parse(driver.getPageSource());
        Element standing = doc.getElementById("standings_1a");
        Elements teams = standing.getElementsByClass("blocks");
        int index = 0;
        for (Element team : teams) {
            Elements columns = team.getElementsByTag("td");

            JsonObject teamRank = new JsonObject();
            teamRank.addProperty("achievement", columns.get(0).attr("title"));
            teamRank.addProperty("team", columns.get(1).text());
            teamRank.addProperty("points", columns.get(2).text());
            teamRank.addProperty("mp", columns.get(3).text());
            teamRank.addProperty("wins", columns.get(4).text());
            teamRank.addProperty("draws", columns.get(5).text());
            teamRank.addProperty("losses", columns.get(6).text());
            teamRank.addProperty("goals", columns.get(7).text());
            teamRank.addProperty("goalsDifference", columns.get(8).text());

            writer.print(teamRank.toString());
            if (index != teams.size() - 1) {
                writer.print(",\n");
            }
            index++;
        }
        writer.flush();
        writer.close();

    }

    public static void main(String[] args) throws InterruptedException, IOException {
        long begin = System.currentTimeMillis();
        initPhantomJS();
        for (int year = 1993; year < 2019; year++) {
            String s = year + "-" + (year + 1);
                scrapeSerieA(s);
            scrapeClassifica(s);
        }

        long end = System.currentTimeMillis();
        Date date = new Date(end - begin);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateFormatted = formatter.format(date);
        System.out.println("End of script. Elapsed " + dateFormatted);

    }

}
