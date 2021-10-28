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
TMP=$(mktemp -d -t npb3-$DATE.XXX)
OUT=$TMP"/out.txt"
ERR=$TMP"/err.txt"
echo $TMP

#
# Repetitions and programs
#

I=1

NPB3_CG_CLJ=1
NPB3_FT_CLJ=1
NPB3_IS_CLJ=1
NPB3_MG_CLJ=1
NPB3_CG_DCJ=1
NPB3_FT_DCJ=1
NPB3_IS_DCJ=1
NPB3_MG_DCJ=1

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
CLASS=w
VERBOSE=false

#
# Runs: clj
#

if [ "$NPB3_CG_CLJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.cg/$k/clj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} npb3.cg {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$NPB3_FT_CLJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.ft/$k/clj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} npb3.ft {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$NPB3_IS_CLJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.is/$k/clj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} npb3.is {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$NPB3_MG_CLJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.mg/$k/clj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} npb3.mg {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

#
# Runs: dcj
#

if [ "$NPB3_CG_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.cg/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} npb3.cg {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$NPB3_FT_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.ft/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} npb3.ft {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$NPB3_IS_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.is/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} npb3.is {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

if [ "$NPB3_MG_DCJ" -eq "1" ]; then
  for k in 2 4 6 8 10 12 14 16; do
    echo npb3.mg/$k/dcj
    for i in $(seq $I); do
      time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} npb3.mg {:k $k, :class $CLASS, :verbose $VERBOSE} 1>> $OUT 2>> $ERR;
    done
  done
fi

#
# Bookkeeping
#

cp -r $TMP .