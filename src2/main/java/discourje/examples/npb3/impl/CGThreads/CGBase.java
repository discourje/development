/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               C G B a s e                               !
!									  !
!-------------------------------------------------------------------------!
!									  !
!    CGbase implements base class for CG benchmark.                       !
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
package discourje.examples.npb3.impl.CGThreads;

import discourje.core.AsyncJ;
import discourje.core.SpecJ;
import discourje.examples.npb3.impl.CG;
import discourje.examples.npb3.impl.Timer;

public class CGBase extends Thread{
  public static final String BMName="CG";
  public static final int cgitmax=25;
  public char CLASS = 'S';

  public int na, nonzer, niter;
  public double shift,rcond,zeta_verify_value;
  public int nz, naa, nzz;
  public int firstrow, lastrow, firstcol, lastcol;

  public int colidx[], rowstr[],iv[], arow[], acol[];
  public double v[],aelt[],a[],x[],z[],p[],q[],r[];
  public double alpha,beta;

  public String t_names[];
  public double timer_read;
  public boolean timeron;
  public static final int t_init=1, t_bench=2,
                          t_conj_grad=3, t_last=3;
  public static Timer timer;

  public CGWorker worker[];
  public CG master;
  public int num_threads;
  public double dmaster[],rhomaster[],rnormmaster[];

  public CGBase(){};

  public CGBase(char clss, int np, boolean serial){
    CLASS=clss;
    num_threads = np;
    switch(CLASS){
    case 'S':
    	na=1400;
    	nonzer=7;
    	shift=10;
    	niter=15;
    	rcond=.1;
    	zeta_verify_value = 8.5971775078648;
    	break;
    case 'W':
    	na=7000;
    	nonzer=8;
    	shift=12;
    	niter=15;
    	rcond=.1;
    	zeta_verify_value = 10.362595087124;
    	break;
    case 'A':
    	na=14000;
    	nonzer=11;
    	shift=20;
    	niter=15;
    	rcond=.1;
    	zeta_verify_value = 17.130235054029;
    	break;
    case 'B':
    	na=75000;
    	nonzer=13;
    	shift=60;
    	niter=75;
    	rcond=.1;
    	zeta_verify_value = 22.712745482631;
    	break;
    case 'C':
    	na=150000;
    	nonzer=15;
    	shift=110;
    	niter=75;
    	rcond=.1;
    	zeta_verify_value = 28.973605592845;
    	break;
    }
    t_names = new String[t_last+1];
    timer = new Timer();

    nz = (na*(nonzer+1)*(nonzer+1)+ na*(nonzer+2) );
    colidx = new int[nz +1];
    rowstr = new int[na+2];
    iv = new int[2*na+2];
    arow = new int[nz+1];
    acol = new int[nz+1];
    v = new double[na+2];
    aelt = new double[nz+1];
    a = new double[nz+1];
    p = new double[na+3];
    q = new double[na+3];
    r = new double[na+3];
    x = new double[na+3];
    z = new double[na+3];
  }
  public void setupThreads(CG cg){
    worker = new CGWorker[num_threads];
    master = cg;

    var m = AsyncJ.dcj() ? AsyncJ.monitor(SpecJ.session("::cg", new Object[]{num_threads})) : null;

    int div = na/num_threads;
    int rem = na%num_threads;
    int start=1,end=0;

    dmaster = new double[num_threads];
    rhomaster = new double[num_threads];
    rnormmaster = new double[num_threads];
    for(int i=0;i<num_threads;i++){
      end += div;
      if(rem!=0){
        rem--;
        end++;
      }
      worker[i] = new CGWorker(cg, start, end,
              AsyncJ.channel(1, SpecJ.role("::master"), SpecJ.role("::worker", i), m),
              AsyncJ.channel(1, SpecJ.role("::worker", i), SpecJ.role("::master"), m));
      worker[i].id=i;
      start=end+1;
    }
    for(int i=0;i<num_threads;i++) worker[i].start();
  }
}
