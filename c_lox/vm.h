#ifndef clox_vm_h
#define clox_vm_h

#include "chunk.h"
#include "value.h"
#include "table.h"
#include "object.h"

#define FRAMES_MAX 64
#define STACK_MAX (FRAMES_MAX * UINT8_COUNT)

typedef struct {
    ObjClosure* closure;
    Value* slots;
    uint8_t* ip;
} CallFrame;

typedef struct {
    CallFrame frames[FRAMES_MAX];
    Value stack[STACK_MAX];
    Table globals;
    Table strings;
    Obj** grayStack;
    ObjUpvalue* openUpvalues;
    Value* stackTop;
    Obj* objects;
    size_t bytesAllocated;
    size_t nextGC;
    int frameCount;
    int grayCount;
    int grayCapacity;
} VM;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR
} InterpretResult;

extern VM vm;

void initVM();
void freeVM();
void push(Value value);
Value pop();
InterpretResult interpret(const char* source);

#endif