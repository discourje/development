#!/usr/bin/env bash

lein with-profile tacas2020 uberjar

cp src/discourje/examples/tacas2020/misc/chess/stockfish-linux target/
cp src/discourje/examples/tacas2020/misc/chess/stockfish-mac target/
cp src/discourje/examples/tacas2020/misc/chess/stockfish-win32.exe target/
cp src/discourje/examples/tacas2020/misc/chess/stockfish-win64.exe target/
