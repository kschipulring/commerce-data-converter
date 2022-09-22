package com.migrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

public class FilesZipper {
     public static void main(String[] args) throws IOException {

          Config.getInstance();

          System.out.println( Config.base_save_dir );

          String srcFile = Config.base_save_dir + File.separator + "test1.txt";
          String dest_zipfile_name = Config.base_save_dir + File.separator + "test2.zip";

          zipFileUp(srcFile, dest_zipfile_name);
     }

     //zip multiple files up.
     public static void zipFilesUp(List<String> srcFiles, String dest_zipfile_name) throws IOException {
          FileOutputStream fos = new FileOutputStream(dest_zipfile_name);

          M2SSystem.println( "dest_zipfile_name = " + dest_zipfile_name );

          ZipOutputStream zipOut = new ZipOutputStream(fos);
          for (String srcFile : srcFiles) {
              File fileToZip = new File(srcFile);
              FileInputStream fis = new FileInputStream(fileToZip);
              ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
              zipOut.putNextEntry(zipEntry);
  
              byte[] bytes = new byte[1024];
              int length;
              while((length = fis.read(bytes)) >= 0) {
                  zipOut.write(bytes, 0, length);
              }
              fis.close();
          }
          zipOut.close();
          fos.close();
     }

     //just one file
     public static void zipFileUp(String srcFile, String dest_zipfile_name) throws IOException {

          List<String> srcFiles = new ArrayList<String>();
          srcFiles.add(srcFile);

          zipFilesUp(srcFiles, dest_zipfile_name);
     }
}
