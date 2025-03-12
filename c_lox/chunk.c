#include <stdlib.h>
#include <stdio.h>
#include "chunk.h"
#include "memory.h"
#include "vm.h"

void initChunk(Chunk* chunk) {
    chunk->capacity = 0;
    chunk->count = 0;
    chunk->code = NULL;
    chunk->constants.count = 0;
    chunk->constants.capacity = 0;
    chunk->constants.values = NULL;
    initLineArray(&chunk->lines);
}

void freeChunk(Chunk* chunk) {
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    freeLineArray(&chunk->lines);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}

void initLineArray(LineArray* lines) {
    lines->count = 0;
    lines->capacity = 0;
    lines->lineNumbers = NULL;
}

void freeLineArray(LineArray* lines) {
    FREE_ARRAY(int, lines->lineNumbers, lines->capacity);
    initLineArray(lines);
}

void addLine(LineArray* lines, int line) {
    if (lines->count > 0 && lines->lineNumbers[lines->count - 1] == line) {
        return;
    }

    if (lines->count >= lines->capacity) {
        int oldCapacity = lines->capacity;
        lines->capacity = GROW_CAPACITY(oldCapacity);
        lines->lineNumbers = GROW_ARRAY(int, lines->lineNumbers, oldCapacity, lines->capacity);
    }

    lines->lineNumbers[lines->count++] = line;
}

void writeChunk(Chunk* chunk, uint8_t byte, int line) {
    if (chunk->capacity <= chunk->count + 1) {
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
    }

    chunk->code[chunk->count] = byte;
    addLine(&chunk->lines, line);
    chunk->count++;
}

void writeConstant(Chunk* chunk, Value value, int line) {
    long curr = AS_NUMBER(value);
    if (curr > 256) {
        Value val1 = NUMBER_VAL((curr & 255));
        curr >>= 8;
        Value val2 = NUMBER_VAL((curr & 255));
        curr >>= 8;
        Value val3 = NUMBER_VAL((curr & 255));

        int const1 = addConstant(chunk, val1);
        int const2 = addConstant(chunk, val2);
        int const3 = addConstant(chunk, val3);

        writeChunk(chunk, OP_CONSTANT_LONG, line);
        writeChunk(chunk, const1, line);
        writeChunk(chunk, const2, line);
        writeChunk(chunk, const3, line);
    } else {
        writeChunk(chunk, OP_CONSTANT, line);
        int constant = addConstant(chunk, value);
        writeChunk(chunk, constant, line);
    }
}

int addConstant(Chunk* chunk, Value value) {
    push(value);
    writeValueArray(&chunk->constants, value);
    pop();
    return chunk->constants.count - 1;
}