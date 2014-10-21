#!/bin/bash

#cd /home/lucas/hypersignal/Includes/
if [ -f ".lock-update" ] 
then
	echo "Já rodando..."
else
	touch .lock-update
	python tilemaker.py #2>>1 1>> /dev/null
	#if	[ -d "VIVO" -o -d "CLARO BR" -o -d "TIM" -o -d "OI" ]
	#then
		#scp -r VIVO CLARO\ BR TIM OI root@10.0.5.20:/var/www/hstiles/
	#fi
	#rm -fr VIVO CLARO\ BR TIM OI
	rm .lock-update
fi
