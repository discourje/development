/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               M G B a s e                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    MGBase implements base class for MG benchmark.                       !
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
! Translation to Java and MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.MGThreads;

import discourje.core.AsyncJ;
import discourje.core.SpecJ;
import discourje.examples.npb3.impl.MG;
import discourje.examples.npb3.impl.Timer;

public class MGBase extends Thread{
  public static final String BMName="MG";
  public char CLASS = 'S';
  
  public static final int maxlevel=11;
  public static int nit;

  public int nx_default, ny_default, nz_default;
  public int nit_default, lm, lt_default;
  public int ndim1, ndim2, ndim3;
  
  public int nm,nv,nr,nm2;
  public int nx[],ny[],nz[];
  public int ir[],m1[],m2[],m3[];
  public int lt,lb;
  
  public double u[],v[],r[],a[],c[];
  public int zoff,zsize3,zsize2,zsize1;
  public int uoff,usize1,usize2,usize3;
  public int roff,rsize1,rsize2,rsize3;
  
  protected static final int T_total=0, T_init=1, T_bench=2, T_mg3P=3,
  	            T_psinv=4, T_resid=5, T_resid2=6, T_rprj3=7,
  	            T_interp=8, T_norm2=9, T_last=9;
  public boolean timeron=false;
  public Timer timer = new Timer();

  public MGBase(){}
  public MGBase(char clss,int np,boolean serial){
    CLASS=clss;
    num_threads=np;
    nx = new int[maxlevel];
    ny = new int[maxlevel];
    nz = new int[maxlevel];
    ir = new int[maxlevel];
    m1 = new int[maxlevel];
    m2 = new int[maxlevel];
    m3 = new int[maxlevel];

    switch( CLASS ){
    case 'S':
      nx_default=32; 
      ny_default=32; 
      nz_default=32;
      nit_default=4; 
      lm=5; 
      lt_default=5;
      ndim1 = 5; 
      ndim2 = 5; 
      ndim3 = 5;
      lt=lt_default;
      nit = nit_default;
      nx[lt-1] = nx_default;
      ny[lt-1] = ny_default;
      nz[lt-1] = nz_default;
    break;
    case 'W':
      nx_default=64; 
      ny_default=64; 
      nz_default=64;
      nit_default=40; 
      lm=6; 
      lt_default=6;
      ndim1 = 6; 
      ndim2 = 6; 
      ndim3 = 6;
      lt=lt_default;
      nit = nit_default;
      nx[lt-1] = nx_default;
      ny[lt-1] = ny_default;
      nz[lt-1] = nz_default;
    break;
    case 'A':
      nx_default=256; 
      ny_default=256; 
      nz_default=256;
      nit_default=4; 
      lm=8; 
      lt_default=8;
      ndim1 = 8; 
      ndim2 = 8; 
      ndim3 = 8;
      lt=lt_default;
      nit = nit_default;
      nx[lt-1] = nx_default;
      ny[lt-1] = ny_default;
      nz[lt-1] = nz_default;
    break;
    case 'B':
      nx_default=256; 
      ny_default=256; 
      nz_default=256;
      nit_default=20; 
      lm=8; 
      lt_default=8;
      ndim1 = 8; 
      ndim2 = 8; 
      ndim3 = 8;
      lt=lt_default;
      nit = nit_default;
      nx[lt-1] = nx_default;
      ny[lt-1] = ny_default;
      nz[lt-1] = nz_default;
    break;
    case 'C':
      nx_default=512; 
      ny_default=512; 
      nz_default=512;
      nit_default=20; 
      lm=9; 
      lt_default=9;
      ndim1 = 9; 
      ndim2 = 9; 
      ndim3 = 9;
      lt=lt_default;
      nit = nit_default;
      nx[lt-1] = nx_default;
      ny[lt-1] = ny_default;
      nz[lt-1] = nz_default;
    break;
    }
    nm=2+(1<<lm);
    nv=(2+(1<<ndim1))*(2+(1<<ndim2))*(2+(1<<ndim3));
    nr=(8*(nv+nm*nm+5*nm+7*lm))/7;
    nm2=2*nm*nm;
	
//System.out.println(" Allocation of grids: nr="+nr+" words nv="+nv+" words");
//System.out.print(" r="+(nr*8/(1024*1024))+" MB...");
	r = new double[nr];
//System.out.println(" OK.");
//System.out.print(" v="+(nv*8/(1024*1024))+" MB...");
	v = new double[nv];
//System.out.println(" OK.");
//System.out.print(" u="+(nr*8/(1024*1024))+" MB...");
	u = new double[nr];
//System.out.println(" OK.");

	a = new double[4];
	c = new double[4];

        a[0] = -8.0/3.0; 
        a[1] =  0.0; 
        a[2] =  1.0/6.0; 
        a[3] =  1.0/12.0;
      
      if(CLASS=='A'||CLASS=='S'||CLASS=='W') {
//c---------------------------------------------------------------------
//c     Coefficients for the S(a) smoother
//c---------------------------------------------------------------------
         c[0] =  -3.0/8.0;
         c[1] =  +1.0/32.0;
         c[2] =  -1.0/64.0;
         c[3] =   0.0;
      }else{
//c---------------------------------------------------------------------
//c     Coefficients for the S(b) smoother
//c---------------------------------------------------------------------
         c[0] =  -3.0/17.0;
         c[1] =  +1.0/33.0;
         c[2] =  -1.0/61.0;
         c[3] =   0.0;
      }
    }

  public int num_threads=0;
  public int wstart,wend;    
  public Interp interp[];
  public Psinv psinv[];
  public Rprj rprj[];
  public Resid resid[];
  public Thread master;

  public void setupThreads(MG mg){
    interp = new Interp[num_threads];
    psinv = new Psinv[num_threads];
    rprj = new Rprj[num_threads];
    resid = new Resid[num_threads];

    var m = AsyncJ.dcj() ? AsyncJ.monitor(SpecJ.session("::mg", new Object[]{num_threads})) : null;

    for(int i=0;i<num_threads;i++){
      interp[i]=new Interp(mg,
              AsyncJ.channel(1, SpecJ.role("::master"), SpecJ.role("::interp", i), m),
              AsyncJ.channel(1, SpecJ.role("::interp", i), SpecJ.role("::master"), m));
      interp[i].id=i;
      interp[i].start();
      
      psinv[i]=new Psinv(mg,
              AsyncJ.channel(1, SpecJ.role("::master"), SpecJ.role("::psinv", i), m),
              AsyncJ.channel(1, SpecJ.role("::psinv", i), SpecJ.role("::master"), m));
      psinv[i].id=i;
      psinv[i].start();

      rprj[i]=new Rprj(mg,
              AsyncJ.channel(1, SpecJ.role("::master"), SpecJ.role("::rprj", i), m),
              AsyncJ.channel(1, SpecJ.role("::rprj", i), SpecJ.role("::master"), m));
      rprj[i].id=i;
      rprj[i].start();

      resid[i]=new Resid(mg,
              AsyncJ.channel(1, SpecJ.role("::master"), SpecJ.role("::resid", i), m),
              AsyncJ.channel(1, SpecJ.role("::resid", i), SpecJ.role("::master"), m));
      resid[i].id=i;
      resid[i].start();
    } 
  }
    public void checksum(int arr[], String name, boolean stop){
      double csum=0;
      for(int i=0;i<arr.length;i++) csum+=arr[i];
      System.out.println(name + " checksum MG " + csum);
      if(stop) System.exit(0);
    }  
    public double dmax1(double a,double b){if(a<b)return b;else return a;}
    
    public void comm3(double u[],int off,int n1,int n2,int n3){
//c---------------------------------------------------------------------
//c     comm3 organizes the communication on all borders 
//c---------------------------------------------------------------------
//      double precision u(n1,n2,n3)
      int i1, i2, i3;

      for(i3=1;i3<n3-1;i3++)
         for(i2=1;i2<n2-1;i2++){
            u[off+n1*(i2+n2*i3)] = u[off+n1-2+n1*(i2+n2*i3)];
            u[off+n1-1+n1*(i2+n2*i3)] = u[off+1+n1*(i2+n2*i3)];
         }

      for(i3=1;i3<n3-1;i3++)
         for(i1=0;i1<n1;i1++){
            u[off+i1+n1*n2*i3] = u[off+i1+n1*(n2-2+n2*i3)];
            u[off+i1+n1*(n2-1+n2*i3)] = u[off+i1+n1*(1+n2*i3)];
         }

      for(i2=0;i2<n2;i2++)
         for(i1=0;i1<n1;i1++){
            u[off+i1+n1*i2] = u[off+i1+n1*(i2+n2*(n3-2))];
            u[off+i1+n1*(i2+n2*(n3-1))] = u[off+i1+n1*(i2+n2)];
         }
   }
}
