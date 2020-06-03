/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               S P B a s e                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    SPbase implements base class for SP benchmark.                       !
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
import discourje.examples.npb3.impl.Timer;
import discourje.examples.npb3.impl.SP;

public class SPBase extends Thread{

  public static final String BMName="SP";
  public char CLASS = 'S';

  protected int IMAX=0, JMAX=0, KMAX=0, 
                problem_size=0, nx2=0, ny2=0, nz2=0;
  protected int grid_points[] = {0,0,0};
  protected int niter_default=0;
  protected double dt_default=0.0;
  
  protected double u[], rhs[], forcing[];
  protected int isize1, jsize1, ksize1;

  protected double us[], vs[], ws[], qs[], 
                   rho_i[], speed[], square[];
  protected int jsize2, ksize2;
  
  protected double ue[], buf[];
  protected int jsize3;

  protected double lhs[], lhsp[], lhsm[];
  protected int jsize4;

  protected double cv[], rhon[], rhos[], 
                   rhoq[], cuf[], q[]; 
 
  protected static double  tx1, tx2, tx3, ty1, 
          ty2, ty3, tz1, tz2, tz3, 
          dx1, dx2, dx3, dx4, dx5, dy1, dy2, dy3, dy4, 
          dy5, dz1, dz2, dz3, dz4, dz5, dssp, dt, 
          dxmax, dymax, dzmax, xxcon1, xxcon2, 
          xxcon3, xxcon4, xxcon5, dx1tx1, dx2tx1, dx3tx1,
          dx4tx1, dx5tx1, yycon1, yycon2, yycon3, yycon4,
          yycon5, dy1ty1, dy2ty1, dy3ty1, dy4ty1, dy5ty1,
          zzcon1, zzcon2, zzcon3, zzcon4, zzcon5, dz1tz1, 
          dz2tz1, dz3tz1, dz4tz1, dz5tz1, dnxm1, dnym1, 
          dnzm1, c1c2, c1c5, c3c4, c1345, conz1, c1, c2, 
          c3, c4, c5, c4dssp, c5dssp, dtdssp, dttx1, bt,
          dttx2, dtty1, dtty2, dttz1, dttz2, c2dttx1, 
          c2dtty1, c2dttz1, comz1, comz4, comz5, comz6, 
          c3c4tx3, c3c4ty3, c3c4tz3, c2iv, con43, con16;
  
  protected double ce[] ={
                2.0,1.0,2.0,2.0,5.0,
		0.0,0.0,2.0,2.0,4.0,
		0.0,0.0,0.0,0.0,3.0,
		4.0,0.0,0.0,0.0,2.0,
		5.0,1.0,0.0,0.0,0.1,
		3.0,2.0,2.0,2.0,0.4,
		0.5,3.0,3.0,3.0,0.3,
		0.02,0.01,0.04,0.03,0.05,
		0.01,0.03,0.03,0.05,0.04,
		0.03,0.02,0.05,0.04,0.03,
		0.5,0.4,0.3,0.2,0.1,
		0.4,0.3,0.5,0.1,0.3,
		0.3,0.5,0.4,0.3,0.2};

  public boolean timeron = false;
  public Timer timer = new Timer();
  public static final int t_total = 1, t_rhsx = 2,
                   t_rhsy = 3,t_rhsz = 4, t_rhs = 5,
                   t_xsolve = 6, t_ysolve = 7, t_zsolve = 8, t_rdis1 = 9, 
                   t_rdis2 = 10, t_txinvr = 11, t_pinvr = 12, t_ninvr = 13,
                   t_tzetar = 14, t_add = 15, t_last = 15;

  public SPBase(){}

  public SPBase(char clss, int np){ 
    CLASS = clss;
    num_threads = np;
    switch(clss){
    case 'S':
      IMAX=JMAX=KMAX=problem_size
          =grid_points[0]=grid_points[1]=grid_points[2]=12; 
      dt_default = .015;
      niter_default = 100;
      break;    
    case 'W':
      IMAX=JMAX=KMAX=problem_size
          =grid_points[0]=grid_points[1]=grid_points[2]=36; 
      dt_default = .0015;
      niter_default = 400;
      break;
    case 'A':
      IMAX=JMAX=KMAX=problem_size=grid_points[0]=
                     grid_points[1]=grid_points[2]=64; 
      dt_default = .0015;
      niter_default = 400;
      break;
    case 'B':
      IMAX=JMAX=KMAX=problem_size=grid_points[0]=
                     grid_points[1]=grid_points[2]=102; 
      dt_default = .001;
      niter_default = 400;
      break;
    case 'C':
      IMAX=JMAX=KMAX=problem_size=grid_points[0]=
                     grid_points[1]=grid_points[2]=162; 
      dt_default = .00067;
      niter_default = 400;
      break;
    }
    
    isize1 = 5;
    jsize1 = 5*(IMAX+1);
    ksize1 = 5*(IMAX+1)*(JMAX+1);
    u = new double[5*(IMAX+1)*(JMAX+1)*KMAX];
    rhs = new double[5*(IMAX+1)*(JMAX+1)*KMAX];
    forcing = new double[5*(IMAX+1)*(JMAX+1)*KMAX];
    
    jsize2 = (IMAX+1);
    ksize2 = (IMAX+1)*(JMAX+1);
    us =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    vs =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    ws =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    qs =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    rho_i =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    speed =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    square =  new double[(IMAX+1)*(JMAX+1)*KMAX];
    
    jsize3 = problem_size;
    ue = new double[problem_size*5];
    buf = new double[problem_size*5];
    
    jsize4 = 5;

    lhs = new double[5*(problem_size+1)];
    lhsp = new double[5*(problem_size+1)];
    lhsm = new double[5*(problem_size+1)];    
    
    cv = new double[problem_size];
    rhon = new double[problem_size];
    rhos = new double[problem_size];
    rhoq = new double[problem_size];
    cuf  = new double[problem_size];
    q = new double[problem_size];
  }
  
  protected Thread master=null;
  protected int num_threads;
  
  protected RHSCompute rhscomputer[];
  protected TXInverse txinverse[];
  protected XSolver xsolver[];
  protected YSolver ysolver[];
  protected ZSolver zsolver[];
  protected RHSAdder rhsadder[];

  public void setupThreads(SP sp){
    master = sp;
    if(num_threads>problem_size-2)
      num_threads=problem_size-2;

    int interval1[]=new int[num_threads];
    int interval2[]=new int[num_threads];    
    set_interval(problem_size, interval1);
    set_interval(problem_size-2, interval2);
    int partition1[][] = new int[interval1.length][2];
    int partition2[][] = new int[interval2.length][2];
    set_partition(0,interval1,partition1);
    set_partition(1,interval2,partition2);
  
    rhscomputer = new RHSCompute[num_threads];
    txinverse = new TXInverse[num_threads];
    xsolver = new XSolver[num_threads];
    ysolver = new YSolver[num_threads];
    zsolver = new ZSolver[num_threads];
    rhsadder = new RHSAdder[num_threads];

  // create and start threads   
    for(int ii=0;ii<num_threads;ii++){
      rhscomputer[ii] =  new RHSCompute(sp,partition1[ii][0],partition1[ii][1],
                                        partition2[ii][0],partition2[ii][1]);
      rhscomputer[ii].id=ii;
      rhscomputer[ii].start();

      xsolver[ii] = new XSolver(sp,partition2[ii][0],partition2[ii][1]);
      xsolver[ii].id=ii;
      xsolver[ii].start();

      txinverse[ii] = new TXInverse(sp,partition2[ii][0],partition2[ii][1]);
      txinverse[ii].id=ii;
      txinverse[ii].start();

      ysolver[ii] = new YSolver(sp,partition2[ii][0],partition2[ii][1]);
      ysolver[ii].id=ii;
      ysolver[ii].start();

      zsolver[ii] = new ZSolver(sp,partition2[ii][0],partition2[ii][1]);
      zsolver[ii].id=ii;
      zsolver[ii].start();

      rhsadder[ii] = new RHSAdder(sp,partition2[ii][0],partition2[ii][1]);
      rhsadder[ii].id=ii;
      rhsadder[ii].start();
    }    
  }
  
  public void set_interval(int problem_size, int interval[] ){
    interval[0]= problem_size/num_threads;
    for(int i=1;i<num_threads;i++) interval[i]=interval[0];
    int remainder = problem_size%num_threads;
    for(int i=0;i<remainder;i++) interval[i]++;
  }
  
  public void set_partition(int start, int interval[], int prtn[][]){
    prtn[0][0]=start;
    if(start==0) prtn[0][1]=interval[0]-1;
    else prtn[0][1]=interval[0];
    
    for(int i=1;i<interval.length;i++){
      prtn[i][0]=prtn[i-1][1]+1;
      prtn[i][1]=prtn[i-1][1]+interval[i];
    }
  }

  public double dmax1(double a, double b){
    if(a<b) return b; else return a;
  }

  public double dmax1(double a, double b, double c, double d ){
    return dmax1(dmax1(a,b), dmax1(c,d) );
  }

  public void checksum(double array[], int size, String arrayname, 
                       boolean stop){
    double sum = 0;
    for(int i=0; i<size; i++) sum += array[i];
    System.out.println("array:"+arrayname + " checksum is: " + sum);
    if(stop) System.exit(0);
  }

  public void exact_solution(double xi, double eta, double zeta, 
                             double dtemp[], int offset ){
    for(int m=0;m<=4;m++){
       dtemp[m + offset] =  ce[m+0*5] +
       xi*(ce[m+1*5] + xi*(ce[m+4*5] + xi*(ce[m+7*5] + xi*ce[m+10*5]))) +
       eta*(ce[m+2*5] + eta*(ce[m+5*5] + eta*(ce[m+8*5] + eta*ce[m+11*5])))+
       zeta*(ce[m+3*5] + zeta*(ce[m+6*5] + zeta*(ce[m+9*5] + 
       zeta*ce[m+12*5])));
    }			   
  }
 
  public void lhsinit(int size){
//---------------------------------------------------------------------
//     zap the whole left hand side for starters
//---------------------------------------------------------------------
    for(int i=0;i<=size;i+=size){
       for(int n=0;n<=4;n++){
    	  lhs[n+i*jsize4] = 0.0;
    	  lhsp[n+i*jsize4] = 0.0;
    	  lhsm[n+i*jsize4] = 0.0;
       }
    }
//---------------------------------------------------------------------
//      next, set all diagonal values to 1. This is overkill, but 
//      convenient
//---------------------------------------------------------------------
    for(int i=0;i<=size;i+=size){
       lhs[2+i*jsize4] = 1.0;
       lhsp[2+i*jsize4] = 1.0;
       lhsm[2+i*jsize4] = 1.0;
    }
  }
  
  public void initialize(){
    int i, j, k, m, ix, iy, iz;
    double  xi, eta, zeta, Pface[]=new double[5*3*2], Pxi, Peta, 
      Pzeta, temp[] = new double[5];
    
//---------------------------------------------------------------------
//  Later (in compute_rhs) we compute 1/u for every element. A few of 
//  the corner elements are not used, but it convenient (and faster) 
//  to compute the whole thing with a simple loop. Make sure those 
//  values are nonzero by initializing the whole thing here. 
//---------------------------------------------------------------------
      for(k=0;k<grid_points[2];k++){
         for(j=0;j<grid_points[1];j++){
            for(i=0;i<grid_points[0];i++){
               u[0+i*isize1+j*jsize1+k*ksize1] = 1.0;
               u[1+i*isize1+j*jsize1+k*ksize1] = 0.0;
               u[2+i*isize1+j*jsize1+k*ksize1] = 0.0;
               u[3+i*isize1+j*jsize1+k*ksize1] = 0.0;
               u[4+i*isize1+j*jsize1+k*ksize1] = 1.0;
            }
         }
      }

//---------------------------------------------------------------------
// first store the "interpolated" values everywhere on the grid    
//---------------------------------------------------------------------
          for(k=0;k<grid_points[2];k++){
             zeta = k * dnzm1;
             for(j=0;j<grid_points[1];j++){
                eta = j * dnym1;
                for(i=0;i<grid_points[0];i++){
                   xi = i * dnxm1;
                 
                   for(ix=0;ix<=1;ix++){
                      Pxi = ix;
                      exact_solution(Pxi, eta, zeta, 
                                          Pface, 0+0*5+ix*15 );
                   }
                   for(iy=0;iy<=1;iy++){
                      Peta = iy;
                      exact_solution(xi, Peta, zeta, 
                                          Pface, 0+1*5+iy*15);
                   }

                   for(iz=0;iz<=1;iz++){
                      Pzeta = iz;
                       exact_solution(xi, eta, Pzeta,   
                                          Pface, 0+2*5+iz*15);
                   }

                   for(m=0;m<=4;m++){
                      Pxi   = xi   * Pface[m+0*5+1*15] + 
                              (1.0-xi)   * Pface[m+0*5+0*15];
                      Peta  = eta  * Pface[m+1*5+1*15] + 
                              (1.0-eta)  * Pface[m+1*5+0*15];
                      Pzeta = zeta * Pface[m+2*5+1*15] + 
                              (1.0-zeta) * Pface[m+2*5+0*15];

                      u[m+i*isize1+j*jsize1+k*ksize1] = 
		                Pxi + Peta + Pzeta - 
                                Pxi*Peta - Pxi*Pzeta - Peta*Pzeta + 
                                Pxi*Peta*Pzeta;

                   }
                }
             }
          }

//---------------------------------------------------------------------
// now store the exact values on the boundaries        
//---------------------------------------------------------------------

//---------------------------------------------------------------------
// west face                                                  
//---------------------------------------------------------------------

       xi = 0.0;
       i  = 0;
       for(k=0;k<grid_points[2];k++){
          zeta = k * dnzm1;
          for(j=0;j<grid_points[1];j++){
             eta = j * dnym1;
              exact_solution(xi, eta, zeta, temp,0);
             for(m=0;m<=4;m++){
                u[m+i*isize1+j*jsize1+k*ksize1] = temp[m];
             }
          }
       }

//---------------------------------------------------------------------
// east face                                                      
//---------------------------------------------------------------------

       xi = 1.0;
       i  = grid_points[0]-1;
       for(k=0;k<grid_points[2];k++){
          zeta = k * dnzm1;
          for(j=0;j<grid_points[1];j++){
             eta = j * dnym1;
              exact_solution(xi, eta, zeta, temp, 0);
             for(m=0;m<=4;m++){
                u[m+i*isize1+j*jsize1+k*ksize1] = temp[m];
             }
          }
       }

//---------------------------------------------------------------------
// south face                                                 
//---------------------------------------------------------------------

       eta = 0.0;
       j   = 0;
       for(k=0;k<grid_points[2];k++){
          zeta = k * dnzm1;
          for(i=0;i<grid_points[0];i++){
             xi = i * dnxm1;
              exact_solution(xi, eta, zeta, temp, 0);
             for(m=0;m<=4;m++){
                u[m+i*isize1+j*jsize1+k*ksize1] = temp[m];
             }
          }
       }

//---------------------------------------------------------------------
// north face                                    
//---------------------------------------------------------------------

       eta = 1.0;
       j   = grid_points[1]-1;
       for(k=0;k<grid_points[2];k++){
          zeta = k * dnzm1;
          for(i=0;i<grid_points[0];i++){
             xi = i * dnxm1;
             exact_solution(xi, eta, zeta, temp,0);
             for(m=0;m<=4;m++){
               u[m+i*isize1+j*jsize1+k*ksize1] = temp[m];
             }
          }
       }

//---------------------------------------------------------------------
// bottom face                                       
//---------------------------------------------------------------------

       zeta = 0.0;
       k    = 0;
       for(i=0;i<grid_points[0];i++){
          xi = i *dnxm1;
          for(j=0;j<grid_points[1];j++){
             eta = j * dnym1;
              exact_solution(xi, eta, zeta, temp, 0);
             for(m=0;m<=4;m++){
                u[m+i*isize1+j*jsize1+k*ksize1] = temp[m];
             }
          }
       }

//---------------------------------------------------------------------
// top face     
//---------------------------------------------------------------------

       zeta = 1.0;
       k    = grid_points[2]-1;
       for(i=0;i<grid_points[0];i++){
          xi = i * dnxm1;
          for(j=0;j<grid_points[1];j++){
             eta = j * dnym1;
              exact_solution(xi, eta, zeta, temp, 0);
             for(m=0;m<=4;m++){
                u[m+i*isize1+j*jsize1+k*ksize1] = temp[m];
             }
          }
       }
  }
  public void set_constants(int ndid){
    ce[0]=2.0*(1.0+((double)ndid)*0.01);
//    ce[0]=2.0;
    
    c1 = 1.4;
    c2 = 0.4;
    c3 = 0.1;
    c4 = 1.0;
    c5 = 1.4;

    bt = Math.sqrt(0.5);

    dnxm1 = 1.0 / (grid_points[0]-1);
    dnym1 = 1.0 / (grid_points[1]-1);
    dnzm1 = 1.0 / (grid_points[2]-1);

    c1c2 = c1 * c2;
    c1c5 = c1 * c5;
    c3c4 = c3 * c4;
    c1345 = c1c5 * c3c4;

    conz1 = (1.0-c1c5);

    tx1 = 1.0 / (dnxm1 * dnxm1);
    tx2 = 1.0 / (2.0 * dnxm1);
    tx3 = 1.0 / dnxm1;

    ty1 = 1.0 / (dnym1 * dnym1);
    ty2 = 1.0 / (2.0 * dnym1);
    ty3 = 1.0 / dnym1;
 
    tz1 = 1.0 / (dnzm1 * dnzm1);
    tz2 = 1.0 / (2.0 * dnzm1);
    tz3 = 1.0 / dnzm1;

    dx1 = 0.75;
    dx2 = 0.75;
    dx3 = 0.75;
    dx4 = 0.75;
    dx5 = 0.75;

    dy1 = 0.75;
    dy2 = 0.75;
    dy3 = 0.75;
    dy4 = 0.75;
    dy5 = 0.75;

    dz1 = 1.0;
    dz2 = 1.0;
    dz3 = 1.0;
    dz4 = 1.0;
    dz5 = 1.0;

    dxmax = dmax1(dx3, dx4);
    dymax = dmax1(dy2, dy4);
    dzmax = dmax1(dz2, dz3);

    dssp = 0.25 * dmax1(dx1, dmax1(dy1, dz1) );

    c4dssp = 4.0 * dssp;
    c5dssp = 5.0 * dssp;

    dttx1 = dt*tx1;
    dttx2 = dt*tx2;
    dtty1 = dt*ty1;
    dtty2 = dt*ty2;
    dttz1 = dt*tz1;
    dttz2 = dt*tz2;

    c2dttx1 = 2.0*dttx1;
    c2dtty1 = 2.0*dtty1;
    c2dttz1 = 2.0*dttz1;

    dtdssp = dt*dssp;

    comz1  = dtdssp;
    comz4  = 4.0*dtdssp;
    comz5  = 5.0*dtdssp;
    comz6  = 6.0*dtdssp;

    c3c4tx3 = c3c4*tx3;
    c3c4ty3 = c3c4*ty3;
    c3c4tz3 = c3c4*tz3;

    dx1tx1 = dx1*tx1;
    dx2tx1 = dx2*tx1;
    dx3tx1 = dx3*tx1;
    dx4tx1 = dx4*tx1;
    dx5tx1 = dx5*tx1;
     
    dy1ty1 = dy1*ty1;
    dy2ty1 = dy2*ty1;
    dy3ty1 = dy3*ty1;
    dy4ty1 = dy4*ty1;
    dy5ty1 = dy5*ty1;
     
    dz1tz1 = dz1*tz1;
    dz2tz1 = dz2*tz1;
    dz3tz1 = dz3*tz1;
    dz4tz1 = dz4*tz1;
    dz5tz1 = dz5*tz1;

    c2iv  = 2.5;
    con43 = 4.0/3.0;
    con16 = 1.0/6.0;
     
    xxcon1 = c3c4tx3*con43*tx3;
    xxcon2 = c3c4tx3*tx3;
    xxcon3 = c3c4tx3*conz1*tx3;
    xxcon4 = c3c4tx3*con16*tx3;
    xxcon5 = c3c4tx3*c1c5*tx3;

    yycon1 = c3c4ty3*con43*ty3;
    yycon2 = c3c4ty3*ty3;
    yycon3 = c3c4ty3*conz1*ty3;
    yycon4 = c3c4ty3*con16*ty3;
    yycon5 = c3c4ty3*c1c5*ty3;

    zzcon1 = c3c4tz3*con43*tz3;
    zzcon2 = c3c4tz3*tz3;
    zzcon3 = c3c4tz3*conz1*tz3;
    zzcon4 = c3c4tz3*con16*tz3;
    zzcon5 = c3c4tz3*c1c5*tz3;
  }
}






