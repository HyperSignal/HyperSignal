/*
  e_hello_world.c

  Copyright (C) 2012 Adapteva, Inc.
  Contributed by Yaniv Sapir <yaniv@adapteva.com>

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program, see the file COPYING.  If not, see
  <http://www.gnu.org/licenses/>.
*/

// This is the DEVICE side of the Hello World example.
// The host may load this program to any eCore. When
// launched, the program queries the CoreID and prints
// a message identifying itself to the shared external
// memory buffer.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#include "common.h"
#include "tools.h"
#include "e_lib.h"

#define BUFSTART (0x8f000000)

unsigned *vals = (unsigned *)CURRENT_POS; //  4 byte array: CURX, CURY, SX, SY - Last two written by HOST

int main(void) {
  e_coreid_t coreid = e_get_coreid();
  float x2, y2;
  unsigned char val;
  unsigned row, col;
  unsigned sx, sy;
  float costable[MAX_CIRCLE_ANGLE];

  sx = vals[2];
  sy = vals[3];

  e_coords_from_coreid(coreid, &row, &col); 


  char workid = row * sx + col;

  HSWork *ext_works = (void *) BUFSTART;
  HSWork work;

  e_dma_copy(&work, &ext_works[workid], sizeof(HSWork));
  e_dma_copy(&costable, &ext_works[sx*sy], sizeof(float) * MAX_CIRCLE_ANGLE);

  int *curx = (int *)CURRENT_POS, *cury = (int *)(CURRENT_POS + 1);
  vals[0] = 0;
  vals[1] = 0;

  const float sw_ow = ((SAMPLE_WIDTH-1) /  (float)OUTPUT_WIDTH ) / work.sx;
  const float sh_oh = ((SAMPLE_HEIGHT-1) / (float)OUTPUT_HEIGHT) /  work.sy;

  for(int y=0;y<OUTPUT_HEIGHT;y++)  {
    vals[1] = y;
    for(int x=0;x<OUTPUT_WIDTH;x++) {
      vals[0] = x;
      x2 = sw_ow * x;
      y2 = sh_oh * y;
      x2 += work.x0;
      y2 += work.y0;
      #ifdef BICOSINE_INTERPOLATION
      val = bicosine(work.sample, x2, y2, SAMPLE_WIDTH, costable);
      #else
      val = bilinear(work.sample,x2,y2,SAMPLE_WIDTH);
      #endif
      setval(work.output,x,y,OUTPUT_WIDTH,val);
    }
  }
  
  vals[0] = -1;
  vals[1] = -1;
  work.done = 1;
  work.error = 0;

  e_dma_copy(&ext_works[workid], &work, sizeof(HSWork));
  return EXIT_SUCCESS;
}