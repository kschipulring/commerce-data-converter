package com.migrator;

import java.util.ArrayList;

// print and println with actual context
class M2SSystem {
     protected static StackTraceElement getStackTraceElement(Integer num){
         Integer index = 3 + num;
         
         return Thread.currentThread().getStackTrace()[index];
     }
     
     public static void print(Object... object){
          ArrayList<Object> al = new ArrayList<Object>();

          Integer index = 0;

          //what was normally sent to 'System.out.print(...) or ...println(...)'
          al.add( object[0] );

          if( object.length > 1 && object[1] instanceof Number){
               Integer num = (Integer) object[1];
               
               index += num;
          }

          StackTraceElement ste = getStackTraceElement(index);

          al.add( "AT: " + ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber() );

          System.out.print( al );
     }

     public static void println(Object object){
          print(object, 1);

          //just an empty blank line
          System.out.println();
     }
}
