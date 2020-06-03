/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               L U B a s e                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    LUbase implements base class for LU benchmark.                       !
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
package discourje.examples.npb3.impl.LUThreads;
import discourje.examples.npb3.impl.Timer;
import discourje.examples.npb3.impl.LU;

public class LUBase extends Thread{

  public static final String BMName="LU";
  public char CLASS = 'S';
  
  protected int isiz1, isiz2, isiz3;
  protected int itmax_default, inorm_default;
  protected double dt_default;
  protected int ipr_default = 1;
  protected static final double 
                   omega_default = 1.2,tolrsd1_def=.00000001, 
                   tolrsd2_def=.00000001, tolrsd3_def=.00000001, 
                   tolrsd4_def=.00000001, tolrsd5_def=.00000001,
		   c1 = 1.4, c2 = 0.4, c3 = .1, c4 = 1, c5 = 1.4;
//---------------------------------------------------------------------
//   grid
//---------------------------------------------------------------------
  protected int nx, ny, nz;
  protected int nx0, ny0, nz0;
  protected int ist, iend;
  protected int jst, jend;
  protected int ii1, ii2;
  protected int ji1, ji2;
  protected int ki1, ki2;
  protected double    dxi, deta, dzeta;
  protected double    tx1, tx2, tx3;
  protected double    ty1, ty2, ty3;
  protected double    tz1, tz2, tz3;

//---------------------------------------------------------------------
//   dissipation
//---------------------------------------------------------------------
  protected double   dx1, dx2, dx3, dx4, dx5;
  protected double   dy1, dy2, dy3, dy4, dy5;
  protected double   dz1, dz2, dz3, dz4, dz5;
  protected double   dssp;

//---------------------------------------------------------------------
//   field variables and residuals
//   to improve cache performance, second two dimensions padded by 1 
//   for even number sizes only.
//   Note: corresponding array (called "v") in routines blts, buts, 
//   and l2norm are similarly padded
//---------------------------------------------------------------------

  protected double u[], rsd[], frct[];
  protected int isize1, jsize1, ksize1; 

  protected double flux[]; 
  protected int isize2;

  protected double qs[], rho_i[];
  protected int jsize3, ksize3;
//---------------------------------------------------------------------
//   output control parameters
//---------------------------------------------------------------------
  protected static int ipr, inorm;

//---------------------------------------------------------------------
//   newton-raphson iteration control parameters
//---------------------------------------------------------------------
  protected int itmax;
  protected double dt, omega, frc, ttotal,
            tolrsd[] = new double[5],rsdnm[] = new double[5], 
            errnm[] = new double[5];

  protected double   a[], b[], c[], d[];
  protected int isize4, jsize4, ksize4;
//---------------------------------------------------------------------
//   coefficients of the exact solution
//---------------------------------------------------------------------
  protected static double ce[] ={ 
                 2.0, 1.0, 2.0, 2.0, 5.0,
    		 0.0, 0.0, 2.0, 2.0, 4.0,
    		 0.0, 0.0, 0.0, 0.0, 3.0,
    		 4.0, 0.0, 0.0, 0.0, 2.0,
    		 5.0, 1.0, 0.0, 0.0, .1 ,
    		 3.0, 2.0, 2.0, 2.0, .4 ,
    		 .5 , 3.0, 3.0, 3.0, .3 ,
    		 .02, .01, .04, .03, .05,
    		 .01, .03, .03, .05, .04,
    		 .03, .02, .05, .04, .03,
    		 .5 , .4 , .3 , .2 , .1 ,
    		 .4 , .3 , .5 , .1 , .3 ,
    		 .3 , .5 , .4 , .3 , .2
  	       };
//---------------------------------------------------------------------
//   timers
//---------------------------------------------------------------------
  public static final int t_total = 1, t_rhsx = 2,  t_rhsy = 3, t_rhsz = 4,
               t_rhs = 5, t_jacld = 6, t_blts = 7,t_jacu = 8,
               t_buts = 9, t_add = 10, t_l2norm = 11,t_last = 11;
  public boolean timeron;
  public Timer timer = new Timer();

  public LUBase(){};

  public LUBase(char cls, int np){
    CLASS=cls;
    num_threads = np;
   switch(cls){
    case 'S':
      isiz1=isiz2=isiz3=12;
      itmax_default=inorm_default=50;
      dt_default=.5;
      break;
    case 'W':
      isiz1=isiz2=isiz3=33;
      itmax_default=inorm_default=300;
      dt_default=.0015;
      break;
    case 'A':
      isiz1=isiz2=isiz3=64;
      itmax_default=inorm_default=250;
      dt_default=2;
      break;
    case 'B':
      isiz1=isiz2=isiz3=102;
      itmax_default=inorm_default=250;
      dt_default=2;
      break;
    case 'C':
      isiz1=isiz2=isiz3=162;
      itmax_default=inorm_default=250;
      dt_default=2;
      break;
    }

    u = new double[5*(isiz1/2*2+1)*(isiz2/2*2+1)*isiz3];
    rsd = new double[5*(isiz1/2*2+1)*(isiz2/2*2+1)*isiz3];
    frct = new double[5*(isiz1/2*2+1)*(isiz2/2*2+1)*isiz3];
    isize1=5;
    jsize1=5*(isiz1/2*2+1);
    ksize1=5*(isiz1/2*2+1)*(isiz2/2*2+1);
    
    flux= new double[5*isiz1];
    isize2=5;

    qs = new double[(isiz1/2*2+1)*(isiz2/2*2+1)*isiz3];
    rho_i = new double[(isiz1/2*2+1)*(isiz2/2*2+1)*isiz3];
    jsize3 = (isiz1/2*2+1);
    ksize3 = (isiz1/2*2+1)*(isiz2/2*2+1);

    a = new double[5*5*(isiz1/2*2+1)*(isiz2)]; 
    b = new double[5*5*(isiz1/2*2+1)*(isiz2)];
    c = new double[5*5*(isiz1/2*2+1)*(isiz2)]; 
    d = new double[5*5*(isiz1/2*2+1)*(isiz2)];

    isize4=5;
    jsize4=5*5;
    ksize4=5*5*(isiz1/2*2+1);
  }
  
  protected Thread master=null;
  protected int num_threads;
  protected RHSCompute rhscomputer[] = null;
  protected Scale scaler[] = null;
  protected Adder adder[] =null;
  protected LowerJac lowerjac[] =null;
  protected UpperJac upperjac[] =null;
  
  public void setupThreads(LU lu){
    master = lu;
    if(num_threads>isiz1-2)
      num_threads=isiz1-2;
    int interval1[]=new int[num_threads];
    int interval2[]=new int[num_threads];    
    set_interval(num_threads, isiz1, interval1);
    set_interval(num_threads, isiz1-2, interval2);
    int partition1[][] = new int[interval1.length][2];
    int partition2[][] = new int[interval2.length][2];
    set_partition(0,interval1,partition1);
    set_partition(1,interval2,partition2);
   
    rhscomputer = new RHSCompute[num_threads];
    scaler = new Scale[num_threads];
    adder = new Adder[num_threads];
    lowerjac = new LowerJac[num_threads];
    upperjac = new UpperJac[num_threads];
    for(int ii=0;ii<num_threads;ii++){
      rhscomputer[ii] =  new RHSCompute(lu,partition1[ii][0],partition1[ii][1],
					partition2[ii][0],partition2[ii][1]);
      rhscomputer[ii].id=ii;
      rhscomputer[ii].start();

      scaler[ii] =  new Scale(lu,partition2[ii][0],partition2[ii][1]);
      scaler[ii].id=ii;
      scaler[ii].start();

      adder[ii] =  new Adder(lu,partition2[ii][0],partition2[ii][1]);
      adder[ii].id=ii;
      adder[ii].start();

      lowerjac[ii] =  new LowerJac(lu,partition2[ii][0],partition2[ii][1]);
      lowerjac[ii].id=ii;
      lowerjac[ii].neighbor=lowerjac;
      lowerjac[ii].start();

      upperjac[ii] =  new UpperJac(lu,partition2[num_threads-ii-1][0],
                                      partition2[num_threads-ii-1][1]);
      upperjac[ii].id=ii;
      upperjac[ii].neighbor=upperjac;
      upperjac[ii].start();
    }
  }

  public void checksum(double array[], int size, 
                       String arrayname, boolean stop){
    double sum = 0;
    for(int i=0; i<size; i++) sum += array[i];
    System.out.println("array:"+arrayname + " checksum is: " + sum);
    if(stop)System.exit(0);
  }
  
  public void set_interval(int threads, int problem_size, int interval[] ){
    interval[0]= problem_size/threads;
    for(int i=1;i<threads;i++)
      interval[i]=interval[0];
    int remainder = problem_size%threads;
    for(int i=0;i<remainder;i++)
      interval[i]++;
  } 
  
  public void set_partition(int start, int interval[], int array[][]){
    array[0][0]=start;
    if(start==0) array[0][1]=interval[0]-1;
    else array[0][1]=interval[0];
    
    for(int i=1;i<interval.length;i++){
      array[i][0]=array[i-1][1]+1;
      array[i][1]=array[i-1][1]+interval[i];
    }
  }

  public void exact(double i, double j, double k, double u0[] ){
    double xi  =  (i-1) / ( nx0 - 1 );
    double eta  = (j-1) / ( ny0 - 1 );
    double zeta = (k-1) / ( nz  - 1 );

    for(int m=0;m<=4;m++){
      u0[m] =  ce[m+0*5]
              + (ce[m+1*5]
              + (ce[m+4*5]
              + (ce[m+7*5]
              +  ce[m+10*5] * xi) * xi) * xi) * xi
              + (ce[m+2*5]
              + (ce[m+5*5]
              + (ce[m+8*5]
              +  ce[m+11*5] * eta) * eta) * eta) * eta
              + (ce[m+3*5]
              + (ce[m+6*5]
              + (ce[m+9*5]
              +  ce[m+12*5] * zeta) * zeta) * zeta) * zeta;
      }
  }
  protected double max(double a, double b){
    if(a<b) return b;
    else return a;
  }
  protected double max(double a, double b, double c){
    return max( a, max( b , c) );
  }
}
