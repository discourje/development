/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                               R p r j                                   !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    Rprj implements thread for Rprj subroutine of MG benchmark.          !
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

public class Rprj extends MGBase{
  public int id;
  public boolean done=true;

  public int m1k, m2k, m3k;
  public int m1j, m2j, m3j;
  public int zoff, roff;

  int start,end,work;	      
  int state=0;
  double x1[],y1[];

  public AsyncJ.Channel in;
  public AsyncJ.Channel out;

  public Rprj(MG mg, AsyncJ.Channel in, AsyncJ.Channel out){
    Init(mg);
    x1=new double[nm+1];
    y1=new double[nm+1];
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
    nm=mg.nm;
  }

  public void run(){
    for(;;){
//      synchronized(this){
//    	while(done==true){
//    	  try{
//    	      wait();
//    	  synchronized(master){master.notify();}
//    	  }catch(InterruptedException ie){}
//    	}

    	var o = in.receive();
    	if (o instanceof RprjMessage) {
            var m = (RprjMessage) o;
            wstart = m.wstart;
            wend = m.wend;
            m1k = m.m1k;
            m2k = m.m2k;
            m3k = m.m3k;
            m1j = m.m1j;
            m2j = m.m2j;
            m3j = m.m3j;
            roff = m.roff;
            zoff = m.zoff;
            GetWork();
            step();
            out.send(new DoneMessage());
        }
        if (o instanceof ExitMessage) {
            out.send(new DoneMessage());
            return;
        }

//    	synchronized(master){done=true; master.notify();}
//      }
    }	    
  }

    public void step(){
      int j3, j2, j1, i3, i2, i1, d1, d2, d3, j;
      double x2,y2;
      
      if(work==0) return;
      
      if(m1k==3) d1 = 2; else d1 = 1;
      if(m2k==3) d2 = 2; else d2 = 1;
      if(m3k==3) d3 = 2; else d3 = 1;

      for(j3=start;j3<=end;j3++){
         i3 = 2*j3-d3-1;
         for(j2=2;j2<=m2j-1;j2++){
            i2 = 2*j2-d2-1;
            for(j1=2;j1<=m1j;j1++){
              i1 = 2*j1-d1-1;
              x1[i1-1] = r[roff+i1-1+m1k*(i2-1+m2k*i3)] + r[roff+i1-1+m1k*(i2+1+m2k*i3)]
                       + r[roff+i1-1+m1k*(i2+m2k*(i3-1))] + r[roff+i1-1+m1k*(i2+m2k*(i3+1))];
              y1[i1-1] = r[roff+i1-1+m1k*(i2-1+m2k*(i3-1))] + r[roff+i1-1+m1k*(i2-1+m2k*(i3+1))]
                       + r[roff+i1-1+m1k*(i2+1+m2k*(i3-1))] + r[roff+i1-1+m1k*(i2+1+m2k*(i3+1))];
            }

            for(j1=2;j1<=m1j-1;j1++){
              i1 = 2*j1-d1-1;
              y2 = r[roff+i1+m1k*(i2-1+m2k*(i3-1))]+r[roff+i1+m1k*(i2-1+m2k*(i3+1))]
                 + r[roff+i1+m1k*(i2+1+m2k*(i3-1))]+r[roff+i1+m1k*(i2+1+m2k*(i3+1))];
              x2 = r[roff+i1+m1k*(i2-1+m2k*i3)] + r[roff+i1+m1k*(i2+1+m2k*i3)]
                 + r[roff+i1+m1k*(i2+m2k*(i3-1))] + r[roff+i1+m1k*(i2+m2k*(i3+1))];
              r[zoff+j1-1+m1j*(j2-1+m2j*(j3-1))] =
                     0.5 * r[roff+i1+m1k*(i2+m2k*i3)]
                   + 0.25 * ( r[roff+i1-1+m1k*(i2+m2k*i3)]+r[roff+i1+1+m1k*(i2+m2k*i3)]+x2)
                   + 0.125 * ( x1[i1-1] + x1[i1+1] + y2)
                   + 0.0625 * ( y1[i1-1] + y1[i1+1] );
            }
         }
      }
      for(j3=start-1;j3<=end-1;j3++)
         for(j2=1;j2<=m2j-1;j2++){
            r[zoff+m1j*(j2+m2j*j3)] = r[zoff+m1j-2+m1j*(j2+m2j*j3)];
            r[zoff+m1j-1+m1j*(j2+m2j*j3)] = r[zoff+1+m1j*(j2+m2j*j3)];
         }

      for(j3=start-1;j3<=end-1;j3++)
         for(j1=0;j1<=m1j;j1++){
            r[zoff+j1+m1j*m2j*j3] = r[zoff+j1+m1j*(m2j-2+m2j*j3)];
            r[zoff+j1+m1j*(m2j-1+m2j*j3)] = r[zoff+j1+m1j*(1+m2j*j3)];
         }
    }
    
    void GetWork(){
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
