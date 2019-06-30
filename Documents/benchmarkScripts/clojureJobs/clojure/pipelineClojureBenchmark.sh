#!/usr/bin/env bash

java -jar $1 -c pl -a 2 -i 1000 -o $2
java -jar $1 -c pl -a 3 -i 1000 -o $2
java -jar $1 -c pl -a 4 -i 1000 -o $2
java -jar $1 -c pl -a 6 -i 1000 -o $2
java -jar $1 -c pl -a 8 -i 1000 -o $2
java -jar $1 -c pl -a 12 -i 1000 -o $2
java -jar $1 -c pl -a 16 -i 1000 -o $2
java -jar $1 -c pl -a 24 -i 1000 -o $2
java -jar $1 -c pl -a 32 -i 1000 -o $2