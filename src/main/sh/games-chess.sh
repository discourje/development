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
TMP=$(mktemp -d -t games-chess-$DATE.XXX)
OUT=$TMP"/out.txt"
ERR=$TMP"/err.txt"
echo $TMP

#
# Repetitions and programs
#

I=1

GAMES_CHESS_40_15000_CLJ=1
GAMES_CHESS_40_30000_CLJ=1
GAMES_CHESS_40_45000_CLJ=1
GAMES_CHESS_40_60000_CLJ=1
GAMES_CHESS_40_15000_DCJ=1
GAMES_CHESS_40_30000_DCJ=1
GAMES_CHESS_40_45000_DCJ=1
GAMES_CHESS_40_60000_DCJ=1

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
STOCKFISH=\"/src/main/java/discourje/examples/games/impl/chess/stockfish-mac\"

#
# Runs: clj
#

if [ "$GAMES_CHESS_40_15000_CLJ" -eq "1" ]; then
  echo games.chess/40/15000/clj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 15000} 1>> $OUT 2>> $ERR;
  done
fi

if [ "$GAMES_CHESS_40_30000_CLJ" -eq "1" ]; then
  echo games.chess/40/30000/clj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 30000} 1>> $OUT 2>> $ERR;
  done
fi

if [ "$GAMES_CHESS_40_45000_CLJ" -eq "1" ]; then
  echo games.chess/40/45000/clj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 45000} 1>> $OUT 2>> $ERR;
  done
fi

if [ "$GAMES_CHESS_40_60000_CLJ" -eq "1" ]; then
  echo games.chess/40/60000/clj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :clj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 60000} 1>> $OUT 2>> $ERR;
  done
fi

#
# Runs: dcj
#

if [ "$GAMES_CHESS_40_15000_DCJ" -eq "1" ]; then
  echo games.chess/40/15000/dcj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 15000} 1>> $OUT 2>> $ERR;
  done
fi

if [ "$GAMES_CHESS_40_30000_DCJ" -eq "1" ]; then
  echo games.chess/40/30000/dcj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 30000} 1>> $OUT 2>> $ERR;
  done
fi

if [ "$GAMES_CHESS_40_45000_DCJ" -eq "1" ]; then
  echo games.chess/40/45000/dcj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 45000} 1>> $OUT 2>> $ERR;
  done
fi

if [ "$GAMES_CHESS_40_60000_DCJ" -eq "1" ]; then
  echo games.chess/40/60000/dcj
  for i in $(seq $I); do
    time $JAVA -jar $JAR {:run :dcj :timeout $TIMEOUT} games.chess {:stockfish $STOCKFISH, :turns-per-player 40, :time-per-player 60000} 1>> $OUT 2>> $ERR;
  done
fi

#
# Bookkeeping
#

cp -r $TMP .