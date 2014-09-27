#!/bin/bash

set -e

ESDK=${EPIPHANY_HOME}
ELIBS=${ESDK}/tools/host/lib
EINCS=${ESDK}/tools/host/include
ELDF=${ESDK}/bsps/current/fast.ldf

SCRIPT=$(readlink -f "$0")
EXEPATH=$(dirname "$SCRIPT")
cd $EXEPATH

CROSS_PREFIX=
case $(uname -p) in
	arm*)
		# Use native arm compiler (no cross prefix)
		CROSS_PREFIX=
		;;
	   *)
		# Use cross compiler
		CROSS_PREFIX="arm-linux-gnueabihf-"
		;;
esac

if [ ! -d Build/epiphany/ ]
then
	mkdir -p Build/epiphany/
fi 

rm -fr Build/epiphany/*

echo "Compiling python module"

sudo -E python setup.py install

echo "Compiling tools"
e-gcc -T ${ELDF} -std=c99  -c e_src/tools.c -std=c99 -o Build/epiphany/tools.o -le-lib

# Build HOST side application
${CROSS_PREFIX}gcc -std=c99  e_src/test.c -o Build/epiphany/test.elf -I ${EINCS} -L ${ELIBS} -le-hal -lm #-le-loader

# Build DEVICE side program
e-gcc -T ${ELDF} -std=c99  e_src/e_test.c Build/epiphany/*.o -o Build/epiphany/e_test.elf -le-lib

# Convert ebinary to SREC file
e-objcopy --srec-forceS3 --output-target srec Build/epiphany/e_test.elf Build/epiphany/e_test.srec
