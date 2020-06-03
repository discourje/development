/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               I S B a s e                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    ISbase implements base class for IS benchmark.                       !
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
!     Mathew Schultz	   					          !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.ISThreads;

import discourje.core.AsyncJ;
import discourje.core.SpecJ;
import discourje.examples.npb3.impl.Timer;
import discourje.examples.npb3.impl.IS;

public class ISBase extends Thread{
  public static final String BMName="IS";
  public char CLASS = 'S';
  public static final int  MAX_ITERATIONS=10,TEST_ARRAY_SIZE=5;
  
  public int TOTAL_KEYS_LOG_2;
  public int MAX_KEY_LOG_2;
  public int NUM_BUCKETS_LOG_2;
  //common variables
  public int TOTAL_KEYS;
  public int MAX_KEY;
  public int NUM_BUCKETS;
  public int NUM_KEYS;
  public int SIZE_OF_BUFFERS; 

  public boolean timeron=false;
  public Timer timer = new Timer();
  
  public static int test_index_array[], test_rank_array[];
  public static int 
       S_test_index_array[] = {48427,17148,23627,62548,4431},
       S_test_rank_array[] = {0,18,346,64917,65463},

       W_test_index_array[] = {357773,934767,875723,898999,404505},
       W_test_rank_array[] = {1249,11698,1039987,1043896,1048018},

       A_test_index_array[] = {2112377,662041,5336171,3642833,4250760},
       A_test_rank_array[] = {104,17523,123928,8288932,8388264},

       B_test_index_array[] = {41869,812306,5102857,18232239,26860214},
       B_test_rank_array[] = {33422937,10244,59149,33135281,99}, 

       C_test_index_array[] = {44172927,72999161,74326391,129606274,21736814},
       C_test_rank_array[] = {61147,882988,266290,133997595,133525895};
  
/************************************/
/* These are the three main arrays. */
/************************************/
  public static int passed_verification;
  public int master_hist[], key_array[], partial_verify_vals[];
  
  public ISBase(){}
  
  public ISBase(char clss, int np, boolean serial){
    CLASS=clss;
    num_threads=np;
    switch(CLASS){
    case 'S':
      test_index_array = S_test_index_array;
      test_rank_array  = S_test_rank_array;
      TOTAL_KEYS_LOG_2 = 16;
      MAX_KEY_LOG_2 = 11;
      NUM_BUCKETS_LOG_2 = 9;
      break;
    case 'W':
      test_index_array = W_test_index_array;
      test_rank_array  = W_test_rank_array;
      TOTAL_KEYS_LOG_2 = 20;
      MAX_KEY_LOG_2 = 16;
      NUM_BUCKETS_LOG_2 = 10;
      break;
    case 'A':
      test_index_array = A_test_index_array;
      test_rank_array  = A_test_rank_array;
      TOTAL_KEYS_LOG_2  =  23;
      MAX_KEY_LOG_2	=  19;
      NUM_BUCKETS_LOG_2  = 10;
      break;
    case 'B':
      test_index_array = B_test_index_array;
      test_rank_array  = B_test_rank_array;
      TOTAL_KEYS_LOG_2 = 25;
      MAX_KEY_LOG_2 = 21;
      NUM_BUCKETS_LOG_2 = 10;
      break;
    case 'C':
      test_index_array = C_test_index_array;
      test_rank_array  = C_test_rank_array;
      TOTAL_KEYS_LOG_2 = 27;
      MAX_KEY_LOG_2 = 23;
      NUM_BUCKETS_LOG_2 = 10;
      break;
    }
    //common variables
    TOTAL_KEYS       = (1 << TOTAL_KEYS_LOG_2);
    MAX_KEY	     = (1 << MAX_KEY_LOG_2);
    NUM_BUCKETS      = (1 << NUM_BUCKETS_LOG_2);
    NUM_KEYS	     = TOTAL_KEYS;
    SIZE_OF_BUFFERS  = NUM_KEYS; 

    key_array = new int[SIZE_OF_BUFFERS];
    master_hist = new int[MAX_KEY];    
    partial_verify_vals =  new int[TEST_ARRAY_SIZE];

    for( int i=0; i<MAX_KEY; i++ ) master_hist[i] = 0;
  }
  
  public int num_threads=0;
  public RankThread rankthreads[];
  public IS master;

  public void setupThreads(IS is){
    int start=0, end=0, remainder=TOTAL_KEYS%num_threads, offset=0;
    int rstart=0, rend=0, rremainder=MAX_KEY%num_threads, roffset=0;

    var m = AsyncJ.dcj() ? AsyncJ.monitor(SpecJ.session("::is", new Object[]{num_threads})) : null;

    rankthreads = new RankThread[num_threads];
    for(int i=0;i<num_threads;i++){
      start = i*(TOTAL_KEYS/num_threads) + offset;
      end = i*(TOTAL_KEYS/num_threads) + (TOTAL_KEYS/num_threads) - 1 + offset;
      if(remainder>0){
        remainder--;
        offset++;
        end++;
      }

      rstart = i*(MAX_KEY/num_threads) + roffset;
      rend = i*(MAX_KEY/num_threads) + (MAX_KEY/num_threads) - 1 + roffset;
      if(rremainder>0){
        rremainder--;
        roffset++;
        rend++;
      }
      rankthreads[i]= new RankThread(is,i,start,end,rstart,rend,
              AsyncJ.channel(1, SpecJ.role("::master"), SpecJ.role("::worker", i), m),
              AsyncJ.channel(1, SpecJ.role("::worker", i), SpecJ.role("::master"), m));
      rankthreads[i].start();
    }
    for(int i=0;i<num_threads;i++){
      rankthreads[i].rankthreads=rankthreads;
    }
  }
  
  public void checksum(int array[], String name, boolean stop){
    double check=0;
    for(int i=0;i<array.length;i++) check+=array[i];
    System.out.println(name + " checksum is " + check);
    if(stop) System.exit(0);
  }
}
