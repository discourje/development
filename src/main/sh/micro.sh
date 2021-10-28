#!/bin/bash
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=32
#SBATCH --partition=thin
#SBATCH --time=24:00:00

#
# Bookkeeping
#

DATE=$(date +%s)
TMP=$(mktemp -d -t micro-$DATE.XXX)
OUT=$TMP"/out.txt"
ERR=$TMP"/err.txt"
echo $TMP

#
# Repetitions and programs
#

I=1

MICRO_RING_UNBUFFERED_DCJ=1
MICRO_RING_BUFFERED_DCJ=1
MICRO_MESH_UNBUFFERED_DCJ=0
MICRO_MESH_BUFFERED_DCJ=0
MICRO_STAR_UNBUFFERED_OUTWARDS_DCJ=1
MICRO_STAR_UNBUFFERED_INWARDS_DCJ=1
MICRO_STAR_BUFFERED_OUTWARDS_DCJ=1
MICRO_STAR_BUFFERED_INWARDS_DCJ=1

#
# Commands
#

JAVA=java
JAR=target/discourje-examples.jar
TIMEFORMAT='%3R'

#
# Settings and inputs
#

TIMEOUT=300
N=4096

#
# Runs: Ring
#

if [ "$MICRO_RING_UNBUFFERED_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.ring/unbuffered/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.ring {:flags \#{:unbuffered}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$MICRO_RING_BUFFERED_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.ring/buffered/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.ring {:flags \#{:buffered}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

#
# Runs: Mesh
#

if [ "$MICRO_MESH_UNBUFFERED_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.mesh/unbuffered/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.mesh {:flags \#{:unbuffered}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$MICRO_MESH_BUFFERED_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.mesh/buffered/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.mesh {:flags \#{:buffered}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

#
# Runs: Star
#

if [ "$MICRO_STAR_UNBUFFERED_OUTWARDS_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.star/unbuffered,outwards/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.star {:flags \#{:unbuffered :outwards}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$MICRO_STAR_UNBUFFERED_INWARDS_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.star/unbuffered,inwards/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.star {:flags \#{:unbuffered :inwards}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$MICRO_STAR_BUFFERED_OUTWARDS_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.star/buffered,outwards/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.star {:flags \#{:buffered :outwards}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$MICRO_STAR_BUFFERED_INWARDS_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo micro.star/buffered,inwards/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} micro.star {:flags \#{:buffered :inwards}, :k $k, :n $N} 1>> $OUT 2>> $ERR;
    done
  done
fi

#
# Bookkeeping
#

cp -r $TMP .