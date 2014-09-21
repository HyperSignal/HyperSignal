#!/bin/bash

echo "This is a test build of interpolator"

echo "Cleaning"
rm -fr test/*

echo "Compiling test"
gcc -c interpolate.c -std=c99 -o test/interpolate.o

echo "Linking all together"
gcc -o interpolate  test/*.o -lm