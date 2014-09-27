#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

#include <e-hal.h>
#include "common.h"

#define _BufOffset (0x01000000)

const unsigned char Data[] = {
	157,83 ,153,147,223,114,248,200,
	120,185,30 ,86 ,50 ,28 ,29 ,180,
	153,169,225,146,20 ,115,229,108,
	50 ,206,89 ,51 ,89 ,99 ,19 ,112,
	74 ,9  ,232,185,36 ,54 ,210,66 ,
	151,206,52 ,100,232,163,191,135,
	125,81 ,122,246,20 ,197,194,210,
	87 ,122,11 ,16 ,163,90 ,1  ,152 
};

#define OUT_X 384
#define OUT_Y 384

int main(int argc, char *argv[])
{
	e_platform_t platform;
	e_epiphany_t dev;
	e_mem_t emem;
	char emsg[BUF_SIZE];
	int curx = 0, cury = 0;
	char flag = 0;
	int workid = 0;
	//time_t start, end;
	struct timeval start;
	struct timeval end;
	long elapsedTime;

	unsigned char outdata[OUT_X*OUT_Y];

	const unsigned int SX = OUT_X / OUTPUT_WIDTH;
	const unsigned int SY = OUT_Y / OUTPUT_HEIGHT;
	const float bx = (SAMPLE_WIDTH ) / (float)SX;	
	const float by = (SAMPLE_HEIGHT) / (float)SY;

	HSWork works[SX*SY]; 
	printf("Preparing works. (%02d,%02d) \n",SX,SY);
	for(int y=0;y<SY;y++)	{
		for(int x=0;x<SX;x++)	{
			int p = y*SX+x;
			works[p].workid = p;
			works[p].x0 = x * (bx-(1.0f/SX));
			works[p].y0 = y * (by-(1.0f/SY));
			works[p].sx = SX;
			works[p].sy = SY;
			works[p].done = 0;
			works[p].error = 0;
			memcpy(works[p].sample, Data, sizeof(unsigned char) * SAMPLE_WIDTH * SAMPLE_HEIGHT);
		}
	}
	//	Initializes Epiphany System
	e_init(NULL);
	e_reset_system();
	e_get_platform_info(&platform);

	//	Alocate Works Memory at Shared DRAM Space
	e_alloc(&emem, _BufOffset, sizeof(HSWork)*SX*SY);

	//	Writes the works to that memory
	e_write(&emem, 0, 0,  0, (void *)&works, sizeof(HSWork)*SX*SY);

	//	Open the Workgroup witn SX x SY cores
	e_open(&dev, 0, 0, SY, SX);

	//	Resets the group.
	e_reset_group(&dev);

	// Load the device program onto the selected core
	e_load_group("e_test.srec", &dev, 0, 0, SY, SX, E_FALSE);
	for(int y=0;y<SY;y++)	{
		for(int x=0;x<SX;x++)	{
			e_write(&dev, y, x, CURRENT_POS + sizeof(int) * 2, &SX, sizeof(unsigned));
			e_write(&dev, y, x, CURRENT_POS + sizeof(int) * 3, &SY, sizeof(unsigned));		
		}
	}

	printf("Starting program\n");
	e_start_group(&dev);
	printf("Waiting...\n");

	gettimeofday(&start, NULL);
	//	Every 2 seconds, read the works from Shared DRAM and see if there is any change.
	while(1)	{
		flag = 1;
		e_read(&emem, 0, 0, 0, &works, sizeof(works));
		for(int y=0;y<SY;y++)	{
			for(int x=0;x<SX;x++)	{
				flag &= works[y*SX+x].done;
				e_read(&dev, y, x, CURRENT_POS, &curx, 4);
				e_read(&dev, y, x, CURRENT_POS+sizeof(int), &cury, 4);
				printf("Core %02d: (%03d,%03d)(%d)(%d)\n",y*SX+x,curx,cury, works[y*SX+x].done, works[y*SX+x].error);				
			}
		}
		if(flag)	
			break;

		for(int i=0;i<SX*SY;i++)
			printf("\033[F");
		//sleep(1);
	}
	gettimeofday(&end, NULL);
	elapsedTime = 1000 * (end.tv_sec - start.tv_sec) + (end.tv_usec - start.tv_usec) / 1000;
	printf("Finished work in %ld ms!\n", elapsedTime);

	//	Work is finished, lets get an updated version of the works
	e_read(&emem, 0, 0, 0, &works, sizeof(works));
	printf("Consolidating data\n");
	for(int y=0;y<SY;y++)	{
		for(int x=0;x<SX;x++)	{
			int p = y*SX+x;
			for(int j=0;j<OUTPUT_HEIGHT;j++)	{
				for(int i=0;i<OUTPUT_WIDTH;i++)	{
					int tx = i + x * (OUT_X/SX);
					int ty = j + y * (OUT_Y/SY);
					outdata[ty*OUT_X+tx] = works[p].output[j*OUTPUT_HEIGHT+i];
				}
			}
		}
	}
	gettimeofday(&end, NULL);
	elapsedTime = 1000 * (end.tv_sec - start.tv_sec) + (end.tv_usec - start.tv_usec) / 1000;
	printf("Total time %ld ms!\n", elapsedTime);
	printf("Writting file\n");

	//	We are only using work 0, so only that we will write to the output.
	printf("Saving output matrix to output.mat\n");
	FILE *f = fopen("/media/LINUX_DATA/MathStudies/HyperSignal/Parallella/output.mat","w");
	fwrite(outdata,sizeof(unsigned char),OUT_X*OUT_Y,f);
	fclose(f);
	printf("Done\n");
	
	e_close(&dev);

	e_free(&emem);
	e_finalize();
	return 0;
}
