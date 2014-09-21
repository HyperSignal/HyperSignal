#!/usr/bin/env python

import Image, numpy as np, math

def hsv2rgb(h, s, v):
    h = float(h)
    s = float(s)
    v = float(v)
    h60 = h / 60.0
    h60f = math.floor(h60)
    hi = int(h60f) % 6
    f = h60 - h60f
    p = v * (1 - s)
    q = v * (1 - f * s)
    t = v * (1 - (1 - f) * s)
    r, g, b = 0, 0, 0
    if hi == 0: r, g, b = v, t, p
    elif hi == 1: r, g, b = q, v, p
    elif hi == 2: r, g, b = p, v, t
    elif hi == 3: r, g, b = p, q, v
    elif hi == 4: r, g, b = t, p, v
    elif hi == 5: r, g, b = v, p, q
    r, g, b = int(r * 255), int(g * 255), int(b * 255)
    return r, g, b

def BuildImage(data,w,h):
	arr = np.zeros((w,h,3), dtype=np.uint8)
	for y in range(h):
		for x in range(w):
			p = y * w + x
			#r,g,b = hsv2rgb(360*(ord(data[p])/256.0),1,1)
			arr[y][x][0] = ord(data[p]) #r
			arr[y][x][1] = ord(data[p]) #g
			arr[y][x][2] = ord(data[p]) #b
	return Image.fromarray(arr)

f = open("initial.mat")
data = f.read()
f.close()
image = BuildImage(data,4,4)
image.save("initial.png")

f = open("output.mat")
data = f.read()
f.close()
image = BuildImage(data,320,320)
image.save("output.png")

