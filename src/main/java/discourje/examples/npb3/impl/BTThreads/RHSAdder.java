/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                            R H S A d d e r                              !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    RHSAdder implements thread for add subroutine of BT benchmark.       !
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
package discourje.examples.npb3.impl.BTThreads;
import discourje.examples.npb3.impl.BT;

public class RHSAdder extends BTBase{
  public int id;
  public boolean done = true;

  //private data
  int lower_bound, upper_bound;
  
  public RHSAdder(BT bt,int low, int high){
    Init(bt);
    lower_bound=low;
    upper_bound=high;
    setPriority(Thread.MAX_PRIORITY);
    setDaemon(true);
    master=bt;
  }
  void Init(BT bt){
    //initialize shared data
    IMAX=bt.IMAX;
    JMAX=bt.JMAX; 
    KMAX=bt.KMAX; 
    problem_size=bt.problem_size; 
    grid_points=bt.grid_points;
    niter_default=bt.niter_default;
    dt_default=bt.dt_default;
    
    u=bt.u;
    rhs=bt.rhs;
    forcing=bt.forcing;
    cv=bt.cv;
    q=bt.q;
    cuf=bt.cuf;
    isize2=bt.isize2;
    jsize2=bt.jsize2;
    ksize2=bt.ksize2;
    
    us=bt.us;
    vs=bt.vs;
    ws=bt.ws;
    qs=bt.qs;
    rho_i=bt.rho_i;
    square=bt.square;
    jsize1=bt.jsize1;
    ksize1=bt.ksize1;
    
    ue=bt.ue;
    buf=bt.buf;
    jsize3=bt.jsize3;
  }
  
  public void run(){
    int i, j, k, m;

    for(;;){   
      synchronized(this){ 
      while(done==true){
	try{
	    wait();
	    synchronized(master){ master.notify();}
	}catch(InterruptedException ie){}
      }
	
      for(k=lower_bound;k<=upper_bound;k++){
	for(j=1;j<=grid_points[1]-2;j++){
	  for(i=1;i<=grid_points[0]-2;i++){
	    for(m=0;m<=4;m++){
	      u[m+i*isize2+j*jsize2+k*ksize2] += 
	                        rhs[m+i*isize2+j*jsize2+k*ksize2];
	    }
	  }
	}
      }
      synchronized(master){done = true; master.notify();}
    }
    }  
  }
}










