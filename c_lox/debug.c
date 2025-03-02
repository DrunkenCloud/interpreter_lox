#include <stdio.h>
#include "debug.h"
#include "value.h"

int getLine(Lines* lines, int offset) {
    Lines* curr = lines;
    while (curr != NULL && offset >= curr->count) {
        offset -= curr->count;
        curr = curr->next;
    }
    return (curr != NULL) ? curr->line : -1;
}

void disassembleChunk(Chunk* chunk, const char* name) {
    printf("== %s ==\n", name);
    
    for (int i = 0; i < chunk->count;) {
        i = disassembleInstruction(chunk, i);
    }
}

static int simpleInstruction(const char* name, int offset) {
    printf("%s\n", name);
    return offset+1;
}

static int constantInstruction(const char* name, Chunk* chunk, int offset) {
    uint8_t constant = chunk->code[offset + 1];
    printf("%-16s %4d '", name, constant);
    printValue(chunk->constants.values[constant]);
    printf("'\n");

    return offset+2;
}

static int constantInstructionLong(const char* name, Chunk* chunk, int offset) {
    uint8_t const1 = chunk->code[offset + 1], const2 = chunk->code[offset + 2], const3 = chunk->code[offset + 3];
    long constant = ((uint8_t)(chunk->constants.values[const1])) | ((uint8_t)(chunk->constants.values[const2]) << 8) | ((uint8_t)chunk->constants.values[const3] << 16);
    printf("%-16s %4d %4d %4d '", name, const1, const2, const3);
    printf("%ld", constant);
    printf("'\n");

    return offset+4;
}

int disassembleInstruction(Chunk* chunk, int offset) {
    printf("%04d ", offset);

    if (offset > 0 && getLine(chunk->lines, offset) == getLine(chunk->lines, offset-1)) {
        printf("  |  ");
    } else {
        printf("%4d ", getLine(chunk->lines, offset));
    }

    uint8_t instruction = chunk->code[offset];
    switch (instruction) {
        case OP_RETURN:
            return simpleInstruction("OP_RETURN", offset);
        case OP_CONSTANT:
            return constantInstruction("OP_CONSTANT", chunk, offset);
        case OP_ADD:
            return simpleInstruction("OP_ADD", offset);
        case OP_SUBTRACT:
            return simpleInstruction("OP_SUBTRACT", offset);
        case OP_MULTIPLY:
            return simpleInstruction("OP_MULTIPLY", offset);
        case OP_DIVIDE:
            return simpleInstruction("OP_DIVIDE", offset);
        case OP_NEGATE:
            return simpleInstruction("OP_NEGATE", offset);
        case OP_CONSTANT_LONG:
            return constantInstructionLong("OP_CONSTANT_LONG", chunk, offset);
        default:
            printf("Unknown opcode %d\n", instruction);
            return offset + 1;
    }
}