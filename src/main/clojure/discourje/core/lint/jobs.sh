COMMAND=$1
PROCS=$2
ITERS=$3

$COMMAND job.sh :dcj :cheung :ring $PROCS $ITERS
$COMMAND job.sh :dcj :cheung :star $PROCS $ITERS
$COMMAND job.sh :dcj :cheung :tree $PROCS $ITERS
$COMMAND job.sh :dcj :cheung :mesh-2d $PROCS $ITERS
$COMMAND job.sh :dcj :cheung :mesh-full $PROCS $ITERS

$COMMAND job.sh :mcrl2 :cheung :ring $PROCS $ITERS
$COMMAND job.sh :mcrl2 :cheung :star $PROCS $ITERS
$COMMAND job.sh :mcrl2 :cheung :tree $PROCS $ITERS
$COMMAND job.sh :mcrl2 :cheung :mesh-2d $PROCS $ITERS
$COMMAND job.sh :mcrl2 :cheung :mesh-full $PROCS $ITERS

$COMMAND job.sh :mcrl2-split :cheung :ring $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :cheung :star $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :cheung :tree $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :cheung :mesh-2d $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :cheung :mesh-full $PROCS $ITERS

$COMMAND job.sh :dcj :awerbuch :ring $PROCS $ITERS
$COMMAND job.sh :dcj :awerbuch :star $PROCS $ITERS
$COMMAND job.sh :dcj :awerbuch :tree $PROCS $ITERS
$COMMAND job.sh :dcj :awerbuch :mesh-2d $PROCS $ITERS
$COMMAND job.sh :dcj :awerbuch :mesh-full $PROCS $ITERS

$COMMAND job.sh :mcrl2 :awerbuch :ring $PROCS $ITERS
$COMMAND job.sh :mcrl2 :awerbuch :star $PROCS $ITERS
$COMMAND job.sh :mcrl2 :awerbuch :tree $PROCS $ITERS
$COMMAND job.sh :mcrl2 :awerbuch :mesh-2d $PROCS $ITERS
$COMMAND job.sh :mcrl2 :awerbuch :mesh-full $PROCS $ITERS

$COMMAND job.sh :mcrl2-split :awerbuch :ring $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :awerbuch :star $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :awerbuch :tree $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :awerbuch :mesh-2d $PROCS $ITERS
$COMMAND job.sh :mcrl2-split :awerbuch :mesh-full $PROCS $ITERS
