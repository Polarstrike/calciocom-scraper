/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.scraper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Giacomo
 */
public class Timer {
    private long begin;
    private long end;
    
    public void start(){
        begin = System.currentTimeMillis();
    }
    
    public void elapsed(){
        end = System.currentTimeMillis();
        Date date = new Date(end - begin);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateFormatted = formatter.format(date);
        System.out.println("End of script. Elapsed " + dateFormatted);
    }
}
