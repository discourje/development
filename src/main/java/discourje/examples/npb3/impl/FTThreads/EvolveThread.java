/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                        E v o l v e T h r e a d                          !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    EvolveThread implements array evolving thread for FT benchmark.      !
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
! Translation to Java and MultiThreaded Code	 			  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.FTThreads;
import discourje.core.AsyncJ;
import discourje.examples.npb3.impl.DoneMessage;
import discourje.examples.npb3.impl.ExitMessage;
import discourje.examples.npb3.impl.FT;

public class EvolveThread extends FTBase{
  public int kt=0;
  public int id;
  public boolean done = true;
  
  int lower_bound1,upper_bound1; 
  double xtr[],xnt[]; 
  int ixnt,jxnt,kxnt;
  int ixtr,jxtr,kxtr;
  
  static final double ap =  (- 4.0 * alpha * pi*pi );

  public AsyncJ.Channel in;
  public AsyncJ.Channel out;

  public EvolveThread(FT ft,int low1, int high1, AsyncJ.Channel in, AsyncJ.Channel out){
    Init(ft);
    lower_bound1=low1;
    upper_bound1=high1;
    setPriority(Thread.MAX_PRIORITY);
    setDaemon(true);
    master = ft;
    this.in = in;
    this.out = out;
  }
  void Init(FT ft){
    //initialize shared data
    xtr=ft.xtr;
    xnt=ft.xnt;
    
    nx=ft.nx;
    ny=ft.ny;
    nz=ft.nz;
    
    ixtr=ft.isize3;
    jxtr=ft.jsize3;
    kxtr=ft.ksize3;
    ixnt=ft.isize4;
    jxnt=ft.jsize4;
    kxnt=ft.ksize4;
  }

  public void run(){    
    for(;;){
//      synchronized(this){
//        while(done==true){
//  	  try{
//	     wait();
//             synchronized(master){master.notify();}
//          }catch(InterruptedException ie){}
//        }

        var o = in.receive();
        if (o instanceof EvolveMessage) {
          var m = (EvolveMessage) o;
          kt = m.kt;
          step();
          out.send(new DoneMessage());
        }
        if (o instanceof ExitMessage) {
          out.send(new DoneMessage());
          return;
        }

//        synchronized(master){done=true;master.notify();}
//      }
    }
  }

  public void step(){
    for(int i=lower_bound1;i<=upper_bound1;i++){
      int ii = i-(i/(nx/2))*nx;
      int ii2 = ii*ii;
      for(int k=0;k<nz;k++){
	int kk = k-(k/(nz/2))*nz;
	int ik2 = ii2 + kk*kk;
	for(int j=0;j<ny;j++){
	  int jj = j-(j/(ny/2))*ny;
	  double lexp=Math.exp((ap*(jj*jj + ik2))*(kt+1));
	  int xntidx=j*ixnt+k*jxnt+i*kxnt;
	  int xtridx=j*ixtr+i*jxtr+k*kxtr;
	  xnt[REAL+xntidx] = lexp*xtr[REAL+xtridx]; 
	  xnt[IMAG+xntidx] = lexp*xtr[IMAG+xtridx];
	}
      }
    }   
  }
}
