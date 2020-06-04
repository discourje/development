/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                           F F T T h r e a d                             !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    FFTThread implements a threaded version of FFT  for FT benchmark.    !
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

public class FFTThread extends FTBase{
  public int id;
  public boolean done = true;
  
  double x[];
  double exp1[];
  double exp2[];
  double exp3[];
  int n1,n2,n3;
  int lower_bound1,upper_bound1,lower_bound2,upper_bound2;
  int lb1,ub1,lb2,ub2;
  
  int state;
  int sign;
  double plane[];
  double scr[];

  public AsyncJ.Channel in;
  public AsyncJ.Channel out;

  public FFTThread(FT ft,int low1, int high1, int low2, int high2, AsyncJ.Channel in, AsyncJ.Channel out){
    Init(ft);
    state=1;
    lb1=low1;
    ub1=high1;
    lb2=low2;
    ub2=high2;   
    plane=new double[2*(maxdim+1)*maxdim];
    scr = new double[2*(maxdim+1)*maxdim];
    setPriority(MAX_PRIORITY);
    setDaemon(true);
    master = ft;
    this.in = in;
    this.out = out;
  }
  void Init(FT ft){
    //initialize shared data
    maxdim=ft.maxdim;
  }
  public void run(){    
    for(;;){
//      synchronized(this){
//        while(done==true){
//          try{
//	    wait();
//            synchronized(master){master.notify();}
//          }catch(InterruptedException ie){}
//        }

        var o = in.receive();
        if (o instanceof FFTMessage) {
            step();
            state++;
            if (state == 4) state = 1;
            out.send(new DoneMessage());
        }
        if (o instanceof FFTSetVariablesMessage) {
            var m = (FFTSetVariablesMessage) o;
            setVariables(m.sign1, m.tr, m.x1, m.exp11, m.exp21, m.exp31);
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
  
  public void setVariables(int sign1,boolean tr,double x1[], 
                           double exp11[],double exp21[],double exp31[]){
    sign = sign1; 
    x = x1;
    exp1 = exp11;
    exp2 = exp21;
    exp3 = exp31;
    n1=exp1.length>>1;
    n2=exp2.length>>1;
    n3=exp3.length>>1;
    
    if(tr){
      lower_bound1=lb2;
      upper_bound1=ub2;
      lower_bound2=lb1;
      upper_bound2=ub1;
    }else{
      lower_bound1=lb1;
      upper_bound1=ub1;
      lower_bound2=lb2;
      upper_bound2=ub2;   
    }
  }
  
  public void step(){
    int log = ilog2(n2);
    int isize3=2,
        jsize3=isize3*(n1+1),
        ksize3=jsize3*n2;
    int isize1=2;
    int jsize1=2*(n2+1);

    switch(state){
    case 1:
      for(int k=lower_bound1;k<=upper_bound1;k++)
        Swarztrauber(sign,log,n1,n2,x, k*ksize3 ,n1,exp2,scr);
      break;
    case 2:
      log = ilog2(n1);
      for(int k=lower_bound1;k<=upper_bound1;k++){
	for(int j=0;j<n2;j++){
	  for(int i=0; i<n1;i++){
	    plane[REAL+j*isize1+i*jsize1] = x[REAL+i*isize3+j*jsize3+k*ksize3];
	    plane[IMAG+j*isize1+i*jsize1] = x[IMAG+i*isize3+j*jsize3+k*ksize3];
	  }
	}
	Swarztrauber(sign,log,n2,n1,plane,0,n2,exp1,scr);
	for(int j=0;j<n2;j++){
	  for(int i=0;i<n1;i++){
	    x[REAL+i*isize3+j*jsize3+k*ksize3]=plane[REAL+j*isize1+i*jsize1];
	    x[IMAG+i*isize3+j*jsize3+k*ksize3]=plane[IMAG+j*isize1+i*jsize1];
	  }
	}
      }
      break;
    case 3:
      log = ilog2(n3);     
      jsize1=2*(n1+1);
      for(int k=lower_bound2;k<=upper_bound2;k++) {
	for(int i=0; i<n3;i++){
	  for(int j=0;j<n1;j++){
	    plane[REAL+j*isize1+i*jsize1] = x[REAL+j*isize3+k*jsize3+i*ksize3];
	    plane[IMAG+j*isize1+i*jsize1] = x[IMAG+j*isize3+k*jsize3+i*ksize3];
	  }
	}
	Swarztrauber(sign,log,n1,n3,plane,0,n1,exp3,scr);
	for(int i=0; i<n3;i++){
	  for(int j=0;j<n1;j++){
	    x[REAL+j*isize3+k*jsize3+i*ksize3]=plane[REAL+j*isize1+i*jsize1];
	    x[IMAG+j*isize3+k*jsize3+i*ksize3]=plane[IMAG+j*isize1+i*jsize1];
	  }
	}
      }         
      break;
    }
  }
}














