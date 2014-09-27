#ifndef TOOLS_H
#define M_PI 3.14159265358979323846
#define TOOLS_H

//unsigned char bilinear(unsigned char *data, float x, float y, int mw);




static inline float min(float v1, float v2)	{
	return v1 > v2 ? v2 : v1;
}

static inline unsigned char val(const unsigned char *data, int x, int y, int mw)	{
	return data[y * mw + x];
}


static inline void setval(unsigned char *data, int x, int y, int mw, unsigned char value)	{
	data[y * mw + x] = value;
}

static inline unsigned char bilinear(const unsigned char *data, float x, float y, int mw)	{	
	int rx = (int)(x);
	int ry = (int)(y);
	float fracX = x - rx;
	float fracY = y - ry;
	float invfracX = 1.f - fracX;
	float invfracY = 1.f - fracY;
	
	unsigned char a = val(data,rx,ry,mw);
	unsigned char b = val(data,rx+1,ry,mw);
	unsigned char c = val(data,rx,ry+1,mw);
	unsigned char d = val(data,rx+1,ry+1,mw);
	
	return ( a * invfracX + b * fracX) * invfracY + ( c * invfracX + d * fracX) * fracY;
}
#endif