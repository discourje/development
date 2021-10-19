#!/bin/sh
#SBATCH -N 1
#SBATCH -p normal
#SBATCH -o results/out.%A
#SBATCH -e results/err.%A
#SBATCH -t 12:00:00

JAVA=/home/ssj/jdk-16.0.1/bin/java
JAR=/home/ssj/fse2021.jar

MCRL2_BIN=/home/ssj/mcrl2/build/stage/bin
MCRL2_TMP=/home/ssj/tmp
MCRL2_TMP=$(mktemp -d)

ENGINE=$1    # :dcj, :mcrl2, or :mcrl2-split
ALGORITHM=$2 # :cheung or :awerbuch
NETWORK=$3   # :ring

PROCS=$4
ITERS=$5

module load 2020
module load GCC/9.3.0

echo "ENGINE=$1, ALGORITHM=$2, NETWORK=$3, ITERS=$4"
echo
hostname
echo
cat /proc/cpuinfo
echo

for N in $(seq 2 1 $PROCS); do
    for I in $(seq 1 $ITERS); do
        $JAVA -jar $JAR {:mcrl2-bin \"$MCRL2_BIN\", :mcrl2-tmp \"$MCRL2_TMP\", :engine $ENGINE, :algorithm $ALGORITHM, :initiator 0, :network $NETWORK, :n $N}
    done
done
