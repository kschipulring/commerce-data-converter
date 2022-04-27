package com.migrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadFromFile {
    public static String contents(String file_name){
        try (Stream<String> lines = Files.lines(Paths.get(file_name))) {
    
            // UNIX \n, WIndows \r\n
            String content = lines.collect(Collectors.joining(System.lineSeparator()));

            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
