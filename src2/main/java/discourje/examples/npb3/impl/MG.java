/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                                  MG                                     !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    This benchmark is a serial/multithreaded version of the              !
!    NPB3_0_JAV MG code.                                                  !
!                                                                         !
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
! Authors: E. Barszcz                                                     !
!          P. Frederickson					          !
!          A. Woo					                  !
!          M. Yarrow					                  !
! Translation to Java and MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl;

import discourje.examples.npb3.impl.MGThreads.*;
import discourje.examples.npb3.impl.BMInOut.*;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

public class MG extends MGBase {
  public int bid=-1;
  public BMResults results;
  public boolean serial=false;
  public boolean timeron=false;
  public double rnm2, rnmu, epsilon;
  public int n1, n2, n3, nn;
  int verified;

  String t_names[];
  int is1, is2, is3, ie1, ie2, ie3;
  int nsizes[];

  public MG(char clss,int np,boolean ser ){
    super(clss,np,ser);
    serial=ser;
  }
  public static void main(String argv[] ){
    if (argv.length == 0) {
        argv = new String[]{"-serial", "CLASS=W"};
        argv = new String[]{"-np2", "CLASS=W"};
    }
    MG mg=null;

    BMArgs.ParseCmdLineArgs(argv,BMName);
    char CLSS=BMArgs.CLASS;
    int np=BMArgs.num_threads;
    boolean serial=BMArgs.serial;

    try{ 
      mg = new MG(CLSS,np,serial);
    }catch(OutOfMemoryError e){
      BMArgs.outOfMemoryMessage();
      System.exit(0);
    }
    mg.runBenchMark();
  }
   
  public void run(){runBenchMark();}
    
  public void runBenchMark(){
    BMArgs.Banner(BMName,CLASS,serial,num_threads);

    int niter=getInputPars();
    
    nsizes=new int[3];
    setup(nsizes);
    n1=nsizes[0];
    n2=nsizes[1];
    n3=nsizes[2];
     
    setTimers();
    timer.resetAllTimers();
    timer.start(T_init);

    zero3(u,0,n1,n2,n3);
    zran3(v,n1,n2,n3,nx[lt-1],ny[lt-1]);

    if(!serial) setupThreads(this);
    if(serial) resid(u,v,r,0,n1,n2,n3);
    else residMaster(u,v,r,0,n1,n2,n3);
//--------------------------------------------------------------------
//    One iteration for startup
//--------------------------------------------------------------------
    if(serial){
      mg3P(u,v,r,n1,n2,n3);
      resid(u,v,r,0,n1,n2,n3);
    }else{
      mg3Pmaster(u,v,r,n1,n2,n3);
      residMaster(u,v,r,0,n1,n2,n3);	 
    }

    zero3(u,0,n1,n2,n3);
    zran3(v,n1,n2,n3,nx[lt-1],ny[lt-1]);

    timer.stop(T_init);
    timer.start(T_bench);     
 
    if (timeron) timer.start(T_resid2);
    if(serial) resid(u,v,r,0,n1,n2,n3);
    else residMaster(u,v,r,0,n1,n2,n3);
    if (timeron) timer.stop(T_resid2);
    for(int it=1;it<=nit;it++){
       if (timeron) timer.start(T_mg3P);
       if(serial) mg3P(u,v,r,n1,n2,n3);
       else mg3Pmaster(u,v,r,n1,n2,n3);
       if (timeron) timer.stop(T_mg3P);
       
       if (timeron) timer.start(T_resid2);
       if(serial) resid(u,v,r,0,n1,n2,n3);
       else residMaster(u,v,r,0,n1,n2,n3);
       if (timeron) timer.stop(T_resid2);
    }
    timer.stop(T_bench);
    
    double tinit = timer.readTimer(T_init);
    System.out.println(" Initialization time: "+tinit+" seconds");
    rnm2=norm2u3(r,n1,n2,n3,rnmu,nx[lt-1],ny[lt-1],nz[lt-1]);
    verified=verify(rnm2);
    double tm = timer.readTimer(T_bench);
    results=new BMResults("MG",
  			  CLASS,
  			  nx[lt-1],
  			  ny[lt-1],
  			  nz[lt-1],
  			  nit,
  			  tm,
  			  getMFLOPS(tm,nit),
  			  "floating point",
  			  verified,
  			  serial,
  			  num_threads,
  			  bid);
    results.print();			      
    if (timeron) printTimers();

    for (int m = 0; m < num_threads; m++) {
        interp[m].in.send(new ExitMessage());
        interp[m].out.receive();
        while (true) {
            try {
                interp[m].join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }
    for (int m = 0; m < num_threads; m++) {
        psinv[m].in.send(new ExitMessage());
        psinv[m].out.receive();
        while (true) {
            try {
                psinv[m].join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }
    for (int m = 0; m < num_threads; m++) {
        rprj[m].in.send(new ExitMessage());
        rprj[m].out.receive();
        while (true) {
            try {
                rprj[m].join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }
    for (int m = 0; m < num_threads; m++) {
        resid[m].in.send(new ExitMessage());
        resid[m].out.receive();
        while (true) {
            try {
                resid[m].join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }
    for (int m = 0; m < num_threads; m++) {
        interp[m].in.close();
        interp[m].out.close();
        psinv[m].in.close();
        psinv[m].out.close();
        rprj[m].in.close();
        rprj[m].out.close();
        resid[m].in.close();
        resid[m].out.close();
    }
  }
  
  public int verify(double rnm2){
    double verify_value=0.0;
    epsilon = 1.0E-8;
    if (CLASS != 'U') {
       if(CLASS=='S') {
  	  verify_value = 0.530770700573E-4;
       }else if(CLASS=='W') {
  	  verify_value = 0.250391406439E-17; 
       }else if(CLASS=='A') {
  	  verify_value = 0.2433365309E-5;
       }else if(CLASS=='B') {
  	  verify_value = 0.180056440132E-5;
       }else if(CLASS=='C') {
  	  verify_value = 0.570674826298E-6;
       }
       System.out.println(" L2 Norm is "+rnm2);
       if( Math.abs( rnm2 - verify_value ) < epsilon ) {
  	  verified = 1;
  	  System.out.println(" Deviation is   "+(rnm2 - verify_value));
       }else{
  	  verified = 0;
  	  System.out.println(" The correct L2 Norm is "+verify_value);
       }
    }else{
       verified = -1;
    }
    BMResults.printVerificationStatus(CLASS,verified,BMName); 
    return  verified;  
  }
  public double getMFLOPS(double tm,int niter){
    double mflops = 0.0;    
    if( tm > 0.0 ) {
      mflops = 58.0*n1*n2*n3;
      mflops *= niter / (tm*1000000.0);
    }
    return mflops;
  }
  public int getInputPars(){
    int lnx=32,lny=32,lnz=32;
    File f2 = new File("mg.input");
    if ( f2.exists() ){
      System.out.println("Reading from input file mg.input");
      try{  
        FileInputStream fis = new FileInputStream(f2);
        DataInputStream datafile = new DataInputStream(fis);
        lt = datafile.readInt();
        if(lt>maxlevel) {
          System.out.println("lt="+lt+" Maximum allowable="+maxlevel);
          System.exit(0);
        }
        lnx = datafile.readInt();
        lny = datafile.readInt();
        lnz = datafile.readInt();
        nit = datafile.readInt();
        fis.close();
      }catch(Exception e){  
        System.err.println("Error reading from file mg.input");
      }      
      if (lnx!=lny||lnx!=lnz){
  	CLASS = 'U'; 
      }else if(  lnx==32&&nit==4 ){
  	CLASS = 'S';
      }else if( lnx==64&&nit==40 ){
  	CLASS = 'W';
      }else if( lnx==256&&nit==20 ){
  	CLASS = 'B';
      }else if( lnx==512&&nit==20 ){  
  	CLASS = 'C';
      }else if( lnx==256&&nit==4 ){
  	CLASS = 'A';
      }else{
  	CLASS = 'U';
      }
    }else{
      System.out.println(" No input file mg.input, Using compiled defaults"); 
    }
    System.out.println(" Size:  "+nx[lt-1]+"x"+ny[lt-1]+"x"+nz[lt-1]
                      +" Iterations:   " + nit );
    return nit;
  } 
  public void setTimers(){
    File f1 = new File("timer.flag");
    if( f1.exists() ){
      timeron = true;
      t_names = new String[16];
      t_names[T_init] = "init";
      t_names[T_bench] = "benchmark";
      t_names[T_mg3P] = "mg3P";
      t_names[T_psinv] = "psinv";
      t_names[T_resid] = "resid";
      t_names[T_rprj3] = "rprj3";
      t_names[T_interp] = "interp";
      t_names[T_norm2] = "norm2";
    }
  }
   public void printTimers(){ //% of the ime should be fixed
    DecimalFormat fmt = new DecimalFormat("0.000");
    System.out.println("  SECTION   Time (secs)");
    double tmax = timer.readTimer(T_bench);
    if (tmax == 0.0) tmax = 1.0;
    for (int i=T_bench;i<=T_last;i++){
      double t = timer.readTimer(i);
      if (i==T_resid2) {
  	t = timer.readTimer(T_resid) - t;
  	System.out.println("	  --> total mg-resid "+fmt.format(t)+
  			   " ("+fmt.format(t*100./tmax)+"%)");
      }else{
  	System.out.println("	"+t_names[i]+"  "+fmt.format(t)+
  			   " ("+fmt.format(t*100./tmax)+"%)");
      }
    }
  }
  
  public void setup(int nsizes[]){
    int k;
    int d, i, j;

    int ax;
    int size1=3,size2=10;
    int mi[]=new int[size1*size2];
    int ng[]=new int[size1*size2];
    int s, dir;

    lb = 1;
    ng[  (lt-1)*size1]=nx[lt-1];
    ng[1+(lt-1)*size1]=ny[lt-1];
    ng[2+(lt-1)*size1]=nz[lt-1];

    for(ax=0;ax<size1;ax++)
      for(k=lt-2;k>=0;k--)
  	ng[ax+k*size1]=ng[ax+(k+1)*size1]/2;

    for(k=lt-2;k>=0;k--){
       nx[k]=ng[  k*size1];
       ny[k]=ng[1+k*size1];
       nz[k]=ng[2+k*size1];
     }

    for(k=lt-1;k>=0;k--){
      for(ax=0;ax<size1;ax++){
  	mi[ax+k*size1] = 2 + ng[ax+k*size1];
      }
      m1[k]=mi[k*size1];
      m2[k]=mi[1+k*size1];
      m3[k]=mi[2+k*size1];
    }

    k = lt-1;
    is1 = 2 + ng[k*size1] - ng[k*size1];
    ie1 = 1 + ng[k*size1];
    n1=nsizes[0] = 3 + ie1 - is1;
    is2 = 2 + ng[1+k*size1] - ng[1+k*size1];
    ie2 = 1 + ng[1+k*size1]; 
    n2=nsizes[1] = 3 + ie2 - is2;
    is3 = 2 + ng[2+k*size1] - ng[2+k*size1];
    ie3 = 1 + ng[2+k*size1];
    n3=nsizes[2] = 3 + ie3 - is3;
    
    ir[lt-1]=0;
    for(j = lt-2;j>=0;j--){
       ir[j]=ir[j+1]+m1[j+1]*m2[j+1]*m3[j+1];
    }
  }

  public void zero3(double z[],int off,int n1,int n2,int n3){
    int i1, i2, i3;

    for(i3=0;i3<n3;i3++)
       for(i2=0;i2<n2;i2++)
  	  for(i1=0;i1<n1;i1++)
  	     z[off+i1+n1*(i2+n2*i3)]=0.0;
  }
   
  public void zran3(double z[],int n1,int n2,int n3,int nx,int ny){
//c---------------------------------------------------------------------
//c     zran3  loads +1 at ten randomly chosen points,
//c     loads -1 at a different ten random points,
//c     and zero elsewhere.
//c---------------------------------------------------------------------
      int k, i0, m0, m1;

      int mm=10, i1, i2, i3, d1, e1, e2, e3;
      double xx, x0, x1, a1, a2, ai;
      double ten[]=new double[mm*2];
      double temp, best;
      int i;
      int j1[]=new int[mm*2], 
          j2[]=new int[mm*2],
          j3[]=new int[mm*2];
      int jg[]=new int[4*mm*2], 
          jg_temp[]=new int[4];

      zero3(z,0,n1,n2,n3);
      i = is1-2+nx*(is2-2+ny*(is3-2));

      d1 = ie1 - is1 + 1;
      e1 = ie1 - is1 + 2;
      e2 = ie2 - is2 + 2;
      e3 = ie3 - is3 + 2;

      double seed=314159265.0, a=Math.pow(5.0,13);
      Random rng=new Random();
      a1 = rng.power( a, nx );
      a2 = rng.power( a, nx*ny );
      ai = rng.power( a, i );
      x0 = rng.randlc( seed, ai );
      for(i3=2;i3<=e3;i3++){
        x1 = x0;
        for(i2 = 2;i2<=e2;i2++){
           xx = x1;
           rng.vranlc( d1, xx, a,z,(1+n1*(i2-1+n2*(i3-1))));
           x1 = rng.randlc( x1, a1 );
        }
        x0 = rng.randlc( x0, a2 );
      }

      for(i=0;i<mm;i++){
         ten[i+mm] = 0.0;
         j1[i+mm] = 0;
         j2[i+mm] = 0;
         j3[i+mm] = 0;
         ten[i] = 1.0;
         j1[i] = 0;
         j2[i] = 0;
         j3[i] = 0;
      }

     for(i3=1;i3<n3-1;i3++){
         for(i2=1;i2<n2-1;i2++){
            for(i1=1;i1<n1-1;i1++){
               if( z[i1+n1*(i2+n2*i3)] > ten[mm] ){
                  ten[mm] = z[i1+n1*(i2+n2*i3)]; 
                  j1[mm] = i1;
                  j2[mm] = i2;
                  j3[mm] = i3;
                  bubble( ten, j1, j2, j3, mm, 1 );
               }
               if( z[i1+n1*(i2+n2*i3)] < ten[0] ){
                  ten[0] = z[i1+n1*(i2+n2*i3)]; 
                  j1[0] = i1;
                  j2[0] = i2;
                  j3[0] = i3;
                  bubble( ten, j1, j2, j3, mm, 0 );
               }
            }
         }
      }
//c---------------------------------------------------------------------
//c     Now which of these are globally best?
//c---------------------------------------------------------------------
      i1 = mm;
      i0 = mm;
      for(i=mm-1;i>=0;i--){
         best = z[j1[i1-1+mm]+n1*(j2[i1-1+mm]+n2*(j3[i1-1+mm]))];
         if(best==z[j1[i1-1+mm]+n1*(j2[i1-1+mm]+n2*(j3[i1-1+mm]))]){
            jg[4*(i+mm)] = 0;
            jg[1+4*(i+mm)] = is1 - 2 + j1[i1-1+mm]; 
            jg[2+4*(i+mm)] = is2 - 2 + j2[i1-1+mm]; 
            jg[3+4*(i+mm)] = is3 - 2 + j3[i1-1+mm]; 
            i1 = i1-1;
         }else{
            jg[4*(i+mm)] = 0;
            jg[1+4*(i+mm)] = 0; 
            jg[2+4*(i+mm)] = 0; 
            jg[3+4*(i+mm)] = 0; 
         }	 
         ten[i+mm] = best;

         best = z[j1[i0-1]+n1*(j2[i0-1]+n2*(j3[i0-1]))];
         if(best==z[j1[i0-1]+n1*(j2[i0-1]+n2*(j3[i0-1]))]){
            jg[4*i] = 0;
            jg[1+4*i] = is1 - 2 + j1[i0-1]; 
            jg[2+4*i] = is2 - 2 + j2[i0-1]; 
            jg[3+4*i] = is3 - 2 + j3[i0-1]; 
            i0 = i0-1;
         }else{
            jg[4*i] = 0;
            jg[1+4*i] = 0; 
            jg[2+4*i] = 0; 
            jg[3+4*i] = 0; 
         }
         ten[i] = best;
      }
      m1 = i1+1;
      m0 = i0+1;
      for(i3=0;i3<n3;i3++)
         for(i2=0;i2<n2;i2++)
            for(i1=0;i1<n1;i1++)
               z[i1+n1*(i2+n2*i3)] = 0.0;
      for(i=mm;i>=m0;i--)
         z[j1[i-1]+n1*(j2[i-1]+n2*(j3[i-1]))] = -1.0;
      for(i=mm;i>=m1;i--)
         z[j1[i-1+mm]+n1*(j2[i-1+mm]+n2*(j3[i-1+mm]))] = 1.0;
      comm3(z,0,n1,n2,n3);
   }
  public double norm2u3(double r[],int n1,int n2,int n3,
                     double rnmu,int nx,int ny,int nz){
//c---------------------------------------------------------------------
//c     norm2u3 evaluates approximations to the L2 norm and the
//c     uniform (or L-infinity or Chebyshev) norm, under the
//c     assumption that the boundaries are periodic or zero.  Add the
//c     boundaries in with half weight (quarter weight on the edges
//c     and eighth weight at the corners) for inhomogeneous boundaries.
//c---------------------------------------------------------------------
//      double precision r(n1,n2,n3)
      if (timeron) timer.start(T_norm2);      
      rnmu = 0.0;
      double rnm2=0.0;
      for(int i3=1;i3<n3-1;i3++)
         for(int i2=1;i2<n2-1;i2++)
            for(int i1=1;i1<n1-1;i1++){
               rnm2+=r[i1+n1*(i2+n2*i3)]*r[i1+n1*(i2+n2*i3)];
               double a=Math.abs(r[i1+n1*(i2+n2*i3)]);
               rnmu=dmax1(rnmu,a);
            }

      rnm2=Math.sqrt( rnm2 / ((double) nx*ny*nz ));
      if (timeron) timer.stop(T_norm2);
    return rnm2;
  }

  public double TestNorm(double r[],int n1,int n2,int n3){
      double rnm2=0.0;
      for(int i3=1;i3<n3-1;i3++)
         for(int i2=1;i2<n2-1;i2++)
            for(int i1=1;i1<n1-1;i1++){
               rnm2+=r[i1+n1*(i2+n2*i3)]*r[i1+n1*(i2+n2*i3)];
            }

      rnm2=Math.sqrt( rnm2 / ((double)n1*n2*n3));
System.out.println("*****TestNorm  "+rnm2);
    return rnm2;
  }

  public void bubble(double ten[],int j1[],int j2[],int j3[],int m,int ind ){
//c---------------------------------------------------------------------
//c     bubble        does a bubble sort in direction dir
//c---------------------------------------------------------------------
      double temp;
      int i, j_temp=0;

      if( ind == 1 ){
         for(i=0;i<m-1;i++){
            if( ten[i+m*ind] > ten[i+1+m*ind] ){
               temp = ten[i+1+m*ind];
               ten[i+1+m*ind] = ten[i+m*ind];
               ten[i+m*ind] = temp;

               j_temp           = j1[i+1+m*ind];
               j1[i+1+m*ind] = j1[i+m*ind];
               j1[i+m*ind] = j_temp;

               j_temp           = j2[i+1+m*ind];
               j2[i+1+m*ind] = j2[i+m*ind];
               j2[i+m*ind] = j_temp;

               j_temp           = j3[ i+1+m*ind ];
               j3[i+1+m*ind] = j3[ i+m*ind ];
               j3[i+m*ind] = j_temp;
            }else {
               return;
            }
         }
      }else{
         for(i=0;i<m-1;i++){
            if( ten[i+m*ind] < ten[i+1+m*ind] ){
               temp = ten[i+1+m*ind];
               ten[i+1+m*ind] = ten[i+m*ind];
               ten[i+m*ind] = temp;

               j_temp           = j1[i+1+m*ind];
               j1[i+1+m*ind] = j1[i+m*ind];
               j1[i+m*ind] = j_temp;

               j_temp           = j2[i+1+m*ind];
               j2[i+1+m*ind] = j2[i+m*ind];
               j2[i+m*ind] = j_temp;

               j_temp           = j3[ i+1+m*ind ];
               j3[i+1+m*ind] = j3[ i+m*ind ];
               j3[i+m*ind] = j_temp;
            }else {
               return;
            }
         }
      }
    }   

   
   public void resid(double u[],double v[],double r[],
                     int off,int n1,int n2,int n3){
//c---------------------------------------------------------------------
//c     resid computes the residual:  r = v - Au
//c
//c     This  implementation costs  15A + 4M per result, where
//c     A and M denote the costs of Addition (or Subtraction) and 
//c     Multiplication, respectively. 
//c     Presuming coefficient a(1) is zero (the NPB assumes this,
//c     but it is thus not a general case), 3A + 1M may be eliminated,
//c     resulting in 12A + 3M.
//c     Note that this vectorizes, and is also fine for cache 
//c     based machines.  
//c---------------------------------------------------------------------
      int i3, i2, i1;
      double u1[]=new double[nm+1];
      double u2[]=new double[nm+1];
      if (timeron) timer.start(T_resid);

      for(i3=1;i3<n3-1;i3++)
         for(i2=1;i2<n2-1;i2++){
            for(i1=0;i1<n1;i1++){
               u1[i1] = u[off+i1+n1*(i2-1+n3*i3)] + u[off+i1+n1*(i2+1+n3*i3)]
                      + u[off+i1+n1*(i2+n3*(i3-1))] + u[off+i1+n1*(i2+n3*(i3+1))];
               u2[i1] = u[off+i1+n1*(i2-1+n3*(i3-1))] + u[off+i1+n1*(i2+1+n3*(i3-1))]
                      + u[off+i1+n1*(i2-1+n3*(i3+1))] + u[off+i1+n1*(i2+1+n3*(i3+1))];
            }
            for(i1=1;i1<n1-1;i1++){
               r[off+i1+n1*(i2+n3*i3)] = v[off+i1+n1*(i2+n3*i3)]
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
      comm3(r,off,n1,n2,n3);
      if (timeron) timer.stop(T_resid);
  }

  public void mg3P(double u[],double v[],double r[],int n1,int n2,int n3){
//c---------------------------------------------------------------------
//c     multigrid V-cycle routine
//c---------------------------------------------------------------------
//      double precision u(nr),v(nv),r(nr)
      int j,k;

//c---------------------------------------------------------------------
//c     down cycle.
//c     restrict the residual from the find grid to the coarse
//c---------------------------------------------------------------------
      for(k=lt-1;k>=lb;k--){
         j = k-1;
         rprj3(r,ir[k],m1[k],m2[k],m3[k],ir[j],m1[j],m2[j],m3[j]);
      }
      k = lb-1;
//c---------------------------------------------------------------------
//c     compute an approximate solution on the coarsest grid
//c---------------------------------------------------------------------
      zero3(u,ir[k],m1[k],m2[k],m3[k]);
      psinv(r,ir[k],u,ir[k],m1[k],m2[k],m3[k]);
      for(k=lb;k<lt-1;k++){     
          j = k-1;
//c---------------------------------------------------------------------
//c        prolongate from level k-1  to k
//c---------------------------------------------------------------------
	 zero3(u,ir[k],m1[k],m2[k],m3[k]);
         interp(u,ir[j],m1[j],m2[j],m3[j],ir[k],m1[k],m2[k],m3[k]);
//c---------------------------------------------------------------------
//c        compute residual for level k
//c---------------------------------------------------------------------
         resid(u,r,r,ir[k],m1[k],m2[k],m3[k]);
//c---------------------------------------------------------------------
//c        apply smoother
//c---------------------------------------------------------------------
         psinv(r,ir[k],u,ir[k],m1[k],m2[k],m3[k]);
      }
      j = lt - 2;
      k = lt-1;
      interp(u,ir[j],m1[j],m2[j],m3[j],0,n1,n2,n3);
      resid(u,v,r,0,n1,n2,n3);
      psinv(r,0,u,0,n1,n2,n3);
  }

  public void mg3Pmaster(double u[],double v[],double r[],int n1,int n2,int n3){
//c---------------------------------------------------------------------
//c     multigrid V-cycle routine
//c---------------------------------------------------------------------
//      double precision u(nr),v(nv),r(nr)
      int j,k;

//c---------------------------------------------------------------------
//c     down cycle.
//c     restrict the residual from the find grid to the coarse
//c---------------------------------------------------------------------
      for(k=lt-1;k>=lb;k--){
         j = k-1;
         rprj3Master(r,ir[k],m1[k],m2[k],m3[k],ir[j],m1[j],m2[j],m3[j]);
      }
      k = lb-1;
//c---------------------------------------------------------------------
//c     compute an approximate solution on the coarsest grid
//c---------------------------------------------------------------------
      zero3(u,ir[k],m1[k],m2[k],m3[k]);
      psinvMaster(r,ir[k],u,ir[k],m1[k],m2[k],m3[k]);
      for(k=lb;k<lt-1;k++){     
          j = k-1;
//c---------------------------------------------------------------------
//c        prolongate from level k-1  to k
//c---------------------------------------------------------------------
	 zero3(u,ir[k],m1[k],m2[k],m3[k]);
         interpMaster(u,ir[j],m1[j],m2[j],m3[j],ir[k],m1[k],m2[k],m3[k]);
//c---------------------------------------------------------------------
//c        compute residual for level k
//c---------------------------------------------------------------------
         residMaster(u,r,r,ir[k],m1[k],m2[k],m3[k]);
//c---------------------------------------------------------------------
//c        apply smoother
//c---------------------------------------------------------------------
         psinvMaster(r,ir[k],u,ir[k],m1[k],m2[k],m3[k]);
      }
      j = lt - 2;
      k = lt-1;
      interpMaster(u,ir[j],m1[j],m2[j],m3[j],0,n1,n2,n3);
      residMaster(u,v,r,0,n1,n2,n3);
      psinvMaster(r,0,u,0,n1,n2,n3);
  }
 
  public void rprj3(double r[],int roff,int m1k,int m2k,int m3k,
                   int soff,int m1j,int m2j,int m3j){
//c---------------------------------------------------------------------
//c     rprj3 projects onto the next coarser grid, 
//c     using a trilinear Finite Element projection:  s = r' = P r
//c     
//c     This  implementation costs  20A + 4M per result, where
//c     A and M denote the costs of Addition and Multiplication.  
//c     Note that this vectorizes, and is also fine for cache 
//c     based machines.  
//c---------------------------------------------------------------------
//      double precision r(m1k,m2k,m3k), s(m1j,m2j,m3j)
      int j3, j2, j1, i3, i2, i1, d1, d2, d3, j;

      double x2,y2;
      double x1[] = new double[nm+1], 
             y1[] = new double[nm+1];

      if (timeron) timer.start(T_rprj3);
      if(m1k==3){
        d1 = 2;
      }else{
        d1 = 1;
      }

      if(m2k==3){
        d2 = 2;
      }else{
        d2 = 1;
      }

      if(m3k==3){
        d3 = 2;
      }else{
        d3 = 1;
      }

      for(j3=2;j3<=m3j-1;j3++){
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
              r[soff+j1-1+m1j*(j2-1+m2j*(j3-1))] =
                     0.5 * r[roff+i1+m1k*(i2+m2k*i3)]
                   + 0.25 * ( r[roff+i1-1+m1k*(i2+m2k*i3)]+r[roff+i1+1+m1k*(i2+m2k*i3)]+x2)
                   + 0.125 * ( x1[i1-1] + x1[i1+1] + y2)
                   + 0.0625 * ( y1[i1-1] + y1[i1+1] );
            }
         }
      }
      comm3(r,soff,m1j,m2j,m3j);
      if (timeron) timer.stop(T_rprj3);
  }
  
  public void interp(double u[],int zoff,int mm1,int mm2,int mm3,
                     int uoff,int n1,int n2,int n3 ){
//c---------------------------------------------------------------------
//c     interp adds the trilinear interpolation of the correction
//c     from the coarser grid to the current approximation:  u = u + Qu'
//c     
//c     Observe that this  implementation costs  16A + 4M, where
//c     A and M denote the costs of Addition and Multiplication.  
//c     Note that this vectorizes, and is also fine for cache 
//c     based machines.  Vector machines may get slightly better 
//c     performance however, with 8 separate "do i1" loops, rather than 4.
//c---------------------------------------------------------------------
//      double precision z(mm1,mm2,mm3),u(n1,n2,n3)
      int i3, i2, i1, d1, d2, d3, t1, t2, t3;

//c note that m = 1037 in globals.h but for this only need to be
//c 535 to handle up to 1024^3
//c      integer m
//c      parameter( m=535 )
      int m=535;
      double z1[]=new double[m],
             z2[]=new double[m],
	     z3[]=new double[m];
      if (timeron) timer.start(T_interp);
      if( n1 != 3 && n2 != 3 && n3 != 3 ){
         for(i3=1;i3<=mm3-1;i3++){
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
         
         for(i3=1;i3<=mm3-1;i3++){
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

         for(i3=1;i3<=mm3-1;i3++){
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
      if (timeron) timer.stop(T_interp);
  }

  public void psinv(double r[],int roff,double u[],int uoff,int n1,int n2,int n3){
//c---------------------------------------------------------------------
//c     psinv applies an approximate inverse as smoother:  u = u + Cr
//c
//c     This  implementation costs  15A + 4M per result, where
//c     A and M denote the costs of Addition and Multiplication.  
//c     Presuming coefficient c(3) is zero (the NPB assumes this,
//c     but it is thus not a general case), 2A + 1M may be eliminated,
//c     resulting in 13A + 3M.
//c     Note that this vectorizes, and is also fine for cache 
//c     based machines.  
//c---------------------------------------------------------------------
//      double precision u(n1,n2,n3),r(n1,n2,n3),c(0:3)
      int i3, i2, i1;

      double r1[]=new double[nm+1],
             r2[]=new double[nm+1];

      if (timeron) timer.start(T_psinv);
      for(i3=1;i3<n3-1;i3++){
         for(i2=1;i2<n2-1;i2++){
            for(i1=0;i1<n1;i1++){
               r1[i1] = r[roff+i1+n1*(i2-1+n2*i3)] + r[roff+i1+n1*(i2+1+n2*i3)]
                      + r[roff+i1+n1*(i2+n2*(i3-1))] + r[roff+i1+n1*(i2+n2*(i3+1))];
               r2[i1] = r[roff+i1+n1*(i2-1+n2*(i3-1))] + r[roff+i1+n1*(i2+1+n2*(i3-1))]
                      + r[roff+i1+n1*(i2-1+n2*(i3+1))] + r[roff+i1+n1*(i2+1+n2*(i3+1))];
            }
            for(i1=1;i1<n1-1;i1++){
               u[uoff+i1+n1*(i2+n2*i3)] += 
                             c[0] * r[roff+i1+n1*(i2+n2*i3)]
                           + c[1] * ( r[roff+i1-1+n1*(i2+n2*i3)] + r[roff+i1+1+n1*(i2+n2*i3)]
                                    + r1[i1] )
                           + c[2] * ( r2[i1] + r1[i1-1] + r1[i1+1] );
//c---------------------------------------------------------------------
//c  Assume c(3) = 0    (Enable line below if c(3) not= 0)
//c---------------------------------------------------------------------
//c    >                     + c(3) * ( r2(i1-1) + r2(i1+1) )
//c---------------------------------------------------------------------
            }
         }
      }

//c---------------------------------------------------------------------
//c     exchange boundary points
//c---------------------------------------------------------------------
      comm3(u,uoff,n1,n2,n3);
      if (timeron) timer.stop(T_psinv);
  }
  
   public void residMaster(double u[],double v[],double r[],
                           int off,int n1,int n2,int n3){
     if (timeron) timer.start(T_resid);
     if(num_threads==1) resid(u,v,r,off,n1,n2,n3); 
     else{       
       boolean visr=false;
       if(v==r)visr=true;
//       synchronized(this){
//         for(int l=0;l<num_threads;l++)
//           synchronized(resid[l]){
//             resid[l].done=false;
//             resid[l].visr=visr;
//             resid[l].wstart=1;
//             resid[l].wend=n3;
//
//             resid[l].n1=n1;
//             resid[l].n2=n2;
//             resid[l].n3=n3;
//             resid[l].off=off;
//
//             resid[l].notify();
//           }
//         for(int l=0;l<num_threads;l++)
//           while(!resid[l].done){
//             try{wait();}catch(InterruptedException e){}
//             notifyAll();
//           }
//       }
       for (int l = 0; l < num_threads; l++) {
           resid[l].in.send(new ResidMessage(visr, 1, n3, n1, n2, n3, off));
       }
       for (int l = 0; l < num_threads; l++) {
           resid[l].out.receive();
       }
       comm3(r,off,n1,n2,n3);
     }
     if (timeron) timer.stop(T_resid);
  }
  
  public void psinvMaster(double r[],int roffl,double u[],
                          int uoffl,int n1,int n2,int n3){
    if (timeron) timer.start(T_psinv);
    if(num_threads==1) psinv(r,roffl,u,uoffl,n1,n2,n3);
    else{	
//      synchronized(this){
//        for(int l=0;l<num_threads;l++)
//          synchronized(psinv[l]){
//            psinv[l].done=false;
//            psinv[l].wstart=1;
//            psinv[l].wend=n3;
//
//            psinv[l].n1=n1;
//            psinv[l].n2=n2;
//            psinv[l].n3=n3;
//            psinv[l].roff=roffl;
//            psinv[l].uoff=uoffl;
//
//            psinv[l].notify();
//          }
//        for(int l=0;l<num_threads;l++)
//          while(!psinv[l].done){
//            try{wait();}catch(InterruptedException e){}
//            notifyAll();
//          }
//      }
      for (int l = 0; l < num_threads; l++) {
          psinv[l].in.send(new PsinvMessage(1, n3, n1, n2, n3, roffl, uoffl));
      }
      for (int l = 0; l < num_threads; l++) {
          psinv[l].out.receive();
      }
      comm3(u,uoffl,n1,n2,n3);
    }
    if (timeron) timer.stop(T_psinv);
  }
  
  public void interpMaster(double u[],
                           int zoffl,int mm1,int mm2,int mm3,
                           int uoffl,int n1,int n2,int n3 ){
     if (timeron) timer.start(T_interp);
     if(num_threads==1) interp(u,zoffl,mm1,mm2,mm3,uoffl,n1,n2,n3);
     else{       
//	synchronized(this){
//	  for(int l=0;l<num_threads;l++)
//	    synchronized(interp[l]){
//	      interp[l].done=false;
//	      interp[l].wstart=1;
//	      interp[l].wend=mm3;
//
//	      interp[l].mm1=mm1;
//	      interp[l].mm2=mm2;
//	      interp[l].mm3=mm3;
//	      interp[l].n1=n1;
//	      interp[l].n2=n2;
//	      interp[l].n3=n3;
//	      interp[l].zoff=zoffl;
//	      interp[l].uoff=uoffl;
//
//	      interp[l].notify();
//	    }
//	  for(int l=0;l<num_threads;l++)
//	    while(!interp[l].done){
//	      try{wait();}catch(InterruptedException e){}
//	      notifyAll();
//	    }
//	}
	for (int l = 0; l < num_threads; l++) {
	    interp[l].in.send(new InterpMessage(1, mm3, mm1, mm2, mm3, n1, n2, n3, zoffl, uoffl));
    }
	for (int l = 0; l < num_threads; l++) {
	    interp[l].out.receive();
    }
     }
     if (timeron) timer.stop(T_interp);
  }
  
  public void rprj3Master(double r[],
                          int roffl,int m1k,int m2k,int m3k,
                          int soffl,int m1j,int m2j,int m3j){
     if (timeron) timer.start(T_rprj3);
     if(num_threads==1) rprj3(r,roffl,m1k,m2k,m3k,soffl,m1j,m2j,m3j); 
     else{       
//        synchronized(this){
//	  for(int l=0;l<num_threads;l++)
//	    synchronized(rprj[l]){
//	      rprj[l].done=false;
//	      rprj[l].wstart=2;
//	      rprj[l].wend=m3j;
//	      rprj[l].m1k=m1k;
//	      rprj[l].m2k=m2k;
//	      rprj[l].m3k=m3k;
//	      rprj[l].m1j=m1j;
//	      rprj[l].m2j=m2j;
//	      rprj[l].m3j=m3j;
//	      rprj[l].roff=roffl;
//	      rprj[l].zoff=soffl;
//
//	      rprj[l].notify();
//	    }
//	  for(int l=0;l<num_threads;l++){
//	    while(!rprj[l].done){
//	      try{wait();}catch(InterruptedException e){}
//	      notifyAll();
//	    }
//	  }
//	}
	for (int l = 0; l < num_threads; l++) {
	    rprj[l].in.send(new RprjMessage(2, m3j, m1k, m2k, m3k, m1j, m2j, m3j, roffl, soffl));
    }
   	for (int l = 0; l < num_threads; l++) {
	    rprj[l].out.receive();
    }
        comm3(r,soffl,m1j,m2j,m3j);
     }
     if (timeron) timer.stop(T_rprj3);
  }
  
  public double getTime(){return timer.readTimer(T_bench);}
  public void finalize() throws Throwable{
    System.out.println("MG: is about to be garbage collected"); 
    super.finalize();
  }
}

