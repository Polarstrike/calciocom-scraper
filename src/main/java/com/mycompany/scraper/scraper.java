/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.scraper;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/**
 *
 * @author Giacomo
 */
public class scraper {

    public static String camelCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        int i=0;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            if(i==0)
                ch = Character.toLowerCase(ch);
            converted.append(ch);
            i++;
        }

        return converted.toString().replace(" ", "");
    }
    
    public static String normalizeText(String text){
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return text.replaceAll("[^\\p{ASCII}]", "");
    }

    public static void scrapeCampionato(String campionato, String filename) throws IOException{
       //PARAMETERS
        int MAX_GIORNATE = 38;
        final int STARTING_YEAR = 2015;
        final int ACTUAL_YEAR = 2019;
        String championship = campionato;
        System.out.println("Scraping "+championship);
        //String championship = "ita-serie-a";
        //String championship = "eng-premier-league";
        //END OF PARAMETERS
        
        
        List<String> months = new ArrayList<>(Arrays.asList("gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"));
        String base = "http://www.calcio.com";
        String year = "";
        JSONObject league = new JSONObject();
        for(int y = STARTING_YEAR; y < ACTUAL_YEAR; y++){
            year = y + "-" + (y+1) + "/";
            System.out.println("-- Getting season " + year);
        
            String connectTo = "http://www.calcio.com/giocatori/" + championship + "-" + year;
            if(connectTo.equals("http://www.calcio.com/giocatori/esp-primera-division-2016-2017/"))
                connectTo = "http://www.calcio.com/giocatori/esp-primera-division-2016-2017_2/";
            Document doc = Jsoup.connect(connectTo).get();
            Elements e = doc.getElementsByClass("standard_tabelle");
            Element el = e.get(0);
            Elements teams = el.getElementsByTag("tr");
            JSONObject season = new JSONObject();
            doc = Jsoup.connect("http://www.calcio.com/storia/" + championship).get();
            Elements seasonRows = doc.getElementsByClass("standard_tabelle").get(0).getElementsByTag("tr");
            for(Element s : seasonRows){
                if(s.text().contains(STARTING_YEAR + "/" + (STARTING_YEAR+1))){
                    String winner = s.getElementsByTag("td").get(5).text();
                    season.put("winner", normalizeText(winner));
                    break;
                }                    
            }
            for (Element t : teams) {
                JSONObject team = new JSONObject();
                String link = t.getElementsByTag("td").get(3).getElementsByTag("a").attr("href");                
                doc = Jsoup.connect(base + link).get();
                Elements infos = doc.getElementsByClass("standard_tabelle").get(0).getElementsByTag("tr");
                //GET INFO
                JSONObject info = new JSONObject();            
                for (Element row : infos) {
                    Elements tds = row.getElementsByTag("td");
                    String key = camelCase(tds.get(0).text()).replace(":", "");
                    if(key.equals("stadio")){
                        Elements els = tds.get(1).getAllElements();
                        info.put(key, normalizeText(normalizeText(els.get(1).text())));                        
                        info.put("capacity", normalizeText(tds.get(1).text().replace(els.get(1).text() + " ", "")));          
                    }
                    else{
                        info.put(key, normalizeText(tds.get(1).text().replace("\n", "")));
                    }                
                }

                //GET ROSA
                link = t.getElementsByTag("td").get(5).getElementsByTag("a").attr("href");
                doc = Jsoup.connect(base + link).get();
                Elements rows = doc.getElementsByClass("standard_tabelle").get(0).getElementsByTag("tr");
                String role = "";
                JSONObject roster = new JSONObject();
                for(Element row : rows){
                    JSONObject player = new JSONObject();
                    Elements tds = row.getElementsByTag("td");
                    if(row.text().equals("Portiere") || row.text().equals("Difesa") || row.text().equals("Centrocampo") || row.text().equals("Attacco") || row.text().equals("Allenatore") || row.text().equals("Allenatore in seconda") || row.text().equals("Preparatore dei portieri")){
                        role = row.text();
                    }
                    else{
                        if(role.equals("Preparatore dei portieri"))
                            break;                           
                        if(role.equals("Allenatore in seconda"))
                            role = "Allenatore";
                        player.put("role", role);
                        String number = tds.get(1).text();
                        if(!number.equals(""))
                            player.put("number", number);
                        player.put("name", normalizeText(tds.get(2).text()));
                        player.put("nationality", tds.get(4).text());
                        String birthSplit[] = tds.get(5).text().split("\\.");
                        if(birthSplit.length > 1){
                            String birth = birthSplit[2] + "-" + birthSplit[1] + "-" + birthSplit[0] + "T" + "00:00:00:000+01:00";                     
                            player.put("birth", birth);
                        }
                        roster.append("player", player);

                        if(role.equals("Allenatore"))
                            break;   
                        }

                }


                /* to go to /presenze_squadra/atalanta/ita-serie-a-2018-2019/
                Elements options = doc.getElementsByClass("auswahlbox").get(0).getElementsByTag("td").get(3).getElementsByTag("option");
                for(Element tmpElement : options){
                    String tmpLink = tmpElement.attr("value");
                    if(tmpLink.contains(championship))
                        link = tmpLink;                 
                }
                */


                team.append("roster", roster);
                team.append("info", info);            

                season.append("team", team);
            }

            //GET RISULTATI
            String linkRisultati = base + "/calendario/" + championship + "-" + year.replace("/", "") + "-spieltag/";
            if(linkRisultati.equals("http://www.calcio.com/calendario/esp-primera-division-2016-2017-spieltag/"))
                linkRisultati = "http://www.calcio.com/calendario/esp-primera-division-2016-2017-spieltag_2/";            
            for (int i = 1; i <= MAX_GIORNATE; i++) {
                JSONObject round = new JSONObject();
                System.out.println("   -- Risultati giornata " + i);
                doc = Jsoup.connect(linkRisultati + i).get();
                //ON FIRST ROUND -> GET NUMBER OF ROUNDS
                if(i == 1){
                    Elements options = doc.getElementsByAttributeValueContaining("name", "runde").get(0).getAllElements();
                    options.remove(0);
                    MAX_GIORNATE = options.size();
                }
                //ON LAST ROUND -> GET RANKINGS
                if(i == MAX_GIORNATE){
                    Elements colorExplainations = doc.getElementsByClass("data").get(4).getElementsByTag("table").get(1).getElementsByTag("tr");
                    List<JSONObject> colors = new ArrayList<>();
                    for(Element row : colorExplainations){
                        Elements tds = row.getElementsByTag("td");
                        JSONObject colorExplaination = new JSONObject();
                        colorExplaination.put("color", tds.get(0).attr("bgcolor"));
                        colorExplaination.put("result", tds.get(1).text());
                        colors.add(colorExplaination);
                    }                                        
                    
                    JSONObject rankings = new JSONObject();
                    Elements ranks = doc.getElementsByClass("standard_tabelle").get(1).getElementsByTag("tr");
                    ranks.remove(0);
                    for(Element rank : ranks){
                        JSONObject team = new JSONObject();
                        Elements tds = rank.getElementsByTag("td");
                        team.put("name", normalizeText(tds.get(2).text()));
                        if(!tds.get(2).attr("bgcolor").equals("FFFFFF")){
                            String target = tds.get(2).attr("bgcolor");
                            for(JSONObject c : colors){
                                if(c.get("color").equals(target)){
                                    team.put("award", c.get("result"));
                                    break;
                                }
                            }
                        }
                        team.put("played", tds.get(3).text());
                        team.put("won", tds.get(4).text());
                        team.put("draw", tds.get(5).text());
                        team.put("loss", tds.get(6).text());
                        String[] goals = tds.get(7).text().split(":");
                        team.put("goalScored", goals[0]);
                        team.put("goalAllowed", goals[1]);
                        team.put("points", tds.get(9).text());
                                                
                        rankings.append("team", team);
                    }
                    season.put("ranking", rankings);
                }
                //DONE WITH RANKINGS, GET RISULTATI
                Elements matches = doc.getElementsByClass("standard_tabelle").get(0).getElementsByTag("tr");
                for (Element m : matches) {
                    JSONObject match = new JSONObject();
                    String linkMatch = m.getElementsByTag("td").get(5).getElementsByTag("a").get(0).attr("href");
                    doc = Jsoup.connect(base + linkMatch).get();
                    Elements tables = doc.getElementsByClass("standard_tabelle");
                    //get match teams + date + result
                    Elements headers = tables.get(0).getElementsByTag("th");
                    match.put("homeTeam", normalizeText(headers.get(0).text()));
                    match.put("awayTeam", normalizeText(headers.get(2).text()));
                    String matchDate = headers.get(1).text();
                    matchDate = matchDate.split(", ")[1];
                    matchDate = matchDate.replace(".", "");
                    String[] matchDates = matchDate.split(" ");
                    matchDate = matchDates[2]+"-"+months.indexOf(matchDates[1])+"-"+matchDates[0]+"T"+matchDates[3]+":00:000+01:00";
                    match.put("date", matchDate);
                    String[] result = tables.get(0).getElementsByTag("td").get(1).text().split(":");
                    match.put("homeResult", result[0]);
                    match.put("awayResult", result[1]);
                    //get match history (goals)
                    Elements rows = tables.get(1).getElementsByTag("tr");
                    rows.remove(0);
                    for(Element row : rows){
                        if(row.text().equals("Nessuno"))
                            break;
                        JSONObject goal = new JSONObject();
                        Elements tds = row.getElementsByTag("td");
                        String[] partial = tds.get(0).text().split(":");
                        goal.put("homePartial", partial[0]);
                        goal.put("awayPartial", partial[1]);
                        String scorer = tds.get(1).text();                    
                        String assist = "";
                        String kind = "";
                        String[] infos = scorer.split("\\s\\d+.");
                        scorer = infos[0];
                        if(infos.length > 1){
                            infos[1] = infos[1].replace("\\s\\/\\s", "");
                            infos = infos[1].split("\\s\\(");    
                            if(infos.length > 1)
                                assist = infos[1].replace(")", "");
                            kind = infos[0].replace(" / ", "");
                        }
                        goal.put("scorer", normalizeText(scorer));
                        goal.put("assist", normalizeText(assist));
                        goal.put("kind", kind);           
                        String minute = tds.get(1).text().replace(scorer+" ", "");
                        if(!kind.equals("")){
                            minute = minute.replace(kind, "");
                        }
                        if(!assist.equals("")){
                            assist = "(" + assist + ")";                            
                            minute = minute.replace(assist, "");
                        }
                        minute = minute.replace(".", "").replace("/", "").replace(" ", "");
                        goal.put("minute", minute);
                        
                        match.append("goal", goal);
                    }
                    int index = 2;
                    if(tables.get(index).getElementsByTag("tr").get(0).text().equals("avvenimenti particolari"))
                        index++;

                    //get formazioni home
                    rows = tables.get(index).getElementsByTag("tr");
                    Boolean titolare = true;
                    JSONObject roster = new JSONObject();
                    for(Element row : rows){                    
                        JSONObject player = new JSONObject();
                        Elements tds = row.getElementsByTag("td");
                        if(tds.size() == 1){
                            if(tds.get(0).text().equals("Giocatori di riserva"))
                                titolare = false;     
                        }
                        else{
                            player.put("number", tds.get(0).text());
                            Elements portions = tds.get(1).getAllElements();
                            portions.remove(0);      
                            player.put("name", normalizeText(portions.get(0).text()));
                            if(portions.size() > 1){
                                JSONObject event = new JSONObject();
                                String what = portions.get(1).attr("alt");                                
                                event.put("event", what);
                                if(portions.size() == 3){                                    
                                    String when = portions.get(2).text().replace("'", "");
                                    event.put("time", when);
                                }
                                player.put("event", event);
                            }
                            if(!tds.get(2).text().equals("")){
                                if(titolare)
                                    player.put("leaveTime", tds.get(2).text().replace("'", ""));
                                else
                                    player.put("enterTime", tds.get(2).text().replace("'", ""));
                            }
                            player.put("starter", titolare);    //titolare
                            roster.append("player", player);
                        }                    
                    }
                    match.put("homeLineup", roster);
                    index++;

                    //get formazioni away
                    rows = tables.get(index).getElementsByTag("tr");
                    titolare = true;
                    roster = new JSONObject();
                    for(Element row : rows){     
                        JSONObject player = new JSONObject();
                        Elements tds = row.getElementsByTag("td");
                        if(tds.size() == 1){
                            if(tds.get(0).text().equals("Giocatori di riserva"))
                                titolare = false;   
                        }
                        else{
                            player.put("number", tds.get(0).text());
                            Elements portions = tds.get(1).getAllElements();
                            portions.remove(0);                   
                            player.put("name", normalizeText(portions.get(0).text()));
                            if(portions.size() > 1){
                                JSONObject event = new JSONObject();
                                String what = portions.get(1).attr("alt");
                                event.put("event", what);
                                if(portions.size() == 3){                                    
                                    String when = portions.get(2).text().replace("'", "");
                                    event.put("time", when);
                                }
                                player.put("event", event);
                            }
                            if(!tds.get(2).text().equals("")){
                                if(titolare)
                                    player.put("leaveTime", tds.get(2).text().replace("'", ""));
                                else
                                    player.put("enterTime", tds.get(2).text().replace("'", ""));
                            }
                            player.put("starter", titolare);    //titolare
                            roster.append("player", player);
                        }                    
                    }
                    match.put("awayLineup", roster);
                    index++;

                    //get coach                
                    rows = tables.get(index).getElementsByTag("th");
                    match.put("homeCoach", normalizeText(rows.get(0).text().replace("Allenatore:\\s*", "")));  
                    match.put("awayCoach", normalizeText(rows.get(1).text().replace("Allenatore:\\s*", "")));
                    index++;

                    //get informations
                    rows = tables.get(index).getElementsByTag("tr");
                    //System.out.println(match.get("homeTeam")+ " - "+ match.get("awayTeam"));
                    match.put("stadium", normalizeText(rows.get(0).getElementsByTag("td").get(2).text()));
                    match.put("attendance", rows.get(1).getElementsByTag("td").get(2).text().replace(".", ""));
                    match.put("referee", normalizeText(rows.get(2).getElementsByTag("td").get(2).text()));

                    round.append("match", match);
                }
                season.append("round", round);   
                season.put("year", year.replace("/", ""));
            }
            league.put("name", championship);
            league.append("season", season);
        }
        
        //PrintWriter writer = new PrintWriter("provafile.txt", "UTF-8");
        PrintWriter writer = new PrintWriter(filename+".txt", "UTF-8");
        writer.println(league.toString());
        writer.close();
    }
    
    public static void main(String[] args) throws InterruptedException, IOException {
        String[] campionati = {/*"ita-serie-a", "bundesliga", "eng-premier-league", "fra-ligue-1", */"esp-primera-division"};
        for(String campionato : campionati){           
            scrapeCampionato(campionato, campionato);
        }
        System.out.println("----------------SCRAPING OVER----------------");
    }

}
