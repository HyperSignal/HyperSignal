
#define _POSIX_C_SOURCE 199309L
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>
#include <time.h>

#include <ctype.h>
#include <stdlib.h>

#include <e-hal.h>
#include "tools.h"
#include "common.h"

#define _BufOffset (0x01000000)

typedef int bool;
#define true 1
#define false 0

unsigned char Data[] = {
	157,83 ,153,147,223,114,248,200,
	120,185,30 ,86 ,50 ,28 ,29 ,180,
	153,169,225,146,20 ,115,229,108,
	50 ,206,89 ,51 ,89 ,99 ,19 ,112,
	74 ,9  ,232,185,36 ,54 ,210,66 ,
	151,206,52 ,100,232,163,191,135,
	125,81 ,122,246,20 ,197,194,210,
	87 ,122,11 ,16 ,163,90 ,1  ,152 
};

unsigned char *colortable;

//#define OUT_X 384
//#define OUT_Y 384

unsigned int OUT_X = 384;
unsigned int OUT_Y = 384;

// Crop positions
unsigned int A = 0;
unsigned int B = 0;
unsigned int C = 384;
unsigned int D = 384;

bool cosine_mode	= 	false;
bool pipestdout 	= 	false;
bool pipestdin 		= 	false;
bool verbose		=	false;
bool colorize		=	false;

void USleep(int microseconds)	{
	long nanoseconds = microseconds * 1000L;	//	To nano
    struct timespec s;
    s.tv_sec = 0;
    s.tv_nsec = nanoseconds;
    nanosleep(&s, NULL);
}

void ShowHelp()	{
	fprintf(stderr, "HyperSignal Tile Generator for Epiphany V" B_PROG_VERSION " - Build: " B_BUILD_VERSION "\n");
	fprintf(stderr, "Build on " B_HOSTNAME " at " B_DATE "\n");
	fprintf(stderr, "Options: \n");
	fprintf(stderr, "\tCROP Parameters\n");
	fprintf(stderr, "\t\tCrop Points \t\tP0(A,B) P1(C,D)\n");
	fprintf(stderr, "\t\t-A VALUE\t\t Crop A Position \n");
	fprintf(stderr, "\t\t-B VALUE\t\t Crop B Position\n");
	fprintf(stderr, "\t\t-C VALUE\t\t Crop C Position\n");
	fprintf(stderr, "\t\t-D VALUE\t\t Crop D Position\n");
	fprintf(stderr, "\tConfiguration Parameters\n");
	fprintf(stderr, "\t\t-t      \t\t Colorize Output using colortable.mat\n");
	fprintf(stderr, "\t\t-c      \t\t Cosine Mode Interpolation (DEFAULT: Bilinear)\n");
	fprintf(stderr, "\t\t-w      \t\t Tile Width  ( Excluding CROP )\n");
	fprintf(stderr, "\t\t-h      \t\t Tile Height ( Excluding CROP )\n");
	fprintf(stderr, "\t\t-k      \t\t Sample Input from STDIN\n");
	fprintf(stderr, "\t\t-z      \t\t Redirect output to STDOUT (Default save to output.mat)\n");
	fprintf(stderr, "\n\n");
	exit(1);
}

int main(int argc, char *argv[])
{
	e_platform_t platform;
	e_epiphany_t dev;
	e_mem_t emem;
	char emsg[BUF_SIZE];
	int curx = 0, cury = 0;
	char flag = 0;
	int workid = 0;
	struct timeval start;
	struct timeval end;
	long elapsedTime;

	int c;
	extern char *optarg;
	extern int optind, optopt, opterr;
	char *filename;

	while ((c = getopt(argc, argv, "A:B:C:D:ckztvw:h:")) != -1) {
        switch(c) {
        case 'c':
        	if(verbose)
           		fprintf(stderr,"Enabling Cosine Mode\n");
            cosine_mode = true;
            break;
        case 'k':
            if(verbose)
           		fprintf(stderr,"Input from stdin\n");
            pipestdin = true;
            break;
        case 'z':
            if(verbose)
           		fprintf(stderr,"Output for stdout\n");
            pipestdout = true;
            break;
        case 'w':
        	OUT_X = atoi(optarg);
        	break;
    	case 'h':
    		OUT_Y = atoi(optarg);
    		break;
		case 'v':
			verbose = true;
			break;
		case 't':
			colorize = true;
			break;	
		case 'A':
			A = atoi(optarg);
			break;
		case 'B':
			B = atoi(optarg);
			break;
		case 'C':
			C = atoi(optarg);
			break;
		case 'D':
			D = atoi(optarg);
			break;
        case ':': 
        	fprintf(stderr,"Option -%c requires an operand\n", optopt);
            break;
        case '?':
        	ShowHelp();
        	break;
        }
	}
	// (A,B) - (C,D)
	//unsigned char outdata[OUT_X*OUT_Y];
	int WIDTH = C-A;
	int HEIGHT = D-B;
	unsigned char outdata[WIDTH*HEIGHT];
	unsigned char *outcolor;

	const unsigned int SX = OUT_X / OUTPUT_WIDTH;
	const unsigned int SY = OUT_Y / OUTPUT_HEIGHT;
	const float bx = (SAMPLE_WIDTH ) / (float)SX;	
	const float by = (SAMPLE_HEIGHT) / (float)SY;

	if(verbose)	{
		fprintf(stderr, "HyperSignal Tile Generator for Epiphany V" B_PROG_VERSION " - Build: " B_BUILD_VERSION "\n");
		fprintf(stderr, "Build on " B_HOSTNAME " at " B_DATE "\n");		
	}

   	if(colorize)	{
   		if(verbose)
			fprintf(stderr, "Reading color table\n");
   		FILE *f = fopen("colortable.mat","rb");
   		colortable = malloc(sizeof(char)*256*4);
   		fread(colortable, sizeof(char), 256*4, f);
   		fclose(f);
   	}

	if(verbose)
   		fprintf(stderr, "Tile Size(%03d,%03d)\n",WIDTH, HEIGHT);
	if(cosine_mode)	{
		if(verbose)
   			fprintf(stderr, "Building cosine table\n");
		BuildCosTable();
	}

	if(pipestdin)	{
		freopen (NULL,"rb",stdin);
		int c = fread(Data, sizeof(char), SAMPLE_WIDTH*SAMPLE_HEIGHT, stdin);	
		if(verbose)
           		fprintf(stderr, "Read %d bytes from stdin. \n", c);	
		freopen (NULL,"r",stdin);
	}

	gettimeofday(&start, NULL);
	HSWork works[SX*SY]; 
	if(verbose)
   		fprintf(stderr,"Preparing works. (%02d,%02d) \n",SX,SY);

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

	//	Alocate Works and cosine table Memory at Shared DRAM Space
	e_alloc(&emem, _BufOffset, sizeof(HSWork)*SX*SY + sizeof(float) * MAX_CIRCLE_ANGLE);

	//	Writes the works to that memory
	e_write(&emem, 0, 0,  0, (void *)&works, sizeof(HSWork)*SX*SY);
	//	Writes cosines to memory
	if(argc > 1)
		e_write(&emem, 0, 0, sizeof(HSWork)*SX*SY, (void *)fast_cossin_table, sizeof(float) * MAX_CIRCLE_ANGLE);

	//	Open the Workgroup witn SX x SY cores
	e_open(&dev, 0, 0, SY, SX);

	//	Resets the group.
	e_reset_group(&dev);

	// Load the device program onto the selected core
	if(argc > 1)
		e_load_group("e_test_bicosine.srec", &dev, 0, 0, SY, SX, E_FALSE);
	else
		e_load_group("e_test_bilinear.srec", &dev, 0, 0, SY, SX, E_FALSE);

	for(int y=0;y<SY;y++)	{
		for(int x=0;x<SX;x++)	{
			e_write(&dev, y, x, CURRENT_POS + sizeof(int) * 2, &SX, sizeof(unsigned));
			e_write(&dev, y, x, CURRENT_POS + sizeof(int) * 3, &SY, sizeof(unsigned));		
		}
	}

	if(verbose)
   		fprintf(stderr, "Starting program\n");
	e_start_group(&dev);
	if(verbose)
   		fprintf(stderr, "Waiting...\n");

	//	Every 2 seconds, read the works from Shared DRAM and see if there is any change.
	while(1)	{
		flag = 1;
		e_read(&emem, 0, 0, 0, &works, sizeof(works));
		for(int y=0;y<SY;y++)	{
			for(int x=0;x<SX;x++)	{
				flag &= works[y*SX+x].done;
				e_read(&dev, y, x, CURRENT_POS, &curx, 4);
				e_read(&dev, y, x, CURRENT_POS+sizeof(int), &cury, 4);
				if(verbose)
           			fprintf(stderr, "Core %02d: (%03d,%03d)(%d)(%d)\n",y*SX+x,curx,cury, works[y*SX+x].done, works[y*SX+x].error);				
			}
		}
		if(flag)	
			break;

		if(verbose)
       		for(int i=0;i<SX*SY;i++)
				fprintf(stderr, "\033[F");
		USleep(1);
		//sleep(1);
	}
	gettimeofday(&end, NULL);
	elapsedTime = 1000 * (end.tv_sec - start.tv_sec) + (end.tv_usec - start.tv_usec) / 1000;
	if(verbose)
   		fprintf(stderr, "Finished work in %ld ms!\n", elapsedTime);

	//	Work is finished, lets get an updated version of the works
	e_read(&emem, 0, 0, 0, &works, sizeof(works));
	if(verbose)
   		fprintf(stderr, "Consolidating data\n");

   	if(colorize)
   		outcolor = malloc(WIDTH*HEIGHT*4*sizeof(char));
   	// (A,B) - (C,D)
   	
	for(int y=0;y<SY;y++)	{
		for(int x=0;x<SX;x++)	{
			int p = y*SX+x;
			for(int j=0;j<OUTPUT_HEIGHT;j++)	{
				for(int i=0;i<OUTPUT_WIDTH;i++)	{
					int tx 	=	i + x * (OUT_X/SX);
					int ty 	= 	j + y * (OUT_Y/SY);
					if(tx >= A && tx < C && ty >= B && ty < D)	{
						int pt 	=	(ty-B)*(WIDTH)+(tx-A);
						unsigned char val = works[p].output[j*OUTPUT_WIDTH+i];
						if(colorize)	{
							outcolor[pt*4] 		= 	colortable[val*4];
							outcolor[pt*4+1] 	= 	colortable[val*4+1];
							outcolor[pt*4+2] 	= 	colortable[val*4+2];
							outcolor[pt*4+3] 	= 	colortable[val*4+3];
						}else
							outdata[pt] = val;
					}	
				}
			}
		}
	}
	gettimeofday(&end, NULL);
	elapsedTime = 1000 * (end.tv_sec - start.tv_sec) + (end.tv_usec - start.tv_usec) / 1000;
	if(verbose)
   		fprintf(stderr, "Total time %ld ms!\n", elapsedTime);

	//	We are only using work 0, so only that we will write to the output.
	if(pipestdout)	{
		freopen (NULL,"wb",stdout);
		if(colorize)	
			fwrite(outcolor, sizeof(char),4*WIDTH*HEIGHT,stdout);
		else
			fwrite(outdata,sizeof(char),WIDTH*HEIGHT,stdout);
		fclose(stdout);
	}else{
		if(verbose)
			fprintf(stderr, "Saving output matrix to output.mat\n");
		FILE *f = fopen("/media/LINUX_DATA/MathStudies/HyperSignal/Parallella/output.mat","wb");
		if(colorize)	
			fwrite(outcolor, sizeof(char),4*WIDTH*HEIGHT,f);
		else
			fwrite(outdata,sizeof(char),WIDTH*HEIGHT,f);
		fclose(f);
	}
	if(verbose)
		fprintf(stderr, "Done\n");
	e_close(&dev);

	e_free(&emem);
	e_finalize();
	return 0;
}
