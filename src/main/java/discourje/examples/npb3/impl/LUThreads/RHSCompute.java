/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                            R H S C o m p u t e                          !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    RHSCompute implements thread for compute_rhs subroutine of LU        !
!    benchmark.                                                           !
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
! Translation to Java and MultiThreaded Code				  !
!	   M. Frumkin							  !
!	   M. Schultz							  !
!-------------------------------------------------------------------------!
*/
package discourje.examples.npb3.impl.LUThreads;
import discourje.examples.npb3.impl.LU;

public class RHSCompute extends LUBase{
  public int id;
  public boolean done=true;

  //private arrays and data
  int lower_bound1;
  int upper_bound1;
  int lower_bound2;
  int upper_bound2;
  double flux[]=null;
  int state;

  public RHSCompute(LU lu,int low1, int high1, int low2, int high2){
    Init(lu);
    lower_bound1=low1;
    upper_bound1=high1;
    lower_bound2=low2;
    upper_bound2=high2;
    state=1;
    flux = new double[5*isiz1];    
    setPriority(Thread.MAX_PRIORITY);
    setDaemon(true);
    master=lu;
  }
  void Init(LU lu){
    //initialize shared data
    isiz1=lu.isiz1;
    isiz2=lu.isiz2;
    isiz3=lu.isiz3;
    
    itmax_default=lu.itmax_default;
    dt_default=lu.dt_default;
    inorm_default=lu.inorm_default;
    
    u=lu.u;
    rsd=lu.rsd;
    frct=lu.frct;
    isize1=lu.isize1;
    jsize1=lu.jsize1;
    ksize1=lu.ksize1;
    
    flux=lu.flux;
    isize2=lu.isize2;
    
    qs=lu.qs;
    rho_i=lu.rho_i;
    jsize3=lu.jsize3;
    ksize3=lu.ksize3;
    
    a=lu.a;
    b=lu.b;
    c=lu.c;
    d=lu.d;
    isize4=lu.isize4;
    jsize4=lu.jsize4;
    ksize4=lu.ksize4;
    nx=lu.nx;
    ny=lu.ny;
    nz=lu.nz;
    
    nx0=lu.nx0;
    ny0=lu.ny0;
    nz0=lu.nz0;
    
    ist=lu.ist;
    iend=lu.iend;
    jst=lu.jst;
    jend=lu.jend;
    ii1=lu.ii1;
    ii2=lu.ii2;
    ji1=lu.ji1;
    ji2=lu.ji2;
    ki1=lu.ki1;
    ki2=lu.ki2;
    
    dxi=lu.dxi;
    deta=lu.deta; 
    dzeta=lu.dzeta;
    tx1=lu.tx1;
    tx2=lu.tx2;
    tx3=lu.tx3;
    ty1=lu.ty1;
    ty2=lu.ty2;
    ty3=lu.ty3;
    tz1=lu.tz1;
    tz2=lu.tz2;
    tz3=lu.tz3;
    
    dx1=lu.dx1;
    dx2=lu.dx2;
    dx3=lu.dx3;
    dx4=lu.dx4;
    dx5=lu.dx5;

    dy1=lu.dy1;
    dy2=lu.dy2;
    dy3=lu.dy3;
    dy4=lu.dy4;
    dy5=lu.dy5;

    dz1=lu.dz1;
    dz2=lu.dz2;
    dz3=lu.dz3;
    dz4=lu.dz4;
    dz5=lu.dz5;
   
    dssp=lu.dssp;
    dt=lu.dt;
    omega=lu.omega;
    frc=lu.frc;
    ttotal=lu.ttotal;
  }
  public void run(){    
    for(;;){
      synchronized(this){ 
      while(done==true){
	try{
	  wait();
          synchronized(master){master.notify();}
	}catch(InterruptedException ie){}
      }
      step();
      synchronized(master){done=true;master.notify();}
      }
    }
  }
  
  public void step(){
    int i, j, k, m;
    double  q;
    double  tmp;
    double  u21, u31, u41;
    double  u21i, u31i, u41i, u51i;
    double  u21j, u31j, u41j, u51j;
    double  u21k, u31k, u41k, u51k;
    double  u21im1, u31im1, u41im1, u51im1;
    double  u21jm1, u31jm1, u41jm1, u51jm1;
    double  u21km1, u31km1, u41km1, u51km1;
    
    switch(state){
      case 1:
      for(k=lower_bound1;k<=upper_bound1;k++){
	for(j=0;j<=ny-1;j++){
	  for(i=0;i<=nx-1;i++){
	    for(m=0;m<=4;m++){
	      rsd[m+i*isize1+j*jsize1+k*ksize1] = - frct[m+i*isize1+j*jsize1+k*ksize1];
	    }
	    tmp = 1.0 / u[0+i*isize1+j*jsize1+k*ksize1];
	    rho_i[i+j*jsize3+k*ksize3] = tmp;
	    qs[i+j*jsize3+k*ksize3] = 0.50 * (  u[1+i*isize1+j*jsize1+k*ksize1] 
					      * u[1+i*isize1+j*jsize1+k*ksize1]
					      + u[2+i*isize1+j*jsize1+k*ksize1] 
					      * u[2+i*isize1+j*jsize1+k*ksize1]
					      + u[3+i*isize1+j*jsize1+k*ksize1] 
					      * u[3+i*isize1+j*jsize1+k*ksize1] )* tmp;
	  }
	}
      }
      break;
      case 2:

//---------------------------------------------------------------------
//   xi-direction flux differences
//---------------------------------------------------------------------
  for(k=lower_bound2;k<=upper_bound2;k++){
    for(j=jst-1;j<=jend-1;j++){
      for(i=0;i<=nx-1;i++){
	flux[0+i*isize2] = u[1+i*isize1+j*jsize1+k*ksize1];
	u21 = u[1+i*isize1+j*jsize1+k*ksize1] * rho_i[i+j*jsize3+k*ksize3];

	q = qs[i+j*jsize3+k*ksize3];
	
	flux[1+i*isize2] = u[1+i*isize1+j*jsize1+k*ksize1] * u21 + c2 * 
                              ( u[4+i*isize1+j*jsize1+k*ksize1] - q );
	flux[2+i*isize2] = u[2+i*isize1+j*jsize1+k*ksize1] * u21;
	flux[3+i*isize2] = u[3+i*isize1+j*jsize1+k*ksize1] * u21;
	flux[4+i*isize2] = ( c1 * u[4+i*isize1+j*jsize1+k*ksize1] - c2 * q ) * u21;
      }

      for(i=ist-1;i<=iend-1;i++){
	for(m=0;m<=4;m++){
	  rsd[m+i*isize1+j*jsize1+k*ksize1] =  rsd[m+i*isize1+j*jsize1+k*ksize1]
                       - tx2 * ( flux[m+(i+1)*isize2] - flux[m+(i-1)*isize2] );
	}
      }

      for(i=ist-1;i<=nx-1;i++){
	tmp = rho_i[i+j*jsize3+k*ksize3];
	
	u21i = tmp * u[1+i*isize1+j*jsize1+k*ksize1];
	u31i = tmp * u[2+i*isize1+j*jsize1+k*ksize1];
	u41i = tmp * u[3+i*isize1+j*jsize1+k*ksize1];
	u51i = tmp * u[4+i*isize1+j*jsize1+k*ksize1];
	
	tmp = rho_i[(i-1)+j*jsize3+k*ksize3];
	
	u21im1 = tmp * u[1+(i-1)*isize1+j*jsize1+k*ksize1];
	u31im1 = tmp * u[2+(i-1)*isize1+j*jsize1+k*ksize1];
	u41im1 = tmp * u[3+(i-1)*isize1+j*jsize1+k*ksize1];
	u51im1 = tmp * u[4+(i-1)*isize1+j*jsize1+k*ksize1];
	
	flux[1+i*isize2] = (4.0/3.0) * tx3 * (u21i-u21im1);
	flux[2+i*isize2] = tx3 * ( u31i - u31im1 );
	flux[3+i*isize2] = tx3 * ( u41i - u41im1 );
	flux[4+i*isize2] = 0.50 * ( 1.0 - c1*c5 )
	  * tx3 * ( ( Math.pow(u21i,2) + Math.pow(u31i,2) + Math.pow(u41i,2) )
		   - ( Math.pow(u21im1,2) + Math.pow(u31im1,2) + Math.pow(u41im1,2) ) )
	    + (1.0/6.0)
	      * tx3 * ( Math.pow(u21i,2) - Math.pow(u21im1,2) )
		+ c1 * c5 * tx3 * ( u51i - u51im1 );
      }

      for(i=ist-1;i<=iend-1;i++){
	rsd[0+i*isize1+j*jsize1+k*ksize1] = rsd[0+i*isize1+j*jsize1+k*ksize1]
	  + dx1 * tx1 * (            u[0+(i-1)*isize1+j*jsize1+k*ksize1]
			 - 2.0 * u[0+i*isize1+j*jsize1+k*ksize1]
			 +           u[0+(i+1)*isize1+j*jsize1+k*ksize1] );
	rsd[1+i*isize1+j*jsize1+k*ksize1] = rsd[1+i*isize1+j*jsize1+k*ksize1]
	  + tx3 * c3 * c4 * ( flux[1+(i+1)*isize2] - flux[1+i*isize2] )
	    + dx2 * tx1 * (            u[1+(i-1)*isize1+j*jsize1+k*ksize1]
                                   - 2.0 * u[1+i*isize1+j*jsize1+k*ksize1]
			   +           u[1+(i+1)*isize1+j*jsize1+k*ksize1] );
	rsd[2+i*isize1+j*jsize1+k*ksize1] = rsd[2+i*isize1+j*jsize1+k*ksize1]
	  + tx3 * c3 * c4 * ( flux[2+(i+1)*isize2] - flux[2+i*isize2] )
	    + dx3 * tx1 * (            u[2+(i-1)*isize1+j*jsize1+k*ksize1]
			   - 2.0 * u[2+i*isize1+j*jsize1+k*ksize1]
			   +           u[2+(i+1)*isize1+j*jsize1+k*ksize1] );
	rsd[3+i*isize1+j*jsize1+k*ksize1] = rsd[3+i*isize1+j*jsize1+k*ksize1]
	  + tx3 * c3 * c4 * ( flux[3+(i+1)*isize2] - flux[3+i*isize2] )
	    + dx4 * tx1 * (            u[3+(i-1)*isize1+j*jsize1+k*ksize1]
			   - 2.0 * u[3+i*isize1+j*jsize1+k*ksize1]
			   +           u[3+(i+1)*isize1+j*jsize1+k*ksize1] );
	rsd[4+i*isize1+j*jsize1+k*ksize1] = rsd[4+i*isize1+j*jsize1+k*ksize1]
	  + tx3 * c3 * c4 * ( flux[4+(i+1)*isize2] - flux[4+i*isize2] )
	    + dx5 * tx1 * (            u[4+(i-1)*isize1+j*jsize1+k*ksize1]
                                   - 2.0 * u[4+i*isize1+j*jsize1+k*ksize1]
			   +           u[4+(i+1)*isize1+j*jsize1+k*ksize1] );
      }
      
//---------------------------------------------------------------------
//   Fourth-order dissipation
//---------------------------------------------------------------------
    for(m=0;m<=4;m++){
      rsd[m+1*isize1+j*jsize1+k*ksize1] = rsd[m+1*isize1+j*jsize1+k*ksize1]
	- dssp * ( + 5.0 * u[m+1*isize1+j*jsize1+k*ksize1]
		  - 4.0 * u[m+2*isize1+j*jsize1+k*ksize1]
		  +           u[m+3*isize1+j*jsize1+k*ksize1] );
      rsd[m+2*isize1+j*jsize1+k*ksize1] = rsd[m+2*isize1+j*jsize1+k*ksize1]
	- dssp * ( - 4.0 * u[m+1*isize1+j*jsize1+k*ksize1]
		  + 6.0 * u[m+2*isize1+j*jsize1+k*ksize1]
		  - 4.0 * u[m+3*isize1+j*jsize1+k*ksize1]
		  +           u[m+4*isize1+j*jsize1+k*ksize1] );
    }
      
      for(i=3;i<=nx - 4;i++){
	for(m=0;m<=4;m++){
	  rsd[m+i*isize1+j*jsize1+k*ksize1] = rsd[m+i*isize1+j*jsize1+k*ksize1]
	    - dssp * (            u[m+(i-2)*isize1+j*jsize1+k*ksize1]
		      - 4.0 * u[m+(i-1)*isize1+j*jsize1+k*ksize1]
		      + 6.0 * u[m+i*isize1+j*jsize1+k*ksize1]
		      - 4.0 * u[m+(i+1)*isize1+j*jsize1+k*ksize1]
		      +           u[m+(i+2)*isize1+j*jsize1+k*ksize1] );
	}
      }
      
	    
      for(m=0;m<=4;m++){
	rsd[m+(nx-3)*isize1+j*jsize1+k*ksize1] = rsd[m+(nx-3)*isize1+j*jsize1+k*ksize1]
	  - dssp * (             u[m+(nx-5)*isize1+j*jsize1+k*ksize1]
		    - 4.0 * u[m+(nx-4)*isize1+j*jsize1+k*ksize1]
		    + 6.0 * u[m+(nx-3)*isize1+j*jsize1+k*ksize1]
		    - 4.0 * u[m+(nx-2)*isize1+j*jsize1+k*ksize1]  );
	rsd[m+(nx-2)*isize1+j*jsize1+k*ksize1] = rsd[m+(nx-2)*isize1+j*jsize1+k*ksize1]
	  - dssp * (             u[m+(nx-4)*isize1+j*jsize1+k*ksize1]
		    - 4.0 * u[m+(nx-3)*isize1+j*jsize1+k*ksize1]
		    + 5.0 * u[m+(nx-2)*isize1+j*jsize1+k*ksize1] );
      }
	    
    }
  }
    break;
    case 3:

//---------------------------------------------------------------------
//   eta-direction flux differences
//---------------------------------------------------------------------
  for(k=lower_bound2;k<=upper_bound2;k++){
    for(i=ist-1;i<=iend-1;i++){
      for(j=0;j<=ny-1;j++){
	flux[0+j*isize2] = u[2+i*isize1+j*jsize1+k*ksize1];
	u31 = u[2+i*isize1+j*jsize1+k*ksize1] * rho_i[i+j*jsize3+k*ksize3];

	q = qs[i+j*jsize3+k*ksize3];
	
	flux[1+j*isize2] = u[1+i*isize1+j*jsize1+k*ksize1] * u31 ;
	flux[2+j*isize2] = u[2+i*isize1+j*jsize1+k*ksize1] * u31 + c2 * (u[4+i*isize1+j*jsize1+k*ksize1]-q);
	flux[3+j*isize2] = u[3+i*isize1+j*jsize1+k*ksize1] * u31;
	flux[4+j*isize2] = ( c1 * u[4+i*isize1+j*jsize1+k*ksize1] - c2 * q ) * u31;
      }

      for(j=jst-1;j<=jend-1;j++){
	for(m=0;m<=4;m++){
	  rsd[m+i*isize1+j*jsize1+k*ksize1] =  rsd[m+i*isize1+j*jsize1+k*ksize1]
	    - ty2 * ( flux[m+(j+1)*isize2] - flux[m+(j-1)*isize2] );
	}
      }

      for(j=jst-1;j<=ny-1;j++){
	tmp = rho_i[i+j*jsize3+k*ksize3];
	
	u21j = tmp * u[1+i*isize1+j*jsize1+k*ksize1];
	u31j = tmp * u[2+i*isize1+j*jsize1+k*ksize1];
	u41j = tmp * u[3+i*isize1+j*jsize1+k*ksize1];
	u51j = tmp * u[4+i*isize1+j*jsize1+k*ksize1];
	
	tmp = rho_i[i+(j-1)*jsize3+k*ksize3];
	u21jm1 = tmp * u[1+i*isize1+(j-1)*jsize1+k*ksize1];
	u31jm1 = tmp * u[2+i*isize1+(j-1)*jsize1+k*ksize1];
	u41jm1 = tmp * u[3+i*isize1+(j-1)*jsize1+k*ksize1];
	u51jm1 = tmp * u[4+i*isize1+(j-1)*jsize1+k*ksize1];
	
	flux[1+j*isize2] = ty3 * ( u21j - u21jm1 );
	flux[2+j*isize2] = (4.0/3.0) * ty3 * (u31j-u31jm1);
	flux[3+j*isize2] = ty3 * ( u41j - u41jm1 );
	flux[4+j*isize2] = 0.50 * ( 1.0 - c1*c5 )
	  * ty3 * ( ( Math.pow(u21j,2) + Math.pow(u31j,2) + Math.pow(u41j,2) )
		   - ( Math.pow(u21jm1,2) + Math.pow(u31jm1,2) + Math.pow(u41jm1,2) ) )
	    + (1.0/6.0)
	      * ty3 * ( Math.pow(u31j,2) - Math.pow(u31jm1,2) )
		+ c1 * c5 * ty3 * ( u51j - u51jm1 );
      }

      for(j=jst-1;j<=jend-1;j++){
	
	rsd[0+i*isize1+j*jsize1+k*ksize1] = rsd[0+i*isize1+j*jsize1+k*ksize1]
	  + dy1 * ty1 * (            u[0+i*isize1+(j-1)*jsize1+k*ksize1]
			 - 2.0 * u[0+i*isize1+j*jsize1+k*ksize1]
			 +           u[0+i*isize1+(j+1)*jsize1+k*ksize1] );
	
	rsd[1+i*isize1+j*jsize1+k*ksize1] = rsd[1+i*isize1+j*jsize1+k*ksize1]
	  + ty3 * c3 * c4 * ( flux[1+(j+1)*isize2] - flux[1+j*isize2] )
	    + dy2 * ty1 * (            u[1+i*isize1+(j-1)*jsize1+k*ksize1]
			   - 2.0 * u[1+i*isize1+j*jsize1+k*ksize1]
			   +           u[1+i*isize1+(j+1)*jsize1+k*ksize1] );

	rsd[2+i*isize1+j*jsize1+k*ksize1] = rsd[2+i*isize1+j*jsize1+k*ksize1]
	  + ty3 * c3 * c4 * ( flux[2+(j+1)*isize2] - flux[2+j*isize2] )
	    + dy3 * ty1 * (            u[2+i*isize1+(j-1)*jsize1+k*ksize1]
			   - 2.0 * u[2+i*isize1+j*jsize1+k*ksize1]
			   +           u[2+i*isize1+(j+1)*jsize1+k*ksize1] );
	
	rsd[3+i*isize1+j*jsize1+k*ksize1] = rsd[3+i*isize1+j*jsize1+k*ksize1]
	  + ty3 * c3 * c4 * ( flux[3+(j+1)*isize2] - flux[3+j*isize2] )
	    + dy4 * ty1 * (            u[3+i*isize1+(j-1)*jsize1+k*ksize1]
			   - 2.0 * u[3+i*isize1+j*jsize1+k*ksize1]
			   +           u[3+i*isize1+(j+1)*jsize1+k*ksize1] );
	
	rsd[4+i*isize1+j*jsize1+k*ksize1] = rsd[4+i*isize1+j*jsize1+k*ksize1]
	  + ty3 * c3 * c4 * ( flux[4+(j+1)*isize2] - flux[4+j*isize2] )
	    + dy5 * ty1 * (            u[4+i*isize1+(j-1)*jsize1+k*ksize1]
			   - 2.0 * u[4+i*isize1+j*jsize1+k*ksize1]
			   +           u[4+i*isize1+(j+1)*jsize1+k*ksize1] );	
      }

//---------------------------------------------------------------------
//   fourth-order dissipation
//---------------------------------------------------------------------
  for(m=0;m<=4;m++){
    rsd[m+i*isize1+1*jsize1+k*ksize1] = rsd[m+i*isize1+1*jsize1+k*ksize1]
      - dssp * ( + 5.0 * u[m+i*isize1+1*jsize1+k*ksize1]
		- 4.0 * u[m+i*isize1+2*jsize1+k*ksize1]
		+           u[m+i*isize1+3*jsize1+k*ksize1] );
    rsd[m+i*isize1+2*jsize1+k*ksize1] = rsd[m+i*isize1+2*jsize1+k*ksize1]
      - dssp * ( - 4.0 * u[m+i*isize1+1*jsize1+k*ksize1]
		+ 6.0 * u[m+i*isize1+2*jsize1+k*ksize1]
		- 4.0 * u[m+i*isize1+3*jsize1+k*ksize1]
		+           u[m+i*isize1+4*jsize1+k*ksize1] );
  }
      
      for(j=3;j<=ny - 4;j++){
	for(m=0;m<=4;m++){
	  rsd[m+i*isize1+j*jsize1+k*ksize1] = rsd[m+i*isize1+j*jsize1+k*ksize1]
	    - dssp * (            u[m+i*isize1+(j-2)*jsize1+k*ksize1]
		      - 4.0 * u[m+i*isize1+(j-1)*jsize1+k*ksize1]
		      + 6.0 * u[m+i*isize1+j*jsize1+k*ksize1]
		      - 4.0 * u[m+i*isize1+(j+1)*jsize1+k*ksize1]
		      +           u[m+i*isize1+(j+2)*jsize1+k*ksize1] );
	}
      }
      
      for(m=0;m<=4;m++){
	rsd[m+i*isize1+(ny-3)*jsize1+k*ksize1] = rsd[m+i*isize1+(ny-3)*jsize1+k*ksize1]
	  - dssp * (             u[m+i*isize1+(ny-5)*jsize1+k*ksize1]
		    - 4.0 * u[m+i*isize1+(ny-4)*jsize1+k*ksize1]
		    + 6.0 * u[m+i*isize1+(ny-3)*jsize1+k*ksize1]
		    - 4.0 * u[m+i*isize1+(ny-2)*jsize1+k*ksize1]  );
	rsd[m+i*isize1+(ny-2)*jsize1+k*ksize1] = rsd[m+i*isize1+(ny-2)*jsize1+k*ksize1]
                 - dssp * (             u[m+i*isize1+(ny-4)*jsize1+k*ksize1]
			   - 4.0 * u[m+i*isize1+(ny-3)*jsize1+k*ksize1]
			   + 5.0 * u[m+i*isize1+(ny-2)*jsize1+k*ksize1] );
      }      
    }
  }
      break;
      case 4: 
    
//---------------------------------------------------------------------
//   zeta-direction flux differences
//---------------------------------------------------------------------
  for(j=lower_bound2;j<=upper_bound2;j++){
    for(i=ist-1;i<=iend-1;i++){
      for(k=0;k<=nz-1;k++){
	flux[0+k*isize2] = u[3+i*isize1+j*jsize1+k*ksize1];
	u41 = u[3+i*isize1+j*jsize1+k*ksize1] * rho_i[i+j*jsize3+k*ksize3];

	q = qs[i+j*jsize3+k*ksize3];
	
	flux[1+k*isize2] = u[1+i*isize1+j*jsize1+k*ksize1] * u41 ;
	flux[2+k*isize2] = u[2+i*isize1+j*jsize1+k*ksize1] * u41 ;
	flux[3+k*isize2] = u[3+i*isize1+j*jsize1+k*ksize1] * u41 + c2 * (u[4+i*isize1+j*jsize1+k*ksize1]-q);
               flux[4+k*isize2] = ( c1 * u[4+i*isize1+j*jsize1+k*ksize1] - c2 * q ) * u41;
      }
      
      for(k=1;k<=nz - 2;k++){
	for(m=0;m<=4;m++){
	  rsd[m+i*isize1+j*jsize1+k*ksize1] =  rsd[m+i*isize1+j*jsize1+k*ksize1]
	    - tz2 * ( flux[m+(k+1)*isize2] - flux[m+(k-1)*isize2] );
	}
      }
      
      for(k=1;k<=nz-1;k++){
	tmp = rho_i[i+j*jsize3+k*ksize3];
	
	u21k = tmp * u[1+i*isize1+j*jsize1+k*ksize1];
	u31k = tmp * u[2+i*isize1+j*jsize1+k*ksize1];
	u41k = tmp * u[3+i*isize1+j*jsize1+k*ksize1];
	u51k = tmp * u[4+i*isize1+j*jsize1+k*ksize1];
	
	tmp = rho_i[i+j*jsize3+(k-1)*ksize3];
	
	u21km1 = tmp * u[1+i*isize1+j*jsize1+(k-1)*ksize1];
	u31km1 = tmp * u[2+i*isize1+j*jsize1+(k-1)*ksize1];
	u41km1 = tmp * u[3+i*isize1+j*jsize1+(k-1)*ksize1];
	u51km1 = tmp * u[4+i*isize1+j*jsize1+(k-1)*ksize1];
	
	flux[1+k*isize2] = tz3 * ( u21k - u21km1 );
	flux[2+k*isize2] = tz3 * ( u31k - u31km1 );
	flux[3+k*isize2] = (4.0/3.0) * tz3 * (u41k-u41km1);
	flux[4+k*isize2] = 0.50 * ( 1.0 - c1*c5 )
	  * tz3 * ( (Math.pow(u21k,2) + Math.pow(u31k,2) +Math.pow(u41k,2) )
		   - ( Math.pow(u21km1,2) + Math.pow(u31km1,2) +Math.pow(u41km1,2) ) )
	    + (1.0/6.0)
	      * tz3 * ( Math.pow(u41k,2) - Math.pow(u41km1,2) )
		+ c1 * c5 * tz3 * ( u51k - u51km1 );
            }
      
      for(k=1;k<=nz - 2;k++){
	rsd[0+i*isize1+j*jsize1+k*ksize1] = rsd[0+i*isize1+j*jsize1+k*ksize1]
	  + dz1 * tz1 * (            u[0+i*isize1+j*jsize1+(k-1)*ksize1]
			 - 2.0 * u[0+i*isize1+j*jsize1+k*ksize1]
			 +           u[0+i*isize1+j*jsize1+(k+1)*ksize1] );
	rsd[1+i*isize1+j*jsize1+k*ksize1] = rsd[1+i*isize1+j*jsize1+k*ksize1]
	  + tz3 * c3 * c4 * ( flux[1+(k+1)*isize2] - flux[1+k*isize2] )
	    + dz2 * tz1 * (            u[1+i*isize1+j*jsize1+(k-1)*ksize1]
			   - 2.0 * u[1+i*isize1+j*jsize1+k*ksize1]
			   +           u[1+i*isize1+j*jsize1+(k+1)*ksize1] );
	rsd[2+i*isize1+j*jsize1+k*ksize1] = rsd[2+i*isize1+j*jsize1+k*ksize1]
	  + tz3 * c3 * c4 * ( flux[2+(k+1)*isize2] - flux[2+k*isize2] )
	    + dz3 * tz1 * (            u[2+i*isize1+j*jsize1+(k-1)*ksize1]
			   - 2.0 * u[2+i*isize1+j*jsize1+k*ksize1]
			   +           u[2+i*isize1+j*jsize1+(k+1)*ksize1] );
	rsd[3+i*isize1+j*jsize1+k*ksize1] = rsd[3+i*isize1+j*jsize1+k*ksize1]
                + tz3 * c3 * c4 * ( flux[3+(k+1)*isize2] - flux[3+k*isize2] )
		  + dz4 * tz1 * (            u[3+i*isize1+j*jsize1+(k-1)*ksize1]
				 - 2.0 * u[3+i*isize1+j*jsize1+k*ksize1]
				 +           u[3+i*isize1+j*jsize1+(k+1)*ksize1] );
	rsd[4+i*isize1+j*jsize1+k*ksize1] = rsd[4+i*isize1+j*jsize1+k*ksize1]
	  + tz3 * c3 * c4 * ( flux[4+(k+1)*isize2] - flux[4+k*isize2] )
	    + dz5 * tz1 * (            u[4+i*isize1+j*jsize1+(k-1)*ksize1]
			   - 2.0 * u[4+i*isize1+j*jsize1+k*ksize1]
                                   +           u[4+i*isize1+j*jsize1+(k+1)*ksize1] );
      }
      
//---------------------------------------------------------------------
//   fourth-order dissipation
//---------------------------------------------------------------------
    for(m=0;m<=4;m++){
      rsd[m+i*isize1+j*jsize1+1*ksize1] = rsd[m+i*isize1+j*jsize1+1*ksize1]
        - dssp * ( + 5.0 * u[m+i*isize1+j*jsize1+1*ksize1]
		   - 4.0 * u[m+i*isize1+j*jsize1+2*ksize1]
		   +       u[m+i*isize1+j*jsize1+3*ksize1] );
      rsd[m+i*isize1+j*jsize1+2*ksize1] = rsd[m+i*isize1+j*jsize1+2*ksize1]
        - dssp * ( - 4.0 * u[m+i*isize1+j*jsize1+1*ksize1]
        	   + 6.0 * u[m+i*isize1+j*jsize1+2*ksize1]
		   - 4.0 * u[m+i*isize1+j*jsize1+3*ksize1]
		   +       u[m+i*isize1+j*jsize1+4*ksize1] );
    }
      
      for(k=3;k<=nz - 4;k++){
	for(m=0;m<=4;m++){
	  rsd[m+i*isize1+j*jsize1+k*ksize1] = rsd[m+i*isize1+j*jsize1+k*ksize1]
	    - dssp * (            u[m+i*isize1+j*jsize1+(k-2)*ksize1]
		      - 4.0 * u[m+i*isize1+j*jsize1+(k-1)*ksize1]
		      + 6.0 * u[m+i*isize1+j*jsize1+k*ksize1]
		      - 4.0 * u[m+i*isize1+j*jsize1+(k+1)*ksize1]
		      +           u[m+i*isize1+j*jsize1+(k+2)*ksize1] );
	}
      }
      
      for(m=0;m<=4;m++){
	rsd[m+i*isize1+j*jsize1+(nz-3)*ksize1] = rsd[m+i*isize1+j*jsize1+(nz-3)*ksize1]
	  - dssp * (             u[m+i*isize1+j*jsize1+(nz-5)*ksize1]
		    - 4.0 * u[m+i*isize1+j*jsize1+(nz-4)*ksize1]
		    + 6.0 * u[m+i*isize1+j*jsize1+(nz-3)*ksize1]
		    - 4.0 * u[m+i*isize1+j*jsize1+(nz-2)*ksize1]  );
	rsd[m+i*isize1+j*jsize1+(nz-2)*ksize1] = rsd[m+i*isize1+j*jsize1+(nz-2)*ksize1]
	  - dssp * (             u[m+i*isize1+j*jsize1+(nz-4)*ksize1]
		    - 4.0 * u[m+i*isize1+j*jsize1+(nz-3)*ksize1]
		    + 5.0 * u[m+i*isize1+j*jsize1+(nz-2)*ksize1] );
      }
    }
  }
    break;
    
    }
    state++;
    if(state==5)state=1;
  } 
}












