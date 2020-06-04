/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                          T X I n v e r s e                              !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    TXInverse implements thread for TXInverse subroutine of              !
!    the SP benchmark.                                                    !
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
package discourje.examples.npb3.impl.SPThreads;
import discourje.examples.npb3.impl.SP;

public class TXInverse extends SPBase{
  public int id;
  public boolean done = true;

  //private arrays and data
  int lower_bound;
  int upper_bound;

  public TXInverse(SP sp,int low, int high){
    Init(sp);
    setPriority(Thread.MAX_PRIORITY);
    setDaemon(true);
    master=sp;
    lower_bound=low;
    upper_bound=high;
  }
  void Init(SP sp){
    //initialize shared data
    IMAX=sp.IMAX;
    JMAX=sp.JMAX; 
    KMAX=sp.KMAX; 
    problem_size=sp.problem_size; 
    nx2=sp.nx2;
    ny2=sp.ny2;
    nz2=sp.nz2;
    grid_points=sp.grid_points;
    niter_default=sp.niter_default;
    dt_default=sp.dt_default;    
    u=sp.u;
    rhs=sp.rhs;
    forcing=sp.forcing;
    isize1=sp.isize1;
    jsize1=sp.jsize1;
    ksize1=sp.ksize1;
    us=sp.us;
    vs=sp.vs;
    ws=sp.ws;
    qs=sp.qs;
    rho_i=sp.rho_i;
    speed=sp.speed;
    square=sp.square;
    jsize2=sp.jsize2;
    ksize2=sp.ksize2;
    ue=sp.ue;
    buf=sp.buf;
    jsize3=sp.jsize3;
    lhs=sp.lhs;
    lhsp=sp.lhsp;
    lhsm=sp.lhsm;
    jsize4=sp.jsize4;
    cv=sp.cv;
    rhon=sp.rhon;
    rhos=sp.rhos;
    rhoq=sp.rhoq;
    cuf=sp.cuf;
    q=sp.q;
    ce=sp.ce;
  }

  public void run(){
    for(;;){
      synchronized(this){ 
      while(done==true){
	try{
	  wait();
	synchronized(master){ master.notify();}
	}catch(InterruptedException ie){}
      }
      step();
      synchronized(master){done=true;master.notify();}
      }
    }
  }    
  
  public void step(){
    int i, j, k;
    double t1, t2, t3, ac, ru1, uu, vv, ww, r1, r2, r3, r4, r5, ac2inv;

       for(k=lower_bound;k<=upper_bound;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){
                ru1 = rho_i[i+j*jsize2+k*ksize2];
                uu = us[i+j*jsize2+k*ksize2];
                vv = vs[i+j*jsize2+k*ksize2];
                ww = ws[i+j*jsize2+k*ksize2];
                ac = speed[i+j*jsize2+k*ksize2];
                ac2inv = 1.0 / ( ac*ac );

                r1 = rhs[0+i*isize1+j*jsize1+k*ksize1];
                r2 = rhs[1+i*isize1+j*jsize1+k*ksize1];
                r3 = rhs[2+i*isize1+j*jsize1+k*ksize1];
                r4 = rhs[3+i*isize1+j*jsize1+k*ksize1];
                r5 = rhs[4+i*isize1+j*jsize1+k*ksize1];

                t1 = c2 * ac2inv * ( qs[i+j*jsize2+k*ksize2]*r1 - uu*r2  - 
                        vv*r3 - ww*r4 + r5 );
                t2 = bt * ru1 * ( uu * r1 - r2 );
                t3 = ( bt * ru1 * ac ) * t1;

                rhs[0+i*isize1+j*jsize1+k*ksize1] = r1 - t1;
                rhs[1+i*isize1+j*jsize1+k*ksize1] = - ru1 * ( ww*r1 - r4 );
                rhs[2+i*isize1+j*jsize1+k*ksize1] =   ru1 * ( vv*r1 - r3 );
                rhs[3+i*isize1+j*jsize1+k*ksize1] = - t2 + t3;
                rhs[4+i*isize1+j*jsize1+k*ksize1] =   t2 + t3;

             }
          }
       }
  }
}
