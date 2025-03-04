#include <stdio.h>
#include "common.h"
#include "vm.h"
#include "debug.h"

VM vm; 

static void resetStack() {
    vm.stackTop = vm.stack;
}

void initVM() {
    resetStack();
}

void freeVM() {
}

void push(Value value) {
    *vm.stackTop = value;
    vm.stackTop++;
}

Value pop() {
    vm.stackTop--;
    return *vm.stackTop;
}

static InterpretResult run() {
    #define READ_BYTE() (*vm.ip++)
    #define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
    #define BINARY_OP(op) \
        do { \
        double b = pop(); \
        double a = pop(); \
        push(a op b); \
        } while (false)
    #ifdef DEBUG_TRACE_EXECUTION 
        printf("          ");
        for (Value* slot = vm.stack; slot < vm.stackTop; slot++) {
            printf("[ ");
            printValue(*slot);
            printf(" ]");
        }
        printf("\n");
        disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
    #endif

    for (;;) {
        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                push(constant);
                break;
            }
            case OP_ADD:      BINARY_OP(+); break;
            case OP_SUBTRACT: BINARY_OP(-); break;
            case OP_MULTIPLY: BINARY_OP(*); break;
            case OP_DIVIDE:   BINARY_OP(/); break;
            case OP_NEGATE: {
                size_t top = vm.stackTop;
                vm.stack[top - 1] = -vm.stack[top - 1];
            }
            case OP_CONSTANT_LONG: {
                uint8_t const1 = READ_BYTE();
                uint8_t const2 = READ_BYTE();
                uint8_t const3 = READ_BYTE();
                uint32_t index = (const1) | (const2 << 8) | (const3 << 16);
                Value constant = vm.chunk->constants.values[index];
                push(constant);
                break;
            }            
            case OP_RETURN: {
                printValue(pop());
                printf("\n");
                return INTERPRET_OK;
                break;
            }
            default:
                break;
        }
    }
    
    #undef READ_CONSTANT
    #undef BINARY_OP
    #undef READ_BYTE
}

InterpretResult interpret(Chunk* chunk) {
    vm.chunk = chunk;
    vm.ip = vm.chunk->code;
    return run();
}