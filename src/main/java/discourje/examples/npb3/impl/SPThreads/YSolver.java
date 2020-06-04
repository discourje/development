/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                             Y S o l v e r                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    YSolver implements thread for y_solve subroutine                     !
!    of the SP benchmark.                                                 !
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

public class YSolver extends SPBase{
  public int id;
  public boolean done = true;

  //private arrays and data
  int lower_bound;
  int upper_bound;
  int state= 1;
  double lhs[],lhsm[],lhsp[],cv[],rhoq[]; 

  public YSolver(SP sp,int low, int high){
    Init(sp);
    lower_bound=low;
    upper_bound=high;
    setPriority(Thread.MAX_PRIORITY);
    setDaemon(true);
    master=sp;
    lhs = new double[5*(problem_size+1)];
    lhsp = new double[5*(problem_size+1)];
    lhsm = new double[5*(problem_size+1)];    
    cv = new double[problem_size];
    rhoq = new double[problem_size];
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
    int i, j, k, n, j1, j2, m;
    double ru1, fac1, fac2, r1, r2, r3, r4, r5, t1, t2;

    switch(state){
    case 1:
       for(k=lower_bound;k<=upper_bound;k++){
          for(i=1;i<=grid_points[0]-2;i++){

//---------------------------------------------------------------------
// Computes the left hand side for the three y-factors   
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//      first fill the lhs for the u-eigenvalue         
//---------------------------------------------------------------------

             for(j=0;j<=grid_points[1]-1;j++){
                ru1 = c3c4*rho_i[i+j*jsize2+k*ksize2];
                cv[j] = vs[i+j*jsize2+k*ksize2];
                rhoq[j] = dmax1( dy3 + con43 * ru1,
                                 dy5 + c1c5*ru1,
                                 dymax + ru1,
                                 dy1);
             }
            
              lhsinit(grid_points[1]-1);

             for(j=1;j<=grid_points[1]-2;j++){
                lhs[0+j*jsize4] =  0.0;
                lhs[1+j*jsize4] = -dtty2 * cv[j-1] - dtty1 * rhoq[j-1];
                lhs[2+j*jsize4] =  1.0 + c2dtty1 * rhoq[j];
                lhs[3+j*jsize4] =  dtty2 * cv[j+1] - dtty1 * rhoq[j+1];
                lhs[4+j*jsize4] =  0.0;
             }

//---------------------------------------------------------------------
//      add fourth order dissipation                             
//---------------------------------------------------------------------

             j = 1;

             lhs[2+j*jsize4] = lhs[2+j*jsize4] + comz5;
             lhs[3+j*jsize4] = lhs[3+j*jsize4] - comz4;
             lhs[4+j*jsize4] = lhs[4+j*jsize4] + comz1;
       
             lhs[1+(j+1)*jsize4] = lhs[1+(j+1)*jsize4] - comz4;
             lhs[2+(j+1)*jsize4] = lhs[2+(j+1)*jsize4] + comz6;
             lhs[3+(j+1)*jsize4] = lhs[3+(j+1)*jsize4] - comz4;
             lhs[4+(j+1)*jsize4] = lhs[4+(j+1)*jsize4] + comz1;

             for(j=3;j<=grid_points[1]-4;j++){

                lhs[0+j*jsize4] = lhs[0+j*jsize4] + comz1;
                lhs[1+j*jsize4] = lhs[1+j*jsize4] - comz4;
                lhs[2+j*jsize4] = lhs[2+j*jsize4] + comz6;
                lhs[3+j*jsize4] = lhs[3+j*jsize4] - comz4;
                lhs[4+j*jsize4] = lhs[4+j*jsize4] + comz1;
             }

             j = grid_points[1]-3;
             lhs[0+j*jsize4] = lhs[0+j*jsize4] + comz1;
             lhs[1+j*jsize4] = lhs[1+j*jsize4] - comz4;
             lhs[2+j*jsize4] = lhs[2+j*jsize4] + comz6;
             lhs[3+j*jsize4] = lhs[3+j*jsize4] - comz4;

             lhs[0+(j+1)*jsize4] = lhs[0+(j+1)*jsize4] + comz1;
             lhs[1+(j+1)*jsize4] = lhs[1+(j+1)*jsize4] - comz4;
             lhs[2+(j+1)*jsize4] = lhs[2+(j+1)*jsize4] + comz5;

//---------------------------------------------------------------------
//      subsequently, do the other two factors                    
//---------------------------------------------------------------------
             for(j=1;j<=grid_points[1]-2;j++){

	       lhsp[0+j*jsize4] = lhs[0+j*jsize4];
	       lhsp[1+j*jsize4] = lhs[1+j*jsize4] - 
                                  dtty2 * speed[i+(j-1)*jsize2+k*ksize2];
	       lhsp[2+j*jsize4] = lhs[2+j*jsize4];
	       lhsp[3+j*jsize4] = lhs[3+j*jsize4] + 
		                  dtty2 * speed[i+(j+1)*jsize2+k*ksize2];
	       lhsp[4+j*jsize4] = lhs[4+j*jsize4];

	       lhsm[0+j*jsize4] = lhs[0+j*jsize4];
	       lhsm[1+j*jsize4] = lhs[1+j*jsize4] + 
                                  dtty2 * speed[i+(j-1)*jsize2+k*ksize2];
	       lhsm[2+j*jsize4] = lhs[2+j*jsize4];
	       lhsm[3+j*jsize4] = lhs[3+j*jsize4] - 
                                  dtty2 * speed[i+(j+1)*jsize2+k*ksize2];
	       lhsm[4+j*jsize4] = lhs[4+j*jsize4];

             }

//---------------------------------------------------------------------
//                          FORWARD ELIMINATION  
//---------------------------------------------------------------------

             for(j=0;j<=grid_points[1]-3;j++){
                j1 = j  + 1;
                j2 = j  + 2;
                fac1      = 1./lhs[2+j*jsize4];
                lhs[3+j*jsize4]  = fac1*lhs[3+j*jsize4];
                lhs[4+j*jsize4]  = fac1*lhs[4+j*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
                lhs[2+j1*jsize4] = lhs[2+j1*jsize4] -
                               lhs[1+j1*jsize4]*lhs[3+j*jsize4];
                lhs[3+j1*jsize4] = lhs[3+j1*jsize4] -
                               lhs[1+j1*jsize4]*lhs[4+j*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
                               lhs[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
                lhs[1+j2*jsize4] = lhs[1+j2*jsize4] -
                               lhs[0+j2*jsize4]*lhs[3+j*jsize4];
                lhs[2+j2*jsize4] = lhs[2+j2*jsize4] -
                               lhs[0+j2*jsize4]*lhs[4+j*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j2*jsize1+k*ksize1] = rhs[m+i*isize1+j2*jsize1+k*ksize1] -
                               lhs[0+j2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
             }

//---------------------------------------------------------------------
//      The last two rows in this grid block are a bit different, 
//      since they do not have two more rows available for the
//      elimination of off-diagonal entries
//---------------------------------------------------------------------

             j  = grid_points[1]-2;
             j1 = grid_points[1]-1;
             fac1      = 1./lhs[2+j*jsize4];
             lhs[3+j*jsize4]  = fac1*lhs[3+j*jsize4];
             lhs[4+j*jsize4]  = fac1*lhs[4+j*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }
             lhs[2+j1*jsize4] = lhs[2+j1*jsize4] -
                            lhs[1+j1*jsize4]*lhs[3+j*jsize4];
             lhs[3+j1*jsize4] = lhs[3+j1*jsize4] -
                            lhs[1+j1*jsize4]*lhs[4+j*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
                            lhs[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }
//---------------------------------------------------------------------
//            scale the last row immediately 
//---------------------------------------------------------------------
             fac2      = 1./lhs[2+j1*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j1*jsize1+k*ksize1] = fac2*rhs[m+i*isize1+j1*jsize1+k*ksize1];
             }

//---------------------------------------------------------------------
//      do the u+c and the u-c factors                 
//---------------------------------------------------------------------
             for(j=0;j<=grid_points[1]-3;j++){
             	j1 = j  + 1;
             	j2 = j  + 2;
	     	m = 3;
             	fac1	   = 1./lhsp[2+j*jsize4];
             	lhsp[3+j*jsize4]  = fac1*lhsp[3+j*jsize4];
             	lhsp[4+j*jsize4]  = fac1*lhsp[4+j*jsize4];
             	rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsp[2+j1*jsize4] = lhsp[2+j1*jsize4] -
             		    lhsp[1+j1*jsize4]*lhsp[3+j*jsize4];
             	lhsp[3+j1*jsize4] = lhsp[3+j1*jsize4] -
             		    lhsp[1+j1*jsize4]*lhsp[4+j*jsize4];
             	rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		    lhsp[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsp[1+j2*jsize4] = lhsp[1+j2*jsize4] -
             		    lhsp[0+j2*jsize4]*lhsp[3+j*jsize4];
             	lhsp[2+j2*jsize4] = lhsp[2+j2*jsize4] -
             		    lhsp[0+j2*jsize4]*lhsp[4+j*jsize4];
             	rhs[m+i*isize1+j2*jsize1+k*ksize1] = rhs[m+i*isize1+j2*jsize1+k*ksize1] -
             		    lhsp[0+j2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
	     	m = 4;
             	fac1	   = 1./lhsm[2+j*jsize4];
             	lhsm[3+j*jsize4]  = fac1*lhsm[3+j*jsize4];
             	lhsm[4+j*jsize4]  = fac1*lhsm[4+j*jsize4];
             	rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsm[2+j1*jsize4] = lhsm[2+j1*jsize4] -
             		    lhsm[1+j1*jsize4]*lhsm[3+j*jsize4];
             	lhsm[3+j1*jsize4] = lhsm[3+j1*jsize4] -
             		    lhsm[1+j1*jsize4]*lhsm[4+j*jsize4];
             	rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		    lhsm[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsm[1+j2*jsize4] = lhsm[1+j2*jsize4] -
             		    lhsm[0+j2*jsize4]*lhsm[3+j*jsize4];
             	lhsm[2+j2*jsize4] = lhsm[2+j2*jsize4] -
             		    lhsm[0+j2*jsize4]*lhsm[4+j*jsize4];
             	rhs[m+i*isize1+j2*jsize1+k*ksize1] = rhs[m+i*isize1+j2*jsize1+k*ksize1] -
             		    lhsm[0+j2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }

//---------------------------------------------------------------------
//         And again the last two rows separately
//---------------------------------------------------------------------
             j  = grid_points[1]-2;
             j1 = grid_points[1]-1;
	     m = 3;
             fac1	= 1./lhsp[2+j*jsize4];
             lhsp[3+j*jsize4]  = fac1*lhsp[3+j*jsize4];
             lhsp[4+j*jsize4]  = fac1*lhsp[4+j*jsize4];
             rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             lhsp[2+j1*jsize4] = lhsp[2+j1*jsize4] -
             		 lhsp[1+j1*jsize4]*lhsp[3+j*jsize4];
             lhsp[3+j1*jsize4] = lhsp[3+j1*jsize4] -
             		 lhsp[1+j1*jsize4]*lhsp[4+j*jsize4];
             rhs[m+i*isize1+j1*jsize1+k*ksize1]   = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		 lhsp[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
	     m = 4;
             fac1	= 1./lhsm[2+j*jsize4];
             lhsm[3+j*jsize4]  = fac1*lhsm[3+j*jsize4];
             lhsm[4+j*jsize4]  = fac1*lhsm[4+j*jsize4];
             rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             lhsm[2+j1*jsize4] = lhsm[2+j1*jsize4] -
             		 lhsm[1+j1*jsize4]*lhsm[3+j*jsize4];
             lhsm[3+j1*jsize4] = lhsm[3+j1*jsize4] -
             		 lhsm[1+j1*jsize4]*lhsm[4+j*jsize4];
             rhs[m+i*isize1+j1*jsize1+k*ksize1]   = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		 lhsm[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
//---------------------------------------------------------------------
//               Scale the last row immediately 
//---------------------------------------------------------------------
             rhs[3+i*isize1+j1*jsize1+k*ksize1]   = rhs[3+i*isize1+j1*jsize1+k*ksize1]/lhsp[2+j1*jsize4];
             rhs[4+i*isize1+j1*jsize1+k*ksize1]   = rhs[4+i*isize1+j1*jsize1+k*ksize1]/lhsm[2+j1*jsize4];

//---------------------------------------------------------------------
//                         BACKSUBSTITUTION 
//---------------------------------------------------------------------

             j  = grid_points[1]-2;
             j1 = grid_points[1]-1;
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] -
                                 lhs[3+j*jsize4]*rhs[m+i*isize1+j1*jsize1+k*ksize1];
             }

             rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] -
                                 lhsp[3+j*jsize4]*rhs[3+i*isize1+j1*jsize1+k*ksize1];
             rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] -
                                 lhsm[3+j*jsize4]*rhs[4+i*isize1+j1*jsize1+k*ksize1];

//---------------------------------------------------------------------
//      The first three factors
//---------------------------------------------------------------------
             for(j=grid_points[1]-3;j>=0;j--){
                j1 = j  + 1;
                j2 = j  + 2;
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - 
                                lhs[3+j*jsize4]*rhs[m+i*isize1+j1*jsize1+k*ksize1] -
                                lhs[4+j*jsize4]*rhs[m+i*isize1+j2*jsize1+k*ksize1];
                }

//---------------------------------------------------------------------
//      And the remaining two
//---------------------------------------------------------------------
                rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] - 
                                lhsp[3+j*jsize4]*rhs[3+i*isize1+j1*jsize1+k*ksize1] -
                                lhsp[4+j*jsize4]*rhs[3+i*isize1+j2*jsize1+k*ksize1];
                rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] - 
                                lhsm[3+j*jsize4]*rhs[4+i*isize1+j1*jsize1+k*ksize1] -
                                lhsm[4+j*jsize4]*rhs[4+i*isize1+j2*jsize1+k*ksize1];
             }
          }
       }

      break;
    case 2:

       for(k=lower_bound;k<=upper_bound;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){

                r1 = rhs[0+i*isize1+j*jsize1+k*ksize1];
                r2 = rhs[1+i*isize1+j*jsize1+k*ksize1];
                r3 = rhs[2+i*isize1+j*jsize1+k*ksize1];
                r4 = rhs[3+i*isize1+j*jsize1+k*ksize1];
                r5 = rhs[4+i*isize1+j*jsize1+k*ksize1];

                t1 = bt * r1;
                t2 = 0.5 * ( r4 + r5 );

                rhs[0+i*isize1+j*jsize1+k*ksize1] =  bt * ( r4 - r5 );
                rhs[1+i*isize1+j*jsize1+k*ksize1] = -r3;
                rhs[2+i*isize1+j*jsize1+k*ksize1] =  r2;
                rhs[3+i*isize1+j*jsize1+k*ksize1] = -t1 + t2;
                rhs[4+i*isize1+j*jsize1+k*ksize1] =  t1 + t2;
             }
          }
       }
      break;      
    }
    state++;
    if(state==3)state=1;
  }

  public void lhsinit(int size){
           int i, n;

//---------------------------------------------------------------------
//     zap the whole left hand side for starters
//---------------------------------------------------------------------
       for(i=0;i<=size;i+=size){
          for(n=0;n<=4;n++){
             lhs[n+i*jsize4] = 0.0;
             lhsp[n+i*jsize4] = 0.0;
             lhsm[n+i*jsize4] = 0.0;
          }
       }

//---------------------------------------------------------------------
//      next, set all diagonal values to 1. This is overkill, but 
//      convenient
//---------------------------------------------------------------------
       for(i=0;i<=size;i+=size){
          lhs[2+i*jsize4] = 1.0;
          lhsp[2+i*jsize4] = 1.0;
          lhsm[2+i*jsize4] = 1.0;
       }
  }
}
