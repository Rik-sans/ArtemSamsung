//
// search.h
//
//      Copyright (c) Microsoft Corporation. All rights reserved.
//
// Declarations of functions for sorting and searching.
//
#pragma once
#ifndef _INC_SEARCH // include guard for 3rd party interop
#define _INC_SEARCH

#include <search.h>
#include <stdlib.h>
#include "facecube.h"
#include "coordcube.h"

#define MIN(a, b) (((a)<(b))?(a):(b))
#define MAX(a, b) (((a)>(b))?(a):(b))

typedef struct {
    int ax[32];       // The axis of the move
    int po[32];       // The power of the move
    int flip[32];     // phase1 coordinates
    int twist[32];
    int parity[32];   // phase2 coordinates
    int slice[32];
    int URFtoDLF[32];
    int FRtoBR[32];
    int URtoUL[32];
    int UBtoDF[32];
    int minDistPhase1[32];
    int minDistPhase2[32];
    int URtoDF[32];
} search_t;

// Объявления функций
char* solutionToString(search_t* search, int length, int depthPhase1);
char* solution(char* facelets, int maxDepth, long timeOut, int useSeparator, const char* cache_dir);
int totalDepth(search_t* search, int depthPhase1, int maxDepth);
void patternize(char* facelets, char* pattern, char* patternized);
#endif // _INC_SEARCH
