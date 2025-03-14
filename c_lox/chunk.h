#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

typedef enum {
    OP_CONSTANT_LONG,
    OP_CONSTANT,
    OP_NEGATE,
    OP_PRINT,
    OP_JUMP_IF_FALSE,
    OP_JUMP,
    OP_LOOP,
    OP_NIL,
    OP_TRUE,
    OP_NOT,
    OP_FALSE,
    OP_POP,
    OP_GET_GLOBAL,
    OP_SET_GLOBAL,
    OP_GET_LOCAL,
    OP_SET_LOCAL,
    OP_GET_UPVALUE,
    OP_SET_UPVALUE,
    OP_GET_PROPERTY,
    OP_SET_PROPERTY,
    OP_GET_PROPERTY_VAR,
    OP_SET_PROPERTY_VAR,
    OP_DEFINE_GLOBAL,
    OP_CLASS,
    OP_METHOD,
    OP_EQUAL,
    OP_GREATER,
    OP_INHERIT,
    OP_GET_SUPER,
    OP_CLOSURE,
    OP_CLOSE_UPVALUE,
    OP_LESS,
    OP_ADD,
    OP_SUBTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_MODULO,
    OP_CALL,
    OP_RETURN,
} OpCode;

typedef struct {
    int* lineNumbers;
    int count;
    int capacity;
} LineArray;

typedef struct {
    uint8_t* code;
    ValueArray constants;
    LineArray lines;
    int count;
    int capacity;
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
int addConstant(Chunk* chunk, Value value);
void writeConstant(Chunk* chunk, Value value, int line);
void initLineArray(LineArray* lines);
void freeLineArray(LineArray* lines);
void addLine(LineArray* lines, int line);
int getLine(LineArray* lines, int instructionIndex);

#endif