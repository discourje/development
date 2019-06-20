#!/bin/bash
# Set job requirements
#SBATCH -N 1
#SBATCH -t 5:00
#SBATCH --mem=90G

#loading modules
module load Java

#Create output directory on scratch
mkdir "$TMPDIR"/benchmarks

# ----
# This part is only useful for programs that use OpenMP for multithreading
# We can explicitely set the number of threads the program should use.
# Note: nproc --all returns the number of cores in the system.
export OMP_NUM_THREADS=`nproc --all`
# ----

# We can enable core affinity, to ensure the operating system keeps a given thread running on the same core.
# This often improves performance, but it depends on the application, so it is best to experiment with it.
module load paffinity

# Execute program located in $HOME
srun clojure/scattergatherClojureBenchmark.sh $HOME/discourje-0.1.0-standalone.jar "$TMPDIR"/benchmarks

#Copy output directory from scratch to home
cp -r "$TMPDIR"/benchmarks $HOME