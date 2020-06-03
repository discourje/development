/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               R e s i d                                 !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    Resid implements thread for Resid subroutine of MG benchmark.        !
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
import discourje.examples.npb3.impl.DoneMessage;
import discourje.examples.npb3.impl.ExitMessage;
import discourje.examples.npb3.impl.MG;

public class Resid extends MGBase{
  public int id;
  public boolean visr;
  public boolean done=true;
  
  public int n1, n2, n3;
  public int off;
  
  int state=0;
  int start,end,work;	      
  double u1[],u2[];

  public AsyncJ.Channel in;
  public AsyncJ.Channel out;

  public Resid(MG mg, AsyncJ.Channel in, AsyncJ.Channel out){
    Init(mg);
    u1=new double[nm+1];
    u2=new double[nm+1];
    setPriority(MAX_PRIORITY);
    setDaemon(true);
    master=mg;
    this.in = in;
    this.out = out;
  }
  void Init(MG mg){
    //initialize shared data
    num_threads=mg.num_threads;
    r=mg.r;
    v=mg.v;
    u=mg.u;
    a=mg.a;
    nm=mg.nm;
  }

  public void run(){
      for(;;){
//        synchronized(this){
//          while(done==true){
//              try{
//        	  wait();
//              synchronized(master){master.notify();}
//              }catch(InterruptedException ie){}
//          }

          var o = in.receive();
          if (o instanceof ResidMessage) {
              var m = (ResidMessage) o;
              visr = m.visr;
              wstart = m.wstart;
              wend = m.wend;
              n1 = m.n1;
              n2 = m.n2;
              n3 = m.n3;
              off = m.off;
              GetWork();
              step();
              out.send(new DoneMessage());
          }
          if (o instanceof ExitMessage) {
              out.send(new DoneMessage());
              return;
          }

//          synchronized(master){done=true; master.notify();}
//        }
      }       
  }

    public void step(){
      int i3, i2, i1;
      if(work==0) return;
      double tmp[]=v;
      if(visr) tmp=r;
      for(i3=start;i3<=end;i3++)
         for(i2=1;i2<n2-1;i2++){
            for(i1=0;i1<n1;i1++){
               u1[i1] = u[off+i1+n1*(i2-1+n3*i3)] + u[off+i1+n1*(i2+1+n3*i3)]
                      + u[off+i1+n1*(i2+n3*(i3-1))] + u[off+i1+n1*(i2+n3*(i3+1))];
               u2[i1] = u[off+i1+n1*(i2-1+n3*(i3-1))] + u[off+i1+n1*(i2+1+n3*(i3-1))]
                      + u[off+i1+n1*(i2-1+n3*(i3+1))] + u[off+i1+n1*(i2+1+n3*(i3+1))];
            }
            for(i1=1;i1<n1-1;i1++){
               r[off+i1+n1*(i2+n3*i3)] = 
	       tmp[off+i1+n1*(i2+n3*i3)]
                           - a[0] * u[off+i1+n1*(i2+n3*i3)]
//c---------------------------------------------------------------------
//c  Assume a(1) = 0      (Enable 2 lines below if a(1) not= 0)
//c---------------------------------------------------------------------
//c    >                     - a[1] * ( u(i1-1,i2,i3) + u(i1+1,i2,i3)
//c    >                              + u1(i1) )
//c---------------------------------------------------------------------
                           - a[2] * ( u2[i1] + u1[i1-1] + u1[i1+1] )
                           - a[3] * ( u2[i1-1] + u2[i1+1] );
            }
         }
//c---------------------------------------------------------------------
//c     exchange boundary data
//c---------------------------------------------------------------------
      for(i3=start;i3<=end;i3++)
         for(i2=1;i2<n2-1;i2++){
            r[off+n1*(i2+n2*i3)] = r[off+n1-2+n1*(i2+n2*i3)];
            r[off+n1-1+n1*(i2+n2*i3)] = r[uoff+1+n1*(i2+n2*i3)];
         }

      for(i3=start;i3<=end;i3++)
         for(i1=0;i1<n1;i1++){
            r[off+i1+n1*n2*i3] = r[off+i1+n1*(n2-2+n2*i3)];
            r[off+i1+n1*(n2-1+n2*i3)] = r[off+i1+n1*(1+n2*i3)];
         }
    }
    
    private void GetWork(){
      int workpt=(wend-wstart)/num_threads;
      int remainder=wend-wstart-workpt*num_threads;
      if(workpt==0){
        if(id<=wend-wstart){
	  work=1;
	  start=end=wstart+id;
	}else{
	  work=0;
	}
      }else{
        if(id<remainder){
	  workpt++;
          start=wstart+workpt*id;
	  end=start+workpt-1;
	  work=workpt;
	}else{	
          start=wstart+remainder+workpt*id;
	  end=start+workpt-1;
	  work=workpt;
	}
      }
    }
}
