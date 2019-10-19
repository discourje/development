/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                                  C G                                    !
!									  !
!-------------------------------------------------------------------------!
!									  !
!    This benchmark is a serial/multithreaded version of the              !
!    NPB3_0_JAV CG code.                                                  !
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
! Authors: M. Yarrow                                                      !
!          C. Kuszmaul					                  !
! Translation to Java and to MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.tacas2020.npb3;

import discourje.examples.tacas2020.npb3.BMInOut.BMArgs;
import discourje.examples.tacas2020.npb3.BMInOut.BMResults;
import discourje.examples.tacas2020.npb3.CGThreads.CGBase;
import discourje.examples.tacas2020.npb3.CGThreads.CGMessage;

import java.io.File;
import java.text.DecimalFormat;

public class CG extends CGBase{
  public int bid=-1;
  public BMResults results;  
  public boolean serial=true;
  
  Random rng;	       
  public static final double amult = 1220703125.0;
  
  public CG(char CLSS,int np,boolean ser){
    super(CLSS,np,ser);
    serial=ser;
    rng=new Random();
  }
    
  public static void main(String argv[]){
  	if (argv.length == 0) {
		argv = new String[]{"-serial", "CLASS=W"};
		argv = new String[]{"-np2", "CLASS=W"};
	}

    CG cg = null;
    
    BMArgs.ParseCmdLineArgs(argv,BMName);
    char CLSS=BMArgs.CLASS;
    int np=BMArgs.num_threads;
    boolean serial=BMArgs.serial;
    try{ 
      cg = new CG(CLSS,np,serial);
    }catch(OutOfMemoryError e){
      BMArgs.outOfMemoryMessage();
      System.exit(0);
    }	   
    cg.runBenchMark();
  }

  public void run(){ runBenchMark();}
   
   public void runBenchMark(){
	int i, j, k, it;
	double zeta;
	double rnorm=0;
	
        BMArgs.Banner(BMName,CLASS,serial,num_threads);
	System.out.println(" Size: " + na +" Iterations: " + niter );
	
	timer.resetAllTimers();
        setTimers();
	
	timer.start(t_init);
	
	firstrow = 1;
	lastrow  = na;
	firstcol = 1;
	lastcol  = na;
	
	naa = na;
	nzz = nz;
	
//---------------------------------------------------------------------
//  Inialize random number generator
//---------------------------------------------------------------------
	zeta = rng.randlc(amult);
        if(!serial) setupThreads(this);
	
	makea(naa, nzz, a, colidx, rowstr, nonzer,
	      rcond, arow, acol, aelt, v, iv, shift);
	
//---------------------------------------------------------------------
//  Note: as a result of the above call to makea:
//        values of j used in indexing rowstr go from 1 --> lastrow-firstrow+1
//        values of colidx which are col indexes go from firstcol --> lastcol
//        So:
//        Shift the col index vals from actual (firstcol --> lastcol ) 
//        to local, i.e., (1 --> lastcol-firstcol+1)
//---------------------------------------------------------------------
	for(j=1;j<=lastrow-firstrow+1;j++){
	    for(k=rowstr[j]; k<=rowstr[j+1]-1;k++){
		colidx[k] = colidx[k] - firstcol + 1;
	    }
	}	
//---------------------------------------------------------------------
//  set starting vector to (1, 1, .... 1)
//---------------------------------------------------------------------
	for(i=1;i<=na+1;i++) x[i] = 1.0;
	zeta  = 0.0;
//---------------------------------------------------------------------
//  Do one iteration untimed to init all code and data page tables
//---->                    (then reinit, start timing, to niter its)
//---------------------------------------------------------------------
	    rnorm = conj_grad( colidx, rowstr, x, z, a, 
			       p, q, r, rnorm );	    
	    
//---------------------------------------------------------------------
//  zeta = shift + 1/(x.z)
//  So, first: (x.z)
//  Also, find norm of z
//  So, first: (z.z)
//---------------------------------------------------------------------
	    double tnorm1 = 0.0;
	    double tnorm2 = 0.0;
	    for(j=1;j<=lastcol-firstcol+1;j++){
		tnorm1 += x[j]*z[j];
		tnorm2 += z[j]*z[j];
	    }	    
	    tnorm2 = 1.0/ Math.sqrt(tnorm2);
	    
//---------------------------------------------------------------------
//  Normalize z to obtain x
//---------------------------------------------------------------------
	    for(j=1; j<=lastcol-firstcol+1;j++){      
		x[j] = tnorm1*z[j];
	    }                         
//---------------------------------------------------------------------
//  set starting vector to (1, 1, .... 1)
//---------------------------------------------------------------------
	for(i=1;i<=na+1;i++) x[i] = 1.0;	
	zeta  = 0.0;
	
	timer.stop( t_init );
	timer.start( t_bench );
//---------------------------------------------------------------------
//  Main Iteration for inverse power method
//---------------------------------------------------------------------
	for(it=1;it<=niter;it++){
	  if(timeron)timer.start( t_conj_grad );
	  if(serial) {
	    rnorm = conj_grad(colidx,rowstr,x,z,a,p,q,r,rnorm );
	  }else{
    	    ExecuteTask(3);
    	    double rho = 0.0;
    	    for(int m=0;m<num_threads;m++) rho+=rhomaster[m];
 
    	    for(int ii=0;ii<cgitmax;ii++){
    	      ExecuteTask(0);
    	      double dcff = 0.0;
    	      for(int m=0;m<num_threads;m++) dcff+=dmaster[m];
    	      alpha = rho / dcff;
    	      double rho0 = rho;
    	      ExecuteTask(1);
    	      rho = 0.0;
    	      for(int m=0;m<num_threads;m++) rho+=rhomaster[m];
    	      beta = rho / rho0;
    	      ExecuteTask(2);
    	    }

    	    ExecuteTask(4);
    	    rnorm = 0.0;
    	    for(int m=0;m<num_threads;m++) rnorm+=rnormmaster[m];
	    rnorm = Math.sqrt( rnorm );
	  }
	  if(timeron) timer.stop(t_conj_grad);
//---------------------------------------------------------------------
//  zeta = shift + 1/(x.z)
//  So, first: (x.z)
//  Also, find norm of z
//  So, first: (z.z)
//---------------------------------------------------------------------
	  tnorm1 = 0.0;
	  tnorm2 = 0.0;
	  for(j=1;j<=lastcol-firstcol+1;j++){
	    tnorm1 += x[j]*z[j];
	    tnorm2 += z[j]*z[j];
	  }	  
	  tnorm2 = 1.0 / Math.sqrt(tnorm2);
	  zeta = shift + 1.0 /tnorm1;
	  System.out.println( "    "+ it + "       " + rnorm +" " + zeta );	//---------------------------------------------------------------------
//  Normalize z to obtain x
//---------------------------------------------------------------------
	  for(j=1;j<=lastcol-firstcol+1;j++){	   
	    x[j] = tnorm2*z[j];    
	  }			 
	}
	timer.stop(t_bench);

//---------------------------------------------------------------------
//  End of timed section
//---------------------------------------------------------------------

	int verified=verify(zeta);		
	double time = timer.readTimer( t_bench );	
        results=new BMResults(BMName,
    			  CLASS,
    			  na,
    			  0,
    			  0,
    			  niter,
    			  time,
    			  getMFLOPS(time,niter),
    			  "floating point",
    			  verified,
    			  serial,
    			  num_threads,
    			  bid);
        results.print();				
	if (timeron) PrintTimers();

	for (int m = 0; m < num_threads; m++) {
		worker[m].in.send(new ExitMessage());
		worker[m].out.recv();
		while (true) {
			try {
				worker[m].join();
				break;
			} catch (InterruptedException e) {
			}
		}
	}
     }
  void setTimers(){
    File fp = new File("timer.flag");
    if (fp.exists()){
      timeron = true;
      t_names[t_init] = new String("init");
      t_names[t_bench] = new String("benchmark");
      t_names[t_conj_grad] = new String("conjugate gradient");
    }else{
      timeron = false;
    }
  }    
  public double getMFLOPS(double total_time,int niter){
    double mflops = 0.0;
    if( total_time != 0.0 ){
      mflops = (float)( 2*na )*
    	       ( 3.+(float)( nonzer*(nonzer+1) )
    	         + 25.*(5.+(float)( nonzer*(nonzer+1) ))+ 3. ) ;
      mflops *= niter / (total_time*1000000.0);
    }
    return mflops;
  }
  public int verify(double zeta){
    int verified=0;
    double epsilon = 1.0E-10;	     
    if(CLASS != 'U'){
      System.out.println(" Zeta is   " + zeta );
      if( Math.abs( zeta - zeta_verify_value ) <= epsilon ){
    	verified = 1;
    	System.out.println(" Deviation is   " + (zeta-zeta_verify_value) );
      }else{
    	  verified = 0;
    	  System.out.println(" The correct zeta is " + zeta_verify_value);
      }
    }else{
    	verified = -1;
    }
    BMResults.printVerificationStatus(CLASS,verified,BMName); 
    return verified;
  }
     
  public void makea(int n, int nz, double[] a, int[] colidx, int[] rowstr, 
	  int nonzer, double rcond, int[] arow, int[] acol, 
	  double[] aelt, double[] v, int[] iv, double shift){


//---------------------------------------------------------------------
//       generate the test problem for benchmark 6
//       makea generates a sparse matrix with a
//       prescribed sparsity distribution
//
//       parameter    type        usage
//
//       input
//
//       n            i           number of cols/rows of matrix
//       nz           i           nonzeros as declared array size
//       rcond        r*8         condition number
//       shift        r*8         main diagonal shift
//
//       output
//
//       a            r*8         array for nonzeros
//       colidx       i           col indices
//       rowstr       i           row pointers
//
//       workspace
//
//       iv, arow, acol i
//       v, aelt        r*8
//---------------------------------------------------------------------

	int i, nnza,  ivelt, ivelt1, irow, nzv,jcol;

//---------------------------------------------------------------------
//      nonzer is approximately  (int(sqrt(nnza /n)));
//---------------------------------------------------------------------

	double size, ratio, scale;

	size = 1.0;
	ratio = Math.pow(rcond , (1.0/(float)n) );
	nnza = 0;

//---------------------------------------------------------------------
//  Initialize colidx(n+1 .. 2n) to zero.
//  Used by sprnvc to mark nonzero positions
//---------------------------------------------------------------------

	for(i=1;i<=n;i++){
	    colidx[n+i] = 0;
	}
	for(int iouter=1;iouter<=n;iouter++){
	    nzv = nonzer;
	    sprnvc( n, nzv, v, iv, colidx, 0, colidx, n);
	    nzv = vecset( n, v, iv, nzv, iouter, .5 );
	    
	    for(ivelt=1;ivelt<=nzv;ivelt++){
		jcol = iv[ivelt];
		if (jcol>=firstcol && jcol<=lastcol) {
		    scale = size * v[ivelt];
		    for(ivelt1=1;ivelt1<=nzv;ivelt1++){
			irow = iv[ivelt1];
			if (irow>=firstrow && irow<=lastrow) {
			    nnza = nnza + 1;
			    if (nnza > nz){
				System.out.println("Space for matrix elements exceeded in makea");
				System.out.println("nnza, nzmax = " + nnza +", " + nz);
				System.out.println(" iouter = " + iouter);
				System.exit(0);
			    }
			    acol[nnza] = jcol;
			    arow[nnza] = irow;
			    aelt[nnza] = v[ivelt1] * scale;
			}
		    }
		}
	    }
	    size = size * ratio;
	}	
	    
//---------------------------------------------------------------------
//       ... add the identity * rcond to the generated matrix to bound
//           the smallest eigenvalue from below by rcond
//---------------------------------------------------------------------
	for(i=firstrow; i<=lastrow;i++){
	    if (i >= firstcol && i <= lastcol) {
		int iouter = n + i;
		nnza = nnza + 1;
		if (nnza > nz){
		    System.out.println("Space for matrix elements exceeded in makea");
		    System.out.println("nnza, nzmax = " + nnza +", " + nz);
		  System.out.println(" iouter = " + iouter);
		  System.exit(0);
		}
		acol[nnza] = i;
		arow[nnza] = i;
		aelt[nnza] = rcond - shift;
	    }
	}
	
	
//---------------------------------------------------------------------
//       ... make the sparse matrix from list of elements with duplicates
//           (v and iv are used as  workspace)
//---------------------------------------------------------------------
	
	sparse( a, colidx, rowstr, n, arow, acol, aelt,
		v, iv, 0 , iv, n, nnza);
	return;
  }

  public void sprnvc(int n, int nz, double v[], int iv[], int nzloc[], 
		       int nzloc_offst, int mark[], int mark_offst){
    int nzrow=0,nzv=0,idx;	
    int nn1 = 1;
    
    while(true){
      nn1 = 2 * nn1;
      if (nn1 >= n ) break;
    } 
    
    while(true){
      if(nzv >= nz){
        for(int ii = 1;ii<=nzrow;ii++){
          idx = nzloc[ii+nzloc_offst];
          mark[idx+mark_offst] = 0;
        }
        return;
      }
      double vecelt = rng.randlc(amult);
      double vecloc = rng.randlc(amult);
      idx = (int) (vecloc*nn1)+1;
      if(idx > n) continue;

      if(mark[idx+mark_offst] == 0){
        mark[idx+mark_offst] = 1;
        nzrow = nzrow + 1;
        nzloc[nzrow + nzloc_offst] = idx;
        nzv = nzv + 1;
        v[nzv] = vecelt;
        iv[nzv] = idx;
      }
    }
  }
    
  public int vecset(int n, double v[], int iv[], 
                    int nzv,int ival,double val){
    boolean set = false; 
    for(int k=1; k<=nzv;k++){
      if (iv[k] == ival){
        v[k]=val;
        set=true;
      }
    }
    if(!set){
      nzv     = nzv + 1;
      v[nzv]  = val;
      iv[nzv] = ival;
    }
    return nzv;    
  }

  public void sparse(double a[], int colidx[], int rowstr[], 
                     int n, int arow[], int acol[], 
		     double aelt[],  
		     double x[], int mark[], 
		     int mark_offst, int nzloc[], int nzloc_offst, 
		     int nnza){
//---------------------------------------------------------------------
//       rows range from firstrow to lastrow
//       the rowstr pointers are defined for nrows = lastrow-firstrow+1 values
//---------------------------------------------------------------------
    int nrows;
//---------------------------------------------------
//       generate a sparse matrix from a list of
//       [col, row, element] tri
//---------------------------------------------------  
    int i, j, jajp1, nza, k, nzrow;
    double xi;	
//---------------------------------------------------------------------
//    how many rows of result
//---------------------------------------------------------------------
    nrows = lastrow - firstrow + 1;

//---------------------------------------------------------------------
//     ...count the number of triples in each row
//---------------------------------------------------------------------
    for(j=1;j<=n;j++){
	rowstr[j] = 0;
	mark[j+mark_offst] = 0;
    }
    rowstr[n+1] = 0;
	
    for(nza=1;nza<=nnza;nza++){
	j = (arow[nza] - firstrow + 1)+1;
	rowstr[j] = rowstr[j] + 1;
    }
    
    rowstr[1] = 1;
    for(j=2;j<=nrows+1;j++){
	rowstr[j] = rowstr[j] + rowstr[j-1];
    }
//---------------------------------------------------------------------
//     ... rowstr(j) now is the location of the first nonzero
//           of row j of a
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//     ... do a bucket sort of the triples on the row index
//---------------------------------------------------------------------
    for(nza=1;nza<=nnza;nza++){
	j = arow[nza] - firstrow + 1;
	k = rowstr[j];
	a[k] = aelt[nza];
	colidx[k] = acol[nza];
	rowstr[j] = rowstr[j] + 1;

    }    

//---------------------------------------------------------------------
//       ... rowstr(j) now points to the first element of row j+1
//---------------------------------------------------------------------
    for(j=nrows-1;j>=0;j--){
	rowstr[j+1] = rowstr[j];
    }
    rowstr[1] = 1;
//---------------------------------------------------------------------
//       ... generate the actual output rows by adding elements
//---------------------------------------------------------------------
    nza = 0;
    for(i=1;i<=n;i++){
	x[i]    = 0.0;
	mark[i+mark_offst] = 0;
    }
    
    jajp1 = rowstr[1];

    for(j=1;j<=nrows;j++){
	nzrow = 0;
	
//---------------------------------------------------------------------
//          ...loop over the jth row of a
//---------------------------------------------------------------------
	for(k = jajp1; k<=(rowstr[j+1]-1);k++){
	    i = colidx[k];
	    x[i] = x[i] + a[k];
	    if ( (mark[i+mark_offst]==0) && (x[i] != 0)){
		mark[i+mark_offst] = 1;
		nzrow = nzrow + 1;
		nzloc[nzrow+nzloc_offst] = i;
	    }
	}
	
//---------------------------------------------------------------------
//          ... extract the nonzeros of this row
//---------------------------------------------------------------------
	for(k=1;k<=nzrow;k++){
	    i = nzloc[k+nzloc_offst];
	    mark[i+mark_offst] = 0;
	    xi = x[i];
	    x[i] = 0;
	    if (xi != 0){
		nza = nza + 1;
		a[nza] = xi;
		colidx[nza] = i;
	    }
	}
	jajp1 = rowstr[j+1];
	rowstr[j+1] = nza + rowstr[1];
    }   
    return;
  }
    
  public double conj_grad( int colidx[], int rowstr[],
			    double x[],double z[],double a[],
			    double p[],double q[],double r[],
			    double rnorm ){
//---------------------------------------------------------------------
//  Floating point arrays here are named as in NPB1 spec discussion of 
//  CG algorithm
//---------------------------------------------------------------------

    int i, j, k;
    int cgit;
    double d, sum, rho, rho0;
	
//---------------------------------------------------------------------
//  Initialize the CG algorithm:
//---------------------------------------------------------------------
    for(j=1;j<=naa+1;j++){
      q[j] = 0.0;
      z[j] = 0.0;
      r[j] = x[j];
      p[j] = r[j];
    }
//---------------------------------------------------------------------
//  rho = r.r
//  Now, obtain the norm of r: First, sum squares of r elements locally...
//---------------------------------------------------------------------
    rho = 0.0;
    for(j=1;j<=lastcol-firstcol+1;j++) rho += r[j]*r[j];
//---------------------------------------------------------------------
//  The conj grad iteration loop
//---------------------------------------------------------------------
    for(cgit=1;cgit<=cgitmax;cgit++){
//---------------------------------------------------------------------
//  q = A.p
//  The partition submatrix-vector multiply: use workspace w
//---------------------------------------------------------------------
//
//  NOTE: this version of the multiply is actually (slightly: maybe %5) 
//        faster on the sp2 on 16 nodes than is the unrolled-by-2 version 
//        below.   On the Cray t3d, the reverse is true, i.e., the 
//        unrolled-by-two version is some 10% faster.  
//        The unrolled-by-8 version below is significantly faster
//        on the Cray t3d - overall speed of code is 1.5 times faster.
//
	for(j=1;j<=lastrow-firstrow+1;j++){
	  sum = 0.0;
	  for(k=rowstr[j];k<=rowstr[j+1]-1;k++){
	      sum += a[k]*p[colidx[k]];
	  }
	  q[j] = sum;
	}
//---------------------------------------------------------------------
//  Obtain p.q
//---------------------------------------------------------------------
	d = 0.0;
	for(j=1;j<=lastcol-firstcol+1;j++) d += p[j]*q[j];
//---------------------------------------------------------------------
//  Obtain alpha = rho / (p.q)
//---------------------------------------------------------------------
	alpha = rho / d;
//---------------------------------------------------------------------
//  Obtain z = z + alpha*p
//  and    r = r - alpha*q
//---------------------------------------------------------------------
	for(j=1;j<=lastcol-firstcol+1;j++){
	  z[j] = z[j] + alpha*p[j];
	  r[j] = r[j] - alpha*q[j];
	}
//---------------------------------------------------------------------
//  rho = r.r
//  Obtain the norm of r: First, sum squares of r elements locally...
//---------------------------------------------------------------------
	rho0 = rho;	
	rho = 0.0;
	for(j=1;j<=lastcol-firstcol+1;j++) rho += r[j]*r[j];
	beta = rho / rho0;
//---------------------------------------------------------------------
//  p = r + beta*p
//---------------------------------------------------------------------
	for(j=1;j<=lastcol-firstcol+1;j++){
	    p[j] = r[j] + beta*p[j];
	}
   }                            
//---------------------------------------------------------------------
//  Compute residual norm explicitly:  ||r|| = ||x - A.z||
//  First, form A.z
//  The partition submatrix-vector multiply
//---------------------------------------------------------------------
    for(j=1;j<=lastrow-firstrow+1;j++){
      sum = 0.0;
      for(k=rowstr[j];k<=rowstr[j+1]-1;k++){
          sum += a[k]*z[colidx[k]];
      }
      r[j] = sum;
    }
//---------------------------------------------------------------------
//  At this point, r contains A.z
//---------------------------------------------------------------------
    sum = 0.0;
    for(j=1;j<=lastcol-firstcol+1;j++) sum +=(x[j]-r[j])*(x[j]-r[j]);      
    return Math.sqrt( sum );
  }
    
  public double endWork(){
    double sum;
//---------------------------------------------------------------------
//  Compute residual norm explicitly:  ||r|| = ||x - A.z||
//  First, form A.z
//  The partition submatrix-vector multiply
//---------------------------------------------------------------------
    for(int j=1;j<=lastrow-firstrow+1;j++){
    	sum = 0.0;
    	for(int k=rowstr[j];k<=rowstr[j+1]-1;k++){
    	    sum += a[k]*z[colidx[k]];
    	}
    	r[j] = sum;
    }
//---------------------------------------------------------------------
//  At this point, r contains A.z
//---------------------------------------------------------------------
    sum = 0.0;
    for(int j=1;j<=lastcol-firstcol+1;j++) sum+=(x[j]-r[j])*(x[j]-r[j]);
    return Math.sqrt(sum);
  }

  private void PrintTimers(){
    DecimalFormat fmt = new DecimalFormat("0.000");
    System.out.println("  SECTION   Time (secs)");
    double ttot = timer.readTimer(t_bench);
    if (ttot == 0.0) ttot = 1.0;
    for(int i=1;i<=t_last;i++){
      double tm = timer.readTimer(i);
      if(i==t_init){
        System.out.println("  "+t_names[i]+":"+fmt.format(tm));
      }else{	  
        System.out.println("  "+t_names[i]+":"+fmt.format(tm) 
                	  +"  ("+fmt.format(tm*100.0/ttot)+"%)");
        if (i==t_conj_grad){
            tm = ttot - tm;
            System.out.println("    --> total rest :" + fmt.format(tm) 
                	      +"  ("+fmt.format(tm*100.0/ttot)+"%)");
        }
      }
    }
  }

  private void ExecuteTask(int OrderNum){
//  private synchronized void ExecuteTask(int OrderNum){
    for(int m=0;m<num_threads;m++){
//      synchronized(worker[m]){
//     	worker[m].TaskOrder=OrderNum;
//     	worker[m].done=false;
//      	worker[m].alpha=alpha;
//      	worker[m].beta=beta;
//   	worker[m].notify();
//      }
		worker[m].in.send(new CGMessage(OrderNum, alpha, beta));
    }
    for(int m=0;m<num_threads;m++){
//      while(!worker[m].done){
//     	try{wait();}catch(InterruptedException e){}
//  	notifyAll();
//      }
		worker[m].out.recv();
    }
  }
    
  public double getTime(){return timer.readTimer(t_bench);}
  public void finalize() throws Throwable{
    System.out.println("CG: is about to be garbage collected"); 
    super.finalize();
  }
}
