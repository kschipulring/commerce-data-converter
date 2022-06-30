package com.migrator;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class M2SLogger {
 
    //do new log messages add on to the prior output?
    private static boolean logger_append = true;

    private static FileHandler handler = null;

    private static Logger LOGGER = null;

    //used to help name the logfile
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    static {
        try {
            Config.getInstance();
            
            //todays date.
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            //above, but formatted.
            String today_stamp = sdf.format(timestamp);

            String log_dir = Config.log_dir;
            
            //logging file name. Will always have the current date as part so that you do not have giant log files.
            String logFile_name = log_dir + "/entries_" + today_stamp + ".log";

            System.out.println( logFile_name );
            
            handler = new FileHandler(logFile_name, logger_append);

            LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

            LOGGER.addHandler(handler);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void severe(Object log_msg){
        LOGGER.severe( log_msg.toString() );
    }

    public static void warning(Object log_msg){
        LOGGER.warning( log_msg.toString() );
    }

    public static void info(Object log_msg){
        LOGGER.info( log_msg.toString() );
    }

    public static void config(Object log_msg){
        LOGGER.config( log_msg.toString() );
    }

    public static void fine(Object log_msg){
        LOGGER.fine( log_msg.toString() );
    }
 
    public static void finer(Object log_msg){
        LOGGER.finer( log_msg.toString() );
    }

    public static void finest(Object log_msg){
        LOGGER.finest( log_msg.toString() );
    }

    public static void main( String[] args ){
        warning( "abc" );
    }
}
