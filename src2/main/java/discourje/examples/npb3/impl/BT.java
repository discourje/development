/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                                  B T                                    !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    This benchmark is a serial/multithreaded version of the BT code.     !
!								          !
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
! Translation to Java and to MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl;

import discourje.examples.npb3.impl.BTThreads.*;
import discourje.examples.npb3.impl.BMInOut.*;

import java.io.*;
import java.text.*;

public class BT extends BTBase {
  public int bid=-1;
  public BMResults results;
  public boolean serial=true;
  double fjac[];  
  double njac[];  
  double lhs[];  
  
  double tmp1;
  double tmp2;
  double tmp3;
   
  public BT(char clss, int threads,boolean ser){            
    super(clss, threads);
    serial=ser;
    fjac =  new double[5*5*(problem_size+1)];
    njac =  new double[5*5*(problem_size+1)];
    lhs =  new double[5*5*3*(problem_size+1)];
  }
  public static void main(String argv[]){
    BT bt = null;

    BMArgs.ParseCmdLineArgs(argv,BMName);
    char CLSS=BMArgs.CLASS;
    int np=BMArgs.num_threads;
    boolean serial=BMArgs.serial;
    try{ 
      bt = new BT(CLSS,np,serial);
    }catch(OutOfMemoryError e){
      BMArgs.outOfMemoryMessage();
      System.exit(0);
    }         
     bt.runBenchMark();
  }
  
  public void run(){runBenchMark();}

  public void runBenchMark(){
    BMArgs.Banner(BMName,CLASS,serial,num_threads);

    int numTimers=t_last+1;
    String t_names[] = new String[numTimers];
    double trecs[] = new double[numTimers];
    setTimers(t_names);
    int niter=getInputPars();

    set_constants();
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

    for(int step=1;step<=niter;step++){   //niter
      if ( step % 20 == 0 || step == 1||step==niter) {   
        System.out.println("Time step "+step);
      }
      if(serial) adi_serial();
      else adi(); 
    }

    timer.stop(t_total);
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
      double n3 = grid_points[0]*grid_points[1]*grid_points[2];
      double navg = (grid_points[0]+grid_points[1]+grid_points[2])/3.0;
      mflops = 3478.8*n3-17655.7*Math.pow(navg,2)+28023.7*navg;
      mflops *= niter / (total_time*1000000.0);
    }
    return mflops;
  }

  public void adi_serial(){
    compute_rhs();
    x_solve();
    y_solve();
    z_solve();
    add();
  }

  public void adi(){
    if(timeron)timer.start(t_rhs);
    doRHS();

    if(timeron)timer.start(t_rhsx);
    doRHS();
    if(timeron)timer.stop(t_rhsx);   

    if(timeron)timer.start(t_rhsy);
    doRHS(); 
    if(timeron)timer.stop(t_rhsy);

    if(timeron)timer.start(t_rhsz);
    doRHS();
    if(timeron)timer.stop(t_rhsz);

    doRHS();
    if(timeron)timer.stop(t_rhs);

    if(timeron)timer.start(t_xsolve);
    synchronized(this){
      for(int m=0;m<num_threads;m++)
	synchronized(xsolver[m]){
          xsolver[m].done=false;
          xsolver[m].notify();
        }
      for(int m=0;m<num_threads;m++)
          while(!xsolver[m].done){
	    try{wait();}catch(InterruptedException e){} 
            notifyAll();
	  }
    }
    if(timeron)timer.stop(t_xsolve);

    if(timeron)timer.start(t_ysolve);
    synchronized(this){
      for(int m=0;m<num_threads;m++)
	synchronized(ysolver[m]){
          ysolver[m].done=false;
          ysolver[m].notify();
        }
      for(int m=0;m<num_threads;m++)
          while(!ysolver[m].done){
	    try{wait();}catch(InterruptedException e){}
            notifyAll();
	  }
     }
     if(timeron)timer.stop(t_ysolve);

    if(timeron)timer.start(t_zsolve);
    synchronized(this){
      for(int m=0;m<num_threads;m++)
	synchronized(zsolver[m]){
          zsolver[m].done=false;
          zsolver[m].notify();
        }
      for(int m=0;m<num_threads;m++)
          while(!zsolver[m].done){
	    try{wait();}catch(InterruptedException e){} 
            notifyAll();
	  }
    }
    if(timeron)timer.stop(t_zsolve);

    if(timeron)timer.start(t_add);
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
    if(timeron)timer.stop(t_add);    
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

  public void printTimers(String t_names[], double trecs[],double tmax){
    DecimalFormat fmt = new DecimalFormat("0.000");
    double t;      
    System.out.println("SECTION  Time           (secs)");
    for(int i=1;i<=t_last;i++) trecs[i] = timer.readTimer(i);
    if (tmax == 0.0) tmax = 1.0;
    for(int i=1;i<=t_last;i++){
      System.out.println(t_names[i]+":"+fmt.format(trecs[i])+":"+
                         "  ("+fmt.format(trecs[i]*100/tmax)+"%)");
      
      if (i==t_rhs) {
	t = trecs[t_rhsx] + trecs[t_rhsy] + trecs[t_rhsz];
	System.out.print("    --> total ");
	System.out.print("sub-rhs ");
	System.out.print(fmt.format(t));
	System.out.print( "  (" );
	System.out.print(fmt.format(t*100/tmax));
	System.out.println("%)");
	t = trecs[t_rhs] - trecs[t_rhsx] + trecs[t_rhsy] + trecs[t_rhsz];
	System.out.print("    --> total ");
	System.out.print("rest-rhs ");
	System.out.print(fmt.format(t));
	System.out.print( "  (" );
	System.out.print(fmt.format(t*100/tmax));
	System.out.println("%)");
      }else if(i==t_zsolve) {
	t = trecs[t_zsolve] - trecs[t_rdis1] - trecs[t_rdis2];
	System.out.print("    --> total ");
	System.out.print("sub-zsol ");
	System.out.print(fmt.format(t));
	System.out.print( "  " );
	System.out.println(fmt.format(t*100/tmax));
	System.out.println();
      }else if(i==t_rdis2) {
	t = trecs[t_rdis1] + trecs[t_rdis2];
	System.out.print("    --> total ");
	System.out.print("redist ");
	System.out.print(fmt.format(t));
	System.out.print( "  " );
	System.out.println(fmt.format(t*100/tmax));  	
      }
    } 
  }
  
  public int getInputPars(){
    int niter=0;
    File f2 = new File("inputbt.data");
    if ( f2.exists() ){
      try{
      FileInputStream fis = new FileInputStream(f2);
      DataInputStream datafile = new DataInputStream(fis);
      System.out.println("Reading from input file inputbt.data");
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
      System.out.println("No input file inputbt.data, Using compiled defaults");
      niter = niter_default;
      dt    = dt_default;
      grid_points[0] = problem_size;
      grid_points[1] = problem_size;
      grid_points[2] = problem_size;
    }
    System.out.println( "Size: "+grid_points[0]
  		       +" X "+grid_points[1]
  		       +" X "+grid_points[2]);
    if ( (grid_points[0] > IMAX) ||
  	 (grid_points[1] > JMAX) ||
  	 (grid_points[2] > KMAX) ) {
      System.out.println("Problem size too big for array");
      System.exit(0);
    }
    System.out.println("Iterations: "+niter+" dt: "+dt); 
    return niter;
  }
  
  public void setTimers(String t_names[]){
    File f1 = new File("timer.flag");
    timeron = false;
    if( f1.exists() ){
      timeron = true;
      t_names[t_total] = new  String("total    ");
      t_names[t_rhsx] = new   String("rhsx     ");
      t_names[t_rhsy] = new   String("rhsy     ");
      t_names[t_rhsz] = new   String("rhsz     ");
      t_names[t_rhs] = new    String("rhs      ");
      t_names[t_xsolve] = new String("xsolve   ");
      t_names[t_ysolve] = new String("ysolve   ");
      t_names[t_zsolve] = new String("zsolve   ");
      t_names[t_rdis1] = new  String("redist1  ");
      t_names[t_rdis2] = new  String("redist2  ");
      t_names[t_add] = new    String("add      ");
    }
  }

  public void rhs_norm( double rms[], int rmsoffst ){
   int i, j, k, d, m;
   double add;

      for(m=0;m<rms.length;m++) rms[m+rmsoffst] = 0.0;
      
      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
               for(m=0;m<rms.length;m++){
                  add = rhs[m+i*isize2+j*jsize2+k*ksize2];
                  rms[m] += add*add;
               } 
            } 
         } 
      } 

      for(m=0;m<rms.length;m++){
         for(d=0;d<=2;d++){
            rms[m] /= grid_points[d]-2;
         } 
         rms[m] = Math.sqrt(rms[m+rmsoffst]);
      } 
  }
  
  public void error_norm( double rms[], int rmsoffst ){
  int i, j, k, m, d;
  double xi, eta, zeta, u_exact[]=new double[5], add;

      for(m=0;m<rms.length;m++) rms[m+rmsoffst] = 0.0;

      for(k=0;k<=grid_points[2]-1;k++){
         zeta = k * dnzm1;
         for(j=0;j<=grid_points[1]-1;j++){
            eta = j * dnym1;
            for(i=0;i<=grid_points[0]-1;i++){
               xi = i * dnxm1;
	       exact_solution(xi, eta, zeta, u_exact, 0);
	       for(m=0;m<rms.length;m++){
		 add = u[m+i*isize2+j*jsize2+k*ksize2]-u_exact[m];
		 rms[m] += add*add;
	       }
            }
	 }
      }

      for(m=0;m<rms.length;m++){
         for(d=0;d<=2;d++){
            rms[m] /= grid_points[d]-2;
         }
         rms[m] = Math.sqrt(rms[m]);
      }
  }

  public int verify(int no_time_steps){
    double xcrref[]=new double[5],xceref[]=new double[5],
           xcrdif[]=new double[5],xcedif[]=new double[5], 
           xce[]=new double[5], xcr[]=new double[5], 
	   dtref=0;
    int m;
    int verified=-1;
    char clss = 'U';
//---------------------------------------------------------------------
//   compute the error norm and the residual norm, and exit if not printing
//---------------------------------------------------------------------
    error_norm(xce,0);
    compute_rhs();
    rhs_norm(xcr,0);

    for(m=0;m<xcr.length;m++) xcr[m] = xcr[m] / dt;

    for(m=1;m<xcrref.length;m++){
      xcrref[m] = 1.0;
      xceref[m] = 1.0;
    }

//---------------------------------------------------------------------
//    reference data for 12X12X12 grids after 100 time steps, with DT = 1.0d-02
//---------------------------------------------------------------------
     if (  grid_points[0] == 12
	       &&grid_points[1] == 12
	       &&grid_points[2] == 12
	       &&no_time_steps  == 60    
     ){

       clss = 'S';
       dtref = .01;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
       xcrref[0] = 1.7034283709541311E-01;
       xcrref[1] = 1.2975252070034097E-02;
       xcrref[2] = 3.2527926989486055E-02;
       xcrref[3] = 2.6436421275166801E-02;
       xcrref[4] = 1.9211784131744430E-01;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
       xceref[0] = 4.9976913345811579E-04;
       xceref[1] = 4.5195666782961927E-05;
       xceref[2] = 7.3973765172921357E-05;
       xceref[3] = 7.3821238632439731E-05;
       xceref[4] = 8.9269630987491446E-04;

//---------------------------------------------------------------------
//    reference data for 24X24X24 grids after 200 time steps, with DT = 0.8d-3
//---------------------------------------------------------------------
     }else if( (grid_points[0] == 24) &&
	       (grid_points[1] == 24) &&
	       (grid_points[2] == 24) &&
	       (no_time_steps  == 200) ) {
       
       clss = 'W';
       dtref = 0.0008;
//---------------------------------------------------------------------
//  Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
       xcrref[0] = 0.1125590409344E+03;
       xcrref[1] = 0.1180007595731E+02;
       xcrref[2] = 0.2710329767846E+02;
       xcrref[3] = 0.2469174937669E+02;
       xcrref[4] = 0.2638427874317E+03;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
       xceref[0] = 0.4419655736008E+01;
       xceref[1] = 0.4638531260002;
       xceref[2] = 0.1011551749967E+01;
       xceref[3] = 0.9235878729944;
       xceref[4] = 0.1018045837718E+02;


//---------------------------------------------------------------------
//    reference data for 64X64X64 grids after 200 time steps, with DT = 0.8d-3
//---------------------------------------------------------------------
     }else if( (grid_points[0] == 64) &&
	       (grid_points[1] == 64) &&
	       (grid_points[2] == 64) &&
	       (no_time_steps == 200) ) {

       clss = 'A';
       dtref = 0.0008;
//---------------------------------------------------------------------
//  Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
       xcrref[0] = 1.0806346714637264E+02;
       xcrref[1] = 1.1319730901220813E+01;
       xcrref[2] = 2.5974354511582465E+01;
       xcrref[3] = 2.3665622544678910E+01;
       xcrref[4] = 2.5278963211748344E+02;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
       xceref[0] = 4.2348416040525025;
       xceref[1] = 4.4390282496995698E-01;
       xceref[2] = 9.6692480136345650E-01;
       xceref[3] = 8.8302063039765474E-01;
       xceref[4] = 9.7379901770829278;

//---------------------------------------------------------------------
//    reference data for 102X102X102 grids after 200 time steps,
//    with DT = 3.0d-04
//---------------------------------------------------------------------
     }else if ( (grid_points[0] == 102) &&
		(grid_points[1] == 102) &&
		(grid_points[2] == 102) &&
		(no_time_steps == 200) ) {
       
       clss = 'B';
       dtref = .0003;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
       xcrref[0] = 1.4233597229287254E+03;
       xcrref[1] = 9.9330522590150238E+01;
       xcrref[2] = 3.5646025644535285E+02;
       xcrref[3] = 3.2485447959084092E+02;
       xcrref[4] = 3.2707541254659363E+03;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
       xceref[0] = 5.2969847140936856E+01;
       xceref[1] = 4.4632896115670668;
       xceref[2] = 1.3122573342210174E+01;
       xceref[3] = 1.2006925323559144E+01;
       xceref[4] = 1.2459576151035986E+02;

//---------------------------------------------------------------------
//    reference data for 162X162X162 grids after 200 time steps,
//    with DT = .0001
//---------------------------------------------------------------------
     }else if( (grid_points[0] == 162) &&
	       (grid_points[1] == 162) &&
	       (grid_points[2] == 162) &&
	       (no_time_steps == 200) ) {
       
       clss = 'C';
       dtref = .0001;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of residual.
//---------------------------------------------------------------------
       xcrref[0] = 0.62398116551764615E+04;
       xcrref[1] = 0.50793239190423964E+03;
       xcrref[2] = 0.15423530093013596E+04;
       xcrref[3] = 0.13302387929291190E+04;
       xcrref[4] = 0.11604087428436455E+05;

//---------------------------------------------------------------------
//  Reference values of RMS-norms of solution error.
//---------------------------------------------------------------------
       xceref[0] = 0.16462008369091265E+03;
       xceref[1] = 0.11497107903824313E+02;
       xceref[2] = 0.41207446207461508E+02;
       xceref[3] = 0.37087651059694167E+02;
       xceref[4] = 0.36211053051841265E+03;
     }
//---------------------------------------------------------------------
//    Compute the difference of solution values and the known reference values.
//---------------------------------------------------------------------
     for(m=0;m<xcr.length;m++){       
	 xcrdif[m] = Math.abs((xcr[m]-xcrref[m])/xcrref[m]);
	 xcedif[m] = Math.abs((xce[m]-xceref[m])/xceref[m]);      
     }
//---------------------------------------------------------------------
//   tolerance level
//---------------------------------------------------------------------
     double epsilon = 1.0*Math.pow(.1,8);
//---------------------------------------------------------------------
//    Output the comparison of computed results to known cases.
//---------------------------------------------------------------------
     if (clss != 'U') {
       System.out.println("Verification being performed for class "+clss);
       System.out.println("accuracy setting for epsilon = "+epsilon);
       if (Math.abs(dt-dtref) <= epsilon ) {
         verified = 1;
       }else{
	 verified = 0;
	 clss = 'U';
	 System.out.println("DT does not match the reference value of "+dtref);	 
       }
     }else{
       System.out.println("Unknown class");  
     }
     
     if (clss != 'U') System.out.println("Comparison of RMS-norms of residual");
     else System.out.println("RMS-norms of residual"); 
     verified=BMResults.printComparisonStatus(clss,verified,epsilon,
                                              xcr,xcrref,xcrdif);

     if (clss != 'U') {
       System.out.println("Comparison of RMS-norms of solution error");
     }else{
       System.out.println("RMS-norms of solution error");
     }
     verified=BMResults.printComparisonStatus(clss,verified,epsilon,
                                              xce,xceref,xcedif);

     BMResults.printVerificationStatus(clss,verified,BMName); 
     return verified;
  }
  
  public void add(){
    int i, j, k, m;
    if(timeron)timer.start(t_add);
    for(k=1;k<=grid_points[2]-2;k++){
      for(j=1;j<=grid_points[1]-2;j++){
	for(i=1;i<=grid_points[0]-2;i++){
	  for(m=0;m<=4;m++){
            u[m+i*isize2+j*jsize2+k*ksize2]+=rhs[m+i*isize2+j*jsize2+k*ksize2];
	  }
	}
      }
    }
    if(timeron)timer.stop(t_add);
  }

  public void exact_rhs(){
    double dtemp[]= new double[5], xi, eta, zeta, dtpp;
    int m, i, j, k, ip1, im1, jp1, jm1, km1, kp1;

//---------------------------------------------------------------------
//     initialize                                  
//---------------------------------------------------------------------
      for(k=0;k<=grid_points[2]-1;k++){
         for(j=0;j<=grid_points[1]-1;j++){
            for(i=0;i<=grid_points[0]-1;i++){
      	       for(m=0;m<=4;m++){
                  forcing[m+i*isize2+j*jsize2+k*ksize2] = 0.0;
               }
            }
         }
      } 
//---------------------------------------------------------------------
//     xi-direction flux differences                      
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

               forcing[0+i*isize2+j*jsize2+k*ksize2] = forcing[0+i*isize2+j*jsize2+k*ksize2] -
                       tx2*( ue[ip1+1*jsize3]-ue[im1+1*jsize3] )+
                       dx1tx1*(ue[ip1+0*jsize3]-2.0*ue[i+0*jsize3]+ue[im1+0*jsize3]);

               forcing[1+i*isize2+j*jsize2+k*ksize2] = forcing[1+i*isize2+j*jsize2+k*ksize2] - tx2 * (
                       (ue[ip1+1*jsize3]*buf[ip1+1*jsize3]+c2*(ue[ip1+4*jsize3]-q[ip1]))-
                       (ue[im1+1*jsize3]*buf[im1+1*jsize3]+c2*(ue[im1+4*jsize3]-q[im1])))+
                       xxcon1*(buf[ip1+1*jsize3]-2.0*buf[i+1*jsize3]+buf[im1+1*jsize3])+
                       dx2tx1*( ue[ip1+1*jsize3]-2.0* ue[i+1*jsize3]+ue[im1+1*jsize3]);

               forcing[2+i*isize2+j*jsize2+k*ksize2] = forcing[2+i*isize2+j*jsize2+k*ksize2] - tx2 * (
                       ue[ip1+2*jsize3]*buf[ip1+1*jsize3]-ue[im1+2*jsize3]*buf[im1+1*jsize3])+
                       xxcon2*(buf[ip1+2*jsize3]-2.0*buf[i+2*jsize3]+buf[im1+2*jsize3])+
                       dx3tx1*( ue[ip1+2*jsize3]-2.0*ue[i+2*jsize3] +ue[im1+2*jsize3]);
                  
               forcing[3+i*isize2+j*jsize2+k*ksize2] = forcing[3+i*isize2+j*jsize2+k*ksize2] - tx2*(
                       ue[ip1+3*jsize3]*buf[ip1+1*jsize3]-ue[im1+3*jsize3]*buf[im1+1*jsize3])+
                       xxcon2*(buf[ip1+3*jsize3]-2.0*buf[i+3*jsize3]+buf[im1+3*jsize3])+
                       dx4tx1*( ue[ip1+3*jsize3]-2.0* ue[i+3*jsize3]+ ue[im1+3*jsize3]);

               forcing[4+i*isize2+j*jsize2+k*ksize2] = forcing[4+i*isize2+j*jsize2+k*ksize2] - tx2*(
                       buf[ip1+1*jsize3]*(c1*ue[ip1+4*jsize3]-c2*q[ip1])-
                       buf[im1+1*jsize3]*(c1*ue[im1+4*jsize3]-c2*q[im1]))+
                       0.5*xxcon3*(buf[ip1+0*jsize3]-2.0*buf[i+0*jsize3]+
                       buf[im1+0*jsize3])+
                       xxcon4*(cuf[ip1]-2.0*cuf[i]+cuf[im1])+
                       xxcon5*(buf[ip1+4*jsize3]-2.0*buf[i+4*jsize3]+buf[im1+4*jsize3])+
                       dx5tx1*( ue[ip1+4*jsize3]-2.0* ue[i+4*jsize3]+ ue[im1+4*jsize3]);
            }
	    
//---------------------------------------------------------------------
//     Fourth-order dissipation                         
//---------------------------------------------------------------------

            for(m=0;m<=4;m++){
               i = 1;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (5.0*ue[i+m*jsize3] - 4.0*ue[i+1+m*jsize3] +ue[i+2+m*jsize3]);
               i = 2;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*ue[i-1+m*jsize3] + 6.0*ue[i+m*jsize3] -
                          4.0*ue[i+1+m*jsize3] +       ue[i+2+m*jsize3]);
            }

            for(m=0;m<=4;m++){
               for(i=3;i<=grid_points[0]-4;i++){
                  forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp*
                          (ue[i-2+m*jsize3] - 4.0*ue[i-1+m*jsize3] +
                          6.0*ue[i+m*jsize3] - 4.0*ue[i+1+m*jsize3] + ue[i+2+m*jsize3]);
               }
            }

            for(m=0;m<=4;m++){
               i = grid_points[0]-3;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (ue[i-2+m*jsize3] - 4.0*ue[i-1+m*jsize3] +
                          6.0*ue[i+m*jsize3] - 4.0*ue[i+1+m*jsize3]);
               i = grid_points[0]-2;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (ue[i-2+m*jsize3] - 4.0*ue[i-1+m*jsize3] + 5.0*ue[i+m*jsize3]);
            }

         }
      }
      
//---------------------------------------------------------------------
//     eta-direction flux differences             
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
                  
               forcing[0+i*isize2+j*jsize2+k*ksize2] = forcing[0+i*isize2+j*jsize2+k*ksize2] -
                       ty2*( ue[jp1+2*jsize3]-ue[jm1+2*jsize3] )+
                       dy1ty1*(ue[jp1+0*jsize3]-2.0*ue[j+0*jsize3]+ue[jm1+0*jsize3]);

               forcing[1+i*isize2+j*jsize2+k*ksize2] = forcing[1+i*isize2+j*jsize2+k*ksize2] - ty2*(
                       ue[jp1+1*jsize3]*buf[jp1+2*jsize3]-ue[jm1+1*jsize3]*buf[jm1+2*jsize3])+
                       yycon2*(buf[jp1+1*jsize3]-2.0*buf[j+1*jsize3]+buf[jm1+1*jsize3])+
                       dy2ty1*( ue[jp1+1*jsize3]-2.0* ue[j+1*jsize3]+ ue[jm1+1*jsize3]);

               forcing[2+i*isize2+j*jsize2+k*ksize2] = forcing[2+i*isize2+j*jsize2+k*ksize2] - ty2*(
                       (ue[jp1+2*jsize3]*buf[jp1+2*jsize3]+c2*(ue[jp1+4*jsize3]-q[jp1]))-
                       (ue[jm1+2*jsize3]*buf[jm1+2*jsize3]+c2*(ue[jm1+4*jsize3]-q[jm1])))+
                       yycon1*(buf[jp1+2*jsize3]-2.0*buf[j+2*jsize3]+buf[jm1+2*jsize3])+
                       dy3ty1*( ue[jp1+2*jsize3]-2.0*ue[j+2*jsize3] +ue[jm1+2*jsize3]);

               forcing[3+i*isize2+j*jsize2+k*ksize2] = forcing[3+i*isize2+j*jsize2+k*ksize2] - ty2*(
                       ue[jp1+3*jsize3]*buf[jp1+2*jsize3]-ue[jm1+3*jsize3]*buf[jm1+2*jsize3])+
                       yycon2*(buf[jp1+3*jsize3]-2.0*buf[j+3*jsize3]+buf[jm1+3*jsize3])+
                       dy4ty1*( ue[jp1+3*jsize3]-2.0*ue[j+3*jsize3]+ ue[jm1+3*jsize3]);

               forcing[4+i*isize2+j*jsize2+k*ksize2] = forcing[4+i*isize2+j*jsize2+k*ksize2] - ty2*(
                       buf[jp1+2*jsize3]*(c1*ue[jp1+4*jsize3]-c2*q[jp1])-
                       buf[jm1+2*jsize3]*(c1*ue[jm1+4*jsize3]-c2*q[jm1]))+
                       0.5*yycon3*(buf[jp1+0*jsize3]-2.0*buf[j+0*jsize3]+
                       buf[jm1+0*jsize3])+
                       yycon4*(cuf[jp1]-2.0*cuf[j]+cuf[jm1])+
                       yycon5*(buf[jp1+4*jsize3]-2.0*buf[j+4*jsize3]+buf[jm1+4*jsize3])+
                       dy5ty1*(ue[jp1+4*jsize3]-2.0*ue[j+4*jsize3]+ue[jm1+4*jsize3]);
            }
	    
//---------------------------------------------------------------------
//     Fourth-order dissipation                      
//---------------------------------------------------------------------
            for(m=0;m<=4;m++){
               j = 1;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (5.0*ue[j+m*jsize3] - 4.0*ue[j+1+m*jsize3] +ue[j+2+m*jsize3]);
               j = 2;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*ue[j-1+m*jsize3] + 6.0*ue[j+m*jsize3] -
                          4.0*ue[j+1+m*jsize3] +       ue[j+2+m*jsize3]);
            }

            for(m=0;m<=4;m++){
               for(j=3;j<=grid_points[1]-4;j++){
                  forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp*
                          (ue[j-2+m*jsize3] - 4.0*ue[j-1+m*jsize3] +
                          6.0*ue[j+m*jsize3] - 4.0*ue[j+1+m*jsize3] + ue[j+2+m*jsize3]);
               }
            }

            for(m=0;m<=4;m++){
               j = grid_points[1]-3;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (ue[j-2+m*jsize3] - 4.0*ue[j-1+m*jsize3] +
                          6.0*ue[j+m*jsize3] - 4.0*ue[j+1+m*jsize3]);
               j = grid_points[1]-2;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (ue[j-2+m*jsize3] - 4.0*ue[j-1+m*jsize3] + 5.0*ue[j+m*jsize3]);

            }
         }
      }

//---------------------------------------------------------------------
//     zeta-direction flux differences                      
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
               
	       forcing[0+i*isize2+j*jsize2+k*ksize2] = forcing[0+i*isize2+j*jsize2+k*ksize2] -
                       tz2*( ue[kp1+3*jsize3]-ue[km1+3*jsize3] )+
                       dz1tz1*(ue[kp1+0*jsize3]-2.0*ue[k+0*jsize3]+ue[km1+0*jsize3]);
	       
               forcing[1+i*isize2+j*jsize2+k*ksize2] = forcing[1+i*isize2+j*jsize2+k*ksize2] - tz2 * (
                       ue[kp1+1*jsize3]*buf[kp1+3*jsize3]-ue[km1+1*jsize3]*buf[km1+3*jsize3])+
                       zzcon2*(buf[kp1+1*jsize3]-2.0*buf[k+1*jsize3]+buf[km1+1*jsize3])+
                       dz2tz1*( ue[kp1+1*jsize3]-2.0* ue[k+1*jsize3]+ ue[km1+1*jsize3]);
	       
               forcing[2+i*isize2+j*jsize2+k*ksize2] = forcing[2+i*isize2+j*jsize2+k*ksize2] - tz2 * (
                       ue[kp1+2*jsize3]*buf[kp1+3*jsize3]-ue[km1+2*jsize3]*buf[km1+3*jsize3])+
                       zzcon2*(buf[kp1+2*jsize3]-2.0*buf[k+2*jsize3]+buf[km1+2*jsize3])+
                       dz3tz1*(ue[kp1+2*jsize3]-2.0*ue[k+2*jsize3]+ue[km1+2*jsize3]);
	
               forcing[3+i*isize2+j*jsize2+k*ksize2] = forcing[3+i*isize2+j*jsize2+k*ksize2] - tz2 * (
                       (ue[kp1+3*jsize3]*buf[kp1+3*jsize3]+c2*(ue[kp1+4*jsize3]-q[kp1]))-
                       (ue[km1+3*jsize3]*buf[km1+3*jsize3]+c2*(ue[km1+4*jsize3]-q[km1])))+
                       zzcon1*(buf[kp1+3*jsize3]-2.0*buf[k+3*jsize3]+buf[km1+3*jsize3])+
                       dz4tz1*( ue[kp1+3*jsize3]-2.0*ue[k+3*jsize3] +ue[km1+3*jsize3]);

               forcing[4+i*isize2+j*jsize2+k*ksize2] = forcing[4+i*isize2+j*jsize2+k*ksize2] - tz2 * (
                       buf[kp1+3*jsize3]*(c1*ue[kp1+4*jsize3]-c2*q[kp1])-
                       buf[km1+3*jsize3]*(c1*ue[km1+4*jsize3]-c2*q[km1]))+
                       0.5*zzcon3*(buf[kp1+0*jsize3]-2.0*buf[k+0*jsize3]
                       +buf[km1+0*jsize3])+
                       zzcon4*(cuf[kp1]-2.0*cuf[k]+cuf[km1])+
                       zzcon5*(buf[kp1+4*jsize3]-2.0*buf[k+4*jsize3]+buf[km1+4*jsize3])+
                       dz5tz1*( ue[kp1+4*jsize3]-2.0*ue[k+4*jsize3]+ ue[km1+4*jsize3]);
            }
	    
//---------------------------------------------------------------------
//     Fourth-order dissipation                        
//---------------------------------------------------------------------
            for(m=0;m<=4;m++){
               k = 1;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (5.0*ue[k+m*jsize3] - 4.0*ue[k+1+m*jsize3] +ue[k+2+m*jsize3]);
               k = 2;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*ue[k-1+m*jsize3] + 6.0*ue[k+m*jsize3] -
                          4.0*ue[k+1+m*jsize3] +       ue[k+2+m*jsize3]);
            }

            for(m=0;m<=4;m++){
               for(k=3;k<=grid_points[2]-4;k++){
                  forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp*
                          (ue[k-2+m*jsize3] - 4.0*ue[k-1+m*jsize3] +
                          6.0*ue[k+m*jsize3] - 4.0*ue[k+1+m*jsize3] + ue[k+2+m*jsize3]);
               }
            }

            for(m=0;m<=4;m++){
               k = grid_points[2]-3;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (ue[k-2+m*jsize3] - 4.0*ue[k-1+m*jsize3] +
                          6.0*ue[k+m*jsize3] - 4.0*ue[k+1+m*jsize3]);
               k = grid_points[2]-2;
               forcing[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (ue[k-2+m*jsize3] - 4.0*ue[k-1+m*jsize3] + 5.0*ue[k+m*jsize3]);
            }
         }
      }

//---------------------------------------------------------------------
//     now change the sign of the forcing function, 
//---------------------------------------------------------------------
      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
      	       for(m=0;m<=4;m++){
                  forcing[m+i*isize2+j*jsize2+k*ksize2] = -1.0 * forcing[m+i*isize2+j*jsize2+k*ksize2];
               }
            }
         }
      }
  }
  
   public void x_solve(){
     int i,j,k,m,n,isize;

     if(timeron) timer.start(t_xsolve);
//---------------------------------------------------------------------
//     This function computes the left hand side in the xi-direction
//---------------------------------------------------------------------

      isize = grid_points[0]-1;

//---------------------------------------------------------------------
//     determine a (labeled f) and n jacobians
//---------------------------------------------------------------------
      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=0;i<=isize;i++){

               tmp1 = rho_i[i+j*jsize1+k*ksize1];
               tmp2 = tmp1 * tmp1;
               tmp3 = tmp1 * tmp2;
//---------------------------------------------------------------------
//---------------------------------------------------------------------
               fjac[0+0*isize4+i*jsize4] = 0.0;
               fjac[0+1*isize4+i*jsize4] = 1.0;
               fjac[0+2*isize4+i*jsize4] = 0.0;
               fjac[0+3*isize4+i*jsize4] = 0.0;
               fjac[0+4*isize4+i*jsize4] = 0.0;

               fjac[1+0*isize4+i*jsize4] = -(u[1+i*isize2+j*jsize2+k*ksize2]) * tmp2 * 
                    u[1+i*isize2+j*jsize2+k*ksize2]
                    + c2 * qs[i+j*jsize1+k*ksize1];
               fjac[1+1*isize4+i*jsize4] = ( 2.0 - c2 )
                    * ( u[1+i*isize2+j*jsize2+k*ksize2]  / u[0+i*isize2+j*jsize2+k*ksize2] );
               fjac[1+2*isize4+i*jsize4] = - c2 * ( u[2+i*isize2+j*jsize2+k*ksize2] * tmp1 );
               fjac[1+3*isize4+i*jsize4] = - c2 * ( u[3+i*isize2+j*jsize2+k*ksize2] * tmp1 );
               fjac[1+4*isize4+i*jsize4] = c2;

               fjac[2+0*isize4+i*jsize4] = - ( u[1+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] ) * tmp2;
               fjac[2+1*isize4+i*jsize4] = u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+2*isize4+i*jsize4] = u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+3*isize4+i*jsize4] = 0.0;
               fjac[2+4*isize4+i*jsize4] = 0.0;

               fjac[3+0*isize4+i*jsize4] = - ( u[1+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] ) * tmp2;
               fjac[3+1*isize4+i*jsize4] = u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+2*isize4+i*jsize4] = 0.0;
               fjac[3+3*isize4+i*jsize4] = u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+4*isize4+i*jsize4] = 0.0;

               fjac[4+0*isize4+i*jsize4] = ( c2 * 2.0 * square[i+j*jsize1+k*ksize1]
                    - c1 * u[4+i*isize2+j*jsize2+k*ksize2] )
                    * ( u[1+i*isize2+j*jsize2+k*ksize2] * tmp2 );
               fjac[4+1*isize4+i*jsize4] = c1 *  u[4+i*isize2+j*jsize2+k*ksize2] * tmp1 
                    - c2
                    * ( u[1+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] * tmp2
                    + qs[i+j*jsize1+k*ksize1]);
               fjac[4+2*isize4+i*jsize4] = - c2 * ( u[2+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[4+3*isize4+i*jsize4] = - c2 * ( u[3+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[4+4*isize4+i*jsize4] = c1 * ( u[1+i*isize2+j*jsize2+k*ksize2] * tmp1 );

               njac[0+0*isize4+i*jsize4] = 0.0;
               njac[0+1*isize4+i*jsize4] = 0.0;
               njac[0+2*isize4+i*jsize4] = 0.0;
               njac[0+3*isize4+i*jsize4] = 0.0;
               njac[0+4*isize4+i*jsize4] = 0.0;

               njac[1+0*isize4+i*jsize4] = - con43 * c3c4 * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[1+1*isize4+i*jsize4] =   con43 * c3c4 * tmp1;
               njac[1+2*isize4+i*jsize4] =   0.0;
               njac[1+3*isize4+i*jsize4] =   0.0;
               njac[1+4*isize4+i*jsize4] =   0.0;

               njac[2+0*isize4+i*jsize4] = - c3c4 * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[2+1*isize4+i*jsize4] =   0.0;
               njac[2+2*isize4+i*jsize4] =   c3c4 * tmp1;
               njac[2+3*isize4+i*jsize4] =   0.0;
               njac[2+4*isize4+i*jsize4] =   0.0;

               njac[3+0*isize4+i*jsize4] = - c3c4 * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[3+1*isize4+i*jsize4] =   0.0;
               njac[3+2*isize4+i*jsize4] =   0.0;
               njac[3+3*isize4+i*jsize4] =   c3c4 * tmp1;
               njac[3+4*isize4+i*jsize4] =   0.0;

               njac[4+0*isize4+i*jsize4] = - ( con43 * c3c4
                    - c1345 ) * tmp3 * ( Math.pow(u[1+i*isize2+j*jsize2+k*ksize2],2) )
                    - ( c3c4 - c1345 ) * tmp3 * ( Math.pow(u[2+i*isize2+j*jsize2+k*ksize2],2) )
                    - ( c3c4 - c1345 ) * tmp3 * ( Math.pow(u[3+i*isize2+j*jsize2+k*ksize2],2) )
                    - c1345 * tmp2 * u[4+i*isize2+j*jsize2+k*ksize2];

               njac[4+1*isize4+i*jsize4] = ( con43 * c3c4
                    - c1345 ) * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[4+2*isize4+i*jsize4] = ( c3c4 - c1345 ) * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[4+3*isize4+i*jsize4] = ( c3c4 - c1345 ) * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[4+4*isize4+i*jsize4] = ( c1345 ) * tmp1;

            }
//---------------------------------------------------------------------
//     now jacobians set, so form left hand side in x direction
//---------------------------------------------------------------------
            lhsinit(lhs, isize);
	    
            for(i=1;i<=isize-1;i++){

               tmp1 = dt * tx1;
               tmp2 = dt * tx2;

               lhs[0+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(i-1)*jsize4]
                    - tmp1 * dx1 ;
               lhs[0+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(i-1)*jsize4];
               lhs[0+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(i-1)*jsize4];
               lhs[0+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(i-1)*jsize4];
               lhs[0+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(i-1)*jsize4];

               lhs[1+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(i-1)*jsize4];
               lhs[1+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(i-1)*jsize4]
                    - tmp1 * dx2;
               lhs[1+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(i-1)*jsize4];
               lhs[1+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(i-1)*jsize4];
               lhs[1+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(i-1)*jsize4];

               lhs[2+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(i-1)*jsize4];
               lhs[2+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(i-1)*jsize4];
               lhs[2+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(i-1)*jsize4]
                    - tmp1 * dx3 ;
               lhs[2+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(i-1)*jsize4];
               lhs[2+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(i-1)*jsize4];

               lhs[3+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(i-1)*jsize4];
               lhs[3+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(i-1)*jsize4];
               lhs[3+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(i-1)*jsize4];
               lhs[3+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(i-1)*jsize4]
                    - tmp1 * dx4;
               lhs[3+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(i-1)*jsize4];

               lhs[4+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(i-1)*jsize4];
               lhs[4+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(i-1)*jsize4];
               lhs[4+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(i-1)*jsize4];
               lhs[4+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(i-1)*jsize4];
               lhs[4+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(i-1)*jsize4]
                    - tmp1 * dx5;
		    
               lhs[0+0*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[0+0*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx1;
               lhs[0+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+1*isize4+i*jsize4];
               lhs[0+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+2*isize4+i*jsize4];
               lhs[0+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+3*isize4+i*jsize4];
               lhs[0+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+4*isize4+i*jsize4];
	      
	       
              
	       lhs[1+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+0*isize4+i*jsize4];
               lhs[1+1*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[1+1*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx2;
               lhs[1+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+2*isize4+i*jsize4];
               lhs[1+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+3*isize4+i*jsize4];
               lhs[1+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+4*isize4+i*jsize4];

               lhs[2+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+0*isize4+i*jsize4];
               lhs[2+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+1*isize4+i*jsize4];
               lhs[2+2*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[2+2*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx3;
               lhs[2+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+3*isize4+i*jsize4];
               lhs[2+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+4*isize4+i*jsize4];

               lhs[3+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+0*isize4+i*jsize4];
               lhs[3+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+1*isize4+i*jsize4];
               lhs[3+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+2*isize4+i*jsize4];
               lhs[3+3*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[3+3*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx4;
               lhs[3+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+4*isize4+i*jsize4];

               lhs[4+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+0*isize4+i*jsize4];
               lhs[4+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+1*isize4+i*jsize4];
               lhs[4+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+2*isize4+i*jsize4];
               lhs[4+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+3*isize4+i*jsize4];
               lhs[4+4*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[4+4*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx5;
	       
               
	       lhs[0+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(i+1)*jsize4]
                    - tmp1 * dx1;
               lhs[0+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(i+1)*jsize4];
               lhs[0+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(i+1)*jsize4];
               lhs[0+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(i+1)*jsize4];
               lhs[0+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(i+1)*jsize4];

               lhs[1+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(i+1)*jsize4];
               lhs[1+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(i+1)*jsize4]
                    - tmp1 * dx2;
               lhs[1+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(i+1)*jsize4];
               lhs[1+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(i+1)*jsize4];
               lhs[1+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(i+1)*jsize4];

               lhs[2+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(i+1)*jsize4];
               lhs[2+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(i+1)*jsize4];
               lhs[2+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(i+1)*jsize4]
                    - tmp1 * dx3;
               lhs[2+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(i+1)*jsize4];
               lhs[2+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(i+1)*jsize4];

               lhs[3+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(i+1)*jsize4];
               lhs[3+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(i+1)*jsize4];
               lhs[3+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(i+1)*jsize4];
               lhs[3+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(i+1)*jsize4]
                    - tmp1 * dx4;
               lhs[3+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(i+1)*jsize4];

               lhs[4+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(i+1)*jsize4];
               lhs[4+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(i+1)*jsize4];
               lhs[4+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(i+1)*jsize4];
               lhs[4+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(i+1)*jsize4];
               lhs[4+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(i+1)*jsize4]
                    - tmp1 * dx5;

            }
//---------------------------------------------------------------------
//     performs guaussian elimination on this cell.
//     
//     assumes that unpacking routines for non-first cells 
//     preload C' and rhs' from previous cell.
//     
//     assumed send happens outside this routine, but that
//     c'(IMAX) and rhs'(IMAX) will be sent to next cell
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//     outer most do loops - sweeping in i direction
//---------------------------------------------------------------------
	    
//---------------------------------------------------------------------
//     multiply c(0,j,k) by b_inverse and copy back to c
//     multiply rhs(0) by b_inverse(0) and copy to rhs
//---------------------------------------------------------------------
             binvcrhs( lhs,0+0*isize4+bb*jsize4+0*ksize4,
                              lhs,0+0*isize4+cc*jsize4+0*ksize4,
                              rhs, 0+0*isize2+j*jsize2+k*ksize2);
	     
//---------------------------------------------------------------------
//     begin inner most do loop
//     do all the elements of the cell unless last 
//---------------------------------------------------------------------
            for(i=1;i<=isize-1;i++){

//---------------------------------------------------------------------
//     rhs(i) = rhs(i) - A*rhs(i-1)
//---------------------------------------------------------------------
                matvec_sub(lhs,0+0*isize4+aa*jsize4+i*ksize4,
                               rhs,0+(i-1)*isize2+j*jsize2+k*ksize2,
	                       rhs,0+i*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     B(i) = B(i) - C(i-1)*A(i)
//---------------------------------------------------------------------
                matmul_sub(lhs,0+0*isize4+aa*jsize4+i*ksize4,
                               lhs,0+0*isize4+cc*jsize4+(i-1)*ksize4,
                               lhs,0+0*isize4+bb*jsize4+i*ksize4);


//---------------------------------------------------------------------
//     multiply c(i,j,k) by b_inverse and copy back to c
//     multiply rhs(1,j,k) by b_inverse(1,j,k) and copy to rhs
//---------------------------------------------------------------------
                binvcrhs( lhs,0+0*isize4+bb*jsize4+i*ksize4,
                              lhs,0+0*isize4+cc*jsize4+i*ksize4,
                              rhs,0+i*isize2+j*jsize2+k*ksize2 );

     }
//---------------------------------------------------------------------
//     rhs(isize) = rhs(isize) - A*rhs(isize-1)
//---------------------------------------------------------------------
             matvec_sub(lhs,0+0*isize4+aa*jsize4+isize*ksize4,
			    rhs,0+(isize-1)*isize2+j*jsize2+k*ksize2,
			    rhs,0+isize*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     B(isize) = B(isize) - C(isize-1)*A(isize)
//---------------------------------------------------------------------
             matmul_sub(lhs,0+0*isize4+aa*jsize4+isize*ksize4,
                               lhs,0+0*isize4+cc*jsize4+(isize-1)*ksize4,
                               lhs,0+0*isize4+bb*jsize4+isize*ksize4);

//---------------------------------------------------------------------
//     multiply rhs() by b_inverse() and copy to rhs
//---------------------------------------------------------------------
             binvrhs( lhs,0+0*isize4+bb*jsize4+isize*ksize4,
                             rhs,0+isize*isize2+j*jsize2+k*ksize2);
			     
//---------------------------------------------------------------------
//     back solve: if last cell, then generate U(isize)=rhs(isize)
//     else assume U(isize) is loaded in un pack backsub_info
//     so just use it
//     after call u(istart) will be sent to next cell
//---------------------------------------------------------------------

            for(i=isize-1;i>=0;i--){
               for(m=0;m<=BLOCK_SIZE-1;m++){
                  for(n=0;n<=BLOCK_SIZE-1;n++){
                     rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] 
                          - lhs[m+n*isize4+cc*jsize4+i*ksize4]*rhs[n+(i+1)*isize2+j*jsize2+k*ksize2];
                  }
               }
            }
         }
      }
      if(timeron)timer.stop(t_xsolve);
  }
 
    public void compute_rhs(){
    int i, j, k, m;
    double rho_inv, uijk, up1, um1, vijk, vp1, vm1,
           wijk, wp1, wm1;

      if (timeron)timer.start(t_rhs);
//---------------------------------------------------------------------
//     compute the reciprocal of density, and the kinetic energy,
//     and the speed of sound.
//---------------------------------------------------------------------
      for(k=0;k<=grid_points[2]-1;k++){
         for(j=0;j<=grid_points[1]-1;j++){
            for(i=0;i<=grid_points[0]-1;i++){
               rho_inv = 1.0/u[0+i*isize2+j*jsize2+k*ksize2];
               rho_i[i+j*jsize1+k*ksize1] = rho_inv;
               us[i+j*jsize1+k*ksize1] = u[1+i*isize2+j*jsize2+k*ksize2] * rho_inv;
               vs[i+j*jsize1+k*ksize1] = u[2+i*isize2+j*jsize2+k*ksize2] * rho_inv;
               ws[i+j*jsize1+k*ksize1] = u[3+i*isize2+j*jsize2+k*ksize2] * rho_inv;
               square[i+j*jsize1+k*ksize1]     = 0.5* (
                       u[1+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[2+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[3+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] ) * rho_inv;
               qs[i+j*jsize1+k*ksize1] = square[i+j*jsize1+k*ksize1] * rho_inv;
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
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = forcing[m+i*isize2+j*jsize2+k*ksize2];
               }
            }
         }
      }

      if (timeron) timer.start(t_rhsx);
//---------------------------------------------------------------------
//     compute xi-direction fluxes
//---------------------------------------------------------------------
      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
               uijk = us[i+j*jsize1+k*ksize1];
               up1  = us[(i+1)+j*jsize1+k*ksize1];
               um1  = us[(i-1)+j*jsize1+k*ksize1];

               rhs[0+i*isize2+j*jsize2+k*ksize2] = rhs[0+i*isize2+j*jsize2+k*ksize2] + dx1tx1 *
                       (u[0+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[0+i*isize2+j*jsize2+k*ksize2] +
                       u[0+(i-1)*isize2+j*jsize2+k*ksize2]) -
                       tx2 * (u[1+(i+1)*isize2+j*jsize2+k*ksize2] - u[1+(i-1)*isize2+j*jsize2+k*ksize2]);

               rhs[1+i*isize2+j*jsize2+k*ksize2] = rhs[1+i*isize2+j*jsize2+k*ksize2] + dx2tx1 *
                       (u[1+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[1+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon2*con43 * (up1 - 2.0*uijk + um1) -
                       tx2 * (u[1+(i+1)*isize2+j*jsize2+k*ksize2]*up1 -
                       u[1+(i-1)*isize2+j*jsize2+k*ksize2]*um1 +
                       (u[4+(i+1)*isize2+j*jsize2+k*ksize2]- square[(i+1)+j*jsize1+k*ksize1]-
                       u[4+(i-1)*isize2+j*jsize2+k*ksize2]+ square[(i-1)+j*jsize1+k*ksize1])*
                       c2);

               rhs[2+i*isize2+j*jsize2+k*ksize2] = rhs[2+i*isize2+j*jsize2+k*ksize2] + dx3tx1 *
                       (u[2+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[2+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon2 * (vs[(i+1)+j*jsize1+k*ksize1] - 2.0*vs[i+j*jsize1+k*ksize1] +
                       vs[(i-1)+j*jsize1+k*ksize1]) -
                       tx2 * (u[2+(i+1)*isize2+j*jsize2+k*ksize2]*up1 -
                       u[2+(i-1)*isize2+j*jsize2+k*ksize2]*um1);

               rhs[3+i*isize2+j*jsize2+k*ksize2] = rhs[3+i*isize2+j*jsize2+k*ksize2] + dx4tx1 *
                       (u[3+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[3+i*isize2+j*jsize2+k*ksize2] +
                       u[3+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon2 * (ws[(i+1)+j*jsize1+k*ksize1] - 2.0*ws[i+j*jsize1+k*ksize1] +
                       ws[(i-1)+j*jsize1+k*ksize1]) -
                       tx2 * (u[3+(i+1)*isize2+j*jsize2+k*ksize2]*up1 -
                       u[3+(i-1)*isize2+j*jsize2+k*ksize2]*um1);

               rhs[4+i*isize2+j*jsize2+k*ksize2] = rhs[4+i*isize2+j*jsize2+k*ksize2] + dx5tx1 *
                       (u[4+(i+1)*isize2+j*jsize2+k*ksize2] - 2.0*u[4+i*isize2+j*jsize2+k*ksize2] +
                       u[4+(i-1)*isize2+j*jsize2+k*ksize2]) +
                       xxcon3 * (qs[(i+1)+j*jsize1+k*ksize1] - 2.0*qs[i+j*jsize1+k*ksize1] +
                       qs[(i-1)+j*jsize1+k*ksize1]) +
                       xxcon4 * (up1*up1 -       2.0*uijk*uijk +
                       um1*um1) +
                       xxcon5 * (u[4+(i+1)*isize2+j*jsize2+k*ksize2]*rho_i[(i+1)+j*jsize1+k*ksize1] -
                       2.0*u[4+i*isize2+j*jsize2+k*ksize2]*rho_i[i+j*jsize1+k*ksize1] +
                       u[4+(i-1)*isize2+j*jsize2+k*ksize2]*rho_i[(i-1)+j*jsize1+k*ksize1]) -
                       tx2 * ( (c1*u[4+(i+1)*isize2+j*jsize2+k*ksize2] -
                       c2*square[(i+1)+j*jsize1+k*ksize1])*up1 -
                       (c1*u[4+(i-1)*isize2+j*jsize2+k*ksize2] -
                       c2*square[(i-1)+j*jsize1+k*ksize1])*um1 );
            }
         }

//---------------------------------------------------------------------
//     add fourth order xi-direction dissipation
//---------------------------------------------------------------------
         for(j=1;j<=grid_points[1]-2;j++){
            i = 1;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2]- dssp *
                          ( 5.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] +
                          u[m+(i+2)*isize2+j*jsize2+k*ksize2]);
            }

            i = 2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*u[m+(i-1)*isize2+j*jsize2+k*ksize2] + 6.0*u[m+i*isize2+j*jsize2+k*ksize2] -
                          4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] + u[m+(i+2)*isize2+j*jsize2+k*ksize2]);
            }

      	    for(m=0;m<=4;m++){
               for(i=3;i<=grid_points[0]-4;i++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (  u[m+(i-2)*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i-1)*isize2+j*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] +
                          u[m+(i+2)*isize2+j*jsize2+k*ksize2] );
               }
            }

            i = grid_points[0]-3;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+(i-2)*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i-1)*isize2+j*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+(i+1)*isize2+j*jsize2+k*ksize2] );
            }

            i = grid_points[0]-2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+(i-2)*isize2+j*jsize2+k*ksize2] - 4.*u[m+(i-1)*isize2+j*jsize2+k*ksize2] +
                          5.*u[m+i*isize2+j*jsize2+k*ksize2] );
            }
         }
      }
      if (timeron)  timer.stop(t_rhsx);

      if (timeron) timer.start(t_rhsy);
//---------------------------------------------------------------------
//     compute eta-direction fluxes
//---------------------------------------------------------------------
      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
               vijk = vs[i+j*jsize1+k*ksize1];
               vp1  = vs[i+(j+1)*jsize1+k*ksize1];
               vm1  = vs[i+(j-1)*jsize1+k*ksize1];
               rhs[0+i*isize2+j*jsize2+k*ksize2] = rhs[0+i*isize2+j*jsize2+k*ksize2] + dy1ty1 *
                       (u[0+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[0+i*isize2+j*jsize2+k*ksize2] +
                       u[0+i*isize2+(j-1)*jsize2+k*ksize2]) -
                       ty2 * (u[2+i*isize2+(j+1)*jsize2+k*ksize2] - u[2+i*isize2+(j-1)*jsize2+k*ksize2]);
               rhs[1+i*isize2+j*jsize2+k*ksize2] = rhs[1+i*isize2+j*jsize2+k*ksize2] + dy2ty1 *
                       (u[1+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[1+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon2 * (us[i+(j+1)*jsize1+k*ksize1] - 2.0*us[i+j*jsize1+k*ksize1] +
                       us[i+(j-1)*jsize1+k*ksize1]) -
                       ty2 * (u[1+i*isize2+(j+1)*jsize2+k*ksize2]*vp1 -
                       u[1+i*isize2+(j-1)*jsize2+k*ksize2]*vm1);
               rhs[2+i*isize2+j*jsize2+k*ksize2] = rhs[2+i*isize2+j*jsize2+k*ksize2] + dy3ty1 *
                       (u[2+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[2+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon2*con43 * (vp1 - 2.0*vijk + vm1) -
                       ty2 * (u[2+i*isize2+(j+1)*jsize2+k*ksize2]*vp1 -
                       u[2+i*isize2+(j-1)*jsize2+k*ksize2]*vm1 +
                       (u[4+i*isize2+(j+1)*jsize2+k*ksize2] - square[i+(j+1)*jsize1+k*ksize1] -
                       u[4+i*isize2+(j-1)*jsize2+k*ksize2] + square[i+(j-1)*jsize1+k*ksize1])
                       *c2);
               rhs[3+i*isize2+j*jsize2+k*ksize2] = rhs[3+i*isize2+j*jsize2+k*ksize2] + dy4ty1 *
                       (u[3+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[3+i*isize2+j*jsize2+k*ksize2] +
                       u[3+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon2 * (ws[i+(j+1)*jsize1+k*ksize1] - 2.0*ws[i+j*jsize1+k*ksize1] +
                       ws[i+(j-1)*jsize1+k*ksize1]) -
                       ty2 * (u[3+i*isize2+(j+1)*jsize2+k*ksize2]*vp1 -
                       u[3+i*isize2+(j-1)*jsize2+k*ksize2]*vm1);
               rhs[4+i*isize2+j*jsize2+k*ksize2] = rhs[4+i*isize2+j*jsize2+k*ksize2] + dy5ty1 *
                       (u[4+i*isize2+(j+1)*jsize2+k*ksize2] - 2.0*u[4+i*isize2+j*jsize2+k*ksize2] +
                       u[4+i*isize2+(j-1)*jsize2+k*ksize2]) +
                       yycon3 * (qs[i+(j+1)*jsize1+k*ksize1] - 2.0*qs[i+j*jsize1+k*ksize1] +
                       qs[i+(j-1)*jsize1+k*ksize1]) +
                       yycon4 * (vp1*vp1       - 2.0*vijk*vijk +
                       vm1*vm1) +
                       yycon5 * (u[4+i*isize2+(j+1)*jsize2+k*ksize2]*rho_i[i+(j+1)*jsize1+k*ksize1] -
                       2.0*u[4+i*isize2+j*jsize2+k*ksize2]*rho_i[i+j*jsize1+k*ksize1] +
                       u[4+i*isize2+(j-1)*jsize2+k*ksize2]*rho_i[i+(j-1)*jsize1+k*ksize1]) -
                       ty2 * ((c1*u[4+i*isize2+(j+1)*jsize2+k*ksize2] -
                       c2*square[i+(j+1)*jsize1+k*ksize1]) * vp1 -
                       (c1*u[4+i*isize2+(j-1)*jsize2+k*ksize2] -
                       c2*square[i+(j-1)*jsize1+k*ksize1]) * vm1);
            }
         }

//---------------------------------------------------------------------
//     add fourth order eta-direction dissipation
//---------------------------------------------------------------------
         for(i=1;i<=grid_points[0]-2;i++){
            j = 1;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2]- dssp *
                          ( 5.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] +
                          u[m+i*isize2+(j+2)*jsize2+k*ksize2]);
            }

            j = 2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*u[m+i*isize2+(j-1)*jsize2+k*ksize2] + 6.0*u[m+i*isize2+j*jsize2+k*ksize2] -
                          4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] + u[m+i*isize2+(j+2)*jsize2+k*ksize2]);
            }
         }

         for(j=3;j<=grid_points[1]-4;j++){
            for(i=1;i<=grid_points[0]-2;i++){
      	       for(m=0;m<=4;m++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (  u[m+i*isize2+(j-2)*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j-1)*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] +
                          u[m+i*isize2+(j+2)*jsize2+k*ksize2] );
               }
            }
         }

         for(i=1;i<=grid_points[0]-2;i++){
            j = grid_points[1]-3;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+(j-2)*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j-1)*jsize2+k*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+(j+1)*jsize2+k*ksize2] );
            }

            j = grid_points[1]-2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+(j-2)*jsize2+k*ksize2] - 4.*u[m+i*isize2+(j-1)*jsize2+k*ksize2] +
                          5.*u[m+i*isize2+j*jsize2+k*ksize2] );
            }
         }
      }
      if (timeron) timer.stop(t_rhsy);
      if (timeron) timer.start(t_rhsz);

//---------------------------------------------------------------------
//     compute zeta-direction fluxes
//---------------------------------------------------------------------
      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
               wijk = ws[i+j*jsize1+k*ksize1];
               wp1  = ws[i+j*jsize1+(k+1)*ksize1];
               wm1  = ws[i+j*jsize1+(k-1)*ksize1];

               rhs[0+i*isize2+j*jsize2+k*ksize2] = rhs[0+i*isize2+j*jsize2+k*ksize2] + dz1tz1 *
                       (u[0+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[0+i*isize2+j*jsize2+k*ksize2] +
                       u[0+i*isize2+j*jsize2+(k-1)*ksize2]) -
                       tz2 * (u[3+i*isize2+j*jsize2+(k+1)*ksize2] - u[3+i*isize2+j*jsize2+(k-1)*ksize2]);
               rhs[1+i*isize2+j*jsize2+k*ksize2] = rhs[1+i*isize2+j*jsize2+k*ksize2] + dz2tz1 *
                       (u[1+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[1+i*isize2+j*jsize2+k*ksize2] +
                       u[1+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon2 * (us[i+j*jsize1+(k+1)*ksize1] - 2.0*us[i+j*jsize1+k*ksize1] +
                       us[i+j*jsize1+(k-1)*ksize1]) -
                       tz2 * (u[1+i*isize2+j*jsize2+(k+1)*ksize2]*wp1 -
                       u[1+i*isize2+j*jsize2+(k-1)*ksize2]*wm1);
               rhs[2+i*isize2+j*jsize2+k*ksize2] = rhs[2+i*isize2+j*jsize2+k*ksize2] + dz3tz1 *
                       (u[2+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[2+i*isize2+j*jsize2+k*ksize2] +
                       u[2+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon2 * (vs[i+j*jsize1+(k+1)*ksize1] - 2.0*vs[i+j*jsize1+k*ksize1] +
                       vs[i+j*jsize1+(k-1)*ksize1]) -
                       tz2 * (u[2+i*isize2+j*jsize2+(k+1)*ksize2]*wp1 -
                       u[2+i*isize2+j*jsize2+(k-1)*ksize2]*wm1);
               rhs[3+i*isize2+j*jsize2+k*ksize2] = rhs[3+i*isize2+j*jsize2+k*ksize2] + dz4tz1 *
                       (u[3+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[3+i*isize2+j*jsize2+k*ksize2] +
                       u[3+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon2*con43 * (wp1 - 2.0*wijk + wm1) -
                       tz2 * (u[3+i*isize2+j*jsize2+(k+1)*ksize2]*wp1 -
                       u[3+i*isize2+j*jsize2+(k-1)*ksize2]*wm1 +
                       (u[4+i*isize2+j*jsize2+(k+1)*ksize2] - square[i+j*jsize1+(k+1)*ksize1] -
                       u[4+i*isize2+j*jsize2+(k-1)*ksize2] + square[i+j*jsize1+(k-1)*ksize1])
                       *c2);
               rhs[4+i*isize2+j*jsize2+k*ksize2] = rhs[4+i*isize2+j*jsize2+k*ksize2] + dz5tz1 *
                       (u[4+i*isize2+j*jsize2+(k+1)*ksize2] - 2.0*u[4+i*isize2+j*jsize2+k*ksize2] +
                       u[4+i*isize2+j*jsize2+(k-1)*ksize2]) +
                       zzcon3 * (qs[i+j*jsize1+(k+1)*ksize1] - 2.0*qs[i+j*jsize1+k*ksize1] +
                       qs[i+j*jsize1+(k-1)*ksize1]) +
                       zzcon4 * (wp1*wp1 - 2.0*wijk*wijk +
                       wm1*wm1) +
                       zzcon5 * (u[4+i*isize2+j*jsize2+(k+1)*ksize2]*rho_i[i+j*jsize1+(k+1)*ksize1] -
                       2.0*u[4+i*isize2+j*jsize2+k*ksize2]*rho_i[i+j*jsize1+k*ksize1] +
                       u[4+i*isize2+j*jsize2+(k-1)*ksize2]*rho_i[i+j*jsize1+(k-1)*ksize1]) -
                       tz2 * ( (c1*u[4+i*isize2+j*jsize2+(k+1)*ksize2] -
                       c2*square[i+j*jsize1+(k+1)*ksize1])*wp1 -
                       (c1*u[4+i*isize2+j*jsize2+(k-1)*ksize2] -
                       c2*square[i+j*jsize1+(k-1)*ksize1])*wm1);
            }
         }
      }

//---------------------------------------------------------------------
//     add fourth order zeta-direction dissipation
//---------------------------------------------------------------------
      for(j=1;j<=grid_points[1]-2;j++){
         for(i=1;i<=grid_points[0]-2;i++){
            k = 1;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2]- dssp *
                          ( 5.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] +
                          u[m+i*isize2+j*jsize2+(k+2)*ksize2]);
            }

            k = 2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (-4.0*u[m+i*isize2+j*jsize2+(k-1)*ksize2] + 6.0*u[m+i*isize2+j*jsize2+k*ksize2] -
                          4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] + u[m+i*isize2+j*jsize2+(k+2)*ksize2]);
            }
         }
      }

      for(k=3;k<=grid_points[2]-4;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
      	       for(m=0;m<=4;m++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          (  u[m+i*isize2+j*jsize2+(k-2)*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k-1)*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] +
                          u[m+i*isize2+j*jsize2+(k+2)*ksize2] );
               }
            }
         }
      }

      for(j=1;j<=grid_points[1]-2;j++){
         for(i=1;i<=grid_points[0]-2;i++){
            k = grid_points[2]-3;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+j*jsize2+(k-2)*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k-1)*ksize2] +
                          6.0*u[m+i*isize2+j*jsize2+k*ksize2] - 4.0*u[m+i*isize2+j*jsize2+(k+1)*ksize2] );
            }

            k = grid_points[2]-2;
      	    for(m=0;m<=4;m++){
               rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] - dssp *
                          ( u[m+i*isize2+j*jsize2+(k-2)*ksize2] - 4.*u[m+i*isize2+j*jsize2+(k-1)*ksize2] +
                          5.0*u[m+i*isize2+j*jsize2+k*ksize2] );
            }
         }
      }
      if (timeron) timer.stop(t_rhsz);

      for(k=1;k<=grid_points[2]-2;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=1;i<=grid_points[0]-2;i++){
      	       for(m=0;m<=4;m++){
                  rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] * dt;
               }
            }
         }
      }
      if (timeron) timer.stop(t_rhs);
  }
  
  public void print_lhs(){
    double count1=0,count2=0,count3=0;
    for(int i=0;i<5;i++){
      for(int j=0;j<5;j++){
	for(int m=0;m<problem_size+1;m++){
	  count2+=njac[i+j*isize4+m*jsize4];
	  count3+=fjac[i+j*isize4+m*jsize4];
	  for(int k=0;k<3;k++){
	    count1+=lhs[i+j*isize4+k*jsize4+m*ksize4];
	  }
	}
      }
    }
    System.out.print("lhs checksum is: ");
    System.out.println(count1);
    System.out.print("fjac checksum is: ");
    System.out.println(count3);
    System.out.print("njac checksum is: ");
    System.out.println(count2);
  }

  public void y_solve(){
    int i, j, k, m, n, jsize;

    if (timeron)   timer.start(t_ysolve);

//---------------------------------------------------------------------
//     This function computes the left hand side for the three y-factors   
//---------------------------------------------------------------------

      jsize = grid_points[1]-1;

//---------------------------------------------------------------------
//     Compute the indices for storing the tri-diagonal matrix;
//     determine a (labeled f) and n jacobians for cell c
//---------------------------------------------------------------------
      for(k=1;k<=grid_points[2]-2;k++){
         for(i=1;i<=grid_points[0]-2;i++){
            for(j=0;j<=jsize;j++){

               tmp1 = rho_i[i+j*jsize1+k*ksize1];
               tmp2 = tmp1 * tmp1;
               tmp3 = tmp1 * tmp2;

               fjac[0+0*isize4+j*jsize4] = 0.0;
               fjac[0+1*isize4+j*jsize4] = 0.0;
               fjac[0+2*isize4+j*jsize4] = 1.0;
               fjac[0+3*isize4+j*jsize4] = 0.0;
               fjac[0+4*isize4+j*jsize4] = 0.0;

               fjac[1+0*isize4+j*jsize4] = - ( u[1+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[1+1*isize4+j*jsize4] = u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[1+2*isize4+j*jsize4] = u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[1+3*isize4+j*jsize4] = 0.0;
               fjac[1+4*isize4+j*jsize4] = 0.0;

               fjac[2+0*isize4+j*jsize4] = - ( u[2+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2]*tmp2)
                    + c2 * qs[i+j*jsize1+k*ksize1];
               fjac[2+1*isize4+j*jsize4] = - c2 *  u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+2*isize4+j*jsize4] = ( 2.0 - c2 )
                    *  u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+3*isize4+j*jsize4] = - c2 * u[3+i*isize2+j*jsize2+k*ksize2] * tmp1 ;
               fjac[2+4*isize4+j*jsize4] = c2;

               fjac[3+0*isize4+j*jsize4] = - ( u[2+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[3+1*isize4+j*jsize4] = 0.0;
               fjac[3+2*isize4+j*jsize4] = u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+3*isize4+j*jsize4] = u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+4*isize4+j*jsize4] = 0.0;

               fjac[4+0*isize4+j*jsize4] = ( c2 * 2.0 * square[i+j*jsize1+k*ksize1]
                    - c1 * u[4+i*isize2+j*jsize2+k*ksize2] )
                    * u[2+i*isize2+j*jsize2+k*ksize2] * tmp2;
               fjac[4+1*isize4+j*jsize4] = - c2 * u[1+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] 
                    * tmp2;
               fjac[4+2*isize4+j*jsize4] = c1 * u[4+i*isize2+j*jsize2+k*ksize2] * tmp1 
                    - c2 
                    * ( qs[i+j*jsize1+k*ksize1]
                    + u[2+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] * tmp2 );
               fjac[4+3*isize4+j*jsize4] = - c2 * ( u[2+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[4+4*isize4+j*jsize4] = c1 * u[2+i*isize2+j*jsize2+k*ksize2] * tmp1 ;

               njac[0+0*isize4+j*jsize4] = 0.0;
               njac[0+1*isize4+j*jsize4] = 0.0;
               njac[0+2*isize4+j*jsize4] = 0.0;
               njac[0+3*isize4+j*jsize4] = 0.0;
               njac[0+4*isize4+j*jsize4] = 0.0;

               njac[1+0*isize4+j*jsize4] = - c3c4 * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[1+1*isize4+j*jsize4] =   c3c4 * tmp1;
               njac[1+2*isize4+j*jsize4] =   0.0;
               njac[1+3*isize4+j*jsize4] =   0.0;
               njac[1+4*isize4+j*jsize4] =   0.0;

               njac[2+0*isize4+j*jsize4] = - con43 * c3c4 * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[2+1*isize4+j*jsize4] =   0.0;
               njac[2+2*isize4+j*jsize4] =   con43 * c3c4 * tmp1;
               njac[2+3*isize4+j*jsize4] =   0.0;
               njac[2+4*isize4+j*jsize4] =   0.0;

               njac[3+0*isize4+j*jsize4] = - c3c4 * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[3+1*isize4+j*jsize4] =   0.0;
               njac[3+2*isize4+j*jsize4] =   0.0;
               njac[3+3*isize4+j*jsize4] =   c3c4 * tmp1;
               njac[3+4*isize4+j*jsize4] =   0.0;

               njac[4+0*isize4+j*jsize4] = - (  c3c4
                    - c1345 ) * tmp3 * (Math.pow(u[1+i*isize2+j*jsize2+k*ksize2],2))
                    - ( con43 * c3c4
                    - c1345 ) * tmp3 * (Math.pow(u[2+i*isize2+j*jsize2+k*ksize2],2))
                    - ( c3c4 - c1345 ) * tmp3 * (Math.pow(u[3+i*isize2+j*jsize2+k*ksize2],2))
                    - c1345 * tmp2 * u[4+i*isize2+j*jsize2+k*ksize2];

               njac[4+1*isize4+j*jsize4] = (  c3c4 - c1345 ) * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[4+2*isize4+j*jsize4] = ( con43 * c3c4
                    - c1345 ) * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[4+3*isize4+j*jsize4] = ( c3c4 - c1345 ) * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[4+4*isize4+j*jsize4] = ( c1345 ) * tmp1;
            }

//---------------------------------------------------------------------
//     now joacobians set, so form left hand side in y direction
//---------------------------------------------------------------------
              lhsinit(lhs, jsize);
	      for(j=1;j<=jsize-1;j++){

               tmp1 = dt * ty1;
               tmp2 = dt * ty2;
	        

               lhs[0+0*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[0+0*isize4+(j-1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(j-1)*jsize4]
                    - tmp1 * dy1 ;
               lhs[0+1*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[0+1*isize4+(j-1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(j-1)*jsize4];
               lhs[0+2*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[0+2*isize4+(j-1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(j-1)*jsize4];
               lhs[0+3*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[0+3*isize4+(j-1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(j-1)*jsize4];
               lhs[0+4*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[0+4*isize4+(j-1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(j-1)*jsize4];
	       
               lhs[1+0*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[1+0*isize4+(j-1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(j-1)*jsize4];
               lhs[1+1*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[1+1*isize4+(j-1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(j-1)*jsize4]
                    - tmp1 * dy2;
               lhs[1+2*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[1+2*isize4+(j-1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(j-1)*jsize4];
               lhs[1+3*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[1+3*isize4+(j-1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(j-1)*jsize4];
               lhs[1+4*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[1+4*isize4+(j-1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(j-1)*jsize4];
	        

               lhs[2+0*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[2+0*isize4+(j-1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(j-1)*jsize4];
               lhs[2+1*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[2+1*isize4+(j-1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(j-1)*jsize4];
               lhs[2+2*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[2+2*isize4+(j-1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(j-1)*jsize4]
                    - tmp1 * dy3 ;
               lhs[2+3*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[2+3*isize4+(j-1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(j-1)*jsize4];
               lhs[2+4*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[2+4*isize4+(j-1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(j-1)*jsize4];
	       
	       
               lhs[3+0*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[3+0*isize4+(j-1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(j-1)*jsize4];
               lhs[3+1*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[3+1*isize4+(j-1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(j-1)*jsize4];
               lhs[3+2*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[3+2*isize4+(j-1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(j-1)*jsize4];
               lhs[3+3*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[3+3*isize4+(j-1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(j-1)*jsize4]
                    - tmp1 * dy4;
               lhs[3+4*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[3+4*isize4+(j-1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(j-1)*jsize4];
	        


               lhs[4+0*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[4+0*isize4+(j-1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(j-1)*jsize4];
               lhs[4+1*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[4+1*isize4+(j-1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(j-1)*jsize4];
               lhs[4+2*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[4+2*isize4+(j-1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(j-1)*jsize4];
               lhs[4+3*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[4+3*isize4+(j-1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(j-1)*jsize4];
               lhs[4+4*isize4+aa*jsize4+j*ksize4] = - tmp2 * fjac[4+4*isize4+(j-1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(j-1)*jsize4]
                    - tmp1 * dy5;

               lhs[0+0*isize4+bb*jsize4+j*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[0+0*isize4+j*jsize4]
                    + tmp1 * 2.0 * dy1;
               lhs[0+1*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[0+1*isize4+j*jsize4];
               lhs[0+2*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[0+2*isize4+j*jsize4];
               lhs[0+3*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[0+3*isize4+j*jsize4];
               lhs[0+4*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[0+4*isize4+j*jsize4];

               lhs[1+0*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[1+0*isize4+j*jsize4];
               lhs[1+1*isize4+bb*jsize4+j*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[1+1*isize4+j*jsize4]
                    + tmp1 * 2.0 * dy2;
               lhs[1+2*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[1+2*isize4+j*jsize4];
               lhs[1+3*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[1+3*isize4+j*jsize4];
               lhs[1+4*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[1+4*isize4+j*jsize4];

               lhs[2+0*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[2+0*isize4+j*jsize4];
               lhs[2+1*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[2+1*isize4+j*jsize4];
               lhs[2+2*isize4+bb*jsize4+j*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[2+2*isize4+j*jsize4]
                    + tmp1 * 2.0 * dy3;
               lhs[2+3*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[2+3*isize4+j*jsize4];
               lhs[2+4*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[2+4*isize4+j*jsize4];

               lhs[3+0*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[3+0*isize4+j*jsize4];
               lhs[3+1*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[3+1*isize4+j*jsize4];
               lhs[3+2*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[3+2*isize4+j*jsize4];
               lhs[3+3*isize4+bb*jsize4+j*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[3+3*isize4+j*jsize4]
                    + tmp1 * 2.0 * dy4;
               lhs[3+4*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[3+4*isize4+j*jsize4];

               lhs[4+0*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[4+0*isize4+j*jsize4];
               lhs[4+1*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[4+1*isize4+j*jsize4];
               lhs[4+2*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[4+2*isize4+j*jsize4];
               lhs[4+3*isize4+bb*jsize4+j*ksize4] = tmp1 * 2.0 * njac[4+3*isize4+j*jsize4];
               lhs[4+4*isize4+bb*jsize4+j*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[4+4*isize4+j*jsize4] 
                    + tmp1 * 2.0 * dy5;

               lhs[0+0*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[0+0*isize4+(j+1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(j+1)*jsize4]
                    - tmp1 * dy1;
               lhs[0+1*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[0+1*isize4+(j+1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(j+1)*jsize4];
               lhs[0+2*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[0+2*isize4+(j+1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(j+1)*jsize4];
               lhs[0+3*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[0+3*isize4+(j+1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(j+1)*jsize4];
               lhs[0+4*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[0+4*isize4+(j+1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(j+1)*jsize4];

               lhs[1+0*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[1+0*isize4+(j+1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(j+1)*jsize4];
               lhs[1+1*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[1+1*isize4+(j+1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(j+1)*jsize4]
                    - tmp1 * dy2;
               lhs[1+2*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[1+2*isize4+(j+1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(j+1)*jsize4];
               lhs[1+3*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[1+3*isize4+(j+1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(j+1)*jsize4];
               lhs[1+4*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[1+4*isize4+(j+1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(j+1)*jsize4];

               lhs[2+0*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[2+0*isize4+(j+1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(j+1)*jsize4];
               lhs[2+1*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[2+1*isize4+(j+1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(j+1)*jsize4];
               lhs[2+2*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[2+2*isize4+(j+1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(j+1)*jsize4]
                    - tmp1 * dy3;
               lhs[2+3*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[2+3*isize4+(j+1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(j+1)*jsize4];
               lhs[2+4*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[2+4*isize4+(j+1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(j+1)*jsize4];

               lhs[3+0*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[3+0*isize4+(j+1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(j+1)*jsize4];
               lhs[3+1*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[3+1*isize4+(j+1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(j+1)*jsize4];
               lhs[3+2*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[3+2*isize4+(j+1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(j+1)*jsize4];
               lhs[3+3*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[3+3*isize4+(j+1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(j+1)*jsize4]
                    - tmp1 * dy4;
               lhs[3+4*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[3+4*isize4+(j+1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(j+1)*jsize4];

               lhs[4+0*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[4+0*isize4+(j+1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(j+1)*jsize4];
               lhs[4+1*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[4+1*isize4+(j+1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(j+1)*jsize4];
               lhs[4+2*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[4+2*isize4+(j+1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(j+1)*jsize4];
               lhs[4+3*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[4+3*isize4+(j+1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(j+1)*jsize4];
               lhs[4+4*isize4+cc*jsize4+j*ksize4] =  tmp2 * fjac[4+4*isize4+(j+1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(j+1)*jsize4]
                    - tmp1 * dy5;
            }
//---------------------------------------------------------------------
//     performs guaussian elimination on this cell.
//     
//     assumes that unpacking routines for non-first cells 
//     preload C' and rhs' from previous cell.
//     
//     assumed send happens outside this routine, but that
//     c'(JMAX) and rhs'(JMAX) will be sent to next cell
//---------------------------------------------------------------------
	      
//---------------------------------------------------------------------
//     multiply c(i,0,k) by b_inverse and copy back to c
//     multiply rhs(0) by b_inverse(0) and copy to rhs
//---------------------------------------------------------------------
              binvcrhs( lhs,0+0*isize4+bb*jsize4+0*ksize4,
			   lhs,0+0*isize4+cc*jsize4+0*ksize4,
			   rhs,0+i*isize2+0*jsize2+k*ksize2 );

//---------------------------------------------------------------------
//     begin inner most do loop
//     do all the elements of the cell unless last 
//---------------------------------------------------------------------
            for(j=1;j<=jsize-1;j++){

//---------------------------------------------------------------------
//     subtract A*lhs_vector(j-1) from lhs_vector(j)
//     
//     rhs(j) = rhs(j) - A*rhs(j-1)
//---------------------------------------------------------------------
                 matvec_sub(lhs,0+0*isize4+aa*jsize4+j*ksize4,
                               rhs,0+i*isize2+(j-1)*jsize2+k*ksize2,
			       rhs,0+i*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     B(j) = B(j) - C(j-1)*A(j)
//---------------------------------------------------------------------
                 matmul_sub(lhs,0+0*isize4+aa*jsize4+j*ksize4,
			    lhs,0+0*isize4+cc*jsize4+(j-1)*ksize4,
			    lhs,0+0*isize4+bb*jsize4+j*ksize4);
		 
//---------------------------------------------------------------------
//     multiply c(i,j,k) by b_inverse and copy back to c
//     multiply rhs(i,1,k) by b_inverse(i,1,k) and copy to rhs
//---------------------------------------------------------------------
                 binvcrhs( lhs,0+0*isize4+bb*jsize4+j*ksize4,
			   lhs,0+0*isize4+cc*jsize4+j*ksize4,
			   rhs,0+i*isize2+j*jsize2+k*ksize2 );
            }

//---------------------------------------------------------------------
//     rhs(jsize) = rhs(jsize) - A*rhs(jsize-1)
//---------------------------------------------------------------------
              matvec_sub(lhs,0+0*isize4+aa*jsize4+jsize*ksize4,
			    rhs,0+i*isize2+(jsize-1)*jsize2+k*ksize2,
			    rhs,0+i*isize2+jsize*jsize2+k*ksize2);
	      
//---------------------------------------------------------------------
//     B(jsize) = B(jsize) - C(jsize-1)*A(jsize)
//       matmul_sub(aa,i,jsize,k,c,
//     $              cc,i,jsize-1,k,c,bb,i,jsize,k)
//---------------------------------------------------------------------
              matmul_sub(lhs,0+0*isize4+aa*jsize4+jsize*ksize4,
			    lhs,0+0*isize4+cc*jsize4+(jsize-1)*ksize4,
			    lhs,0+0*isize4+bb*jsize4+jsize*ksize4);

//---------------------------------------------------------------------
//     multiply rhs(jsize) by b_inverse(jsize) and copy to rhs
//---------------------------------------------------------------------
              binvrhs( lhs,0+0*isize4+bb*jsize4+jsize*ksize4,
			  rhs,0+i*isize2+jsize*jsize2+k*ksize2 );

//---------------------------------------------------------------------
//     back solve: if last cell, then generate U(jsize)=rhs(jsize)
//     else assume U(jsize) is loaded in un pack backsub_info
//     so just use it
//     after   u(jstart) will be sent to next cell
//---------------------------------------------------------------------

	    for(j=jsize-1;j>=0;j--){
               for(m=0;m<=BLOCK_SIZE-1;m++){
                  for(n=0;n<=BLOCK_SIZE-1;n++){
                     rhs[m+i*isize2+j*jsize2+k*ksize2] = rhs[m+i*isize2+j*jsize2+k*ksize2] 
                          - lhs[m+n*isize4+cc*jsize4+j*ksize4]*rhs[n+i*isize2+(j+1)*jsize2+k*ksize2];
                  }
               }
            }
         }
      }
      if (timeron)   timer.stop(t_ysolve); 
  }

  public void z_solve(){
    int i, j, k, m, n, ksize;

    if (timeron)   timer.start(t_zsolve);

//---------------------------------------------------------------------
//     This function computes the left hand side for the three z-factors   
//---------------------------------------------------------------------

      ksize = grid_points[2]-1;

//---------------------------------------------------------------------
//     Compute the indices for storing the block-diagonal matrix;
//     determine c (labeled f) and s jacobians
//---------------------------------------------------------------------
      for(j=1;j<=grid_points[1]-2;j++){
         for(i=1;i<=grid_points[0]-2;i++){
      	    for(k=0;k<=ksize;k++){
	       
               tmp1 = 1.0 / u[0+i*isize2+j*jsize2+k*ksize2];
               tmp2 = tmp1 * tmp1;
               tmp3 = tmp1 * tmp2;

               fjac[0+0*isize4+k*jsize4] = 0.0;
               fjac[0+1*isize4+k*jsize4] = 0.0;
               fjac[0+2*isize4+k*jsize4] = 0.0;
               fjac[0+3*isize4+k*jsize4] = 1.0;
               fjac[0+4*isize4+k*jsize4] = 0.0;

               fjac[1+0*isize4+k*jsize4] = - ( u[1+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] ) 
                    * tmp2 ;
               fjac[1+1*isize4+k*jsize4] = u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[1+2*isize4+k*jsize4] = 0.0;
               fjac[1+3*isize4+k*jsize4] = u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[1+4*isize4+k*jsize4] = 0.0;
	       
               fjac[2+0*isize4+k*jsize4] = - ( u[2+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2 ;
               fjac[2+1*isize4+k*jsize4] = 0.0;
               fjac[2+2*isize4+k*jsize4] = u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+3*isize4+k*jsize4] = u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+4*isize4+k*jsize4] = 0.0;

               fjac[3+0*isize4+k*jsize4] = - (u[3+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] * tmp2 ) 
                    + c2 * qs[i+j*jsize1+k*ksize1];
               fjac[3+1*isize4+k*jsize4] = - c2 *  u[1+i*isize2+j*jsize2+k*ksize2] * tmp1 ;
               fjac[3+2*isize4+k*jsize4] = - c2 *  u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+3*isize4+k*jsize4] = ( 2.0 - c2 )
                    *  u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+4*isize4+k*jsize4] = c2;
	       
	       fjac[4+0*isize4+k*jsize4] = ( c2 * 2.0 * square[i+j*jsize1+k*ksize1] 
                    - c1 * u[4+i*isize2+j*jsize2+k*ksize2] )
                    * u[3+i*isize2+j*jsize2+k*ksize2] * tmp2;
               fjac[4+1*isize4+k*jsize4] = - c2 * ( u[1+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2 ;
               fjac[4+2*isize4+k*jsize4] = - c2 * ( u[2+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[4+3*isize4+k*jsize4] = c1 * ( u[4+i*isize2+j*jsize2+k*ksize2] * tmp1 )
                    - c2
                    * ( qs[i+j*jsize1+k*ksize1]
                    + u[3+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] * tmp2 );
               fjac[4+4*isize4+k*jsize4] = c1 * u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;

               njac[0+0*isize4+k*jsize4] = 0.0;
               njac[0+1*isize4+k*jsize4] = 0.0;
               njac[0+2*isize4+k*jsize4] = 0.0;
               njac[0+3*isize4+k*jsize4] = 0.0;
               njac[0+4*isize4+k*jsize4] = 0.0;

               njac[1+0*isize4+k*jsize4] = - c3c4 * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[1+1*isize4+k*jsize4] =   c3c4 * tmp1;
               njac[1+2*isize4+k*jsize4] =   0.0;
               njac[1+3*isize4+k*jsize4] =   0.0;
               njac[1+4*isize4+k*jsize4] =   0.0;

               njac[2+0*isize4+k*jsize4] = - c3c4 * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[2+1*isize4+k*jsize4] =   0.0;
               njac[2+2*isize4+k*jsize4] =   c3c4 * tmp1;
               njac[2+3*isize4+k*jsize4] =   0.0;
               njac[2+4*isize4+k*jsize4] =   0.0;

               njac[3+0*isize4+k*jsize4] = - con43 * c3c4 * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[3+1*isize4+k*jsize4] =   0.0;
               njac[3+2*isize4+k*jsize4] =   0.0;
               njac[3+3*isize4+k*jsize4] =   con43 * c3 * c4 * tmp1;
               njac[3+4*isize4+k*jsize4] =   0.0;
	
               njac[4+0*isize4+k*jsize4] = - (  c3c4
                    - c1345 ) * tmp3 * (Math.pow(u[1+i*isize2+j*jsize2+k*ksize2],2))
                    - ( c3c4 - c1345 ) * tmp3 * (Math.pow(u[2+i*isize2+j*jsize2+k*ksize2],2))
                    - ( con43 * c3c4
                    - c1345 ) * tmp3 * (Math.pow(u[3+i*isize2+j*jsize2+k*ksize2],2))
                    - c1345 * tmp2 * u[4+i*isize2+j*jsize2+k*ksize2];

               njac[4+1*isize4+k*jsize4] = (  c3c4 - c1345 ) * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[4+2*isize4+k*jsize4] = (  c3c4 - c1345 ) * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[4+3*isize4+k*jsize4] = ( con43 * c3c4
                    - c1345 ) * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[4+4*isize4+k*jsize4] = ( c1345 )* tmp1;
            }

//---------------------------------------------------------------------
//     now jacobians set, so form left hand side in z direction
//---------------------------------------------------------------------
            lhsinit(lhs, ksize);
      	    for(k=1;k<=ksize-1;k++){
	      
               tmp1 = dt * tz1;
               tmp2 = dt * tz2;
               
	       lhs[0+0*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[0+0*isize4+(k-1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(k-1)*jsize4]
                    - tmp1 * dz1 ;
               lhs[0+1*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[0+1*isize4+(k-1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(k-1)*jsize4];
               lhs[0+2*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[0+2*isize4+(k-1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(k-1)*jsize4];
               lhs[0+3*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[0+3*isize4+(k-1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(k-1)*jsize4];
               lhs[0+4*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[0+4*isize4+(k-1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(k-1)*jsize4];
		    
               lhs[1+0*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[1+0*isize4+(k-1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(k-1)*jsize4];
               lhs[1+1*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[1+1*isize4+(k-1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(k-1)*jsize4]
                    - tmp1 * dz2;
               lhs[1+2*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[1+2*isize4+(k-1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(k-1)*jsize4];
               lhs[1+3*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[1+3*isize4+(k-1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(k-1)*jsize4];
               lhs[1+4*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[1+4*isize4+(k-1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(k-1)*jsize4];

               lhs[2+0*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[2+0*isize4+(k-1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(k-1)*jsize4];
               lhs[2+1*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[2+1*isize4+(k-1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(k-1)*jsize4];
               lhs[2+2*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[2+2*isize4+(k-1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(k-1)*jsize4]
                    - tmp1 * dz3;
               lhs[2+3*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[2+3*isize4+(k-1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(k-1)*jsize4];
               lhs[2+4*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[2+4*isize4+(k-1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(k-1)*jsize4];

               lhs[3+0*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[3+0*isize4+(k-1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(k-1)*jsize4];
               lhs[3+1*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[3+1*isize4+(k-1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(k-1)*jsize4];
               lhs[3+2*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[3+2*isize4+(k-1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(k-1)*jsize4];
               lhs[3+3*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[3+3*isize4+(k-1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(k-1)*jsize4]
                    - tmp1 * dz4;
               lhs[3+4*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[3+4*isize4+(k-1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(k-1)*jsize4];

               lhs[4+0*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[4+0*isize4+(k-1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(k-1)*jsize4];
               lhs[4+1*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[4+1*isize4+(k-1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(k-1)*jsize4];
               lhs[4+2*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[4+2*isize4+(k-1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(k-1)*jsize4];
               lhs[4+3*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[4+3*isize4+(k-1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(k-1)*jsize4];
               lhs[4+4*isize4+aa*jsize4+k*ksize4] = - tmp2 * fjac[4+4*isize4+(k-1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(k-1)*jsize4]
                    - tmp1 * dz5;

               lhs[0+0*isize4+bb*jsize4+k*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[0+0*isize4+k*jsize4]
                    + tmp1 * 2.0 * dz1;
               lhs[0+1*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[0+1*isize4+k*jsize4];
               lhs[0+2*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[0+2*isize4+k*jsize4];
               lhs[0+3*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[0+3*isize4+k*jsize4];
               lhs[0+4*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[0+4*isize4+k*jsize4];

               lhs[1+0*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[1+0*isize4+k*jsize4];
               lhs[1+1*isize4+bb*jsize4+k*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[1+1*isize4+k*jsize4]
                    + tmp1 * 2.0 * dz2;
               lhs[1+2*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[1+2*isize4+k*jsize4];
               lhs[1+3*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[1+3*isize4+k*jsize4];
               lhs[1+4*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[1+4*isize4+k*jsize4];

               lhs[2+0*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[2+0*isize4+k*jsize4];
               lhs[2+1*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[2+1*isize4+k*jsize4];
               lhs[2+2*isize4+bb*jsize4+k*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[2+2*isize4+k*jsize4]
                    + tmp1 * 2.0 * dz3;
               lhs[2+3*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[2+3*isize4+k*jsize4];
               lhs[2+4*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[2+4*isize4+k*jsize4];

               lhs[3+0*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[3+0*isize4+k*jsize4];
               lhs[3+1*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[3+1*isize4+k*jsize4];
               lhs[3+2*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[3+2*isize4+k*jsize4];
               lhs[3+3*isize4+bb*jsize4+k*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[3+3*isize4+k*jsize4]
                    + tmp1 * 2.0 * dz4;
               lhs[3+4*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[3+4*isize4+k*jsize4];

               lhs[4+0*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[4+0*isize4+k*jsize4];
               lhs[4+1*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[4+1*isize4+k*jsize4];
               lhs[4+2*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[4+2*isize4+k*jsize4];
               lhs[4+3*isize4+bb*jsize4+k*ksize4] = tmp1 * 2.0 * njac[4+3*isize4+k*jsize4];
               lhs[4+4*isize4+bb*jsize4+k*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[4+4*isize4+k*jsize4] 
                    + tmp1 * 2.0 * dz5;

               lhs[0+0*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[0+0*isize4+(k+1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(k+1)*jsize4]
                    - tmp1 * dz1;
               lhs[0+1*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[0+1*isize4+(k+1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(k+1)*jsize4];
               lhs[0+2*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[0+2*isize4+(k+1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(k+1)*jsize4];
               lhs[0+3*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[0+3*isize4+(k+1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(k+1)*jsize4];
               lhs[0+4*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[0+4*isize4+(k+1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(k+1)*jsize4];

               lhs[1+0*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[1+0*isize4+(k+1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(k+1)*jsize4];
               lhs[1+1*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[1+1*isize4+(k+1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(k+1)*jsize4]
                    - tmp1 * dz2;
               lhs[1+2*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[1+2*isize4+(k+1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(k+1)*jsize4];
               lhs[1+3*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[1+3*isize4+(k+1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(k+1)*jsize4];
               lhs[1+4*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[1+4*isize4+(k+1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(k+1)*jsize4];

               lhs[2+0*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[2+0*isize4+(k+1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(k+1)*jsize4];
               lhs[2+1*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[2+1*isize4+(k+1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(k+1)*jsize4];
               lhs[2+2*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[2+2*isize4+(k+1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(k+1)*jsize4]
                    - tmp1 * dz3;
               lhs[2+3*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[2+3*isize4+(k+1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(k+1)*jsize4];
               lhs[2+4*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[2+4*isize4+(k+1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(k+1)*jsize4];

               lhs[3+0*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[3+0*isize4+(k+1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(k+1)*jsize4];
               lhs[3+1*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[3+1*isize4+(k+1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(k+1)*jsize4];
               lhs[3+2*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[3+2*isize4+(k+1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(k+1)*jsize4];
               lhs[3+3*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[3+3*isize4+(k+1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(k+1)*jsize4]
                    - tmp1 * dz4;
               lhs[3+4*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[3+4*isize4+(k+1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(k+1)*jsize4];

               lhs[4+0*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[4+0*isize4+(k+1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(k+1)*jsize4];
               lhs[4+1*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[4+1*isize4+(k+1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(k+1)*jsize4];
               lhs[4+2*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[4+2*isize4+(k+1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(k+1)*jsize4];
               lhs[4+3*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[4+3*isize4+(k+1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(k+1)*jsize4];
               lhs[4+4*isize4+cc*jsize4+k*ksize4] =  tmp2 * fjac[4+4*isize4+(k+1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(k+1)*jsize4]
                    - tmp1 * dz5;
            }

//---------------------------------------------------------------------
//     performs guaussian elimination on this cell.
//     
//     assumes that unpacking routines for non-first cells 
//     preload C' and rhs' from previous cell.
//     
//     assumed send happens outside this routine, but that
//     c'(KMAX) and rhs'(KMAX) will be sent to next cell.
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//     outer most do loops - sweeping in i direction
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//     multiply c(i,j,0) by b_inverse and copy back to c
//     multiply rhs(0) by b_inverse(0) and copy to rhs
//---------------------------------------------------------------------
              binvcrhs( lhs,0+0*isize4+bb*jsize4+0*ksize4,
			   lhs,0+0*isize4+cc*jsize4+0*ksize4,
			   rhs,0+i*isize2+j*jsize2+0*ksize2 );

//---------------------------------------------------------------------
//     begin inner most do loop
//     do all the elements of the cell unless last 
//---------------------------------------------------------------------
      	    for(k=1;k<=ksize-1;k++){

//---------------------------------------------------------------------
//     subtract A*lhs_vector(k-1) from lhs_vector(k)
//     
//     rhs(k) = rhs(k) - A*rhs(k-1)
//---------------------------------------------------------------------
                 matvec_sub(lhs,0+0*isize4+aa*jsize4+k*ksize4,
                               rhs,0+i*isize2+j*jsize2+(k-1)*ksize2,
			       rhs,0+i*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     B(k) = B(k) - C(k-1)*A(k)
//     call matmul_sub(aa,i,j,k,c,cc,i,j,k-1,c,bb,i,j,k)
//---------------------------------------------------------------------
                 matmul_sub(lhs,0+0*isize4+aa*jsize4+k*ksize4,
                               lhs,0+0*isize4+cc*jsize4+(k-1)*ksize4,
                               lhs,0+0*isize4+bb*jsize4+k*ksize4);

//---------------------------------------------------------------------
//     multiply c(i,j,k) by b_inverse and copy back to c
//     multiply rhs(i,j,1) by b_inverse(i,j,1) and copy to rhs
//---------------------------------------------------------------------
                 binvcrhs( lhs,0+0*isize4+bb*jsize4+k*ksize4,
                              lhs,0+0*isize4+cc*jsize4+k*ksize4,
                              rhs,0+i*isize2+j*jsize2+k*ksize2 );
            }

//---------------------------------------------------------------------
//     Now finish up special cases for last cell
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//     rhs(ksize) = rhs(ksize) - A*rhs(ksize-1)
//---------------------------------------------------------------------
              matvec_sub(lhs,0+0*isize4+aa*jsize4+ksize*ksize4,
			    rhs,0+i*isize2+j*jsize2+(ksize-1)*ksize2,
			    rhs,0+i*isize2+j*jsize2+ksize*ksize2);

//---------------------------------------------------------------------
//     B(ksize) = B(ksize) - C(ksize-1)*A(ksize)
//     call matmul_sub(aa,i,j,ksize,c,
//     $              cc,i,j,ksize-1,c,bb,i,j,ksize)
//---------------------------------------------------------------------
              matmul_sub(lhs,0+0*isize4+aa*jsize4+ksize*ksize4,
			    lhs,0+0*isize4+cc*jsize4+(ksize-1)*ksize4,
			    lhs,0+0*isize4+bb*jsize4+ksize*ksize4);

//---------------------------------------------------------------------
//     multiply rhs(ksize) by b_inverse(ksize) and copy to rhs
//---------------------------------------------------------------------
              binvrhs( lhs,0+0*isize4+bb*jsize4+ksize*ksize4,
			  rhs,0+i*isize2+j*jsize2+ksize*ksize2 );

//---------------------------------------------------------------------
//     back solve: if last cell, then generate U(ksize)=rhs(ksize)
//     else assume U(ksize) is loaded in un pack backsub_info
//     so just use it
//     after call u(kstart) will be sent to next cell
//---------------------------------------------------------------------

	     for(k=ksize-1;k>=0;k--){
               for(m=0;m<=BLOCK_SIZE-1;m++){
                  for(n=0;n<=BLOCK_SIZE-1;n++){
                     rhs[m+i*isize2+j*jsize2+k*ksize2] 
		        += -lhs[m+n*isize4+cc*jsize4+k*ksize4]
			   *rhs[n+i*isize2+j*jsize2+(k+1)*ksize2];
                  }
               }
            }
         }
      }
      if(timeron) timer.stop(t_zsolve);   
  }
  public double checkSum(double arr[]){
    double csum=0.0;
    for(int k=0;k<=grid_points[2]-1;k++){
      for(int j=0;j<=grid_points[1]-1;j++){
	for(int i=0;i<=grid_points[0]-1;i++){
	  for(int m=0;m<=4;m++){
	    int offset=m+i*isize2+j*jsize2+k*ksize2;
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
    System.out.println("BT: is about to be garbage collected");
    super.finalize();
  }
}




