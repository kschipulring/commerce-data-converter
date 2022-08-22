package com.migrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadFromFile {
    public static String contents(String file_name){

        M2SLogger.info("ReadFromFile file_name = " + file_name);

        Path path = Paths.get(file_name);

        if( Files.isReadable(path) ) {
            try (Stream<String> lines = Files.lines(Paths.get(file_name))) {
    
                // UNIX \n, WIndows \r\n
                String content = lines.collect(Collectors.joining(System.lineSeparator()));
    
                return content;
            } catch (IOException e) {
                e.printStackTrace();
    
                M2SLogger.severe("ReadFromFile file_name = " + file_name + " failed");
                M2SLogger.severe(e.getMessage());
            }
        }else{
            M2SLogger.warning("The file = " + file_name + "does not exist. What are you doing?");
        }

        return null;
    }
}
