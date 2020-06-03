/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                         R H S C o m p u t e                             !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    RHSCompute implements thread for rhs_compute subroutine of           !
!    the BT benchmark.                                                    !
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

public class RHSCompute extends BTBase{
  public int id;
  public boolean done = true;

  //private arrays and data
  int lower_bound1;
  int upper_bound1;
  int lower_bound2;
  int upper_bound2;
  int state;
  double rho_inv, uijk, up1, um1, vijk, vp1, vm1,wijk, wp1, wm1;

  public RHSCompute(BT bt,int low1, int high1, int low2, int high2){
    Init(bt);
    lower_bound1=low1;
    upper_bound1=high1;
    lower_bound2=low2;
    upper_bound2=high2;
    state=1;
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
    int i, j, k, m;   
    switch(state){
    case 1:
//---------------------------------------------------------------------
//     compute the reciprocal of density, and the kinetic energy,
//     and the speed of sound.
//---------------------------------------------------------------------
      for(k=lower_bound1;k<=upper_bound1;k++){
         for(j=0;j<=grid_points[1]-1;j++){
            for(i=0;i<=grid_points[0]-1;i++){
               rho_inv = 1.0/u[0+i*isize2+j*jsize2+k*ksize2];
               rho_i[i+j*jsize1+k*ksize1] = rho_inv;
               us[i+j*jsize1+k*ksize1] = u[1+i*isize2+j*jsize2+k*ksize2] * rho_inv;
               vs[i+j*jsize1+k*ksize1] = u[2+i*isize2+j*jsize2+k*ksize2] * rho_inv;
               ws[i+j*jsize1+k*ksize1] = u[3+i*isize2+j*jsize2+k*ksize2] * rho_inv;
               square[i+j*jsize1+k*ksize1]     = 0.5* (
                       u[1+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[2+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[3+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] ) * rho_inv;
               qs[i+j*jsize1+k*ksize1] = square[i+j*jsize1+k*ksize1] * rho_inv;
            }
         }
      }

//---------------------------------------------------------------------
// copy the exact forcing term to the right hand side;  because
// this forcing term is known, we can store it on the whole grid
// including the boundary
//---------------------------------------------------------------------

      for(k=lower_bound1;k<=upper_bound1;k++){
         for(j=0;j<=grid_points[1]-1;j++){
            for(i=0;i<=grid_points[0]-1;i++){
      	       for(m=0;m<=4;m++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2];
               }
            }
         }
      }
      
      break;
    case 2:
      
//---------------------------------------------------------------------
//     compute xi-direction fluxes
//---------------------------------------------------------------------
      for(k=lower_bound2;k<=upper_bound2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
               uijk = us[i+j*jsize1+k*ksize1];
               up1  = us[(i+1)+j*jsize1+k*ksize1];
               um1  = us[(i-1)+j*jsize1+k*ksize1];

               rhs[0+i*isize2+j*jsize2+k*ksize2] = rhs[0+i*isize2+j*jsize2+k*ksize2] + dx1tx1 *
                       (u[0+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[0+i*isize2+j*jsize2+k*ksize2] +
                       u[0+(i-1)*isize2+j*jsize2+k*ksize2]) -
                       tx2 * (u[1+(i+1)*isize2+j*jsize2+k*ksize2] - u[1+(i-1)*isize2+j*jsize2+k*ksize2]);

               rhs[1+i*isize2+j*jsize2+k*ksize2] = rhs[1+i*isize2+j*jsize2+k*ksize2] + dx2tx1 *
                       (u[1+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[1+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon2*con43 * (up1 - 2.0*uijk + um1) -
                       tx2 * (u[1+(i+1)*isize2+j*jsize2+k*ksize2]*up1 -
                       u[1+(i-1)*isize2+j*jsize2+k*ksize2]*um1 +
                       (u[4+(i+1)*isize2+j*jsize2+k*ksize2]- square[(i+1)+j*jsize1+k*ksize1]-
                       u[4+(i-1)*isize2+j*jsize2+k*ksize2]+ square[(i-1)+j*jsize1+k*ksize1])*
                       c2);

               rhs[2+i*isize2+j*jsize2+k*ksize2] = rhs[2+i*isize2+j*jsize2+k*ksize2] + dx3tx1 *
                       (u[2+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[2+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon2 * (vs[(i+1)+j*jsize1+k*ksize1] - 2.0*vs[i+j*jsize1+k*ksize1] +
                       vs[(i-1)+j*jsize1+k*ksize1]) -
                       tx2 * (u[2+(i+1)*isize2+j*jsize2+k*ksize2]*up1 -
                       u[2+(i-1)*isize2+j*jsize2+k*ksize2]*um1);

               rhs[3+i*isize2+j*jsize2+k*ksize2] = rhs[3+i*isize2+j*jsize2+k*ksize2] + dx4tx1 *
                       (u[3+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[3+i*isize2+j*jsize2+k*ksize2] +
                       u[3+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon2 * (ws[(i+1)+j*jsize1+k*ksize1] - 2.0*ws[i+j*jsize1+k*ksize1] +
                       ws[(i-1)+j*jsize1+k*ksize1]) -
                       tx2 * (u[3+(i+1)*isize2+j*jsize2+k*ksize2]*up1 -
                       u[3+(i-1)*isize2+j*jsize2+k*ksize2]*um1);

               rhs[4+i*isize2+j*jsize2+k*ksize2] = rhs[4+i*isize2+j*jsize2+k*ksize2] + dx5tx1 *
                       (u[4+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[4+i*isize2+j*jsize2+k*ksize2] +
                       u[4+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon3 * (qs[(i+1)+j*jsize1+k*ksize1] - 2.0*qs[i+j*jsize1+k*ksize1] +
                       qs[(i-1)+j*jsize1+k*ksize1]) +
                       xxcon4 * (up1*up1 -       2.0*uijk*uijk +
                       um1*um1) +
                       xxcon5 * (u[4+(i+1)*isize2+j*jsize2+k*ksize2]*rho_i[(i+1)+j*jsize1+k*ksize1] -
                       2.0*u[4+i*isize2+j*jsize2+k*ksize2]*rho_i[i+j*jsize1+k*ksize1] +
                       u[4+(i-1)*isize2+j*jsize2+k*ksize2]*rho_i[(i-1)+j*jsize1+k*ksize1]) -
                       tx2 * ( (c1*u[4+(i+1)*isize2+j*jsize2+k*ksize2] -
                       c2*square[(i+1)+j*jsize1+k*ksize1])*up1 -
                       (c1*u[4+(i-1)*isize2+j*jsize2+k*ksize2] -
                       c2*square[(i-1)+j*jsize1+k*ksize1])*um1 );
            }
         }

//---------------------------------------------------------------------
//     add fourth order xi-direction dissipation
//---------------------------------------------------------------------
         for(j=1;j<=grid_points[1]-2;j++){
            i = 1;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2]- dssp *
                          ( 5.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] +
                          u[m+(i+2)*isize2+j*jsize2+k*ksize2]);
            }

            i = 2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*u[m+(i-1)*isize2+j*jsize2+k*ksize2] + 6.0*u[m+i*isize2+j*jsize2+k*ksize2] -
                          4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] + u[m+(i+2)*isize2+j*jsize2+k*ksize2]);
            }

      	    for(m=0;m<=4;m++){
               for(i=3;i<=grid_points[0]-4;i++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (  u[m+(i-2)*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i-1)*isize2+j*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] +
                          u[m+(i+2)*isize2+j*jsize2+k*ksize2] );
               }
            }

            i = grid_points[0]-3;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+(i-2)*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i-1)*isize2+j*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] );
            }

            i = grid_points[0]-2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+(i-2)*isize2+j*jsize2+k*ksize2] - 4.*u[m+(i-1)*isize2+j*jsize2+k*ksize2] +
                          5.*u[m+i*isize2+j*jsize2+k*ksize2] );
            }
         }
      }
 
      break;
    case 3:
//---------------------------------------------------------------------
//     compute eta-direction fluxes
//---------------------------------------------------------------------
      for(k=lower_bound2;k<=upper_bound2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
               vijk = vs[i+j*jsize1+k*ksize1];
               vp1  = vs[i+(j+1)*jsize1+k*ksize1];
               vm1  = vs[i+(j-1)*jsize1+k*ksize1];
               rhs[0+i*isize2+j*jsize2+k*ksize2] = rhs[0+i*isize2+j*jsize2+k*ksize2] + dy1ty1 *
                       (u[0+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[0+i*isize2+j*jsize2+k*ksize2] +
                       u[0+i*isize2+(j-1)*jsize2+k*ksize2]) -
                       ty2 * (u[2+i*isize2+(j+1)*jsize2+k*ksize2] - u[2+i*isize2+(j-1)*jsize2+k*ksize2]);
               rhs[1+i*isize2+j*jsize2+k*ksize2] = rhs[1+i*isize2+j*jsize2+k*ksize2] + dy2ty1 *
                       (u[1+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[1+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon2 * (us[i+(j+1)*jsize1+k*ksize1] - 2.0*us[i+j*jsize1+k*ksize1] +
                       us[i+(j-1)*jsize1+k*ksize1]) -
                       ty2 * (u[1+i*isize2+(j+1)*jsize2+k*ksize2]*vp1 -
                       u[1+i*isize2+(j-1)*jsize2+k*ksize2]*vm1);
               rhs[2+i*isize2+j*jsize2+k*ksize2] = rhs[2+i*isize2+j*jsize2+k*ksize2] + dy3ty1 *
                       (u[2+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[2+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon2*con43 * (vp1 - 2.0*vijk + vm1) -
                       ty2 * (u[2+i*isize2+(j+1)*jsize2+k*ksize2]*vp1 -
                       u[2+i*isize2+(j-1)*jsize2+k*ksize2]*vm1 +
                       (u[4+i*isize2+(j+1)*jsize2+k*ksize2] - square[i+(j+1)*jsize1+k*ksize1] -
                       u[4+i*isize2+(j-1)*jsize2+k*ksize2] + square[i+(j-1)*jsize1+k*ksize1])
                       *c2);
               rhs[3+i*isize2+j*jsize2+k*ksize2] = rhs[3+i*isize2+j*jsize2+k*ksize2] + dy4ty1 *
                       (u[3+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[3+i*isize2+j*jsize2+k*ksize2] +
                       u[3+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon2 * (ws[i+(j+1)*jsize1+k*ksize1] - 2.0*ws[i+j*jsize1+k*ksize1] +
                       ws[i+(j-1)*jsize1+k*ksize1]) -
                       ty2 * (u[3+i*isize2+(j+1)*jsize2+k*ksize2]*vp1 -
                       u[3+i*isize2+(j-1)*jsize2+k*ksize2]*vm1);
               rhs[4+i*isize2+j*jsize2+k*ksize2] = rhs[4+i*isize2+j*jsize2+k*ksize2] + dy5ty1 *
                       (u[4+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[4+i*isize2+j*jsize2+k*ksize2] +
                       u[4+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon3 * (qs[i+(j+1)*jsize1+k*ksize1] - 2.0*qs[i+j*jsize1+k*ksize1] +
                       qs[i+(j-1)*jsize1+k*ksize1]) +
                       yycon4 * (vp1*vp1       - 2.0*vijk*vijk +
                       vm1*vm1) +
                       yycon5 * (u[4+i*isize2+(j+1)*jsize2+k*ksize2]*rho_i[i+(j+1)*jsize1+k*ksize1] -
                       2.0*u[4+i*isize2+j*jsize2+k*ksize2]*rho_i[i+j*jsize1+k*ksize1] +
                       u[4+i*isize2+(j-1)*jsize2+k*ksize2]*rho_i[i+(j-1)*jsize1+k*ksize1]) -
                       ty2 * ((c1*u[4+i*isize2+(j+1)*jsize2+k*ksize2] -
                       c2*square[i+(j+1)*jsize1+k*ksize1]) * vp1 -
                       (c1*u[4+i*isize2+(j-1)*jsize2+k*ksize2] -
                       c2*square[i+(j-1)*jsize1+k*ksize1]) * vm1);
            }
         }

//---------------------------------------------------------------------
//     add fourth order eta-direction dissipation
//---------------------------------------------------------------------
         for(i=1;i<=grid_points[0]-2;i++){
            j = 1;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2]- dssp *
                          ( 5.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] +
                          u[m+i*isize2+(j+2)*jsize2+k*ksize2]);
            }

            j = 2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*u[m+i*isize2+(j-1)*jsize2+k*ksize2] + 6.0*u[m+i*isize2+j*jsize2+k*ksize2] -
                          4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] + u[m+i*isize2+(j+2)*jsize2+k*ksize2]);
            }
         }

         for(j=3;j<=grid_points[1]-4;j++){
            for(i=1;i<=grid_points[0]-2;i++){
      	       for(m=0;m<=4;m++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (  u[m+i*isize2+(j-2)*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j-1)*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] +
                          u[m+i*isize2+(j+2)*jsize2+k*ksize2] );
               }
            }
         }

         for(i=1;i<=grid_points[0]-2;i++){
            j = grid_points[1]-3;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+(j-2)*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j-1)*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] );
            }

            j = grid_points[1]-2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+(j-2)*jsize2+k*ksize2] - 4.*u[m+i*isize2+(j-1)*jsize2+k*ksize2] +
                          5.*u[m+i*isize2+j*jsize2+k*ksize2] );
            }
         }
      }
     

      break;
    case 4:

//---------------------------------------------------------------------
//     compute zeta-direction fluxes
//---------------------------------------------------------------------
      for(j=lower_bound2;j<=upper_bound2;j++){
	for(k=1;k<=grid_points[1]-2;k++){
	  for(i=1;i<=grid_points[0]-2;i++){
	    wijk = ws[i+j*jsize1+k*ksize1];
	    wp1  = ws[i+j*jsize1+(k+1)*ksize1];
	    wm1  = ws[i+j*jsize1+(k-1)*ksize1];
	    
	    rhs[0+i*isize2+j*jsize2+k*ksize2] = rhs[0+i*isize2+j*jsize2+k*ksize2] + dz1tz1 *
                       (u[0+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[0+i*isize2+j*jsize2+k*ksize2] +
                       u[0+i*isize2+j*jsize2+(k-1)*ksize2]) -
                       tz2 * (u[3+i*isize2+j*jsize2+(k+1)*ksize2] - u[3+i*isize2+j*jsize2+(k-1)*ksize2]);
	    rhs[1+i*isize2+j*jsize2+k*ksize2] = rhs[1+i*isize2+j*jsize2+k*ksize2] + dz2tz1 *
                       (u[1+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[1+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon2 * (us[i+j*jsize1+(k+1)*ksize1] - 2.0*us[i+j*jsize1+k*ksize1] +
                       us[i+j*jsize1+(k-1)*ksize1]) -
                       tz2 * (u[1+i*isize2+j*jsize2+(k+1)*ksize2]*wp1 -
                       u[1+i*isize2+j*jsize2+(k-1)*ksize2]*wm1);
	    rhs[2+i*isize2+j*jsize2+k*ksize2] = rhs[2+i*isize2+j*jsize2+k*ksize2] + dz3tz1 *
                       (u[2+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[2+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon2 * (vs[i+j*jsize1+(k+1)*ksize1] - 2.0*vs[i+j*jsize1+k*ksize1] +
                       vs[i+j*jsize1+(k-1)*ksize1]) -
                       tz2 * (u[2+i*isize2+j*jsize2+(k+1)*ksize2]*wp1 -
                       u[2+i*isize2+j*jsize2+(k-1)*ksize2]*wm1);
	    rhs[3+i*isize2+j*jsize2+k*ksize2] = rhs[3+i*isize2+j*jsize2+k*ksize2] + dz4tz1 *
                       (u[3+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[3+i*isize2+j*jsize2+k*ksize2] +
                       u[3+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon2*con43 * (wp1 - 2.0*wijk + wm1) -
                       tz2 * (u[3+i*isize2+j*jsize2+(k+1)*ksize2]*wp1 -
                       u[3+i*isize2+j*jsize2+(k-1)*ksize2]*wm1 +
                       (u[4+i*isize2+j*jsize2+(k+1)*ksize2] - square[i+j*jsize1+(k+1)*ksize1] -
                       u[4+i*isize2+j*jsize2+(k-1)*ksize2] + square[i+j*jsize1+(k-1)*ksize1])
                       *c2);
	    rhs[4+i*isize2+j*jsize2+k*ksize2] = rhs[4+i*isize2+j*jsize2+k*ksize2] + dz5tz1 *
                       (u[4+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[4+i*isize2+j*jsize2+k*ksize2] +
                       u[4+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon3 * (qs[i+j*jsize1+(k+1)*ksize1] - 2.0*qs[i+j*jsize1+k*ksize1] +
                       qs[i+j*jsize1+(k-1)*ksize1]) +
                       zzcon4 * (wp1*wp1 - 2.0*wijk*wijk +
                       wm1*wm1) +
                       zzcon5 * (u[4+i*isize2+j*jsize2+(k+1)*ksize2]*rho_i[i+j*jsize1+(k+1)*ksize1] -
                       2.0*u[4+i*isize2+j*jsize2+k*ksize2]*rho_i[i+j*jsize1+k*ksize1] +
                       u[4+i*isize2+j*jsize2+(k-1)*ksize2]*rho_i[i+j*jsize1+(k-1)*ksize1]) -
                       tz2 * ( (c1*u[4+i*isize2+j*jsize2+(k+1)*ksize2] -
                       c2*square[i+j*jsize1+(k+1)*ksize1])*wp1 -
                       (c1*u[4+i*isize2+j*jsize2+(k-1)*ksize2] -
                       c2*square[i+j*jsize1+(k-1)*ksize1])*wm1);
	  }
	}

//---------------------------------------------------------------------
//     add fourth order zeta-direction dissipation
//---------------------------------------------------------------------

	for(i=1;i<=grid_points[0]-2;i++){
	  k = 1;
	  for(m=0;m<=4;m++){
	    rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2]- dssp *
                          ( 5.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] +
                          u[m+i*isize2+j*jsize2+(k+2)*ksize2]);
	  }

	  k = 2;
	  for(m=0;m<=4;m++){
	    rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*u[m+i*isize2+j*jsize2+(k-1)*ksize2] + 6.0*u[m+i*isize2+j*jsize2+k*ksize2] -
                          4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] + u[m+i*isize2+j*jsize2+(k+2)*ksize2]);
	  }
	}

	for(k=3;k<=grid_points[2]-4;k++){
	  for(i=1;i<=grid_points[0]-2;i++){
	    for(m=0;m<=4;m++){
	      rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (  u[m+i*isize2+j*jsize2+(k-2)*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k-1)*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] +
                          u[m+i*isize2+j*jsize2+(k+2)*ksize2] );
	    }
	  }
	}

	for(i=1;i<=grid_points[0]-2;i++){
	  k = grid_points[2]-3;
	  for(m=0;m<=4;m++){
	    rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+j*jsize2+(k-2)*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k-1)*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] );
	  }

	  k = grid_points[2]-2;
	  for(m=0;m<=4;m++){
	    rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+j*jsize2+(k-2)*ksize2] - 4.*u[m+i*isize2+j*jsize2+(k-1)*ksize2] +
                          5.0*u[m+i*isize2+j*jsize2+k*ksize2] );
	  }
	}
      }
      
      break;
    case 5:

      for(k=lower_bound2;k<=upper_bound2;k++){
	for(j=1;j<=grid_points[1]-2;j++){
	  for(i=1;i<=grid_points[0]-2;i++){
	    for(m=0;m<=4;m++){
	      rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] * dt;
	    }
	  }
	}
      }
      break;
    }
    state++;
    if(state==6)state=1;
  }

  public  void print_arrays(){
    double rhs_density=0,rhs_x_momentum=0, rhs_y_momentum=0,rhs_z_momentum=0,rhs_energy=0;
    for(int i=0;i<grid_points[2];i++){
      for(int j=0;j<grid_points[1];j++){
	for(int k=0;k<grid_points[0];k++){
	  rhs_density+=    u[0+i*isize2+j*jsize2+k*ksize2] + rhs[0+i*isize2+j*jsize2+k*ksize2];
	  rhs_x_momentum+= u[1+i*isize2+j*jsize2+k*ksize2] + rhs[1+i*isize2+j*jsize2+k*ksize2];
	  rhs_y_momentum+= u[2+i*isize2+j*jsize2+k*ksize2] + rhs[2+i*isize2+j*jsize2+k*ksize2];
	  rhs_z_momentum+= u[3+i*isize2+j*jsize2+k*ksize2] + rhs[3+i*isize2+j*jsize2+k*ksize2];
	  rhs_energy+=     u[4+i*isize2+j*jsize2+k*ksize2] + rhs[4+i*isize2+j*jsize2+k*ksize2];	 
	}
      }
    }
    System.out.println(" density: "+rhs_density);
    System.out.println(" x_momentum: "+rhs_x_momentum);
    System.out.println(" y_momentum: "+rhs_y_momentum);
    System.out.println(" z_momentum: "+rhs_z_momentum);
    System.out.println(" energy: "+rhs_energy);    
  }
}



