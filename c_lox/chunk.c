#include <stdlib.h>
#include <stdio.h>
#include "chunk.h"
#include "memory.h"

void initChunk(Chunk* chunk) {
    chunk->capacity = 0;
    chunk->count = 0;
    chunk->code = NULL;
    chunk->lines = NULL;
    initValueArray(&chunk->constants);
}

Lines* createNode(int line) {
    Lines* temp = malloc(sizeof(Lines));
    temp->line = line;
    temp->count = 1;
    temp->next = NULL;
    return temp;
}

Lines* insertLine(Lines* lines, int line) {
    if (lines == NULL) {
        return createNode(line);
    }
    Lines* curr = lines;
    while (curr->next != NULL) {
        if (curr->line == line) {
            curr->count++;
            return lines;
        }
        curr = curr->next;
    }
    if (curr->line == line) {
        curr->count++;
        return lines;
    }
    curr->next = createNode(line);
    return lines;
}

void writeChunk(Chunk* chunk, uint8_t byte, int line) {
    if (chunk->capacity <= chunk->count+1) {
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
    }
    chunk->code[chunk->count] = byte;
    chunk->count++;
    chunk->lines = insertLine(chunk->lines, line);
}

void writeConstant(Chunk* chunk, Value value, int line) {
    long curr = (long) value;
    if (curr > 256) {
        Value val1 = (curr & 255);
        curr >>= 8;
        Value val2 = (curr & 255);
        curr >>= 8;
        Value val3 = (curr & 255);
        
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
    writeValueArray(&chunk->constants, value);
    return chunk->constants.count - 1;
}

void freeChunk(Chunk* chunk) {
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}