#!/usr/bin/env bash

java -jar $1 -d sg -a 2 -i 1000 -o $2
java -jar $1 -d sg -a 3 -i 1000 -o $2
java -jar $1 -d sg -a 4 -i 1000 -o $2
java -jar $1 -d sg -a 6 -i 1000 -o $2
java -jar $1 -d sg -a 8 -i 1000 -o $2
java -jar $1 -d sg -a 12 -i 1000 -o $2
java -jar $1 -d sg -a 16 -i 1000 -o $2
java -jar $1 -d sg -a 24 -i 1000 -o $2
java -jar $1 -d sg -a 32 -i 1000 -o $2