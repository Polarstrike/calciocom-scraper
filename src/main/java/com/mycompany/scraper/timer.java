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
public class timer {
    private static long begin;
    private static long end;
    
    public static void start(){
        begin = System.currentTimeMillis();
    }
    
    public static void elapsed(){
        end = System.currentTimeMillis();
        Date date = new Date(end - begin);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateFormatted = formatter.format(date);
        System.out.println("End of script. Elapsed " + dateFormatted);
    }
}
