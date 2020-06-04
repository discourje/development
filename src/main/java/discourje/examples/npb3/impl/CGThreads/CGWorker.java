/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                           C G W o r k e r                               !
!									  !
!-------------------------------------------------------------------------!
!									  !
!    CGworker implements thread for sparse subroutine of CG benchmark.    !
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
! Translation to Java and to MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.CGThreads;
import discourje.core.AsyncJ;
import discourje.examples.npb3.impl.CG;
import discourje.examples.npb3.impl.DoneMessage;
import discourje.examples.npb3.impl.ExitMessage;

public class CGWorker extends CGBase{
  public boolean done = true;
  public int id;
  public int TaskOrder;
  
  int start1, end1;
  public double alpha,beta;
  public AsyncJ.Channel in, out;
  public CGWorker(CG cg,int st,int end, AsyncJ.Channel in, AsyncJ.Channel out){
    Init(cg);
    start1=st;
    end1=end;
    done = true;
    setDaemon(true);
    setPriority(Thread.MAX_PRIORITY);
    master=cg;
    this.in = in;
    this.out = out;
  }
  void Init(CG cg){
    //initialize shared data
    dmaster = cg.dmaster;
    rhomaster = cg.rhomaster;
    rnormmaster = cg.rnormmaster;
    colidx = cg.colidx;
    rowstr = cg.rowstr;
    a = cg.a;
    p = cg.p;
    q = cg.q;
    r = cg.r;
    x = cg.x;
    z = cg.z;
  }
  
  public void run(){
    int state=0;
    for(;;){
//      synchronized(this){
//        while(done){
//          try{
//             wait();
//             synchronized(master){
//	       master.notify();
////	       alpha=master.alpha;
////	       beta=master.beta;
//	     }
//          }catch(InterruptedException ie){}
//        }

        var o = in.receive();
        if (o instanceof CGMessage) {
            var m = (CGMessage) o;
            TaskOrder = m.OrderNum;
            alpha = m.alpha;
            beta = m.beta;
            switch (TaskOrder) {
                case 0:
                    step0();
                    break;
                case 1:
                    step1();
                    break;
                case 2:
                    step2();
                    break;
                case 3:
                    step3();
                    break;
                case 4:
                    endWork();
                    break;
            }
            out.send(new DoneMessage());
        }
        if (o instanceof ExitMessage) {
            out.send(new DoneMessage());
            return;
        }

//        synchronized(master){
//  	  done=true;master.notify();
//        }
//      }
    }
  }
  void step0(){
    for(int j=start1;j<=end1;j++){
      double sum = 0.0;
      for(int k=rowstr[j];k<rowstr[j+1];k++){
  	sum = sum + a[k]*p[colidx[k]];
      }
      q[j] = sum;
    }	
    double sum = 0.0;
    for(int j=start1;j<=end1;j++) sum += p[j]*q[j];
    dmaster[id]=sum;
  }
  void step1(){
    for(int j=start1;j<=end1;j++){
      z[j] = z[j] + alpha*p[j];
      r[j] = r[j] - alpha*q[j];
    }	    
//---------------------------------------------------------------------
//  rho = r.r
//  Now, obtain the norm of r: First, sum squares of r elements locally...
//---------------------------------------------------------------------
    double rho = 0.0;
    for(int j=start1;j<=end1;j++) rho += r[j]*r[j];
    rhomaster[id]=rho;
  }
  void step2(){
    for(int j=start1;j<=end1;j++) p[j]=r[j]+beta*p[j];
  }
  void step3(){
    double rho = 0.0;
    for(int j=start1;j<=end1;j++){
      q[j] = 0.0;
      z[j] = 0.0;
      r[j] = x[j];
      p[j] = x[j];
      rho += x[j]*x[j];
    }		 
    rhomaster[id]=rho;
  }

  void endWork(){
//---------------------------------------------------------------------
//  Compute residual norm explicitly:  ||r|| = ||x - A.z||
//  First, form A.z
//  The partition submatrix-vector multiply
//---------------------------------------------------------------------
    for(int j=start1;j<=end1;j++){
      double sum = 0.0;
      for(int k=rowstr[j];k<=rowstr[j+1]-1;k++){
  	sum += a[k]*z[colidx[k]];
      }
      r[j] = sum;
    }	    
//---------------------------------------------------------------------
//  At this point, r contains A.z
//---------------------------------------------------------------------
    double sum = 0.0;
    for(int j=start1;j<=end1;j++) sum+=(x[j]-r[j])*(x[j]-r[j]);      
    rnormmaster[id]=sum;
  }
}
