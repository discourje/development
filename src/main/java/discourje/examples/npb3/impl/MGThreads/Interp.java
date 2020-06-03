/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               I n t e r p                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    Interp implements thread for Interp subroutine of MG benchmark.      !
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

public class Interp extends MGBase{
  public int id;
  public boolean done=true;

  public int mm1, mm2, mm3;
  public int n1, n2, n3;
  public int zoff, uoff;

  int start,end,work;	      
  int state=0;
  double z1[],z2[],z3[];

  public AsyncJ.Channel in;
  public AsyncJ.Channel out;

  public Interp(MG mg, AsyncJ.Channel in, AsyncJ.Channel out){
    Init(mg);
    int m=535;
    z1=new double[m];
    z2=new double[m];
    z3=new double[m];
    setPriority(MAX_PRIORITY);
    setDaemon(true);
    master=mg;
    this.in = in;
    this.out = out;
  }
  void Init(MG mg){
    //initialize shared data
    num_threads=mg.num_threads;
    u=mg.u;
  }
    public void run(){
	for(;;){
//	  synchronized(this){
//	    while(done==true){
////This is an extra notity to compensate for lost notification of
////master thread when the benchmark runs under server on SUN(1.1.3)
//		try{
//		    wait();
//  	        synchronized(master){master.notify();}
//		}catch(InterruptedException ie){}
//	    }

	    var o = in.receive();
	    if (o instanceof InterpMessage) {
	        var m = (InterpMessage) o;
            wstart = m.wstart;
            wend = m.wend;
            mm1 = m.mm1;
            mm2 = m.mm2;
            mm3 = m.mm3;
            n1 = m.n1;
            n2 = m.n2;
            n3 = m.n3;
            zoff = m.zoff;
            uoff = m.uoff;
            GetWork();
            step();
            out.send(new DoneMessage());
        }
        if (o instanceof ExitMessage) {
            out.send(new DoneMessage());
            return;
        }

//	    synchronized(master){done=true; master.notify();}
//	  }
 	}	
    }

    public void step(){
      if(work==0) return;
      int i3, i2, i1, d1, d2, d3, t1, t2, t3;
      if( n1 != 3 && n2 != 3 && n3 != 3 ){
         for(i3=start;i3<=end;i3++){
            for(i2=1;i2<=mm2-1;i2++){
               for(i1=1;i1<=mm1;i1++){
                  z1[i1-1] = u[zoff+i1-1+mm1*(i2+mm2*(i3-1))] + u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))];
                  z2[i1-1] = u[zoff+i1-1+mm1*(i2-1+mm2*i3)] + u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))];
                  z3[i1-1] = u[zoff+i1-1+mm1*(i2+mm2*i3)] + u[zoff+i1-1+mm1*(i2-1+mm2*i3)] + z1[i1-1];
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-2+n1*(2*i2-2+n2*(2*i3-2))]+=u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))];
                  u[uoff+2*i1-1+n1*(2*i2-2+n2*(2*i3-2))]+=
                        0.5*(u[zoff+i1+mm1*(i2-1+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-2+n1*(2*i2-1+n2*(2*i3-2))]+=0.5 * z1[i1-1];
                  u[uoff+2*i1-1+n1*(2*i2-1+n2*(2*i3-2))]+=0.25*( z1[i1-1] + z1[i1] );
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-2+n1*(2*i2-2+n2*(2*i3-1))]+=0.5 * z2[i1-1];
                  u[uoff+2*i1-1+n1*(2*i2-2+n2*(2*i3-1))]+=0.25*( z2[i1-1] + z2[i1] );
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-2+n1*(2*i2-1+n2*(2*i3-1))]+=0.25*z3[i1-1];
                  u[uoff+2*i1-1+n1*(2*i2-1+n2*(2*i3-1))]+=0.125*( z3[i1-1] + z3[i1] );
               }
            }
         }
      }else{
         if(n1==3){
            d1 = 2;
            t1 = 1;
         }else{
            d1 = 1;
            t1 = 0;
         }
         
         if(n2==3){
            d2 = 2;
            t2 = 1;
         }else{
            d2 = 1;
            t2 = 0;
         }
         
         if(n3==3){
            d3 = 2;
            t3 = 1;
         }else{
            d3 = 1;
            t3 = 0;
         }
         
         for(i3=start;i3<=end;i3++){
            for(i2=1;i2<=mm2-1;i2++){
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-d1+n1*(2*i2-1-d2+n2*(2*i3-1-d3))]+=
                       u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))];
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-t1+n1*(2*i2-1-d2+n2*(2*i3-1-d3))]+=
                       0.5*(u[zoff+i1+mm1*(i2-1+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
            }
            for(i2=1;i2<=mm2-1;i2++){
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-d1+n1*(2*i2-1-t2+n2*(2*i3-1-d3))]+=
                       0.5*(u[zoff+i1-1+mm1*(i2+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-t1+n1*(2*i2-1-t2+n2*(2*i3-1-d3))]+=
                       0.25*(u[zoff+i1+mm1*(i2+mm2*(i3-1))]+u[zoff+i1+mm1*(i2-1+mm2*(i3-1))]
                       +u[zoff+i1-1+mm1*(i2+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
            }
         }

         for(i3=start;i3<=end;i3++){
            for(i2=1;i2<=mm2-1;i2++){
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-d1+n1*(2*i2-1-d2+n2*(2*i3-1-t3))]=
                       0.5*(u[zoff+i1-1+mm1*(i2-1+mm2*i3)]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-t1+n1*(2*i2-1-d2+n2*(2*i3-1-t3))]+=
                       0.25*(u[zoff+i1+mm1*(i2-1+mm2*i3)]+u[zoff+i1-1+mm1*(i2-1+mm2*i3)]
                       +u[zoff+i1+mm1*(i2-1+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
            }
            for(i2=1;i2<=mm2-1;i2++){
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-d1+n1*(2*i2-1-t2+n2*(2*i3-1-t3))]+=
                       0.25*(u[zoff+i1-1+mm1*(i2+mm2*i3)]+u[zoff+i1-1+mm1*(i2-1+mm2*i3)]
                       +u[zoff+i1-1+mm1*(i2+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
               for(i1=1;i1<=mm1-1;i1++){
                  u[uoff+2*i1-1-t1+n1*(2*i2-1-t2+n2*(2*i3-1-t3))]+=
                       0.125*(u[zoff+i1+mm1*(i2+mm2*i3)]+u[zoff+i1+mm1*(i2-1+mm2*i3)]
                       +u[zoff+i1-1+mm1*(i2+mm2*i3)]+u[zoff+i1-1+mm1*(i2-1+mm2*i3)]
                       +u[zoff+i1+mm1*(i2+mm2*(i3-1))]+u[zoff+i1+mm1*(i2-1+mm2*(i3-1))]
                       +u[zoff+i1-1+mm1*(i2+mm2*(i3-1))]+u[zoff+i1-1+mm1*(i2-1+mm2*(i3-1))]);
               }
            }
         }
      }
    }
    
    public void step2(){
      if(work==0) return;
    }
    
    private void GetWork(){
      int workpt=(wend-wstart)/num_threads;
      int remainder=wend-wstart-workpt*num_threads;
      if(workpt==0){
        if(id<wend-wstart){
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
