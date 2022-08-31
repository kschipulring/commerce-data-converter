package com.migrator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;
 
public class MergerFiles {
 
	public static void main(String[] args) throws IOException {

          Config.getInstance();

          String[] file_source_folder_arr = {Config.csv_save_base_dir, "job_2022-08-30_05-53-52", "orders"};

          String files_source_folder = String.join(File.separator, file_source_folder_arr) + File.separator;

          M2SSystem.println( "files_source_folder = " + files_source_folder );

          folderFiles2MergedFiles(files_source_folder, null, "LegacyOrder_", "LegacyOrderItem_");
	}

     public static void folderFiles2MergedFiles(
          String files_source_folder,
          @Nullable String save_subdir,
          @Nullable String... file_prefixes
     ) throws NullPointerException
     {

          if(save_subdir == null){
               save_subdir = "LegacyOrders";
          }

          if(file_prefixes == null){
               System.out.println("viva las vegas");

               return;
          }
	
          File folder = new File(files_source_folder);
          File[] listOfFiles = folder.listFiles();

          //if there are no files here, then what is the point?
          if(listOfFiles == null){
               return;
          }

          M2SSystem.println( "listOfFiles = " + listOfFiles );

          //multidimensional collection of files - each sub-collection is for a filename prefix
          HashMap<String, ArrayList<String>> file_prefix_lists = new HashMap<String, ArrayList<String>>();

          for (String file_prefix : file_prefixes) {
               file_prefix_lists.put(file_prefix, new ArrayList<String>() );
          }
     
          //loop through all the files in this directory
          for (int i = 0; i < listOfFiles.length; i++) {
               if (listOfFiles[i].isFile()) {
                    
                    //loop through the filename prefixes again
                    for (String file_prefix : file_prefixes) {
                         if( listOfFiles[i].getName().contains(file_prefix) ){
                              //add the filename to its respective list
                              file_prefix_lists.get(file_prefix).add( listOfFiles[i].getName() );
                         }
                    }
               }
          }

          M2SSystem.println( file_prefix_lists.get( "LegacyOrder_" ) );


          System.out.println( file_prefix_lists.get( "LegacyOrder_" ).size() );


          for (String file_prefix : file_prefixes) {
               File[] temp_files = new File[ file_prefix_lists.get(file_prefix).size() ];
          
               for(int i = 0; i < file_prefix_lists.get(file_prefix).size(); i++ ){
                    temp_files[i] = new File( files_source_folder + file_prefix_lists.get(file_prefix).get(i) );
               }
          
               String mergedFilePath = files_source_folder + File.separator + save_subdir + File.separator + file_prefix + ".csv";
          
               File mergedFile = new File(mergedFilePath);
          
               if(!mergedFile.getParentFile().exists()) {
                    mergedFile.getParentFile().mkdirs();
               }
           
               mergeFiles(temp_files, mergedFile);
          }


     }
 
	public static void mergeFiles(File[] files, File mergedFile) {
 
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			fstream = new FileWriter(mergedFile, true);
			 out = new BufferedWriter(fstream);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
 
		for (File f : files) {
			System.out.println("merging: " + f.getName());
			FileInputStream fis;
			try {
				fis = new FileInputStream(f);
				BufferedReader in = new BufferedReader(new InputStreamReader(fis));
 
				String aLine;
				while ((aLine = in.readLine()) != null) {
					out.write(aLine);
					out.newLine();
				}
 
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
 
		try {
			out.close();

               System.out.println("completed merge to: " + mergedFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
 
	}
}