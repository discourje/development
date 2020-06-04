/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               B M A R G S                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    BMArgs implements Command Line Benchmark Arguments class             !
!									  !
!    Permission to use, copy, distribute and modify this software	  !
!    for any purpose with or without fee is hereby granted.  We 	  !
!    request, however, that all derived work reference the NAS  	  !
!    Parallel Benchmarks 3.0. This software is provided "as is" 	  !
!    without express or implied warranty.				  !
!									  !
!    Information on NPB 3.0, including the Technical Report NAS-02-008	  !
!    "Implementation of the NAS Parallel Benchmarks in Java",		  !
!    original specifications, source code, results and information	  !
!    on how to submit new results, is available at:			  !
!									  !
!	    http://www.nas.nasa.gov/Software/NPB/			  !
!									  !
!    Send comments or suggestions to  npb@nas.nasa.gov  		  !
!									  !
!	   NAS Parallel Benchmarks Group				  !
!	   NASA Ames Research Center					  !
!	   Mail Stop: T27A-1						  !
!	   Moffett Field, CA   94035-1000				  !
!									  !
!	   E-mail:  npb@nas.nasa.gov					  !
!	   Fax:     (650) 604-3957					  !
!									  !
!-------------------------------------------------------------------------!
!     Translation to Java and to MultiThreaded Code:			  !
!     Michael A. Frumkin					          !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.BMInOut;
import java.io.*;

public class BMArgs implements Serializable{
  public static char CLASS='U';
  public static int num_threads=4;
  public static boolean serial = true;
  public BMArgs(){
    CLASS='U';
    num_threads=4;
    serial = true;  
  }
  static public void ParseCmdLineArgs(String argv[],String BMName){  
   for(int i=0; i<argv.length; i++){
     if(   argv[i].equals("SERIAL") 
     	|| argv[i].equals("serial") 
     	|| argv[i].equals("-serial")  
     	|| argv[i].equals("-SERIAL")){
       serial = true; 
     }else  
     if(   argv[i].startsWith("class=") 
     	|| argv[i].startsWith("CLASS=") 
     	|| argv[i].startsWith("-class") 
     	|| argv[i].startsWith("-CLASS")){
       
       if( argv[i].length()>6 )
     	 CLASS = Character.toUpperCase( argv[i].charAt(6) ); 
       if(CLASS!='A' && CLASS!='B' && CLASS!='C' && CLASS!='S' && CLASS!='W' ){
     	 System.out.println("classes allowed are A,B,C,W and S.");  
     	 commandLineError(BMName);
       }
     }else if(   argv[i].startsWith("np=") 
     	      || argv[i].startsWith("NP=") 
     	      || argv[i].startsWith("-NP") 
     	      || argv[i].startsWith("-np")){
     	 try{
     	   if( argv[i].length()>3 )
     	     num_threads = Integer.parseInt(argv[i].substring(3));
             serial = false; 
     	 }catch(Exception e){
     	   System.out.println( "argument to " + argv[i].substring(0,3) 
     			      +" must be an integer." );
     	   commandLineError(BMName);
     	 }
       }    
    }
  }
  public static void commandLineError(String BMName){
    System.out.println("synopsis: java "+BMName
    		      +" CLASS=[ABCWS] -serial [-NPnnn]");
    System.out.println( "[ABCWS] is the size class \n" 
    		       +"-serial specifies the serial version and\n" 
    		       +"-NP specifies number of threads where nnn "
    		       +"is an integer");
    System.exit(1);	
  } 
  public static void outOfMemoryMessage(){
    System.out.println( "The java maximum heap size is "
    		       +"to small to run this benchmark class");
    System.out.println( "To allocate more memory, use the -mxn option" 
        	       +" where n is the number of bytes to be allocated");
  }
  public static void Banner(String BMName,
                            char clss,boolean serial,int np){
    System.out.println(" NAS Parallel Benchmarks Java version (NPB3_0_JAV)");
    if(serial) System.out.println(" Serial Version "+BMName+"."+clss);
    else System.out.println(" Multithreaded Version "+BMName+"."+clss+
                            " np="+np);
  }
}
