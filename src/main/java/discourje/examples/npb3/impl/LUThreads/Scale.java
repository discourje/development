/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               S c a l e                                 !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    Scale implements thread for Scale subroutine of LU benchmark.        !
!                                                                         !
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
! Translation to Java and MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.LUThreads;
import discourje.examples.npb3.impl.LU;

public class Scale extends LUBase{
  public int id;
  public boolean done=true;

  //private arrays and data
  int lower_bound1;
  int upper_bound1;

  public Scale(LU lu,int low1, int high1){
    Init(lu);
    lower_bound1=low1;
    upper_bound1=high1;
    setPriority(MAX_PRIORITY);
    setDaemon(true);
    master=lu;
  }
  void Init(LU lu){
    //initialize shared data
    isiz1=lu.isiz1;
    isiz2=lu.isiz2;
    isiz3=lu.isiz3;
    
    itmax_default=lu.itmax_default;
    dt_default=lu.dt_default;
    inorm_default=lu.inorm_default;
    
    u=lu.u;
    rsd=lu.rsd;
    frct=lu.frct;
    isize1=lu.isize1;
    jsize1=lu.jsize1;
    ksize1=lu.ksize1;
    
    flux=lu.flux;
    isize2=lu.isize2;
    
    qs=lu.qs;
    rho_i=lu.rho_i;
    jsize3=lu.jsize3;
    ksize3=lu.ksize3;
    
    a=lu.a;
    b=lu.b;
    c=lu.c;
    d=lu.d;
    isize4=lu.isize4;
    jsize4=lu.jsize4;
    ksize4=lu.ksize4;
    
    nx=lu.nx;
    ny=lu.ny;
    nz=lu.nz;
    
    nx0=lu.nx0;
    ny0=lu.ny0;
    nz0=lu.nz0;
    
    ist=lu.ist;
    iend=lu.iend;
    jst=lu.jst;
    jend=lu.jend;
    ii1=lu.ii1;
    ii2=lu.ii2;
    ji1=lu.ji1;
    ji2=lu.ji2;
    ki1=lu.ki1;
    ki2=lu.ki2;

    dxi=lu.dxi;
    deta=lu.deta; 
    dzeta=lu.dzeta;
    tx1=lu.tx1;
    tx2=lu.tx2;
    tx3=lu.tx3;
    ty1=lu.ty1;
    ty2=lu.ty2;
    ty3=lu.ty3;
    tz1=lu.tz1;
    tz2=lu.tz1;
    tz3=lu.tz3;
    
    dx1=lu.dx1;
    dx2=lu.dx2;
    dx3=lu.dx3;
    dx4=lu.dx4;
    dx5=lu.dx5;

    dy1=lu.dy1;
    dy2=lu.dy2;
    dy3=lu.dy3;
    dy4=lu.dy4;
    dy5=lu.dy5;

    dz1=lu.dz1;
    dz2=lu.dz2;
    dz3=lu.dz3;
    dz4=lu.dz4;
    dz5=lu.dz5;
   
    dssp=lu.dssp;
    dt=lu.dt;
    omega=lu.omega;
    frc=lu.frc;
    ttotal=lu.ttotal;
 }

  public void run(){    
    for(;;){
      synchronized(this){ 
      while(done==true){
	try{
	  wait();
          synchronized(master){master.notify();}
	}catch(InterruptedException ie){}
      }
      step();
      synchronized(master){done=true;master.notify();}
      }
    }
  }
  
  public void step(){
    int i,j,m, k;
    for(k=lower_bound1;k<=upper_bound1;k++){
      for(j=jst-1;j<=jend-1;j++){
	for(i=ist-1;i<=iend-1;i++){
	  for(m=0;m<=4;m++){
	    rsd[m+i*isize1+j*jsize1+k*ksize1] = dt * rsd[m+i*isize1+j*jsize1+k*ksize1];
	  }
	}
      }
    }
  }
}












