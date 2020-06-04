/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                                  S P                                    !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    This benchmark is a serial/multithreaded version of                  !
!    the NPB3_0_JAV SP code.                                              !
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
!									  !
! Authors: R. Van der Wijngaart 					  !
!	   T. Harris							  !
!	   M. Yarrow							  !
! Modified for PBN (Programming Baseline for NPB):			  !
!	   H. Jin							  !
! Translation to Java and MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/

package discourje.examples.npb3.impl;

import discourje.examples.npb3.impl.SPThreads.*;
import discourje.examples.npb3.impl.BMInOut.*;

import java.io.*;
import java.text.*;

public class SP extends SPBase {
  public int bid=-1;
  public BMResults results;
  public boolean serial=false;
  public SP(char clss, int np, boolean ser){ 
    super(clss,np);
    serial=ser;
  }
  public static void main(String argv[]){
    SP sp = null;

    BMArgs.ParseCmdLineArgs(argv,BMName);
    char CLSS=BMArgs.CLASS;
    int np=BMArgs.num_threads;
    boolean serial=BMArgs.serial;
    try{ 
      sp = new SP(CLSS,np,serial);
    }catch(OutOfMemoryError e){
      BMArgs.outOfMemoryMessage();
      System.exit(0);
    }      
    sp.runBenchMark();
  }

  public void run(){runBenchMark();}

  public void runBenchMark(){ 
    BMArgs.Banner(BMName,CLASS,serial,num_threads);
    
    int numTimers=t_last+1;
    String t_names[] = new String[numTimers];
    double trecs[] = new double[numTimers];
    setTimers(t_names);
//---------------------------------------------------------------------
//      Read input file (if it exists), else take
//      defaults from parameters
//---------------------------------------------------------------------
    int niter=getInputPars();
    set_constants(0);
    initialize();       
    exact_rhs();
    
    if(!serial) setupThreads(this);
//---------------------------------------------------------------------
//      do one time step to touch all code, and reinitialize
//---------------------------------------------------------------------
    if(serial) adi_serial();
    else adi(); 
    initialize();
  
    timer.resetAllTimers();
    timer.start(t_total);
    for(int step = 1;step<=niter;step++){
      if (step%20== 0||step==1||step==niter) {
	System.out.println("Time step " + step);
      }
      if(serial) adi_serial();
      else adi(); 
    }
    timer.stop(1);
    int verified = verify(niter);

    double time = timer.readTimer(t_total);
    results=new BMResults(BMName,
    			  CLASS,
    			  grid_points[0],
    			  grid_points[1],
    			  grid_points[2],
    			  niter,
    			  time,
    			  getMFLOPS(time,niter),
    			  "floating point",
    			  verified,
    			  serial,
    			  num_threads,
    			  bid);
    results.print();				
    if(timeron) printTimers(t_names,trecs,time);  
  }
  
  public double getMFLOPS(double total_time,int niter){
    double mflops = 0.0;
    if( total_time > 0 ){
      int n3 = grid_points[0]*grid_points[1]*grid_points[2];
      double t = (grid_points[0]+grid_points[1]+grid_points[2])/3.0;
      mflops =  881.174 * n3 
               -4683.91 * t*t
               +11484.5 * t   - 19272.4;
      mflops *= niter / (total_time*1000000.0);
    }
    return mflops;
  }

  public void adi_serial(){
    if (timeron)timer.start(t_rhs);
    compute_rhs();
    if (timeron)timer.stop(t_rhs);
    if (timeron)timer.start(t_txinvr);
    txinvr();
    if (timeron)timer.stop(t_txinvr);    
    x_solve();    
    y_solve();
    z_solve();    
    if (timeron)timer.start(t_add);
    add();
    if (timeron)timer.stop(t_add);
  }   

  public void adi(){ 
    if (timeron)timer.start(t_rhs);
    doRHS(); 
    doRHS(); 
 
    if (timeron)timer.start(t_rhsx);
    doRHS(); 
    if (timeron)timer.stop(t_rhsx);
    
    if (timeron)timer.start(t_rhsy);    
    doRHS(); 
    if (timeron)timer.stop(t_rhsy);
    
    if (timeron)timer.start(t_rhsz);    
    doRHS(); 
    if (timeron)timer.stop(t_rhsz); 
       
    doRHS(); 
    if (timeron)timer.stop(t_rhs);

    if (timeron)timer.start(t_txinvr);
      synchronized(this){
      for(int m=0;m<num_threads;m++)
	synchronized(txinverse[m]){
          txinverse[m].done=false;
          txinverse[m].notify();
        }
      for(int m=0;m<num_threads;m++)
        while(!txinverse[m].done){
          try{wait();}catch(InterruptedException e){} 
          notifyAll();
        }
    }    
    if (timeron)timer.stop(t_txinvr);    
          
    if (timeron)timer.start(t_xsolve);
    doXsolve();
    if (timeron)timer.stop(t_xsolve);    

    if (timeron) timer.start(t_ninvr);
    doXsolve();
    if (timeron) timer.stop(t_ninvr);

    if (timeron)timer.start(t_ysolve);
    doYsolve();
    if (timeron)timer.stop(t_ysolve);    

    if (timeron) timer.start(t_pinvr);
    doYsolve();
    if (timeron) timer.stop(t_pinvr);

    if (timeron)timer.start(t_zsolve);
    doZsolve();
    if (timeron)timer.stop(t_zsolve);    

    if (timeron) timer.start(t_tzetar);
    doZsolve();
    if (timeron) timer.stop(t_tzetar);
  
    if (timeron)timer.start(t_add);
    synchronized(this){
      for(int m=0;m<num_threads;m++)
	synchronized(rhsadder[m]){
          rhsadder[m].done=false;
          rhsadder[m].notify();
        }
      for(int m=0;m<num_threads;m++)
        while(!rhsadder[m].done){
          try{wait();}catch(InterruptedException e){} 
          notifyAll();
        }
    }
    if (timeron)timer.stop(t_add);
  }
  
  synchronized void doRHS(){
    int m;
    for(m=0;m<num_threads;m++)
	synchronized(rhscomputer[m]){
          rhscomputer[m].done=false;
          rhscomputer[m].notify();
        }
    for(m=0;m<num_threads;m++)
	while(!rhscomputer[m].done){
	  try{wait();}catch(InterruptedException e){}
          notifyAll();	
	}
  }
  
  synchronized void doXsolve(){
    int m;
    for(m=0;m<num_threads;m++)
	synchronized(xsolver[m]){
          xsolver[m].done=false;
          xsolver[m].notify();
        }
    for(m=0;m<num_threads;m++)
	while(!xsolver[m].done){
	  try{wait();}catch(InterruptedException e){}
          notifyAll();
	}
  }

  synchronized void doYsolve(){
    int m;
    for(m=0;m<num_threads;m++)
	synchronized(ysolver[m]){
          ysolver[m].done=false;
          ysolver[m].notify();
        }
    for(m=0;m<num_threads;m++)
	while(!ysolver[m].done){
	  try{wait();}catch(InterruptedException e){}
          notifyAll();  
	}
  }

  synchronized void doZsolve(){
    int m;
    for(m=0;m<num_threads;m++)
	synchronized(zsolver[m]){
          zsolver[m].done=false;
          zsolver[m].notify();
        }
    for(m=0;m<num_threads;m++)
	while(!zsolver[m].done){
	  try{wait();}catch(InterruptedException e){}
          notifyAll();
	}
  }
  public int getInputPars(){
    int niter=0;
    File f2 = new File("inputsp.data");
    if ( f2.exists() ){
      try{  
	FileInputStream fis = new FileInputStream(f2);
	DataInputStream datafile = new DataInputStream(fis);
	System.out.println("Reading from input file inputsp.data");
	niter = datafile.readInt();
	dt = datafile.readDouble();
	grid_points[0] = datafile.readInt();
	grid_points[1] = datafile.readInt(); 
	grid_points[2] = datafile.readInt();
	fis.close();
      }catch(Exception e){  
	System.err.println("exception caught!");
      }       
    }else{
      System.out.println("No input file inputsp.data,"+
                         "Using compiled defaults"); 
      niter = niter_default;
      dt    = dt_default;
      grid_points[0] = problem_size;
      grid_points[1] = problem_size;
      grid_points[2] = problem_size;
    }
    System.out.println("Size: "+grid_points[0]
                         +" X "+grid_points[1]
			 +" X "+grid_points[2]);
    if ( (grid_points[0] > IMAX) ||
	 (grid_points[1] > JMAX) ||
	 (grid_points[2] > KMAX) ) {
      System.out.println("Problem size too big for array");
      System.exit(0);
    }
    System.out.println("Iterations: "+niter+" dt: "+dt);
    
    nx2 = grid_points[0] - 2;
    ny2 = grid_points[1] - 2;
    nz2 = grid_points[2] - 2;
    return niter;
  }
  public void setTimers(String t_names[]){
    File f1 = new File("timer.flag");
    timeron = false;
    if( f1.exists() ){
      timeron = true;
      t_names[t_total] = "total";
      t_names[t_rhsx] = "rhsx";
      t_names[t_rhsy] = "rhsy";
      t_names[t_rhsz] = "rhsz";
      t_names[t_rhs] = "rhs";
      t_names[t_xsolve] = "xsolve";
      t_names[t_ysolve] = "ysolve";
      t_names[t_zsolve] = "zsolve";
      t_names[t_rdis1] = "redist1";
      t_names[t_rdis2] = "redist2";
      t_names[t_tzetar] = "tzetar";
      t_names[t_ninvr] = "ninvr";
      t_names[t_pinvr] = "pinvr";
      t_names[t_txinvr] = "txinvr";
      t_names[t_add] = "add";
    }
  }
  public void printTimers(String t_names[], double trecs[],double tmax){
    DecimalFormat fmt = new DecimalFormat("0.000");
    double t;
    System.out.println("  SECTION   Time (secs)");
    for(int i=1;i<=t_last;i++){
    	trecs[i] = timer.readTimer(i);
    }
    if (tmax == 0.0) tmax = 1.0;
    for(int i=1;i<t_last;i++){
    	System.out.println(t_names[i]+":" + fmt.format(trecs[i])+
	                   "  (" +fmt.format(trecs[i]*100/tmax) + "%)" );
    	if (i==t_rhs) {
    	    t = trecs[t_rhsx] + trecs[t_rhsy] + trecs[t_rhsz];
    	    System.out.println("    --> total "+"sub-rhs"+":"+fmt.format(t)+
	                       "  ("+fmt.format(t*100./tmax) +"%)" );
    	    t = trecs[t_rhs] - t;
    	    System.out.println("    --> total "+"rest-rhs"+":"+fmt.format(t)+
	                       "  ("+fmt.format(t*100./tmax) +"%)" );
    	}else if (i==t_zsolve) {
    	    t = trecs[t_zsolve] - trecs[t_rdis1] - trecs[t_rdis2];
    	    System.out.println("    --> total "+"sub-zsol"+":"+fmt.format(t)+
	                       "  ("+fmt.format(t*100./tmax) +"%)" );
    	}else if (i==t_rdis2) {
    	    t = trecs[t_rdis1] + trecs[t_rdis2];
    	    System.out.println("    --> total "+"redist"+":"+fmt.format(t)+
	                       "  ("+ fmt.format(t*100./tmax) +"%)" );
    	}
    }
  }
  

  public void add(){
    int i,j,k,m;
    for(k=1;k<=nz2;k++){
      for(j=1;j<=ny2;j++){
        for(i=1;i<=nx2;i++){
          for(m=0;m<=4;m++){
            u[m+i*isize1+j*jsize1+k*ksize1] +=
          			   rhs[m+i*isize1+j*jsize1+k*ksize1];
          }
        }
      }
    }
  }

  public void error_norm(double rms[]){
    int i, j, k, m, d;
    double xi, eta, zeta, u_exact[] = new double[5] , add;

       for(m=0;m<=4;m++){
          rms[m] = 0.0;
       }

       for(k=0;k<=grid_points[2]-1;k++){
          zeta = k * dnzm1;
          for(j=0;j<=grid_points[1]-1;j++){
             eta = j * dnym1;
             for(i=0;i<=grid_points[0]-1;i++){
                xi = i * dnxm1;
                 exact_solution(xi, eta, zeta, u_exact, 0);

                for(m=0;m<=4;m++){
                   add = u[m+i*isize1+j*jsize1+k*ksize1]-u_exact[m];
                   rms[m] = rms[m] + add*add;
                }
             }
          }
       }
       for(m=0;m<=4;m++){
          for(d=0;d<=2;d++){
             rms[m] = rms[m] / (grid_points[d]-2);
          }
          rms[m] = Math.sqrt(rms[m]);
       }
  }

  public void rhs_norm(double rms[]){

    int i, j, k, d, m;
    double add;

    for(m=0;m<=4;m++){
      rms[m] = 0.0;
    }

    for(k=1;k<=nz2;k++){
      for(j=1;j<=ny2;j++){
	for(i=1;i<=nx2;i++){
	  for(m=0;m<=4;m++){
	    add = rhs[m+i*isize1+j*jsize1+k*ksize1];
	    rms[m] = rms[m] + add*add;
	  } 
	} 
      } 
    } 
    for(m=0;m<=4;m++){
      for(d=0;d<=2;d++){
	rms[m] = rms[m] / (grid_points[d]-2);
      }
      rms[m] = Math.sqrt(rms[m]);
    }       
  }


  public void exact_rhs(){
    double dtemp[] = new double[5], xi, eta, zeta, dtpp;
    int m, i, j, k, ip1, im1, jp1, jm1, km1, kp1;

//---------------------------------------------------------------------
//      initialize                                  
//---------------------------------------------------------------------
    for(k=0;k<=grid_points[2]-1;k++){
      for(j=0;j<=grid_points[1]-1;j++){
	for(i=0;i<=grid_points[0]-1;i++){
	  for(m=0;m<=4;m++){
	    forcing[m+i*isize1+j*jsize1+k*ksize1] = 0.0;
	  }
	}
      }
    }
//---------------------------------------------------------------------
//      xi-direction flux differences                      
//---------------------------------------------------------------------
    for(k=1;k<=grid_points[2]-2;k++){
      zeta = k * dnzm1;
      for(j=1;j<=grid_points[1]-2;j++){
	eta = j * dnym1;
	for(i=0;i<=grid_points[0]-1;i++){
	  xi = i * dnxm1;

	  exact_solution(xi, eta, zeta, dtemp,0);
	  for(m=0;m<=4;m++){
	    ue[i+m*jsize3] = dtemp[m];
	  }

	  dtpp = 1.0 / dtemp[0];
	  
	  for(m=1;m<=4;m++){
	    buf[i+m*jsize3] = dtpp * dtemp[m];
	  }

	  cuf[i]   = buf[i+1*jsize3] * buf[i+1*jsize3];
	  buf[i+0*jsize3] = cuf[i] + buf[i+2*jsize3] * buf[i+2*jsize3] + 
                           buf[i+3*jsize3] * buf[i+3*jsize3] ;
	  q[i] = 0.5*(buf[i+1*jsize3]*ue[i+1*jsize3] + buf[i+2*jsize3]*ue[i+2*jsize3] +
                              buf[i+3*jsize3]*ue[i+3*jsize3]);

	}

	for(i=1;i<=grid_points[0]-2;i++){
	  im1 = i-1;
	  ip1 = i+1;

	  forcing[0+i*isize1+j*jsize1+k*ksize1] = forcing[0+i*isize1+j*jsize1+k*ksize1] -
                       tx2*( ue[ip1+1*jsize3]-ue[im1+1*jsize3] )+
                       dx1tx1*(ue[ip1+0*jsize3]-2.0*ue[i+0*jsize3]+ue[im1+0*jsize3]);

	  forcing[1+i*isize1+j*jsize1+k*ksize1] = forcing[1+i*isize1+j*jsize1+k*ksize1] - tx2 * (
                      (ue[ip1+1*jsize3]*buf[ip1+1*jsize3]+c2*(ue[ip1+4*jsize3]-q[ip1]))-
                      (ue[im1+1*jsize3]*buf[im1+1*jsize3]+c2*(ue[im1+4*jsize3]-q[im1])))+
                       xxcon1*(buf[ip1+1*jsize3]-2.0*buf[i+1*jsize3]+buf[im1+1*jsize3])+
                       dx2tx1*( ue[ip1+1*jsize3]-2.0* ue[i+1*jsize3]+ue[im1+1*jsize3]);

	  forcing[2+i*isize1+j*jsize1+k*ksize1] = forcing[2+i*isize1+j*jsize1+k*ksize1] - tx2 * (
                       ue[ip1+2*jsize3]*buf[ip1+1*jsize3]-ue[im1+2*jsize3]*buf[im1+1*jsize3])+
                       xxcon2*(buf[ip1+2*jsize3]-2.0*buf[i+2*jsize3]+buf[im1+2*jsize3])+
                       dx3tx1*( ue[ip1+2*jsize3]-2.0*ue[i+2*jsize3] +ue[im1+2*jsize3]);

                  
	  forcing[3+i*isize1+j*jsize1+k*ksize1] = forcing[3+i*isize1+j*jsize1+k*ksize1] - tx2*(
                       ue[ip1+3*jsize3]*buf[ip1+1*jsize3]-ue[im1+3*jsize3]*buf[im1+1*jsize3])+
                       xxcon2*(buf[ip1+3*jsize3]-2.0*buf[i+3*jsize3]+buf[im1+3*jsize3])+
                       dx4tx1*( ue[ip1+3*jsize3]-2.0* ue[i+3*jsize3]+ ue[im1+3*jsize3]);

	  forcing[4+i*isize1+j*jsize1+k*ksize1] = forcing[4+i*isize1+j*jsize1+k*ksize1] - tx2*(
                       buf[ip1+1*jsize3]*(c1*ue[ip1+4*jsize3]-c2*q[ip1])-
                       buf[im1+1*jsize3]*(c1*ue[im1+4*jsize3]-c2*q[im1]))+
                       0.5*xxcon3*(buf[ip1+0*jsize3]-2.0*buf[i+0*jsize3]+
                                     buf[im1+0*jsize3])+
                       xxcon4*(cuf[ip1]-2.0*cuf[i]+cuf[im1])+
                       xxcon5*(buf[ip1+4*jsize3]-2.0*buf[i+4*jsize3]+buf[im1+4*jsize3])+
                       dx5tx1*( ue[ip1+4*jsize3]-2.0* ue[i+4*jsize3]+ ue[im1+4*jsize3]);

	}

//---------------------------------------------------------------------
//            Fourth-order dissipation                         
//---------------------------------------------------------------------
	for(m=0;m<=4;m++){
	  i = 1;
	  forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          (5.0*ue[i+m*jsize3] - 4.0*ue[i+1+m*jsize3] +ue[i+2+m*jsize3]);
	  i = 2;
	  forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (-4.0*ue[i-1+m*jsize3] + 6.0*ue[i+m*jsize3] -
                           4.0*ue[i+1+m*jsize3] +       ue[i+2+m*jsize3]);
	}
	     
	for(m=0;m<=4;m++){
	  for(i=3;i<=grid_points[0]-4;i++){
	    forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp*
                         (ue[i-2+m*jsize3] - 4.0*ue[i-1+m*jsize3] +
                          6.0*ue[i+m*jsize3] - 4.0*ue[i+1+m*jsize3] + ue[i+2+m*jsize3]);
	  }
	}

	for(m=0;m<=4;m++){
	  i = grid_points[0]-3;
	  forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (ue[i-2+m*jsize3] - 4.0*ue[i-1+m*jsize3] +
                          6.0*ue[i+m*jsize3] - 4.0*ue[i+1+m*jsize3]);
	  i = grid_points[0]-2;
	  forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (ue[i-2+m*jsize3] - 4.0*ue[i-1+m*jsize3] + 5.0*ue[i+m*jsize3]);
	}
      }
    }

//---------------------------------------------------------------------
//  eta-direction flux differences             
//---------------------------------------------------------------------
       for(k=1;k<=grid_points[2]-2;k++){
          zeta = k * dnzm1;
          for(i=1;i<=grid_points[0]-2;i++){
             xi = i * dnxm1;

             for(j=0;j<=grid_points[1]-1;j++){
                eta = j * dnym1;

                 exact_solution(xi, eta, zeta, dtemp,0);
                for(m=0;m<=4 ;m++){
                   ue[j+m*jsize3] = dtemp[m];
                }
                dtpp = 1.0/dtemp[0];

                for(m=1;m<=4;m++){
                   buf[j+m*jsize3] = dtpp * dtemp[m];
                }

                cuf[j]   = buf[j+2*jsize3] * buf[j+2*jsize3];
                buf[j+0*jsize3] = cuf[j] + buf[j+1*jsize3] * buf[j+1*jsize3] + 
                           buf[j+3*jsize3] * buf[j+3*jsize3];
                q[j] = 0.5*(buf[j+1*jsize3]*ue[j+1*jsize3] + buf[j+2*jsize3]*ue[j+2*jsize3] +
                              buf[j+3*jsize3]*ue[j+3*jsize3]);
             }

             for(j=1;j<=grid_points[1]-2;j++){
                jm1 = j-1;
                jp1 = j+1;
                  
                forcing[0+i*isize1+j*jsize1+k*ksize1] = forcing[0+i*isize1+j*jsize1+k*ksize1] -
                      ty2*( ue[jp1+2*jsize3]-ue[jm1+2*jsize3] )+
                      dy1ty1*(ue[jp1+0*jsize3]-2.0*ue[j+0*jsize3]+ue[jm1+0*jsize3]);

                forcing[1+i*isize1+j*jsize1+k*ksize1] = forcing[1+i*isize1+j*jsize1+k*ksize1] - ty2*(
                      ue[jp1+1*jsize3]*buf[jp1+2*jsize3]-ue[jm1+1*jsize3]*buf[jm1+2*jsize3])+
                      yycon2*(buf[jp1+1*jsize3]-2.0*buf[j+1*jsize3]+buf[jm1+1*jsize3])+
                      dy2ty1*( ue[jp1+1*jsize3]-2.0* ue[j+1*jsize3]+ ue[jm1+1*jsize3]);

                forcing[2+i*isize1+j*jsize1+k*ksize1] = forcing[2+i*isize1+j*jsize1+k*ksize1] - ty2*(
                      (ue[jp1+2*jsize3]*buf[jp1+2*jsize3]+c2*(ue[jp1+4*jsize3]-q[jp1]))-
                      (ue[jm1+2*jsize3]*buf[jm1+2*jsize3]+c2*(ue[jm1+4*jsize3]-q[jm1])))+
                      yycon1*(buf[jp1+2*jsize3]-2.0*buf[j+2*jsize3]+buf[jm1+2*jsize3])+
                      dy3ty1*( ue[jp1+2*jsize3]-2.0*ue[j+2*jsize3] +ue[jm1+2*jsize3]);

                forcing[3+i*isize1+j*jsize1+k*ksize1] = forcing[3+i*isize1+j*jsize1+k*ksize1] - ty2*(
                      ue[jp1+3*jsize3]*buf[jp1+2*jsize3]-ue[jm1+3*jsize3]*buf[jm1+2*jsize3])+
                      yycon2*(buf[jp1+3*jsize3]-2.0*buf[j+3*jsize3]+buf[jm1+3*jsize3])+
                      dy4ty1*( ue[jp1+3*jsize3]-2.0*ue[j+3*jsize3]+ ue[jm1+3*jsize3]);

                forcing[4+i*isize1+j*jsize1+k*ksize1] = forcing[4+i*isize1+j*jsize1+k*ksize1] - ty2*(
                      buf[jp1+2*jsize3]*(c1*ue[jp1+4*jsize3]-c2*q[jp1])-
                      buf[jm1+2*jsize3]*(c1*ue[jm1+4*jsize3]-c2*q[jm1]))+
                      0.5*yycon3*(buf[jp1+0*jsize3]-2.0*buf[j+0*jsize3]+
                                    buf[jm1+0*jsize3])+
                      yycon4*(cuf[jp1]-2.0*cuf[j]+cuf[jm1])+
                      yycon5*(buf[jp1+4*jsize3]-2.0*buf[j+4*jsize3]+buf[jm1+4*jsize3])+
                      dy5ty1*(ue[jp1+4*jsize3]-2.0*ue[j+4*jsize3]+ue[jm1+4*jsize3]);
             }

//---------------------------------------------------------------------
//            Fourth-order dissipation                      
//---------------------------------------------------------------------
             for(m=0;m<=4;m++){
                j = 1;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          (5.0*ue[j+m*jsize3] - 4.0*ue[j+1+m*jsize3] +ue[j+2+m*jsize3]);
                j = 2;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (-4.0*ue[j-1+m*jsize3] + 6.0*ue[j+m*jsize3] -
                           4.0*ue[j+1+m*jsize3] +       ue[j+2+m*jsize3]);
             }

             for(m=0;m<=4;m++){
                for(j=3;j<=grid_points[1]-4;j++){
                   forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp*
                         (ue[j-2+m*jsize3] - 4.0*ue[j-1+m*jsize3] +
                          6.0*ue[j+m*jsize3] - 4.0*ue[j+1+m*jsize3] + ue[j+2+m*jsize3]);
                }
             }

             for(m=0;m<=4;m++){
                j = grid_points[1]-3;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (ue[j-2+m*jsize3] - 4.0*ue[j-1+m*jsize3] +
                          6.0*ue[j+m*jsize3] - 4.0*ue[j+1+m*jsize3]);
                j = grid_points[1]-2;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (ue[j-2+m*jsize3] - 4.0*ue[j-1+m*jsize3] + 5.0*ue[j+m*jsize3]);

             }

          }
       }

//---------------------------------------------------------------------
//      zeta-direction flux differences                      
//---------------------------------------------------------------------
       for(j=1;j<=grid_points[1]-2;j++){
          eta = j * dnym1;
          for(i=1;i<=grid_points[0]-2;i++){
             xi = i * dnxm1;

             for(k=0;k<=grid_points[2]-1;k++){
                zeta = k * dnzm1;

                 exact_solution(xi, eta, zeta, dtemp,0);
                for(m=0;m<=4;m++){
                   ue[k+m*jsize3] = dtemp[m];
                }

                dtpp = 1.0/dtemp[0];

                for(m=1;m<=4;m++){
                   buf[k+m*jsize3] = dtpp * dtemp[m];
                }

                cuf[k]   = buf[k+3*jsize3] * buf[k+3*jsize3];
                buf[k+0*jsize3] = cuf[k] + buf[k+1*jsize3] * buf[k+1*jsize3] +
                           buf[k+2*jsize3] * buf[k+2*jsize3];
                q[k] = 0.5*(buf[k+1*jsize3]*ue[k+1*jsize3] + buf[k+2*jsize3]*ue[k+2*jsize3] +
                              buf[k+3*jsize3]*ue[k+3*jsize3]);
             }

             for(k=1;k<=grid_points[2]-2;k++){
                km1 = k-1;
                kp1 = k+1;

                forcing[0+i*isize1+j*jsize1+k*ksize1] = forcing[0+i*isize1+j*jsize1+k*ksize1] -
                       tz2*( ue[kp1+3*jsize3]-ue[km1+3*jsize3] )+
                       dz1tz1*(ue[kp1+0*jsize3]-2.0*ue[k+0*jsize3]+ue[km1+0*jsize3]);

                forcing[1+i*isize1+j*jsize1+k*ksize1] = forcing[1+i*isize1+j*jsize1+k*ksize1] - tz2 * (
                       ue[kp1+1*jsize3]*buf[kp1+3*jsize3]-ue[km1+1*jsize3]*buf[km1+3*jsize3])+
                       zzcon2*(buf[kp1+1*jsize3]-2.0*buf[k+1*jsize3]+buf[km1+1*jsize3])+
                       dz2tz1*( ue[kp1+1*jsize3]-2.0* ue[k+1*jsize3]+ ue[km1+1*jsize3]);

                forcing[2+i*isize1+j*jsize1+k*ksize1] = forcing[2+i*isize1+j*jsize1+k*ksize1] - tz2 * (
                       ue[kp1+2*jsize3]*buf[kp1+3*jsize3]-ue[km1+2*jsize3]*buf[km1+3*jsize3])+
                       zzcon2*(buf[kp1+2*jsize3]-2.0*buf[k+2*jsize3]+buf[km1+2*jsize3])+
                       dz3tz1*(ue[kp1+2*jsize3]-2.0*ue[k+2*jsize3]+ue[km1+2*jsize3]);

                forcing[3+i*isize1+j*jsize1+k*ksize1] = forcing[3+i*isize1+j*jsize1+k*ksize1] - tz2 * (
                      (ue[kp1+3*jsize3]*buf[kp1+3*jsize3]+c2*(ue[kp1+4*jsize3]-q[kp1]))-
                      (ue[km1+3*jsize3]*buf[km1+3*jsize3]+c2*(ue[km1+4*jsize3]-q[km1])))+
                      zzcon1*(buf[kp1+3*jsize3]-2.0*buf[k+3*jsize3]+buf[km1+3*jsize3])+
                      dz4tz1*( ue[kp1+3*jsize3]-2.0*ue[k+3*jsize3] +ue[km1+3*jsize3]);

                forcing[4+i*isize1+j*jsize1+k*ksize1] = forcing[4+i*isize1+j*jsize1+k*ksize1] - tz2 * (
                       buf[kp1+3*jsize3]*(c1*ue[kp1+4*jsize3]-c2*q[kp1])-
                       buf[km1+3*jsize3]*(c1*ue[km1+4*jsize3]-c2*q[km1]))+
                       0.5*zzcon3*(buf[kp1+0*jsize3]-2.0*buf[k+0*jsize3]
                                    +buf[km1+0*jsize3])+
                       zzcon4*(cuf[kp1]-2.0*cuf[k]+cuf[km1])+
                       zzcon5*(buf[kp1+4*jsize3]-2.0*buf[k+4*jsize3]+buf[km1+4*jsize3])+
                       dz5tz1*( ue[kp1+4*jsize3]-2.0*ue[k+4*jsize3]+ ue[km1+4*jsize3]);
             }

//---------------------------------------------------------------------
//            Fourth-order dissipation
//---------------------------------------------------------------------
             for(m=0;m<=4;m++){
                k = 1;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          (5.0*ue[k+m*jsize3] - 4.0*ue[k+1+m*jsize3] +ue[k+2+m*jsize3]);
                k = 2;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (-4.0*ue[k-1+m*jsize3] + 6.0*ue[k+m*jsize3] -
                           4.0*ue[k+1+m*jsize3] +       ue[k+2+m*jsize3]);
             }

             for(m=0;m<=4;m++){
                for(k=3;k<=grid_points[2]-4;k++){
                   forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp*
                         (ue[k-2+m*jsize3] - 4.0*ue[k-1+m*jsize3] +
                          6.0*ue[k+m*jsize3] - 4.0*ue[k+1+m*jsize3] + ue[k+2+m*jsize3]);
                }
             }

             for(m=0;m<=4;m++){
                k = grid_points[2]-3;
                forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (ue[k-2+m*jsize3] - 4.0*ue[k-1+m*jsize3] +
                          6.0*ue[k+m*jsize3] - 4.0*ue[k+1+m*jsize3]);
                   k = grid_points[2]-2;
                   forcing[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                         (ue[k-2+m*jsize3] - 4.0*ue[k-1+m*jsize3] + 5.0*ue[k+m*jsize3]);
             }
          }
       }

//---------------------------------------------------------------------
// now change the sign of the forcing function, 
//---------------------------------------------------------------------
       for(k=1;k<=grid_points[2]-2;k++){
          for(j=1;j<=grid_points[1]-2;j++){
             for(i=1;i<=grid_points[0]-2;i++){
                for(m=0;m<=4;m++){
                   forcing[m+i*isize1+j*jsize1+k*ksize1] = -1. * forcing[m+i*isize1+j*jsize1+k*ksize1];
                }
             }
          }
       }
  }  


  public void ninvr(){
           int  i, j, k;
       double r1, r2, r3, r4, r5, t1, t2;

       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){

                r1 = rhs[0+i*isize1+j*jsize1+k*ksize1];
                r2 = rhs[1+i*isize1+j*jsize1+k*ksize1];
                r3 = rhs[2+i*isize1+j*jsize1+k*ksize1];
                r4 = rhs[3+i*isize1+j*jsize1+k*ksize1];
                r5 = rhs[4+i*isize1+j*jsize1+k*ksize1];
               
                t1 = bt * r3;
                t2 = 0.5 * ( r4 + r5 );

                rhs[0+i*isize1+j*jsize1+k*ksize1] = -r2;
                rhs[1+i*isize1+j*jsize1+k*ksize1] =  r1;
                rhs[2+i*isize1+j*jsize1+k*ksize1] = bt * ( r4 - r5 );
                rhs[3+i*isize1+j*jsize1+k*ksize1] = -t1 + t2;
                rhs[4+i*isize1+j*jsize1+k*ksize1] =  t1 + t2;
             }    
          }
      }
  }

  public void pinvr(){
           int i, j, k;
       double r1, r2, r3, r4, r5, t1, t2;

       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){

                r1 = rhs[0+i*isize1+j*jsize1+k*ksize1];
                r2 = rhs[1+i*isize1+j*jsize1+k*ksize1];
                r3 = rhs[2+i*isize1+j*jsize1+k*ksize1];
                r4 = rhs[3+i*isize1+j*jsize1+k*ksize1];
                r5 = rhs[4+i*isize1+j*jsize1+k*ksize1];

                t1 = bt * r1;
                t2 = 0.5 * ( r4 + r5 );

                rhs[0+i*isize1+j*jsize1+k*ksize1] =  bt * ( r4 - r5 );
                rhs[1+i*isize1+j*jsize1+k*ksize1] = -r3;
                rhs[2+i*isize1+j*jsize1+k*ksize1] =  r2;
                rhs[3+i*isize1+j*jsize1+k*ksize1] = -t1 + t2;
                rhs[4+i*isize1+j*jsize1+k*ksize1] =  t1 + t2;
             }
          }
      }
  }

  public void compute_rhs(){
    int i, j, k, m;
    double aux, rho_inv, uijk, up1, um1, vijk, vp1, vm1,
           wijk, wp1, wm1;
//---------------------------------------------------------------------
//      compute the reciprocal of density, and the kinetic energy, 
//      and the speed of sound. 
//---------------------------------------------------------------------

       for(k=0;k<=grid_points[2]-1;k++){
          for(j=0;j<=grid_points[1]-1;j++){
             for(i=0;i<=grid_points[0]-1;i++){
                rho_inv = 1.0/u[0+i*isize1+j*jsize1+k*ksize1];
                rho_i[i+j*jsize2+k*ksize2] = rho_inv;
                us[i+j*jsize2+k*ksize2] = u[1+i*isize1+j*jsize1+k*ksize1] * rho_inv;
                vs[i+j*jsize2+k*ksize2] = u[2+i*isize1+j*jsize1+k*ksize1] * rho_inv;
                ws[i+j*jsize2+k*ksize2] = u[3+i*isize1+j*jsize1+k*ksize1] * rho_inv;
                square[i+j*jsize2+k*ksize2]     = 0.5* (
                              u[1+i*isize1+j*jsize1+k*ksize1]*u[1+i*isize1+j*jsize1+k*ksize1] + 
                              u[2+i*isize1+j*jsize1+k*ksize1]*u[2+i*isize1+j*jsize1+k*ksize1] +
                              u[3+i*isize1+j*jsize1+k*ksize1]*u[3+i*isize1+j*jsize1+k*ksize1] ) * rho_inv;
                qs[i+j*jsize2+k*ksize2] = square[i+j*jsize2+k*ksize2] * rho_inv;
//---------------------------------------------------------------------
//               (don't need speed and ainx until the lhs computation)
//---------------------------------------------------------------------
                aux = c1c2*rho_inv* (u[4+i*isize1+j*jsize1+k*ksize1] - square[i+j*jsize2+k*ksize2]);
                speed[i+j*jsize2+k*ksize2] = Math.sqrt(aux);
             }
          }
       }
    
//---------------------------------------------------------------------
// copy the exact forcing term to the right hand side;  because 
// this forcing term is known, we can store it on the whole grid
// including the boundary                   
//---------------------------------------------------------------------

       for(k=0;k<=grid_points[2]-1;k++){
          for(j=0;j<=grid_points[1]-1;j++){
             for(i=0;i<=grid_points[0]-1;i++){
                for(m=0;m<=4;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = forcing[m+i*isize1+j*jsize1+k*ksize1];
                }
             }
          }
       }

//---------------------------------------------------------------------
//      compute xi-direction fluxes 
//---------------------------------------------------------------------
       if (timeron) timer.start(t_rhsx);
       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){
                uijk = us[i+j*jsize2+k*ksize2];
                up1  = us[i+1+j*jsize2+k*ksize2];
                um1  = us[i-1+j*jsize2+k*ksize2];

                rhs[0+i*isize1+j*jsize1+k*ksize1] = rhs[0+i*isize1+j*jsize1+k*ksize1] + dx1tx1 * 
                          (u[0+(i+1)*isize1+j*jsize1+k*ksize1] - 2.0*u[0+i*isize1+j*jsize1+k*ksize1] + 
                           u[0+(i-1)*isize1+j*jsize1+k*ksize1]) -
                          tx2 * (u[1+(i+1)*isize1+j*jsize1+k*ksize1] - u[1+(i-1)*isize1+j*jsize1+k*ksize1]);

                rhs[1+i*isize1+j*jsize1+k*ksize1] = rhs[1+i*isize1+j*jsize1+k*ksize1] + dx2tx1 * 
                          (u[1+(i+1)*isize1+j*jsize1+k*ksize1] - 2.0*u[1+i*isize1+j*jsize1+k*ksize1] + 
                           u[1+(i-1)*isize1+j*jsize1+k*ksize1]) +
                          xxcon2*con43 * (up1 - 2.0*uijk + um1) -
                          tx2 * (u[1+(i+1)*isize1+j*jsize1+k*ksize1]*up1 - 
                                 u[1+(i-1)*isize1+j*jsize1+k*ksize1]*um1 +
                                 (u[4+(i+1)*isize1+j*jsize1+k*ksize1]- square[(i+1)+j*jsize2+k*ksize2]-
                                  u[4+(i-1)*isize1+j*jsize1+k*ksize1]+ square[(i-1)+j*jsize2+k*ksize2])*
                                  c2);

                rhs[2+i*isize1+j*jsize1+k*ksize1] = rhs[2+i*isize1+j*jsize1+k*ksize1] + dx3tx1 * 
                          (u[2+(i+1)*isize1+j*jsize1+k*ksize1] - 2.0*u[2+i*isize1+j*jsize1+k*ksize1] +
                           u[2+(i-1)*isize1+j*jsize1+k*ksize1]) +
                          xxcon2 * (vs[i+1+j*jsize2+k*ksize2] - 2.0*vs[i+j*jsize2+k*ksize2] +
                                    vs[i-1+j*jsize2+k*ksize2]) -
                          tx2 * (u[2+(i+1)*isize1+j*jsize1+k*ksize1]*up1 - 
                                 u[2+(i-1)*isize1+j*jsize1+k*ksize1]*um1);

                rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] + dx4tx1 * 
                          (u[3+(i+1)*isize1+j*jsize1+k*ksize1] - 2.0*u[3+i*isize1+j*jsize1+k*ksize1] +
                           u[3+(i-1)*isize1+j*jsize1+k*ksize1]) +
                          xxcon2 * (ws[i+1+j*jsize2+k*ksize2] - 2.0*ws[i+j*jsize2+k*ksize2] +
                                    ws[i-1+j*jsize2+k*ksize2]) -
                          tx2 * (u[3+(i+1)*isize1+j*jsize1+k*ksize1]*up1 - 
                                 u[3+(i-1)*isize1+j*jsize1+k*ksize1]*um1);

                rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] + dx5tx1 * 
                          (u[4+(i+1)*isize1+j*jsize1+k*ksize1] - 2.0*u[4+i*isize1+j*jsize1+k*ksize1] +
                           u[4+(i-1)*isize1+j*jsize1+k*ksize1]) +
                          xxcon3 * (qs[i+1+j*jsize2+k*ksize2] - 2.0*qs[i+j*jsize2+k*ksize2] +
                                    qs[i-1+j*jsize2+k*ksize2]) +
                          xxcon4 * (up1*up1 -       2.0*uijk*uijk + 
                                    um1*um1) +
                          xxcon5 * (u[4+(i+1)*isize1+j*jsize1+k*ksize1]*rho_i[i+1+j*jsize2+k*ksize2] - 
                                    2.0*u[4+i*isize1+j*jsize1+k*ksize1]*rho_i[i+j*jsize2+k*ksize2] +
                                    u[4+(i-1)*isize1+j*jsize1+k*ksize1]*rho_i[i-1+j*jsize2+k*ksize2]) -
                          tx2 * ( (c1*u[4+(i+1)*isize1+j*jsize1+k*ksize1] - 
                                   c2*square[i+1+j*jsize2+k*ksize2])*up1 -
                                  (c1*u[4+(i-1)*isize1+j*jsize1+k*ksize1] - 
                                   c2*square[i-1+j*jsize2+k*ksize2])*um1 );
             }

//---------------------------------------------------------------------
//      add fourth order xi-direction dissipation               
//---------------------------------------------------------------------

             i = 1;
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1]- dssp * 
                          ( 5.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+(i+1)*isize1+j*jsize1+k*ksize1] +
                                  u[m+(i+2)*isize1+j*jsize1+k*ksize1]);
             }

             i = 2;
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp * 
                          (-4.0*u[m+(i-1)*isize1+j*jsize1+k*ksize1] + 6.0*u[m+i*isize1+j*jsize1+k*ksize1] -
                            4.0*u[m+(i+1)*isize1+j*jsize1+k*ksize1] + u[m+(i+2)*isize1+j*jsize1+k*ksize1]);
             }

             for(i=3;i<=nx2-2;i++){
                for(m=0;m<=4;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp * 
                          (  u[m+(i-2)*isize1+j*jsize1+k*ksize1] - 4.0*u[m+(i-1)*isize1+j*jsize1+k*ksize1] + 
                           6.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+(i+1)*isize1+j*jsize1+k*ksize1] + 
                               u[m+(i+2)*isize1+j*jsize1+k*ksize1] );
                }
             }

             i = nx2-1;
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          ( u[m+(i-2)*isize1+j*jsize1+k*ksize1] - 4.0*u[m+(i-1)*isize1+j*jsize1+k*ksize1] + 
                            6.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+(i+1)*isize1+j*jsize1+k*ksize1] );
             }

             i = nx2;
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          ( u[m+(i-2)*isize1+j*jsize1+k*ksize1] - 4.*u[m+(i-1)*isize1+j*jsize1+k*ksize1] +
                            5.*u[m+i*isize1+j*jsize1+k*ksize1] );
             }
          }
       }
       if (timeron) timer.stop(t_rhsx);
    
//---------------------------------------------------------------------
//      compute eta-direction fluxes 
//---------------------------------------------------------------------
       if (timeron) timer.start(t_rhsy);
       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){
                vijk = vs[i+j*jsize2+k*ksize2];
                vp1  = vs[i+(j+1)*jsize2+k*ksize2];
                vm1  = vs[i+(j-1)*jsize2+k*ksize2];
                rhs[0+i*isize1+j*jsize1+k*ksize1] = rhs[0+i*isize1+j*jsize1+k*ksize1] + dy1ty1 * 
                         (u[0+i*isize1+(j+1)*jsize1+k*ksize1] - 2.0*u[0+i*isize1+j*jsize1+k*ksize1] + 
                          u[0+i*isize1+(j-1)*jsize1+k*ksize1]) -
                         ty2 * (u[2+i*isize1+(j+1)*jsize1+k*ksize1] - u[2+i*isize1+(j-1)*jsize1+k*ksize1]);
                rhs[1+i*isize1+j*jsize1+k*ksize1] = rhs[1+i*isize1+j*jsize1+k*ksize1] + dy2ty1 * 
                         (u[1+i*isize1+(j+1)*jsize1+k*ksize1] - 2.0*u[1+i*isize1+j*jsize1+k*ksize1] + 
                          u[1+i*isize1+(j-1)*jsize1+k*ksize1]) +
                         yycon2 * (us[i+(j+1)*jsize2+k*ksize2] - 2.0*us[i+j*jsize2+k*ksize2] + 
                                   us[i+(j-1)*jsize2+k*ksize2]) -
                         ty2 * (u[1+i*isize1+(j+1)*jsize1+k*ksize1]*vp1 - 
                                u[1+i*isize1+(j-1)*jsize1+k*ksize1]*vm1);
                rhs[2+i*isize1+j*jsize1+k*ksize1] = rhs[2+i*isize1+j*jsize1+k*ksize1] + dy3ty1 * 
                         (u[2+i*isize1+(j+1)*jsize1+k*ksize1] - 2.0*u[2+i*isize1+j*jsize1+k*ksize1] + 
                          u[2+i*isize1+(j-1)*jsize1+k*ksize1]) +
                         yycon2*con43 * (vp1 - 2.0*vijk + vm1) -
                         ty2 * (u[2+i*isize1+(j+1)*jsize1+k*ksize1]*vp1 - 
                                u[2+i*isize1+(j-1)*jsize1+k*ksize1]*vm1 +
                                (u[4+i*isize1+(j+1)*jsize1+k*ksize1] - square[i+(j+1)*jsize2+k*ksize2] - 
                                 u[4+i*isize1+(j-1)*jsize1+k*ksize1] + square[i+(j-1)*jsize2+k*ksize2])
                                *c2);
                rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] + dy4ty1 * 
                         (u[3+i*isize1+(j+1)*jsize1+k*ksize1] - 2.0*u[3+i*isize1+j*jsize1+k*ksize1] + 
                          u[3+i*isize1+(j-1)*jsize1+k*ksize1]) +
                         yycon2 * (ws[i+(j+1)*jsize2+k*ksize2] - 2.0*ws[i+j*jsize2+k*ksize2] + 
                                   ws[i+(j-1)*jsize2+k*ksize2]) -
                         ty2 * (u[3+i*isize1+(j+1)*jsize1+k*ksize1]*vp1 - 
                                u[3+i*isize1+(j-1)*jsize1+k*ksize1]*vm1);
                rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] + dy5ty1 * 
                         (u[4+i*isize1+(j+1)*jsize1+k*ksize1] - 2.0*u[4+i*isize1+j*jsize1+k*ksize1] + 
                          u[4+i*isize1+(j-1)*jsize1+k*ksize1]) +
                         yycon3 * (qs[i+(j+1)*jsize2+k*ksize2] - 2.0*qs[i+j*jsize2+k*ksize2] + 
                                   qs[i+(j-1)*jsize2+k*ksize2]) +
                         yycon4 * (vp1*vp1       - 2.0*vijk*vijk + 
                                   vm1*vm1) +
                         yycon5 * (u[4+i*isize1+(j+1)*jsize1+k*ksize1]*rho_i[i+(j+1)*jsize2+k*ksize2] - 
                                   2.0*u[4+i*isize1+j*jsize1+k*ksize1]*rho_i[i+j*jsize2+k*ksize2] +
                                   u[4+i*isize1+(j-1)*jsize1+k*ksize1]*rho_i[i+(j-1)*jsize2+k*ksize2]) -
                         ty2 * ((c1*u[4+i*isize1+(j+1)*jsize1+k*ksize1] - 
                                 c2*square[i+(j+1)*jsize2+k*ksize2]) * vp1 -
                                (c1*u[4+i*isize1+(j-1)*jsize1+k*ksize1] - 
                                 c2*square[i+(j-1)*jsize2+k*ksize2]) * vm1);
             }
          }

//---------------------------------------------------------------------
//      add fourth order eta-direction dissipation         
//---------------------------------------------------------------------

          j = 1;
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1]- dssp * 
                          ( 5.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+i*isize1+(j+1)*jsize1+k*ksize1] +
                                  u[m+i*isize1+(j+2)*jsize1+k*ksize1]);
             }
          }

          j = 2;
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp * 
                          (-4.0*u[m+i*isize1+(j-1)*jsize1+k*ksize1] + 6.0*u[m+i*isize1+j*jsize1+k*ksize1] -
                            4.0*u[m+i*isize1+(j+1)*jsize1+k*ksize1] + u[m+i*isize1+(j+2)*jsize1+k*ksize1]);
             }
          }

          for(j=3;j<=ny2-2;j++){
             for(i=1;i<=nx2;i++){
                for(m=0;m<=4;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp * 
                          (  u[m+i*isize1+(j-2)*jsize1+k*ksize1] - 4.0*u[m+i*isize1+(j-1)*jsize1+k*ksize1] + 
                           6.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+i*isize1+(j+1)*jsize1+k*ksize1] + 
                               u[m+i*isize1+(j+2)*jsize1+k*ksize1] );
                }
             }
          }
 
          j = ny2-1;
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          ( u[m+i*isize1+(j-2)*jsize1+k*ksize1] - 4.0*u[m+i*isize1+(j-1)*jsize1+k*ksize1] + 
                            6.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+i*isize1+(j+1)*jsize1+k*ksize1] );
             }
          }

          j = ny2;
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          ( u[m+i*isize1+(j-2)*jsize1+k*ksize1] - 4.*u[m+i*isize1+(j-1)*jsize1+k*ksize1] +
                            5.*u[m+i*isize1+j*jsize1+k*ksize1] );
             }
          }
       }
       if (timeron) timer.stop(t_rhsy);

//---------------------------------------------------------------------
//      compute zeta-direction fluxes 
//---------------------------------------------------------------------
       if (timeron) timer.start(t_rhsz);
       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){
                wijk = ws[i+j*jsize2+k*ksize2];
                wp1  = ws[i+j*jsize2+(k+1)*ksize2];
                wm1  = ws[i+j*jsize2+(k-1)*ksize2];

                rhs[0+i*isize1+j*jsize1+k*ksize1] = rhs[0+i*isize1+j*jsize1+k*ksize1] + dz1tz1 * 
                         (u[0+i*isize1+j*jsize1+(k+1)*ksize1] - 2.0*u[0+i*isize1+j*jsize1+k*ksize1] + 
                          u[0+i*isize1+j*jsize1+(k-1)*ksize1]) -
                         tz2 * (u[3+i*isize1+j*jsize1+(k+1)*ksize1] - u[3+i*isize1+j*jsize1+(k-1)*ksize1]);
                rhs[1+i*isize1+j*jsize1+k*ksize1] = rhs[1+i*isize1+j*jsize1+k*ksize1] + dz2tz1 * 
                         (u[1+i*isize1+j*jsize1+(k+1)*ksize1] - 2.0*u[1+i*isize1+j*jsize1+k*ksize1] + 
                          u[1+i*isize1+j*jsize1+(k-1)*ksize1]) +
                         zzcon2 * (us[i+j*jsize2+(k+1)*ksize2] - 2.0*us[i+j*jsize2+k*ksize2] + 
                                   us[i+j*jsize2+(k-1)*ksize2]) -
                         tz2 * (u[1+i*isize1+j*jsize1+(k+1)*ksize1]*wp1 - 
                                u[1+i*isize1+j*jsize1+(k-1)*ksize1]*wm1);
                rhs[2+i*isize1+j*jsize1+k*ksize1] = rhs[2+i*isize1+j*jsize1+k*ksize1] + dz3tz1 * 
                         (u[2+i*isize1+j*jsize1+(k+1)*ksize1] - 2.0*u[2+i*isize1+j*jsize1+k*ksize1] + 
                          u[2+i*isize1+j*jsize1+(k-1)*ksize1]) +
                         zzcon2 * (vs[i+j*jsize2+(k+1)*ksize2] - 2.0*vs[i+j*jsize2+k*ksize2] + 
                                   vs[i+j*jsize2+(k-1)*ksize2]) -
                         tz2 * (u[2+i*isize1+j*jsize1+(k+1)*ksize1]*wp1 - 
                                u[2+i*isize1+j*jsize1+(k-1)*ksize1]*wm1);
                rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] + dz4tz1 * 
                         (u[3+i*isize1+j*jsize1+(k+1)*ksize1] - 2.0*u[3+i*isize1+j*jsize1+k*ksize1] + 
                          u[3+i*isize1+j*jsize1+(k-1)*ksize1]) +
                         zzcon2*con43 * (wp1 - 2.0*wijk + wm1) -
                         tz2 * (u[3+i*isize1+j*jsize1+(k+1)*ksize1]*wp1 - 
                                u[3+i*isize1+j*jsize1+(k-1)*ksize1]*wm1 +
                                (u[4+i*isize1+j*jsize1+(k+1)*ksize1] - square[i+j*jsize2+(k+1)*ksize2] - 
                                 u[4+i*isize1+j*jsize1+(k-1)*ksize1] + square[i+j*jsize2+(k-1)*ksize2])
                                *c2);
                rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] + dz5tz1 * 
                         (u[4+i*isize1+j*jsize1+(k+1)*ksize1] - 2.0*u[4+i*isize1+j*jsize1+k*ksize1] + 
                          u[4+i*isize1+j*jsize1+(k-1)*ksize1]) +
                         zzcon3 * (qs[i+j*jsize2+(k+1)*ksize2] - 2.0*qs[i+j*jsize2+k*ksize2] + 
                                   qs[i+j*jsize2+(k-1)*ksize2]) +
                         zzcon4 * (wp1*wp1 - 2.0*wijk*wijk + 
                                   wm1*wm1) +
                         zzcon5 * (u[4+i*isize1+j*jsize1+(k+1)*ksize1]*rho_i[i+j*jsize2+(k+1)*ksize2] - 
                                   2.0*u[4+i*isize1+j*jsize1+k*ksize1]*rho_i[i+j*jsize2+k*ksize2] +
                                   u[4+i*isize1+j*jsize1+(k-1)*ksize1]*rho_i[i+j*jsize2+(k-1)*ksize2]) -
                         tz2 * ( (c1*u[4+i*isize1+j*jsize1+(k+1)*ksize1] - 
                                  c2*square[i+j*jsize2+(k+1)*ksize2])*wp1 -
                                 (c1*u[4+i*isize1+j*jsize1+(k-1)*ksize1] - 
                                  c2*square[i+j*jsize2+(k-1)*ksize2])*wm1);
             }
          }
       }

//---------------------------------------------------------------------
//      add fourth order zeta-direction dissipation                
//---------------------------------------------------------------------

       k = 1;
       for(j=1;j<=ny2;j++){
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1]- dssp * 
                          ( 5.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+i*isize1+j*jsize1+(k+1)*ksize1] +
                                  u[m+i*isize1+j*jsize1+(k+2)*ksize1]);
             }
          }
       }

       k = 2;
       for(j=1;j<=ny2;j++){
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp * 
                          (-4.0*u[m+i*isize1+j*jsize1+(k-1)*ksize1] + 6.0*u[m+i*isize1+j*jsize1+k*ksize1] -
                            4.0*u[m+i*isize1+j*jsize1+(k+1)*ksize1] + u[m+i*isize1+j*jsize1+(k+2)*ksize1]);
             }
          }
       }

       for(k=3;k<=nz2-2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){
                for(m=0;m<=4;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp * 
                          (  u[m+i*isize1+j*jsize1+(k-2)*ksize1] - 4.0*u[m+i*isize1+j*jsize1+(k-1)*ksize1] + 
                           6.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+i*isize1+j*jsize1+(k+1)*ksize1] + 
                               u[m+i*isize1+j*jsize1+(k+2)*ksize1] );
                }
             }
          }
       }
 
       k = nz2-1;
       for(j=1;j<=ny2;j++){
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          ( u[m+i*isize1+j*jsize1+(k-2)*ksize1] - 4.0*u[m+i*isize1+j*jsize1+(k-1)*ksize1] + 
                            6.0*u[m+i*isize1+j*jsize1+k*ksize1] - 4.0*u[m+i*isize1+j*jsize1+(k+1)*ksize1] );
             }
          }
       }

       k = nz2;
       for(j=1;j<=ny2;j++){
          for(i=1;i<=nx2;i++){
             for(m=0;m<=4;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - dssp *
                          ( u[m+i*isize1+j*jsize1+(k-2)*ksize1] - 4.*u[m+i*isize1+j*jsize1+(k-1)*ksize1] +
                            5.*u[m+i*isize1+j*jsize1+k*ksize1] );
             }
          }
       }
       if (timeron) timer.stop(t_rhsz);
    

       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){
             for(i=1;i<=nx2;i++){
                for(m=0;m<=4;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] =
		      rhs[m+i*isize1+j*jsize1+k*ksize1] * dt;
                }
             }
          }
       }
  }
  

  public void txinvr(){
    int i, j, k;
    double t1, t2, t3, ac, ru1, uu, vv, ww, 
           r1, r2, r3, r4, r5, ac2inv;

    for(k=1;k<=nz2;k++){
       for(j=1;j<=ny2;j++){
    	  for(i=1;i<=nx2;i++){

    	     ru1 = rho_i[i+j*jsize2+k*ksize2];
    	     uu = us[i+j*jsize2+k*ksize2];
    	     vv = vs[i+j*jsize2+k*ksize2];
    	     ww = ws[i+j*jsize2+k*ksize2];
    	     ac = speed[i+j*jsize2+k*ksize2];
    	     ac2inv = 1.0 / ( ac*ac );

    	     r1 = rhs[0+i*isize1+j*jsize1+k*ksize1];
    	     r2 = rhs[1+i*isize1+j*jsize1+k*ksize1];
    	     r3 = rhs[2+i*isize1+j*jsize1+k*ksize1];
    	     r4 = rhs[3+i*isize1+j*jsize1+k*ksize1];
    	     r5 = rhs[4+i*isize1+j*jsize1+k*ksize1];

    	     t1 = c2 * ac2inv * ( qs[i+j*jsize2+k*ksize2]*r1 - uu*r2  - 
    		     vv*r3 - ww*r4 + r5 );
    	     t2 = bt * ru1 * ( uu * r1 - r2 );
    	     t3 = ( bt * ru1 * ac ) * t1;

    	     rhs[0+i*isize1+j*jsize1+k*ksize1] = r1 - t1;
    	     rhs[1+i*isize1+j*jsize1+k*ksize1] = - ru1 * ( ww*r1 - r4 );
    	     rhs[2+i*isize1+j*jsize1+k*ksize1] =   ru1 * ( vv*r1 - r3 );
    	     rhs[3+i*isize1+j*jsize1+k*ksize1] = - t2 + t3;
    	     rhs[4+i*isize1+j*jsize1+k*ksize1] =   t2 + t3;

    	  }
       }
    }
  }

  public void tzetar(){
    int i, j, k;
    double  t1, t2, t3, ac, xvel, yvel, zvel,  
            r1, r2, r3, r4, r5, btuz, acinv, ac2u, uzik1;

    for(k=1;k<=nz2;k++){
       for(j=1;j<=ny2;j++){
    	  for(i=1;i<=nx2;i++){

    	     xvel = us[i+j*jsize2+k*ksize2];
    	     yvel = vs[i+j*jsize2+k*ksize2];
    	     zvel = ws[i+j*jsize2+k*ksize2];
    	     ac   = speed[i+j*jsize2+k*ksize2];

    	     ac2u = ac*ac;

    	     r1 = rhs[0+i*isize1+j*jsize1+k*ksize1];
    	     r2 = rhs[1+i*isize1+j*jsize1+k*ksize1];
    	     r3 = rhs[2+i*isize1+j*jsize1+k*ksize1];
    	     r4 = rhs[3+i*isize1+j*jsize1+k*ksize1];
    	     r5 = rhs[4+i*isize1+j*jsize1+k*ksize1]	 ;

    	     uzik1 = u[0+i*isize1+j*jsize1+k*ksize1];
    	     btuz  = bt * uzik1;

    	     t1 = btuz/ac * (r4 + r5);
    	     t2 = r3 + t1;
    	     t3 = btuz * (r4 - r5);

    	     rhs[0+i*isize1+j*jsize1+k*ksize1] = t2;
    	     rhs[1+i*isize1+j*jsize1+k*ksize1] = -uzik1*r2 + xvel*t2;
    	     rhs[2+i*isize1+j*jsize1+k*ksize1] =  uzik1*r1 + yvel*t2;
    	     rhs[3+i*isize1+j*jsize1+k*ksize1] =  zvel*t2  + t3;
    	     rhs[4+i*isize1+j*jsize1+k*ksize1] =  uzik1*(-xvel*r2 + yvel*r1) +
    		       qs[i+j*jsize2+k*ksize2]*t2 + c2iv*ac2u*t1 + zvel*t3;

    	  }
       }
    }
  }
  
  public int verify(int no_time_steps){
    double xcrref[] = new double[5],xceref[] = new double[5],
	   xcrdif[] = new double[5],xcedif[] = new double[5],
	   xce[] = new double[5],xcr[] = new double[5],
	   dtref=0;
    int m;
    int verified=-1;
    char clss = 'U';
//---------------------------------------------------------------------
//   compute the error norm and the residual norm, and exit if not printing
//---------------------------------------------------------------------
    error_norm(xce);
    compute_rhs();
    rhs_norm(xcr);

    for(m=0;m<=4;m++) xcr[m] = xcr[m] / dt;

    for(m=0;m<=4;m++){
      xcrref[m] = 1.0;
      xceref[m] = 1.0;
    }
//---------------------------------------------------------------------
//    reference data for 12X12X12 grids after 100 time steps, with DT = 1.50d-02
//---------------------------------------------------------------------
    if (  grid_points[0]  == 12 
	      &&grid_points[1]  == 12
	      &&grid_points[2]  == 12
	      &&no_time_steps   == 100 
    ){
      
      clss = 'S';
      dtref = .015;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
      xcrref[0] = 2.7470315451339479E-2;
      xcrref[1] = 1.0360746705285417E-2;
      xcrref[2] = 1.6235745065095532E-2;
      xcrref[3] = 1.5840557224455615E-2;
      xcrref[4] = 3.4849040609362460E-2;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
      xceref[0] = 2.7289258557377227E-5;
      xceref[1] = 1.0364446640837285E-5;
      xceref[2] = 1.6154798287166471E-5;
      xceref[3] = 1.5750704994480102E-5;
      xceref[4] = 3.4177666183390531E-5;


//---------------------------------------------------------------------
//    reference data for 36X36X36 grids after 400 time steps, with DT = 
//---------------------------------------------------------------------
    }else if ( (grid_points[0] == 36) && 
	       (grid_points[1] == 36) &&
	       (grid_points[2] == 36) &&
	       (no_time_steps == 400) ) {

      clss = 'W';
      dtref = .0015;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
      xcrref[0] = 0.1893253733584E-2;
      xcrref[1] = 0.1717075447775E-3;
      xcrref[2] = 0.2778153350936E-3;
      xcrref[3] = 0.2887475409984E-3;
      xcrref[4] = 0.3143611161242E-2;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
      xceref[0] = 0.7542088599534E-4;
      xceref[1] = 0.6512852253086E-5;
      xceref[2] = 0.1049092285688E-4;
      xceref[3] = 0.1128838671535E-4;
      xceref[4] = 0.1212845639773E-3;

//---------------------------------------------------------------------
//    reference data for 64X64X64 grids after 400 time steps, with DT = 1.5d-03
//---------------------------------------------------------------------
    }else if ( (grid_points[0] == 64) && 
	       (grid_points[1] == 64) &&
	       (grid_points[2] == 64) &&
	       (no_time_steps  == 400) ) {

      clss = 'A';
      dtref = .0015;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
      xcrref[0] = 2.4799822399300195;
      xcrref[1] = 1.1276337964368832;
      xcrref[2] = 1.5028977888770491;
      xcrref[3] = 1.4217816211695179;
      xcrref[4] = 2.1292113035138280;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
      xceref[0] = 1.0900140297820550E-4;
      xceref[1] = 3.7343951769282091E-5;
      xceref[2] = 5.0092785406541633E-5;
      xceref[3] = 4.7671093939528255E-5;
      xceref[4] = 1.3621613399213001E-4;

//---------------------------------------------------------------------
//    reference data for 102X102X102 grids after 400 time steps,
//    with DT = 1.0d-03
//---------------------------------------------------------------------
    }else if ( (grid_points[0] == 102) && 
	       (grid_points[1] == 102) &&
	       (grid_points[2] == 102) &&
	       (no_time_steps  == 400) ) {

      clss = 'B';
      dtref = .001;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
      xcrref[0] = 0.6903293579998E+02;
      xcrref[1] = 0.3095134488084E+02;
      xcrref[2] = 0.4103336647017E+02;
      xcrref[3] = 0.3864769009604E+02;
      xcrref[4] = 0.5643482272596E+02;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
      xceref[0] = 0.9810006190188E-02;
      xceref[1] = 0.1022827905670E-02;
      xceref[2] = 0.1720597911692E-02;
      xceref[3] = 0.1694479428231E-02;
      xceref[4] = 0.1847456263981E-01;

//---------------------------------------------------------------------
//    reference data for 162X162X162 grids after 400 time steps,
//    with DT = 0.67d-03
//---------------------------------------------------------------------
    }else if ( (grid_points[0] == 162) && 
	       (grid_points[1] == 162) &&
	       (grid_points[2] == 162) &&
	       (no_time_steps  == 400) ) {

      clss = 'C';
      dtref = .00067;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
      xcrref[0] = 0.5881691581829E+03;
      xcrref[1] = 0.2454417603569E+03;
      xcrref[2] = 0.3293829191851E+03;
      xcrref[3] = 0.3081924971891E+03;
      xcrref[4] = 0.4597223799176E+03;

//---------------------------------------------------------------------
//    Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
      xceref[0] = 0.2598120500183;
      xceref[1] = 0.2590888922315E-01;
      xceref[2] = 0.5132886416320E-01;
      xceref[3] = 0.4806073419454E-01;
      xceref[4] = 0.5483377491301;   
    }
//---------------------------------------------------------------------
//    verification test for residuals if gridsize is either 12X12X12 or 
//    64X64X64 or 102X102X102 or 162X162X162
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//    Compute the difference of solution values and the known reference values.
//---------------------------------------------------------------------
    for(m=0;m<=4;m++){
      xcrdif[m] = Math.abs((xcr[m]-xcrref[m])/xcrref[m]) ;
      xcedif[m] = Math.abs((xce[m]-xceref[m])/xceref[m]);
    }
//---------------------------------------------------------------------
//   tolerance level
//---------------------------------------------------------------------
    double epsilon = 1.0E-8;
//---------------------------------------------------------------------
//    Output the comparison of computed results to known cases.
//---------------------------------------------------------------------
    if (clss != 'U') {
      System.out.println(" Verification being performed for class " + clss);
      System.out.println(" Accuracy setting for epsilon = " + epsilon);
      if (Math.abs(dt-dtref) <= epsilon) {  
        if(verified==-1) verified=1;
      }else{
	verified = 0;
	clss = 'U';
	System.out.println("DT does not match the reference value of " + dtref );
      }
      System.out.println(" Comparison of RMS-norms of residual");
    }else{ 
      System.out.println(" Unknown CLASS");
      System.out.println(" RMS-norms of residual");
    }
    verified=BMResults.printComparisonStatus(clss,verified,epsilon,
                                             xcr,xcrref,xcrdif);
    if (clss != 'U') {
      System.out.println(" Comparison of RMS-norms of solution error");
    }else{
      System.out.println(" RMS-norms of solution error");
    }
    verified=BMResults.printComparisonStatus(clss,verified,epsilon,
                                             xce,xceref,xcedif);

    BMResults.printVerificationStatus(clss,verified,BMName); 
    return verified;
  }

  public void x_solve(){
    int i, j, k, n, i1, i2, m;
    double  ru1, fac1, fac2;

//---------------------------------------------------------------------
//---------------------------------------------------------------------

       if (timeron) timer.start(t_xsolve);
       for(k=1;k<=nz2;k++){
          for(j=1;j<=ny2;j++){

//---------------------------------------------------------------------
// Computes the left hand side for the three x-factors  
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//      first fill the lhs for the u-eigenvalue                   
//---------------------------------------------------------------------
             for(i=0;i<=grid_points[0]-1;i++){
                ru1 = c3c4*rho_i[i+j*jsize2+k*ksize2];
                cv[i] = us[i+j*jsize2+k*ksize2];
                rhon[i] = dmax1(dx2+con43*ru1, 
                                dx5+c1c5*ru1,
                                dxmax+ru1,
                                dx1);
             }

              lhsinit(grid_points[0]-1);
             for(i=1;i<=nx2;i++){
                lhs[0+i*jsize4] =   0.0;
                lhs[1+i*jsize4] = - dttx2 * cv[(i-1)] - dttx1 * rhon[i-1];
                lhs[2+i*jsize4] =   1.0 + c2dttx1 * rhon[i];
                lhs[3+i*jsize4] =   dttx2 * cv[i+1] - dttx1 * rhon[i+1];
                lhs[4+i*jsize4] =   0.0;
             }

//---------------------------------------------------------------------
//      add fourth order dissipation                             
//---------------------------------------------------------------------

             i = 1;
             lhs[2+i*jsize4] = lhs[2+i*jsize4] + comz5;
             lhs[3+i*jsize4] = lhs[3+i*jsize4] - comz4;
             lhs[4+i*jsize4] = lhs[4+i*jsize4] + comz1;
  
             lhs[1+(i+1)*jsize4] = lhs[1+(i+1)*jsize4] - comz4;
             lhs[2+(i+1)*jsize4] = lhs[2+(i+1)*jsize4] + comz6;
             lhs[3+(i+1)*jsize4] = lhs[3+(i+1)*jsize4] - comz4;
             lhs[4+(i+1)*jsize4] = lhs[4+(i+1)*jsize4] + comz1;

             for(i=3;i<=grid_points[0]-4;i++){
                lhs[0+i*jsize4] = lhs[0+i*jsize4] + comz1;
                lhs[1+i*jsize4] = lhs[1+i*jsize4] - comz4;
                lhs[2+i*jsize4] = lhs[2+i*jsize4] + comz6;
                lhs[3+i*jsize4] = lhs[3+i*jsize4] - comz4;
                lhs[4+i*jsize4] = lhs[4+i*jsize4] + comz1;
             }


             i = grid_points[0]-3;
             lhs[0+i*jsize4] = lhs[0+i*jsize4] + comz1;
             lhs[1+i*jsize4] = lhs[1+i*jsize4] - comz4;
             lhs[2+i*jsize4] = lhs[2+i*jsize4] + comz6;
             lhs[3+i*jsize4] = lhs[3+i*jsize4] - comz4;

             lhs[0+(i+1)*jsize4] = lhs[0+(i+1)*jsize4] + comz1;
             lhs[1+(i+1)*jsize4] = lhs[1+(i+1)*jsize4] - comz4;
             lhs[2+(i+1)*jsize4] = lhs[2+(i+1)*jsize4] + comz5;

//---------------------------------------------------------------------
//      subsequently, fill the other factors (u+c), (u-c) by adding to 
//      the first  
//---------------------------------------------------------------------
             for(i=1;i<=nx2;i++){
                lhsp[0+i*jsize4] = lhs[0+i*jsize4];
                lhsp[1+i*jsize4] = lhs[1+i*jsize4] - 
                                  dttx2 * speed[(i-1)+j*jsize2+k*ksize2];
                lhsp[2+i*jsize4] = lhs[2+i*jsize4];
                lhsp[3+i*jsize4] = lhs[3+i*jsize4] + 
                                  dttx2 * speed[i+1+j*jsize2+k*ksize2];
                lhsp[4+i*jsize4] = lhs[4+i*jsize4];
                lhsm[0+i*jsize4] = lhs[0+i*jsize4];
                lhsm[1+i*jsize4] = lhs[1+i*jsize4] + 
                                  dttx2 * speed[i-1+j*jsize2+k*ksize2];
                lhsm[2+i*jsize4] = lhs[2+i*jsize4];
                lhsm[3+i*jsize4] = lhs[3+i*jsize4] - 
                                  dttx2 * speed[i+1+j*jsize2+k*ksize2];
                lhsm[4+i*jsize4] = lhs[4+i*jsize4];
             }

//---------------------------------------------------------------------
//                          FORWARD ELIMINATION  
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//      perform the Thomas algorithm; first, FORWARD ELIMINATION     
//---------------------------------------------------------------------

             for(i=0;i<=grid_points[0]-3;i++){
                i1 = i  + 1;
                i2 = i  + 2;
                fac1      = 1./lhs[2+i*jsize4];
                lhs[3+i*jsize4]  = fac1*lhs[3+i*jsize4];
                lhs[4+i*jsize4]  = fac1*lhs[4+i*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
                lhs[2+i1*jsize4] = lhs[2+i1*jsize4] -
                               lhs[1+i1*jsize4]*lhs[3+i*jsize4];
                lhs[3+i1*jsize4] = lhs[3+i1*jsize4] -
                               lhs[1+i1*jsize4]*lhs[4+i*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i1*isize1+j*jsize1+k*ksize1] = rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                               lhs[1+i1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
                lhs[1+i2*jsize4] = lhs[1+i2*jsize4] -
                               lhs[0+i2*jsize4]*lhs[3+i*jsize4];
                lhs[2+i2*jsize4] = lhs[2+i2*jsize4] -
                               lhs[0+i2*jsize4]*lhs[4+i*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i2*isize1+j*jsize1+k*ksize1] = rhs[m+i2*isize1+j*jsize1+k*ksize1] -
                               lhs[0+i2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
             }
	     
//---------------------------------------------------------------------
//      The last two rows in this grid block are a bit different, 
//      since they do not have two more rows available for the
//      elimination of off-diagonal entries
//---------------------------------------------------------------------

             i  = grid_points[0]-2;
             i1 = grid_points[0]-1;
             fac1      = 1./lhs[2+i*jsize4];
             lhs[3+i*jsize4]  = fac1*lhs[3+i*jsize4];
             lhs[4+i*jsize4]  = fac1*lhs[4+i*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }
             lhs[2+i1*jsize4] = lhs[2+i1*jsize4] -
                            lhs[1+i1*jsize4]*lhs[3+i*jsize4];
             lhs[3+i1*jsize4] = lhs[3+i1*jsize4] -
                            lhs[1+i1*jsize4]*lhs[4+i*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i1*isize1+j*jsize1+k*ksize1] = rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                            lhs[1+i1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }
//---------------------------------------------------------------------
//            scale the last row immediately 
//---------------------------------------------------------------------
             fac2             = 1./lhs[2+i1*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i1*isize1+j*jsize1+k*ksize1] = fac2*rhs[m+i1*isize1+j*jsize1+k*ksize1];
             }

//---------------------------------------------------------------------
//      do the u+c and the u-c factors                 
//---------------------------------------------------------------------

             for(i=0;i<=grid_points[0]-3;i++){
                i1 = i  + 1;
                i2 = i  + 2;
		m = 3;
                fac1       = 1./lhsp[2+i*jsize4];
                lhsp[3+i*jsize4]  = fac1*lhsp[3+i*jsize4];
                lhsp[4+i*jsize4]  = fac1*lhsp[4+i*jsize4];
                rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
                lhsp[2+i1*jsize4] = lhsp[2+i1*jsize4] -
                              lhsp[1+i1*jsize4]*lhsp[3+i*jsize4];
                lhsp[3+i1*jsize4] = lhsp[3+i1*jsize4] -
                              lhsp[1+i1*jsize4]*lhsp[4+i*jsize4];
                rhs[m+i1*isize1+j*jsize1+k*ksize1] = rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                              lhsp[1+i1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                lhsp[1+i2*jsize4] = lhsp[1+i2*jsize4] -
                              lhsp[0+i2*jsize4]*lhsp[3+i*jsize4];
                lhsp[2+i2*jsize4] = lhsp[2+i2*jsize4] -
                              lhsp[0+i2*jsize4]*lhsp[4+i*jsize4];
                rhs[m+i2*isize1+j*jsize1+k*ksize1] = rhs[m+i2*isize1+j*jsize1+k*ksize1] -
                              lhsp[0+i2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
		m = 4;
                fac1       = 1./lhsm[2+i*jsize4];
                lhsm[3+i*jsize4]  = fac1*lhsm[3+i*jsize4];
                lhsm[4+i*jsize4]  = fac1*lhsm[4+i*jsize4];
                rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
                lhsm[2+i1*jsize4] = lhsm[2+i1*jsize4] -
                              lhsm[1+i1*jsize4]*lhsm[3+i*jsize4];
                lhsm[3+i1*jsize4] = lhsm[3+i1*jsize4] -
                              lhsm[1+i1*jsize4]*lhsm[4+i*jsize4];
                rhs[m+i1*isize1+j*jsize1+k*ksize1] = rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                              lhsm[1+i1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                lhsm[1+i2*jsize4] = lhsm[1+i2*jsize4] -
                              lhsm[0+i2*jsize4]*lhsm[3+i*jsize4];
                lhsm[2+i2*jsize4] = lhsm[2+i2*jsize4] -
                              lhsm[0+i2*jsize4]*lhsm[4+i*jsize4];
                rhs[m+i2*isize1+j*jsize1+k*ksize1] = rhs[m+i2*isize1+j*jsize1+k*ksize1] -
                              lhsm[0+i2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }

//---------------------------------------------------------------------
//         And again the last two rows separately
//---------------------------------------------------------------------
             i  = grid_points[0]-2;
             i1 = grid_points[0]-1;
	     m = 3;
             fac1       = 1./lhsp[2+i*jsize4];
             lhsp[3+i*jsize4]  = fac1*lhsp[3+i*jsize4];
             lhsp[4+i*jsize4]  = fac1*lhsp[4+i*jsize4];
             rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             lhsp[2+i1*jsize4] = lhsp[2+i1*jsize4] -
                            lhsp[1+i1*jsize4]*lhsp[3+i*jsize4];
             lhsp[3+i1*jsize4] = lhsp[3+i1*jsize4] -
                            lhsp[1+i1*jsize4]*lhsp[4+i*jsize4];
             rhs[m+i1*isize1+j*jsize1+k*ksize1] = rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                            lhsp[1+i1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
	     m = 4;
             fac1       = 1./lhsm[2+i*jsize4];
             lhsm[3+i*jsize4]  = fac1*lhsm[3+i*jsize4];
             lhsm[4+i*jsize4]  = fac1*lhsm[4+i*jsize4];
             rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             lhsm[2+i1*jsize4] = lhsm[2+i1*jsize4] -
                            lhsm[1+i1*jsize4]*lhsm[3+i*jsize4];
             lhsm[3+i1*jsize4] = lhsm[3+i1*jsize4] -
                            lhsm[1+i1*jsize4]*lhsm[4+i*jsize4];
             rhs[m+i1*isize1+j*jsize1+k*ksize1] = rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                            lhsm[1+i1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
//---------------------------------------------------------------------
//               Scale the last row immediately
//---------------------------------------------------------------------
             rhs[3+i1*isize1+j*jsize1+k*ksize1] = rhs[3+i1*isize1+j*jsize1+k*ksize1]/lhsp[2+i1*jsize4];
             rhs[4+i1*isize1+j*jsize1+k*ksize1] = rhs[4+i1*isize1+j*jsize1+k*ksize1]/lhsm[2+i1*jsize4];

//---------------------------------------------------------------------
//                         BACKSUBSTITUTION 
//---------------------------------------------------------------------

             i  = grid_points[0]-2;
             i1 = grid_points[0]-1;
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] -
                                   lhs[3+i*jsize4]*rhs[m+i1*isize1+j*jsize1+k*ksize1];
             }

             rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] -
                                lhsp[3+i*jsize4]*rhs[3+i1*isize1+j*jsize1+k*ksize1];
             rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] -
                                lhsm[3+i*jsize4]*rhs[4+i1*isize1+j*jsize1+k*ksize1];
		

//---------------------------------------------------------------------
//      The first three factors
//---------------------------------------------------------------------
             for(i=grid_points[0]-3;i>=0;i--){
                i1 = i  + 1;
                i2 = i  + 2;

                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - 
                                lhs[3+i*jsize4]*rhs[m+i1*isize1+j*jsize1+k*ksize1] -
                                lhs[4+i*jsize4]*rhs[m+i2*isize1+j*jsize1+k*ksize1];
                }
//---------------------------------------------------------------------
//      And the remaining two
//---------------------------------------------------------------------
                rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] - 
                                lhsp[3+i*jsize4]*rhs[3+i1*isize1+j*jsize1+k*ksize1] -
                                lhsp[4+i*jsize4]*rhs[3+i2*isize1+j*jsize1+k*ksize1];
                rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] - 
                                lhsm[3+i*jsize4]*rhs[4+i1*isize1+j*jsize1+k*ksize1] -
                                lhsm[4+i*jsize4]*rhs[4+i2*isize1+j*jsize1+k*ksize1];
             }
          }
       }
       if (timeron) timer.stop(t_xsolve);

//---------------------------------------------------------------------
//      Do the block-diagonal inversion          
//---------------------------------------------------------------------
       if (timeron) timer.start(t_ninvr);
       ninvr();
       if (timeron) timer.stop(t_ninvr);
  }

  public void y_solve(){
    int i, j, k, n, j1, j2, m;
      double ru1, fac1, fac2;

//---------------------------------------------------------------------
//---------------------------------------------------------------------

       if (timeron) timer.start(t_ysolve);
       for(k=1;k<=grid_points[2]-2;k++){
          for(i=1;i<=grid_points[0]-2;i++){

//---------------------------------------------------------------------
// Computes the left hand side for the three y-factors   
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//      first fill the lhs for the u-eigenvalue         
//---------------------------------------------------------------------

             for(j=0;j<=grid_points[1]-1;j++){
                ru1 = c3c4*rho_i[i+j*jsize2+k*ksize2];
                cv[j] = vs[i+j*jsize2+k*ksize2];
                rhoq[j] = dmax1( dy3 + con43 * ru1,
                                 dy5 + c1c5*ru1,
                                 dymax + ru1,
                                 dy1);
             }
            
              lhsinit(grid_points[1]-1);

             for(j=1;j<=grid_points[1]-2;j++){
                lhs[0+j*jsize4] =  0.0;
                lhs[1+j*jsize4] = -dtty2 * cv[j-1] - dtty1 * rhoq[j-1];
                lhs[2+j*jsize4] =  1.0 + c2dtty1 * rhoq[j];
                lhs[3+j*jsize4] =  dtty2 * cv[j+1] - dtty1 * rhoq[j+1];
                lhs[4+j*jsize4] =  0.0;
             }

//---------------------------------------------------------------------
//      add fourth order dissipation                             
//---------------------------------------------------------------------

             j = 1;

             lhs[2+j*jsize4] = lhs[2+j*jsize4] + comz5;
             lhs[3+j*jsize4] = lhs[3+j*jsize4] - comz4;
             lhs[4+j*jsize4] = lhs[4+j*jsize4] + comz1;
       
             lhs[1+(j+1)*jsize4] = lhs[1+(j+1)*jsize4] - comz4;
             lhs[2+(j+1)*jsize4] = lhs[2+(j+1)*jsize4] + comz6;
             lhs[3+(j+1)*jsize4] = lhs[3+(j+1)*jsize4] - comz4;
             lhs[4+(j+1)*jsize4] = lhs[4+(j+1)*jsize4] + comz1;

             for(j=3;j<=grid_points[1]-4;j++){

                lhs[0+j*jsize4] = lhs[0+j*jsize4] + comz1;
                lhs[1+j*jsize4] = lhs[1+j*jsize4] - comz4;
                lhs[2+j*jsize4] = lhs[2+j*jsize4] + comz6;
                lhs[3+j*jsize4] = lhs[3+j*jsize4] - comz4;
                lhs[4+j*jsize4] = lhs[4+j*jsize4] + comz1;
             }

             j = grid_points[1]-3;
             lhs[0+j*jsize4] = lhs[0+j*jsize4] + comz1;
             lhs[1+j*jsize4] = lhs[1+j*jsize4] - comz4;
             lhs[2+j*jsize4] = lhs[2+j*jsize4] + comz6;
             lhs[3+j*jsize4] = lhs[3+j*jsize4] - comz4;

             lhs[0+(j+1)*jsize4] = lhs[0+(j+1)*jsize4] + comz1;
             lhs[1+(j+1)*jsize4] = lhs[1+(j+1)*jsize4] - comz4;
             lhs[2+(j+1)*jsize4] = lhs[2+(j+1)*jsize4] + comz5;

//---------------------------------------------------------------------
//      subsequently, do the other two factors                    
//---------------------------------------------------------------------
             for(j=1;j<=grid_points[1]-2;j++){

	       lhsp[0+j*jsize4] = lhs[0+j*jsize4];
	       lhsp[1+j*jsize4] = lhs[1+j*jsize4] - 
                                  dtty2 * speed[i+(j-1)*jsize2+k*ksize2];
	       lhsp[2+j*jsize4] = lhs[2+j*jsize4];
	       lhsp[3+j*jsize4] = lhs[3+j*jsize4] + 
		                  dtty2 * speed[i+(j+1)*jsize2+k*ksize2];
	       lhsp[4+j*jsize4] = lhs[4+j*jsize4];

	       lhsm[0+j*jsize4] = lhs[0+j*jsize4];
	       lhsm[1+j*jsize4] = lhs[1+j*jsize4] + 
                                  dtty2 * speed[i+(j-1)*jsize2+k*ksize2];
	       lhsm[2+j*jsize4] = lhs[2+j*jsize4];
	       lhsm[3+j*jsize4] = lhs[3+j*jsize4] - 
                                  dtty2 * speed[i+(j+1)*jsize2+k*ksize2];
	       lhsm[4+j*jsize4] = lhs[4+j*jsize4];

             }

//---------------------------------------------------------------------
//                          FORWARD ELIMINATION  
//---------------------------------------------------------------------

             for(j=0;j<=grid_points[1]-3;j++){
                j1 = j  + 1;
                j2 = j  + 2;
                fac1      = 1./lhs[2+j*jsize4];
                lhs[3+j*jsize4]  = fac1*lhs[3+j*jsize4];
                lhs[4+j*jsize4]  = fac1*lhs[4+j*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
                lhs[2+j1*jsize4] = lhs[2+j1*jsize4] -
                               lhs[1+j1*jsize4]*lhs[3+j*jsize4];
                lhs[3+j1*jsize4] = lhs[3+j1*jsize4] -
                               lhs[1+j1*jsize4]*lhs[4+j*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
                               lhs[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
                lhs[1+j2*jsize4] = lhs[1+j2*jsize4] -
                               lhs[0+j2*jsize4]*lhs[3+j*jsize4];
                lhs[2+j2*jsize4] = lhs[2+j2*jsize4] -
                               lhs[0+j2*jsize4]*lhs[4+j*jsize4];
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j2*jsize1+k*ksize1] = rhs[m+i*isize1+j2*jsize1+k*ksize1] -
                               lhs[0+j2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
                }
             }

//---------------------------------------------------------------------
//      The last two rows in this grid block are a bit different, 
//      since they do not have two more rows available for the
//      elimination of off-diagonal entries
//---------------------------------------------------------------------

             j  = grid_points[1]-2;
             j1 = grid_points[1]-1;
             fac1      = 1./lhs[2+j*jsize4];
             lhs[3+j*jsize4]  = fac1*lhs[3+j*jsize4];
             lhs[4+j*jsize4]  = fac1*lhs[4+j*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }
             lhs[2+j1*jsize4] = lhs[2+j1*jsize4] -
                            lhs[1+j1*jsize4]*lhs[3+j*jsize4];
             lhs[3+j1*jsize4] = lhs[3+j1*jsize4] -
                            lhs[1+j1*jsize4]*lhs[4+j*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
                            lhs[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }
//---------------------------------------------------------------------
//            scale the last row immediately 
//---------------------------------------------------------------------
             fac2      = 1./lhs[2+j1*jsize4];
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j1*jsize1+k*ksize1] = fac2*rhs[m+i*isize1+j1*jsize1+k*ksize1];
             }

//---------------------------------------------------------------------
//      do the u+c and the u-c factors                 
//---------------------------------------------------------------------
             for(j=0;j<=grid_points[1]-3;j++){
             	j1 = j  + 1;
             	j2 = j  + 2;
	     	m = 3;
             	fac1	   = 1./lhsp[2+j*jsize4];
             	lhsp[3+j*jsize4]  = fac1*lhsp[3+j*jsize4];
             	lhsp[4+j*jsize4]  = fac1*lhsp[4+j*jsize4];
             	rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsp[2+j1*jsize4] = lhsp[2+j1*jsize4] -
             		    lhsp[1+j1*jsize4]*lhsp[3+j*jsize4];
             	lhsp[3+j1*jsize4] = lhsp[3+j1*jsize4] -
             		    lhsp[1+j1*jsize4]*lhsp[4+j*jsize4];
             	rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		    lhsp[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsp[1+j2*jsize4] = lhsp[1+j2*jsize4] -
             		    lhsp[0+j2*jsize4]*lhsp[3+j*jsize4];
             	lhsp[2+j2*jsize4] = lhsp[2+j2*jsize4] -
             		    lhsp[0+j2*jsize4]*lhsp[4+j*jsize4];
             	rhs[m+i*isize1+j2*jsize1+k*ksize1] = rhs[m+i*isize1+j2*jsize1+k*ksize1] -
             		    lhsp[0+j2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
	     	m = 4;
             	fac1	   = 1./lhsm[2+j*jsize4];
             	lhsm[3+j*jsize4]  = fac1*lhsm[3+j*jsize4];
             	lhsm[4+j*jsize4]  = fac1*lhsm[4+j*jsize4];
             	rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsm[2+j1*jsize4] = lhsm[2+j1*jsize4] -
             		    lhsm[1+j1*jsize4]*lhsm[3+j*jsize4];
             	lhsm[3+j1*jsize4] = lhsm[3+j1*jsize4] -
             		    lhsm[1+j1*jsize4]*lhsm[4+j*jsize4];
             	rhs[m+i*isize1+j1*jsize1+k*ksize1] = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		    lhsm[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             	lhsm[1+j2*jsize4] = lhsm[1+j2*jsize4] -
             		    lhsm[0+j2*jsize4]*lhsm[3+j*jsize4];
             	lhsm[2+j2*jsize4] = lhsm[2+j2*jsize4] -
             		    lhsm[0+j2*jsize4]*lhsm[4+j*jsize4];
             	rhs[m+i*isize1+j2*jsize1+k*ksize1] = rhs[m+i*isize1+j2*jsize1+k*ksize1] -
             		    lhsm[0+j2*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
             }

//---------------------------------------------------------------------
//         And again the last two rows separately
//---------------------------------------------------------------------
             j  = grid_points[1]-2;
             j1 = grid_points[1]-1;
	     m = 3;
             fac1	= 1./lhsp[2+j*jsize4];
             lhsp[3+j*jsize4]  = fac1*lhsp[3+j*jsize4];
             lhsp[4+j*jsize4]  = fac1*lhsp[4+j*jsize4];
             rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             lhsp[2+j1*jsize4] = lhsp[2+j1*jsize4] -
             		 lhsp[1+j1*jsize4]*lhsp[3+j*jsize4];
             lhsp[3+j1*jsize4] = lhsp[3+j1*jsize4] -
             		 lhsp[1+j1*jsize4]*lhsp[4+j*jsize4];
             rhs[m+i*isize1+j1*jsize1+k*ksize1]   = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		 lhsp[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
	     m = 4;
             fac1	= 1./lhsm[2+j*jsize4];
             lhsm[3+j*jsize4]  = fac1*lhsm[3+j*jsize4];
             lhsm[4+j*jsize4]  = fac1*lhsm[4+j*jsize4];
             rhs[m+i*isize1+j*jsize1+k*ksize1] = fac1*rhs[m+i*isize1+j*jsize1+k*ksize1];
             lhsm[2+j1*jsize4] = lhsm[2+j1*jsize4] -
             		 lhsm[1+j1*jsize4]*lhsm[3+j*jsize4];
             lhsm[3+j1*jsize4] = lhsm[3+j1*jsize4] -
             		 lhsm[1+j1*jsize4]*lhsm[4+j*jsize4];
             rhs[m+i*isize1+j1*jsize1+k*ksize1]   = rhs[m+i*isize1+j1*jsize1+k*ksize1] -
             		 lhsm[1+j1*jsize4]*rhs[m+i*isize1+j*jsize1+k*ksize1];
//---------------------------------------------------------------------
//               Scale the last row immediately 
//---------------------------------------------------------------------
             rhs[3+i*isize1+j1*jsize1+k*ksize1]   = rhs[3+i*isize1+j1*jsize1+k*ksize1]/lhsp[2+j1*jsize4];
             rhs[4+i*isize1+j1*jsize1+k*ksize1]   = rhs[4+i*isize1+j1*jsize1+k*ksize1]/lhsm[2+j1*jsize4];

//---------------------------------------------------------------------
//                         BACKSUBSTITUTION 
//---------------------------------------------------------------------

             j  = grid_points[1]-2;
             j1 = grid_points[1]-1;
             for(m=0;m<=2;m++){
                rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] -
                                 lhs[3+j*jsize4]*rhs[m+i*isize1+j1*jsize1+k*ksize1];
             }

             rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] -
                                 lhsp[3+j*jsize4]*rhs[3+i*isize1+j1*jsize1+k*ksize1];
             rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] -
                                 lhsm[3+j*jsize4]*rhs[4+i*isize1+j1*jsize1+k*ksize1];

//---------------------------------------------------------------------
//      The first three factors
//---------------------------------------------------------------------
             for(j=grid_points[1]-3;j>=0;j--){
                j1 = j  + 1;
                j2 = j  + 2;
                for(m=0;m<=2;m++){
                   rhs[m+i*isize1+j*jsize1+k*ksize1] = rhs[m+i*isize1+j*jsize1+k*ksize1] - 
                                lhs[3+j*jsize4]*rhs[m+i*isize1+j1*jsize1+k*ksize1] -
                                lhs[4+j*jsize4]*rhs[m+i*isize1+j2*jsize1+k*ksize1];
                }

//---------------------------------------------------------------------
//      And the remaining two
//---------------------------------------------------------------------
                rhs[3+i*isize1+j*jsize1+k*ksize1] = rhs[3+i*isize1+j*jsize1+k*ksize1] - 
                                lhsp[3+j*jsize4]*rhs[3+i*isize1+j1*jsize1+k*ksize1] -
                                lhsp[4+j*jsize4]*rhs[3+i*isize1+j2*jsize1+k*ksize1];
                rhs[4+i*isize1+j*jsize1+k*ksize1] = rhs[4+i*isize1+j*jsize1+k*ksize1] - 
                                lhsm[3+j*jsize4]*rhs[4+i*isize1+j1*jsize1+k*ksize1] -
                                lhsm[4+j*jsize4]*rhs[4+i*isize1+j2*jsize1+k*ksize1];
             }
          }
       }
       if (timeron) timer.stop(t_ysolve);

       if (timeron) timer.start(t_pinvr);
        pinvr();
       if (timeron) timer.stop(t_pinvr);
  }
  
  public void z_solve(){
           int i, j, k, n, k1, k2, m;
       double ru1, fac1, fac2, rtmp[] = new double[5*(KMAX+1)];


//---------------------------------------------------------------------
//---------------------------------------------------------------------

       if (timeron) timer.start(t_zsolve);
       for(j=1;j<=ny2;j++){
          for(i=1;i<=nx2;i++){

//---------------------------------------------------------------------
// Computes the left hand side for the three z-factors   
//---------------------------------------------------------------------

//---------------------------------------------------------------------
// first fill the lhs for the u-eigenvalue                          
//---------------------------------------------------------------------

             for(k=0;k<=nz2+1;k++){
                ru1 = c3c4*rho_i[i+j*jsize2+k*ksize2];
                cv[k] = ws[i+j*jsize2+k*ksize2];
                rhos[k] = dmax1(dz4 + con43 * ru1,
                                dz5 + c1c5 * ru1,
                                dzmax + ru1,
                                dz1);
             }

             lhsinit(grid_points[2]-1);
             for(k=1;k<=nz2;k++){
                lhs[0+k*jsize4] =  0.0;
                lhs[1+k*jsize4] = -dttz2 * cv[k-1] - dttz1 * rhos[k-1];
                lhs[2+k*jsize4] =  1.0 + c2dttz1 * rhos[k];
                lhs[3+k*jsize4] =  dttz2 * cv[k+1] - dttz1 * rhos[k+1];
                lhs[4+k*jsize4] =  0.0;
             }

//---------------------------------------------------------------------
//      add fourth order dissipation                                  
//---------------------------------------------------------------------

             k = 1;
             lhs[2+k*jsize4] = lhs[2+k*jsize4] + comz5;
             lhs[3+k*jsize4] = lhs[3+k*jsize4] - comz4;
             lhs[4+k*jsize4] = lhs[4+k*jsize4] + comz1;
	       
             k = 2;
             lhs[1+k*jsize4] = lhs[1+k*jsize4] - comz4;
             lhs[2+k*jsize4] = lhs[2+k*jsize4] + comz6;
             lhs[3+k*jsize4] = lhs[3+k*jsize4] - comz4;
             lhs[4+k*jsize4] = lhs[4+k*jsize4] + comz1;

             for(k=3;k<=nz2-2;k++){
                lhs[0+k*jsize4] = lhs[0+k*jsize4] + comz1;
                lhs[1+k*jsize4] = lhs[1+k*jsize4] - comz4;
                lhs[2+k*jsize4] = lhs[2+k*jsize4] + comz6;
                lhs[3+k*jsize4] = lhs[3+k*jsize4] - comz4;
                lhs[4+k*jsize4] = lhs[4+k*jsize4] + comz1;
             }

             k = nz2-1;
             lhs[0+k*jsize4] = lhs[0+k*jsize4] + comz1;
             lhs[1+k*jsize4] = lhs[1+k*jsize4] - comz4;
             lhs[2+k*jsize4] = lhs[2+k*jsize4] + comz6;
             lhs[3+k*jsize4] = lhs[3+k*jsize4] - comz4;

             k = nz2;
             lhs[0+k*jsize4] = lhs[0+k*jsize4] + comz1;
             lhs[1+k*jsize4] = lhs[1+k*jsize4] - comz4;
             lhs[2+k*jsize4] = lhs[2+k*jsize4] + comz5;

//---------------------------------------------------------------------
//      subsequently, fill the other factors (u+c), (u-c) 
//---------------------------------------------------------------------
             for(k=1;k<=nz2;k++){
                lhsp[0+k*jsize4] = lhs[0+k*jsize4];
                lhsp[1+k*jsize4] = lhs[1+k*jsize4] - 
                                  dttz2 * speed[i+j*jsize2+(k-1)*ksize2];
                lhsp[2+k*jsize4] = lhs[2+k*jsize4];
                lhsp[3+k*jsize4] = lhs[3+k*jsize4] + 
                                  dttz2 * speed[i+j*jsize2+(k+1)*ksize2];
                lhsp[4+k*jsize4] = lhs[4+k*jsize4];
                lhsm[0+k*jsize4] = lhs[0+k*jsize4];
                lhsm[1+k*jsize4] = lhs[1+k*jsize4] + 
                                  dttz2 * speed[i+j*jsize2+(k-1)*ksize2];
                lhsm[2+k*jsize4] = lhs[2+k*jsize4];
                lhsm[3+k*jsize4] = lhs[3+k*jsize4] - 
                                  dttz2 * speed[i+j*jsize2+(k+1)*ksize2];
                lhsm[4+k*jsize4] = lhs[4+k*jsize4];
             }

//---------------------------------------------------------------------
// Load a row of K data
//---------------------------------------------------------------------

             for(k=0;k<=nz2+1;k++){
	        rtmp[0+k*5] = rhs[0+i*isize1+j*jsize1+k*ksize1];
	        rtmp[1+k*5] = rhs[1+i*isize1+j*jsize1+k*ksize1];
	        rtmp[2+k*5] = rhs[2+i*isize1+j*jsize1+k*ksize1];
	        rtmp[3+k*5] = rhs[3+i*isize1+j*jsize1+k*ksize1];
	        rtmp[4+k*5] = rhs[4+i*isize1+j*jsize1+k*ksize1];
	     }

//---------------------------------------------------------------------
//                          FORWARD ELIMINATION  
//---------------------------------------------------------------------

             for(k=0;k<=grid_points[2]-3;k++){
                k1 = k  + 1;
                k2 = k  + 2;
                fac1      = 1./lhs[2+k*jsize4];
                lhs[3+k*jsize4]  = fac1*lhs[3+k*jsize4];
                lhs[4+k*jsize4]  = fac1*lhs[4+k*jsize4];
                for(m=0;m<=2;m++){
                   rtmp[m+k*5] = fac1*rtmp[m+k*5];
                }
                lhs[2+k1*jsize4] = lhs[2+k1*jsize4] -
                               lhs[1+k1*jsize4]*lhs[3+k*jsize4];
                lhs[3+k1*jsize4] = lhs[3+k1*jsize4] -
                               lhs[1+k1*jsize4]*lhs[4+k*jsize4];
                for(m=0;m<=2;m++){
                   rtmp[m+k1*5] = rtmp[m+k1*5] -
                               lhs[1+k1*jsize4]*rtmp[m+k*5];
                }
                lhs[1+k2*jsize4] = lhs[1+k2*jsize4] -
                               lhs[0+k2*jsize4]*lhs[3+k*jsize4];
                lhs[2+k2*jsize4] = lhs[2+k2*jsize4] -
                               lhs[0+k2*jsize4]*lhs[4+k*jsize4];
                for(m=0;m<=2;m++){
                   rtmp[m+k2*5] = rtmp[m+k2*5] -
                               lhs[0+k2*jsize4]*rtmp[m+k*5];
                }
             }

//---------------------------------------------------------------------
//      The last two rows in this grid block are a bit different, 
//      since they do not have two more rows available for the
//      elimination of off-diagonal entries
//---------------------------------------------------------------------
             k  = grid_points[2]-2;
             k1 = grid_points[2]-1;
             fac1      = 1./lhs[2+k*jsize4];
             lhs[3+k*jsize4]  = fac1*lhs[3+k*jsize4];
             lhs[4+k*jsize4]  = fac1*lhs[4+k*jsize4];
             for(m=0;m<=2;m++){
                rtmp[m+k*5] = fac1*rtmp[m+k*5];
             }
             lhs[2+k1*jsize4] = lhs[2+k1*jsize4] -
                            lhs[1+k1*jsize4]*lhs[3+k*jsize4];
             lhs[3+k1*jsize4] = lhs[3+k1*jsize4] -
                            lhs[1+k1*jsize4]*lhs[4+k*jsize4];
             for(m=0;m<=2;m++){
                rtmp[m+k1*5] = rtmp[m+k1*5] -
                            lhs[1+k1*jsize4]*rtmp[m+k*5];
             }
//---------------------------------------------------------------------
//               scale the last row immediately
//---------------------------------------------------------------------
             fac2      = 1./lhs[2+k1*jsize4];
             for(m=0;m<=2;m++){
                rtmp[m+k1*5] = fac2*rtmp[m+k1*5];
             }

//---------------------------------------------------------------------
//      do the u+c and the u-c factors               
//---------------------------------------------------------------------
             for(k=0;k<=grid_points[2]-3;k++){
             	k1 = k  + 1;
             	k2 = k  + 2;
	     	m = 3;
             	fac1	   = 1./lhsp[2+k*jsize4];
             	lhsp[3+k*jsize4]  = fac1*lhsp[3+k*jsize4];
             	lhsp[4+k*jsize4]  = fac1*lhsp[4+k*jsize4];
             	rtmp[m+k*5]  = fac1*rtmp[m+k*5];
             	lhsp[2+k1*jsize4] = lhsp[2+k1*jsize4] -
             		    lhsp[1+k1*jsize4]*lhsp[3+k*jsize4];
             	lhsp[3+k1*jsize4] = lhsp[3+k1*jsize4] -
             		    lhsp[1+k1*jsize4]*lhsp[4+k*jsize4];
             	rtmp[m+k1*5] = rtmp[m+k1*5] -
             		    lhsp[1+k1*jsize4]*rtmp[m+k*5];
             	lhsp[1+k2*jsize4] = lhsp[1+k2*jsize4] -
             		    lhsp[0+k2*jsize4]*lhsp[3+k*jsize4];
             	lhsp[2+k2*jsize4] = lhsp[2+k2*jsize4] -
             		    lhsp[0+k2*jsize4]*lhsp[4+k*jsize4];
             	rtmp[m+k2*5] = rtmp[m+k2*5] -
             		    lhsp[0+k2*jsize4]*rtmp[m+k*5];
	     	m = 4;
             	fac1	   = 1./lhsm[2+k*jsize4];
             	lhsm[3+k*jsize4]  = fac1*lhsm[3+k*jsize4];
             	lhsm[4+k*jsize4]  = fac1*lhsm[4+k*jsize4];
             	rtmp[m+k*5]  = fac1*rtmp[m+k*5];
             	lhsm[2+k1*jsize4] = lhsm[2+k1*jsize4] -
             		    lhsm[1+k1*jsize4]*lhsm[3+k*jsize4];
             	lhsm[3+k1*jsize4] = lhsm[3+k1*jsize4] -
             		    lhsm[1+k1*jsize4]*lhsm[4+k*jsize4];
             	rtmp[m+k1*5] = rtmp[m+k1*5] -
             		    lhsm[1+k1*jsize4]*rtmp[m+k*5];
             	lhsm[1+k2*jsize4] = lhsm[1+k2*jsize4] -
             		    lhsm[0+k2*jsize4]*lhsm[3+k*jsize4];
             	lhsm[2+k2*jsize4] = lhsm[2+k2*jsize4] -
             		    lhsm[0+k2*jsize4]*lhsm[4+k*jsize4];
             	rtmp[m+k2*5] = rtmp[m+k2*5] -
             		    lhsm[0+k2*jsize4]*rtmp[m+k*5];
             }

//---------------------------------------------------------------------
//         And again the last two rows separately
//---------------------------------------------------------------------
             k  = grid_points[2]-2;
             k1 = grid_points[2]-1;
	     m = 3;
             fac1	= 1./lhsp[2+k*jsize4];
             lhsp[3+k*jsize4]  = fac1*lhsp[3+k*jsize4];
             lhsp[4+k*jsize4]  = fac1*lhsp[4+k*jsize4];
             rtmp[m+k*5]  = fac1*rtmp[m+k*5];
             lhsp[2+k1*jsize4] = lhsp[2+k1*jsize4] -
             		 lhsp[1+k1*jsize4]*lhsp[3+k*jsize4];
             lhsp[3+k1*jsize4] = lhsp[3+k1*jsize4] -
             		 lhsp[1+k1*jsize4]*lhsp[4+k*jsize4];
             rtmp[m+k1*5] = rtmp[m+k1*5] -
             		 lhsp[1+k1*jsize4]*rtmp[m+k*5];
	     m = 4;
             fac1	= 1.0/lhsm[2+k*jsize4];
             lhsm[3+k*jsize4]  = fac1*lhsm[3+k*jsize4];
             lhsm[4+k*jsize4]  = fac1*lhsm[4+k*jsize4];
             rtmp[m+k*5]  = fac1*rtmp[m+k*5];
             lhsm[2+k1*jsize4] = lhsm[2+k1*jsize4] -
             		 lhsm[1+k1*jsize4]*lhsm[3+k*jsize4];
             lhsm[3+k1*jsize4] = lhsm[3+k1*jsize4] -
             		 lhsm[1+k1*jsize4]*lhsm[4+k*jsize4];
             rtmp[m+k1*5] = rtmp[m+k1*5] -
             		 lhsm[1+k1*jsize4]*rtmp[m+k*5];
//---------------------------------------------------------------------
//               Scale the last row immediately (some of this is overkill
//               if this is the last cell)
//---------------------------------------------------------------------
             rtmp[3+k1*5] = rtmp[3+k1*5]/lhsp[2+k1*jsize4];
             rtmp[4+k1*5] = rtmp[4+k1*5]/lhsm[2+k1*jsize4];

//---------------------------------------------------------------------
//                         BACKSUBSTITUTION 
//---------------------------------------------------------------------

             k  = grid_points[2]-2;
             k1 = grid_points[2]-1;
             for(m=0;m<=2;m++){
                rtmp[m+k*5] = rtmp[m+k*5] -
                                   lhs[3+k*jsize4]*rtmp[m+k1*5];
             }

             rtmp[3+k*5] = rtmp[3+k*5] -
                                   lhsp[3+k*jsize4]*rtmp[3+k1*5];
             rtmp[4+k*5] = rtmp[4+k*5] -
                                   lhsm[3+k*jsize4]*rtmp[4+k1*5];

//---------------------------------------------------------------------
//      Whether or not this is the last processor, we always have
//      to complete the back-substitution 
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//      The first three factors
//---------------------------------------------------------------------
             for(k=grid_points[2]-3;k>=0;k--){
                k1 = k  + 1;
                k2 = k  + 2;
                for(m=0;m<=2;m++){
                   rtmp[m+k*5] = rtmp[m+k*5] - 
                                lhs[3+k*jsize4]*rtmp[m+k1*5] -
                                lhs[4+k*jsize4]*rtmp[m+k2*5];
                }

//---------------------------------------------------------------------
//      And the remaining two
//---------------------------------------------------------------------
                rtmp[3+k*5] = rtmp[3+k*5] - 
                                lhsp[3+k*jsize4]*rtmp[3+k1*5] -
                                lhsp[4+k*jsize4]*rtmp[3+k2*5];
                rtmp[4+k*5] = rtmp[4+k*5] - 
                                lhsm[3+k*jsize4]*rtmp[4+k1*5] -
                                lhsm[4+k*jsize4]*rtmp[4+k2*5];
             }

//---------------------------------------------------------------------
//      Store result
//---------------------------------------------------------------------
             for(k=0;k<=nz2+1;k++){
	        rhs[0+i*isize1+j*jsize1+k*ksize1] = rtmp[0+k*5];
	        rhs[1+i*isize1+j*jsize1+k*ksize1] = rtmp[1+k*5];
	        rhs[2+i*isize1+j*jsize1+k*ksize1] = rtmp[2+k*5];
	        rhs[3+i*isize1+j*jsize1+k*ksize1] = rtmp[3+k*5];
	        rhs[4+i*isize1+j*jsize1+k*ksize1] = rtmp[4+k*5];
	     }

          }
       }
       if (timeron) timer.stop(t_zsolve);
       if (timeron) timer.start(t_tzetar);
       tzetar();
       if (timeron) timer.stop(t_tzetar);
  }
  public double checkSum(double arr[]){
    double csum=0.0;
    for(int k=0;k<=grid_points[2]-1;k++){
      for(int j=0;j<=grid_points[1]-1;j++){
	for(int i=0;i<=grid_points[0]-1;i++){
	  for(int m=0;m<=4;m++){
	    int offset=m+i*isize1+j*jsize1+k*ksize1;
	    csum+=(arr[offset]*arr[offset])/
	         (double)(grid_points[2]*grid_points[1]*grid_points[0]*5);
	  }
	}
      }
    }
    return csum;
  }
  
  public double getTime(){return timer.readTimer(1);}
  public void finalize() throws Throwable{
    System.out.println("LU: is about to be garbage collected"); 
    super.finalize();
  }
}






