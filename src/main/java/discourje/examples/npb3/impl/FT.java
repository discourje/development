/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                                  F T                                    !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    This benchmark is a serial/multithreaded version of the              !
!    NPB3_0_JAV FT code.                                                  !
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
! Authors: D. Bailey 					                  !
!	   W. Saphir							  !
! Translation to Java and MultiThreaded Code	 			  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/

package discourje.examples.npb3.impl;

import discourje.examples.npb3.impl.FTThreads.*;
import discourje.examples.npb3.impl.BMInOut.*;

import java.io.File;
import java.text.DecimalFormat;

public class FT extends FTBase {
  public int bid=-1;
  public BMResults results;
  public boolean serial=true;
  boolean done=false;
  public FT(char clss, int np , boolean ser){
    super(clss,np,ser);
    serial=ser;
  }
  public static void main(String argv[]){
    if (argv.length == 0) {
        argv = new String[] {"-serial", "CLASS=W"};
        argv = new String[] {"-np2", "CLASS=W"};
    }

    FT ft = null;

    BMArgs.ParseCmdLineArgs(argv,BMName);
    char CLSS=BMArgs.CLASS;
    int np=BMArgs.num_threads;
    boolean serial=BMArgs.serial;

    try{
      ft = new FT(CLSS,np,serial);
    }catch(OutOfMemoryError e){
      BMArgs.outOfMemoryMessage();
      System.exit(0);
    }
    ft.runBenchMark();
  }

  public void run(){runBenchMark();}

  public void runBenchMark(){
    BMArgs.Banner(BMName,CLASS,serial,num_threads);
    System.out.println( " Size = " + nx+" X " + ny+" X " + nz
                       +" niter = "+niter_default);
    setTimers();
    timer.resetAllTimers();

    if(serial) appft_serial();
    else appft();

    if(timeron) timer.start(14);
    int verified=verify(4, nx, ny, nz, niter_default, checksum);
    if(timeron) timer.stop(14);
    timer.stop(1);

    double time=timer.readTimer(1);
    results=new BMResults(BMName,
    			  CLASS,
    			  nx,
    			  ny,
    			  nz,
    			  niter_default,
    			  time,
    			  getMFLOPS(time,nx,ny,nz),
    			  "floating point",
    			  verified,
    			  serial,
    			  num_threads,
    			  bid);
    results.print();
    if(timeron) printTimers();
    done = true;

    for (int m = 0; m < num_threads; m++) {
        doFFT[m].in.send(new ExitMessage());
        doFFT[m].out.receive();
        while (true) {
            try {
                doFFT[m].join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }
    for (int m = 0; m < num_threads; m++) {
        doEvolve[m].in.send(new ExitMessage());
        doEvolve[m].out.receive();
        while (true) {
            try {
                doEvolve[m].join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }
    for (int m = 0; m < num_threads; m++) {
        doFFT[m].in.close();
        doFFT[m].out.close();
        doEvolve[m].in.close();
        doEvolve[m].out.close();
    }
  }

  public void appft_serial(){
    if(timeron) timer.start(2);
    initial_conditions(xtr,ny,nx,nz);
    CompExp( nx, exp1 );
    CompExp( ny, exp2 );
    CompExp( nz, exp3 ) ;
    fftXYZ(1, xtr, exp2, exp1, exp3,ny,nx,nz);
    if(timeron) timer.stop(2);

    timer.start(1);
    if(timeron) timer.start(12);
    initial_conditions(xtr,ny,nx,nz);
    if(timeron) timer.stop(12);
    if(timeron) timer.start(15);
    fftXYZ(1,xtr,exp2,exp1,exp3,ny,nx,nz);
    if(timeron) timer.stop(15);

    double ap =  (- 4.0 * alpha * Math.pow(pi,2) );
    int n12 = nx/2;
    int n22 = ny/2;
    int n32 = nz/2;

    for(int it=0;it<niter_default;it++){
      if(timeron) timer.start(11);

      for(int i=0;i<nx;i++){
    	int ii = i-((i)/n12)*nx;
    	int ii2 = ii*ii;
    	for(int k=0;k<nz;k++){
    	  int kk = k-((k)/n32)*nz;
    	  int ik2 = ii2 + kk*kk;
    	  for(int j=0;j<ny;j++){
    	    int jj = j-((j)/n22)*ny;
    	    xnt[REAL+j*isize4+k*jsize4+i*ksize4] =
    		     xtr[REAL+j*isize3+i*jsize3+k*ksize3]*
    		     Math.exp((ap*(jj*jj + ik2))*(it+1));
    	    xnt[IMAG+j*isize4+k*jsize4+i*ksize4] =
    		     xtr[IMAG+j*isize3+i*jsize3+k*ksize3]*
    		     Math.exp((ap*(jj*jj + ik2))*(it+1));
    	  }
    	}
      }
      if(timeron) timer.stop(11);

      if(timeron) timer.start(15);
      fftXYZ(-1,xnt,exp2,exp3,exp1,ny,nz,nx);
      if(timeron) timer.stop(15);

      if(timeron) timer.start(10);
      CalculateChecksum(checksum, REAL+it*isize2, it, xnt, ny, nz, nx);
      if(timeron) timer.stop(10);
    }
  }

  public void appft(){

    if(timeron) timer.start(2);
    initial_conditions(xtr,ny,nx,nz);
    CompExp( nx, exp1 );
    CompExp( ny, exp2 );
    CompExp( nz, exp3 );

    setupThreads(this);
//    for(int m=0;m<num_threads;m++)
//      synchronized(doFFT[m]){
//        doFFT[m].setVariables(1,false,xtr,exp2,exp1,exp3);
//    }
    for (int m = 0; m < num_threads; m++) {
        doFFT[m].in.send(new FFTSetVariablesMessage(1, false, xtr, exp2, exp1, exp3));
    }
    for (int m = 0; m < num_threads; m++) {
        doFFT[m].out.receive();
    }

    doFFT();
    doFFT();
    doFFT();
    if(timeron) timer.stop(2);

    timer.start(1);
    if(timeron) timer.start(12);
    initial_conditions(xtr,ny,nx,nz);
    if(timeron) timer.stop(12);

    if(timeron) timer.start(15);
//    for(int m=0;m<num_threads;m++)
//      synchronized(doFFT[m]){
//        doFFT[m].setVariables(1,false,xtr,exp2,exp1,exp3);
//    }
    for (int m = 0; m < num_threads; m++) {
        doFFT[m].in.send(new FFTSetVariablesMessage(1, false, xtr, exp2, exp1, exp3));
    }
    for (int m = 0; m < num_threads; m++) {
        doFFT[m].out.receive();
    }

    doFFT();
    doFFT();
    doFFT();
    if(timeron) timer.stop(15);

    for(int it=0;it<niter_default;it++){
      if(timeron) timer.start(11);
      doEvolve(it);
      if(timeron) timer.stop(11);

      if(timeron) timer.start(15);
//      for(int m=0;m<num_threads;m++)
//    	synchronized(doFFT[m]){
//          doFFT[m].setVariables(-1,true,xnt,exp2,exp3,exp1);
//      }
      for (int m = 0; m < num_threads; m++) {
          doFFT[m].in.send(new FFTSetVariablesMessage(-1, true, xnt, exp2, exp3, exp1));
      }
      for (int m = 0; m < num_threads; m++) {
          doFFT[m].out.receive();
      }

      if(timeron) timer.start(3);
      if(timeron) timer.start(7);
      doFFT();
      if(timeron) timer.stop(7);

      if(timeron) timer.start(8);
      doFFT();
      if(timeron) timer.stop(8);

      if(timeron) timer.start(9);
      doFFT();
      if(timeron) timer.stop(9);
      if(timeron) timer.stop(3);
      if(timeron) timer.stop(15);

      if(timeron) timer.start(10);
      CalculateChecksum(checksum, REAL+it*isize2, it, xnt, ny, nz, nx);
      if(timeron) timer.stop(10);
    }
  }

  public void doFFT(){
//  public synchronized void doFFT(){
    int m;
//    for(m=0;m<num_threads;m++)
//      synchronized(doFFT[m]){
//        doFFT[m].done=false;
//        doFFT[m].notify();
//      }
    for (m = 0; m < num_threads; m++) {
        doFFT[m].in.send(new FFTMessage());
    }
//      for(m=0;m<num_threads;m++)
//	while(!doFFT[m].done){
//	  try{wait();}catch(InterruptedException e){}
//	  notifyAll();
//	}
    for (m = 0; m < num_threads; m++) {
        doFFT[m].out.receive();
    }
  }

  public void doEvolve(int it){
//  public synchronized void doEvolve(int it){
    int m;
//    for(m=0;m<num_threads;m++)
//      synchronized(doEvolve[m]){
//        doEvolve[m].done=false;
//	doEvolve[m].kt=it;
//        doEvolve[m].notify();
//      }
    for (m = 0; m < num_threads; m++) {
        doEvolve[m].in.send(new EvolveMessage(it));
    }
//    for(m=0;m<num_threads;m++)
//      while(!doEvolve[m].done){
//        try{wait();}catch(InterruptedException e){}
//	notifyAll();
//      }
    for (m = 0; m < num_threads; m++) {
        doEvolve[m].out.receive();
    }
  }
  public void setTimers(){
    File f1 = new File("timer.flag");
    timeron = false;
    if( f1.exists() ) timeron = true;
  }
  public void printTimers(){
    DecimalFormat fmt = new DecimalFormat("0.000");
    System.out.println("  SECTION   Time (secs)");
    System.out.println("FT time =		      "+ fmt.format(timer.readTimer(1)));
    System.out.println("WarmUp time =		      "+ fmt.format(timer.readTimer(2)));
    System.out.println("ffXYZ body time =	      "+ fmt.format(timer.readTimer(3)));
    System.out.println("Swarztrauber body time =      "+ fmt.format(timer.readTimer(4)));
    System.out.println("Redistribution time =	      "+ fmt.format(timer.readTimer(5)));
    System.out.println("Transposition time =	      "+ fmt.format(timer.readTimer(6)));
    System.out.println("X time =		      "+ fmt.format(timer.readTimer(7)));
    System.out.println("Y time =		      "+ fmt.format(timer.readTimer(8)));
    System.out.println("Z time =		      "+ fmt.format(timer.readTimer(9)));
    System.out.println("CalculateChecksum =	      "+ fmt.format(timer.readTimer(10)));
    System.out.println("evolve =		      "+ fmt.format(timer.readTimer(11)));
    System.out.println("compute_initial_conditions =  "+ fmt.format(timer.readTimer(12)));
    System.out.println("twiddle =		      "+ fmt.format(timer.readTimer(13)));
    System.out.println("verify =		      "+ fmt.format(timer.readTimer(14)));
    System.out.println("fftXYZ =		      "+ fmt.format(timer.readTimer(15)));
  }

  public double getMFLOPS(double total_time,int nx,int ny,int nz){
    double mflops = 0.0;
    int ntotal = nx*ny*nz;
    if( total_time > 0 ){
      mflops = 14.8157+7.19641*Math.log(ntotal)
    	      +(5.23518+7.21113*Math.log(ntotal))*niter_default;
      mflops *= ntotal/(total_time*1000000.0);
    }
    return mflops;
  }

  public void CalculateChecksum(double csum[], int csmffst,
                                int iterN,double u[],int d1,
				int d2, int d3){
    int i, ii, ji, ki;
    int isize3=2,
    	jsize3=isize3*(d1+1),
        ksize3=jsize3*d2;
    csum[REAL+csmffst] = 0.0;
    csum[IMAG+csmffst] = 0.0;

    double csumr= 0.0,csumi= 0.0;
    for(i=1;i<=1024;i++){
      ii = (1*i)%d3;
      ji = (3*i)%d1;
      ki = (5*i)%d2;
      csumr+=u[REAL+ji*isize3+ki*jsize3+ii*ksize3];
      csumi+=u[IMAG+ji*isize3+ki*jsize3+ii*ksize3];
    }
    csum[REAL+csmffst] = csumr/(d1*d2*d3);
    csum[IMAG+csmffst] = csumi/(d1*d2*d3);
//  System.out.println("==FT Checksum:"+iterN + " checksum = (" + 
//        	       csum[REAL+csmffst] + "," + csum[IMAG+csmffst] + ")" );	 
  }

  public void fftXYZ(int sign,double x[],double exp1[], double exp2[] ,
		     double exp3[] ,int n1,int n2,int n3){
    int i=0, j=0, k, log;
    int isize3=2,jsize3,ksize3;
        jsize3=isize3*(n1+1);
	ksize3=jsize3*n2;
    int isize1=2,jsize1=2*(n2+1);

    if(timeron) timer.start(3);

    log = ilog2( n2 );
    if(timeron) timer.start(7);
    for(k=0;k<n3;k++) Swarztrauber(sign,log,n1,n2,x,k*ksize3,n1,exp2,scr);
    if(timeron) timer.stop(7);

    log = ilog2( n1 );
    if(timeron) timer.start(8);
    for(k=0;k<n3;k++){
      for(j=0;j<n2;j++){
	for(i=0; i<n1;i++){
	  plane[REAL+j*isize1+i*jsize1] = x[REAL+i*isize3+j*jsize3+k*ksize3];
	  plane[IMAG+j*isize1+i*jsize1] = x[IMAG+i*isize3+j*jsize3+k*ksize3];
	}
      }
      Swarztrauber(sign,log,n2,n1,plane,0,n2,exp1,scr);
      for(j=0;j<n2;j++){
	for(i=0;i<n1;i++){
	  x[REAL+i*isize3+j*jsize3+k*ksize3]=plane[REAL+j*isize1+i*jsize1];
	  x[IMAG+i*isize3+j*jsize3+k*ksize3]=plane[IMAG+j*isize1+i*jsize1];
	}
      }
    }
    if(timeron) timer.stop(8);

    log = ilog2(n3);
    if(timeron) timer.start(9);
    jsize1=2*(n1+1);
    for(k=0;k<n2;k++) {
      for(i=0; i<n3;i++){
        for(j=0;j<n1;j++){
	  plane[REAL+j*isize1+i*jsize1] = x[REAL+j*isize3+k*jsize3+i*ksize3];
	  plane[IMAG+j*isize1+i*jsize1] = x[IMAG+j*isize3+k*jsize3+i*ksize3];
	}
      }
      Swarztrauber(sign,log,n1,n3,plane,0,n1,exp3,scr);
      for(i=0; i<n3;i++){
        for(j=0;j<n1;j++){
	  x[REAL+j*isize3+k*jsize3+i*ksize3]=plane[REAL+j*isize1+i*jsize1];
	  x[IMAG+j*isize3+k*jsize3+i*ksize3]=plane[IMAG+j*isize1+i*jsize1];
	}
      }
    }
    if(timeron) timer.stop(9);
    if(timeron) timer.stop(3);
  }

  public int verify(int ires,int n1,int n2,int n3,int nt, double cksum[]){
    int verified=-1;
    boolean temp[] = new boolean[niter_default];
    double cexpd[] = new double[2*21];
    if( (n1==64)&&(n2==64)&&(n3==64)&&(nt==6) ){
//
// Class S reference values.
//
            cexpd[REAL+0*2] = 554.6087004964;
            cexpd[REAL+1*2] = 554.6385409189;
            cexpd[REAL+2*2] = 554.6148406171;
            cexpd[REAL+3*2] = 554.5423607415;
            cexpd[REAL+4*2] = 554.4255039624;
            cexpd[REAL+5*2] = 554.2683411902;

            cexpd[IMAG+0*2] = 484.5363331978;
            cexpd[IMAG+1*2] = 486.5304269511;
            cexpd[IMAG+2*2] = 488.3910722336;
            cexpd[IMAG+3*2] = 490.1273169046;
            cexpd[IMAG+4*2] = 491.7475857993;
            cexpd[IMAG+5*2] = 493.2597244941;

         }else if ((n1 == 128) && (n2 == 128) &&
                  (n3 == 32) && (nt == 6)) {
//
// Class W reference values.
//
            cexpd[REAL+0*2] = 567.3612178944;
            cexpd[REAL+1*2] = 563.1436885271;
            cexpd[REAL+2*2] = 559.4024089970;
            cexpd[REAL+3*2] = 556.0698047020;
            cexpd[REAL+4*2] = 553.0898991250;
            cexpd[REAL+5*2] = 550.4159734538;

            cexpd[IMAG+0*2] = 529.3246849175;
            cexpd[IMAG+1*2] = 528.2149986629;
            cexpd[IMAG+2*2] = 527.0996558037;
            cexpd[IMAG+3*2] = 526.0027904925;
            cexpd[IMAG+4*2] = 524.9400845633;
            cexpd[IMAG+5*2] = 523.9212247086;
//
         }else if( (n1 == 256) && (n2 == 256) &&
                   (n3 == 128) && (nt == 6)) {
//
// Class A reference values.
//
            cexpd[REAL+0*2] = 504.6735008193;
            cexpd[REAL+1*2] = 505.9412319734;
            cexpd[REAL+2*2] = 506.9376896287;
            cexpd[REAL+3*2] = 507.7892868474;
            cexpd[REAL+4*2] = 508.5233095391;
            cexpd[REAL+5*2] = 509.1487099959;

            cexpd[IMAG+0*2] = 511.4047905510;
            cexpd[IMAG+1*2] = 509.8809666433;
            cexpd[IMAG+2*2] = 509.8144042213;
            cexpd[IMAG+3*2] = 510.1336130759;
            cexpd[IMAG+4*2] = 510.4914655194;
            cexpd[IMAG+5*2] = 510.7917842803;
//
         }else if ((n1 == 512) && (n2 == 256) &&
                  (n3 == 256) && (nt == 20)) {
//
// Class B reference values.
//
            cexpd[REAL+0*2]  = 517.7643571579;
            cexpd[REAL+1*2]  = 515.4521291263;
            cexpd[REAL+2*2]  = 514.6409228649;
            cexpd[REAL+3*2]  = 514.2378756213;
            cexpd[REAL+4*2]  = 513.9626667737;
            cexpd[REAL+5*2]  = 513.7423460082;
            cexpd[REAL+6*2]  = 513.5547056878;
            cexpd[REAL+7*2]  = 513.3910925466;
            cexpd[REAL+8*2]  = 513.2470705390;
            cexpd[REAL+9*2]  = 513.1197729984;
            cexpd[REAL+10*2] = 513.0070319283;
            cexpd[REAL+11*2] = 512.9070537032;
            cexpd[REAL+12*2] = 512.8182883502;
            cexpd[REAL+13*2] = 512.7393733383;
            cexpd[REAL+14*2] = 512.6691062020;
            cexpd[REAL+15*2] = 512.6064276004;
            cexpd[REAL+16*2] = 512.5504076570;
            cexpd[REAL+17*2] = 512.5002331720;
            cexpd[REAL+18*2] = 512.4551951846;
            cexpd[REAL+19*2] = 512.4146770029;

            cexpd[IMAG+0*2]  = 507.7803458597;
            cexpd[IMAG+1*2]  = 508.8249431599;
            cexpd[IMAG+2*2]  = 509.6208912659;
            cexpd[IMAG+3*2]  = 510.1023387619;
            cexpd[IMAG+4*2]  = 510.3976610617;
            cexpd[IMAG+5*2]  = 510.5948019802;
            cexpd[IMAG+6*2]  = 510.7404165783;
            cexpd[IMAG+7*2]  = 510.8576573661;
            cexpd[IMAG+8*2]  = 510.9577278523;
            cexpd[IMAG+9*2]  = 511.0460304483;
            cexpd[IMAG+10*2] = 511.1252433800;
            cexpd[IMAG+11*2] = 511.1968077718;
            cexpd[IMAG+12*2] = 511.2616233064;
            cexpd[IMAG+13*2] = 511.3203605551;
            cexpd[IMAG+14*2] = 511.3735928093;
            cexpd[IMAG+15*2] = 511.4218460548;
            cexpd[IMAG+16*2] = 511.4656139760;
            cexpd[IMAG+17*2] = 511.5053595966;
            cexpd[IMAG+18*2] = 511.5415130407;
            cexpd[IMAG+19*2] = 511.5744692211;
//
         }else if ((n1 == 512) && (n2 == 512) &&
                   (n3 == 512) && (nt == 20)) {
//
// Class C reference values.
//
            cexpd[REAL+0*2]  = 519.5078707457;
            cexpd[REAL+1*2]  = 515.5422171134;
            cexpd[REAL+2*2]  = 514.4678022222;
            cexpd[REAL+3*2]  = 514.0150594328;
            cexpd[REAL+4*2]  = 513.7550426810;
            cexpd[REAL+5*2]  = 513.5811056728;
            cexpd[REAL+6*2]  = 513.4569343165;
            cexpd[REAL+7*2]  = 513.3651975661;
            cexpd[REAL+8*2]  = 513.2955192805;
            cexpd[REAL+9*2]  = 513.2410471738;
            cexpd[REAL+10*2] = 513.1971141679;
            cexpd[REAL+11*2] = 513.1605205716;
            cexpd[REAL+12*2] = 513.1290734194;
            cexpd[REAL+13*2] = 513.1012720314;
            cexpd[REAL+14*2] = 513.0760908195;
            cexpd[REAL+15*2] = 513.0528295923;
            cexpd[REAL+16*2] = 513.0310107773;
            cexpd[REAL+17*2] = 513.0103090133;
            cexpd[REAL+18*2] = 512.9905029333;
            cexpd[REAL+19*2] = 512.9714421109;

            cexpd[IMAG+0*2]  = 514.9019699238;
            cexpd[IMAG+1*2]  = 512.7578201997;
            cexpd[IMAG+2*2]  = 512.2251847514;
            cexpd[IMAG+3*2]  = 512.1090289018;
            cexpd[IMAG+4*2]  = 512.1143685824;
            cexpd[IMAG+5*2]  = 512.1496764568;
            cexpd[IMAG+6*2]  = 512.1870921893;
            cexpd[IMAG+7*2]  = 512.2193250322;
            cexpd[IMAG+8*2]  = 512.2454735794;
            cexpd[IMAG+9*2]  = 512.2663649603;
            cexpd[IMAG+10*2] = 512.2830879827;
            cexpd[IMAG+11*2] = 512.2965869718;
            cexpd[IMAG+12*2] = 512.3075927445;
            cexpd[IMAG+13*2] = 512.3166486553;
            cexpd[IMAG+14*2] = 512.3241541685;
            cexpd[IMAG+15*2] = 512.3304037599;
            cexpd[IMAG+16*2] = 512.3356167976;
            cexpd[IMAG+17*2] = 512.3399592211;
            cexpd[IMAG+18*2] = 512.3435588985;
            cexpd[IMAG+19*2] = 512.3465164008;
         }
     double epsilon = 1.0E-12;
//
// Verification test for results.
//
     if(nt<=0){

     }else{
       for(int it=0;it<nt;it++){
     	 double csumr=(cksum[REAL + it*2]-cexpd[REAL +it*2])/cexpd[REAL +it*2];
     	 double csumi=(cksum[IMAG + it*2]-cexpd[IMAG +it*2])/cexpd[IMAG +it*2];
     	 if (	 Math.abs(csumr) <= epsilon
	      || Math.abs(csumi) <= epsilon
	    ){
     	     if(verified==-1) verified = 1;
    	 }else{
	   verified = 0;
 	 }
       }
     }
     BMResults.printVerificationStatus(CLASS,verified,BMName);
     return verified;
  }

  public double getTime(){return timer.readTimer(1);}
  public boolean isDone(){return done;}
  public void finalize() throws Throwable{
    System.out.println("FT: is about to be garbage collected");
    super.finalize();
  }
}


