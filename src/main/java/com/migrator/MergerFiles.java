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
import java.util.List;

import javax.annotation.Nullable;
 
public class MergerFiles {
 
	public static void main(String[] args) throws IOException {

          Config.getInstance();

          String[] file_source_folder_arr = {Config.csv_save_base_dir, "job_2022-08-30_05-53-52", "orders"};

          String files_source_folder = String.join(File.separator, file_source_folder_arr) + File.separator;

          M2SSystem.println( "files_source_folder = " + files_source_folder );

          folderFiles2MergedFiles(files_source_folder, null, "LegacyOrder_", "LegacyOrderItem_");
	}

     //to merge all files with the same prefix and filetype in a directory
     public static void folderFiles2MergedFiles(
          String files_source_folder,
          @Nullable String save_subdir,
          @Nullable String... file_prefixes
     ) throws NullPointerException, IOException
     {

          //default subdirectory in case not an actual string
          if(save_subdir == null){
               save_subdir = "LegacyOrders";
          }else{
               save_subdir = save_subdir.trim();
          }

          //no point in continuing without even a prefix of a filename to scan for.
          if(file_prefixes == null){
               M2SLogger.warning("no apparent files present");

               return;
          }
	
          //scan the directory for filenames
          File folder = new File(files_source_folder);
          File[] listOfFiles = folder.listFiles();

          //if there are no actual files here, then what is the point?
          if(listOfFiles == null){
               return;
          }

          M2SSystem.println( "listOfFiles = " + listOfFiles );

          //multidimensional collection of files - each sub-collection is for a filename prefix
          HashMap<String, ArrayList<String>> file_prefix_lists = new HashMap<String, ArrayList<String>>();

          for (String file_prefix : file_prefixes) {
               file_prefix_lists.put(file_prefix, new ArrayList<String>());
          }

          //this will be the basename for the zip file. It will come from the first filename of the first collection.
          String first_filename = null;
     
          //loop through all the files in this directory. Add a file if it qualifies
          for (int i = 0; i < listOfFiles.length; i++) {
               if (listOfFiles[i].isFile()) {
                    
                    //loop through the filename prefixes again
                    for (String file_prefix : file_prefixes) {
                         if( listOfFiles[i].getName().contains(file_prefix) ){
                              //add the filename to its respective list
                              file_prefix_lists.get(file_prefix).add( listOfFiles[i].getName() );

                              //shall fire only once
                              if(first_filename == null){
                                   first_filename = listOfFiles[i].getName();

                                   M2SSystem.println( "first_filename = " + first_filename );
                              }
                         }
                    }
               }
          }

          //will be populated with only the end merged files
          List<String> srcFiles = new ArrayList<String>();

          //now merge the file collection types into combined masters
          for (String file_prefix : file_prefixes) {
               File[] temp_files = new File[ file_prefix_lists.get(file_prefix).size() ];
          
               //loop through the contents of all the files that meet the criteria
               for(int i = 0; i < file_prefix_lists.get(file_prefix).size(); i++ ){
                    temp_files[i] = new File( files_source_folder + file_prefix_lists.get(file_prefix).get(i) );
               }

               String mergedFileExtension = getFnExtension(first_filename);

               M2SSystem.println( "mergedFileExtension = " + mergedFileExtension );
          
               //what shall be the merged file name?
               String mergedFilePath = files_source_folder + File.separator + save_subdir;
               mergedFilePath += File.separator + file_prefix + "." + mergedFileExtension;

               srcFiles.add(mergedFilePath);
          
               File mergedFile = new File(mergedFilePath);

               if(!mergedFile.getParentFile().exists()) {
                    mergedFile.getParentFile().mkdirs();
               }
           
               mergeFiles(temp_files, mergedFile);
          }

          //final zip file name
          String dest_zipfile_name = files_source_folder + File.separator + removeFnExtension(first_filename) + ".zip";

          //zip the merged files
          FilesZipper.zipFilesUp(srcFiles, dest_zipfile_name);

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

     public static String getFnExtension(String fileName) {
          String extension = "";
          
          int index = fileName.lastIndexOf('.');
          if (index > 0) {
              extension = fileName.substring(index + 1);
          }
          
          return extension;
     }

     //for removing an extension from a filename. So 'test123.csv' => 'test123'
     public static String removeFnExtension(String fileName) {
          int extPos = fileName.lastIndexOf(".");
          
          if(extPos == -1){
              return fileName;
          }else{
              return fileName.substring(0, extPos);
          }
     }
}