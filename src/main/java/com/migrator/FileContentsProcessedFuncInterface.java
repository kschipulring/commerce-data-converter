package com.migrator;

// for when you want an optional closure to execute for each file contents prior to being merged
@FunctionalInterface //optional
public interface FileContentsProcessedFuncInterface {
     String process(String... file_contents);
}
