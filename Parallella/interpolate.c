#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <math.h>

#define null NULL

/*
	Expanding 4x4 Matrix to 320x320
*/
const unsigned char Data[]	=	{
	255	,	67	,	99	,	44,
	0	,	100	,	255	,	99,
	12	,	77	,	45	,	0,
	25 	,	8	,	66	,	33
};

const int 	MatrixW	=	4,
			MatrixH	=	4;

unsigned char val(const unsigned char *data, int x, int y, int mw)	{
	int p = y * mw + x;
	return data[p];
}

unsigned char bilinear(const unsigned char *data, double x, double y, int mw)	{	
	int rx = floor(x);
	int ry = floor(y);
	double fracX = x - rx;
	double fracY = y - ry;
	double invfracX = 1 - fracX;
	double invfracY = 1 - fracY;
	return ( val(data,rx,ry,mw) * invfracX + val(data,rx+1,ry,mw) * fracX) * invfracY + ( val(data,rx,ry+1,mw) * invfracX + val(data,rx+1,ry+1,mw) * fracX) * fracY;
}

void setval(unsigned char *data, int x, int y, int mw, unsigned char value)	{
	int p = y * mw + x;
	data[p] = value;
}

int main()	{
	unsigned char *out;

	FILE * f;
	f = fopen("initial.mat", "w");
	printf("Writting Initial Matrix to initial.mat.\n");
	fwrite(Data,sizeof(unsigned char), MatrixW*MatrixH, f);
	fclose(f);
	printf("Interpolating to 320x320.\n");

	out = malloc(sizeof(unsigned char) * 320 * 320);	//	Allocate output array
	if(out != null)	{
		//	Interpolate
		int mapdx = 320,
			mapdy = 320;
		for(int y=0;y<mapdy;y++)	{
			for(int x=0;x<mapdx;x++)	{
				printf("Doing %d,%d\n",x,y);
				double x2 = ( (double)x / mapdx) * (MatrixW-1);
				double y2 = ( (double)y / mapdy) * (MatrixH-1);
				setval(out,x,y,mapdx,bilinear(Data,x2,y2,MatrixW));
			}
		}
	}else{
		printf("Error: Not allocated!\n");
	}

	if(out != NULL)	{
		printf("Saving output matrix to output.mat\n");
		f = fopen("output.mat","w");
		fwrite(out,sizeof(unsigned char),320*320,f);
		fclose(f);
	}
	printf("Done!");
	return 0;
}