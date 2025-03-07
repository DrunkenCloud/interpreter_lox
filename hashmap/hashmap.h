#include <stdlib.h>
#include <string.h>
#include <stdint.h>

typedef struct {
    void* key;
    int value;
} Entry;

typedef struct hashmap {
    Entry* arr;
    int count;
    int capacity;
} hashmap;

hashmap* create_hashmap(int capacity);
int hash(void* key, int capacity);
void hashmap_insert(hashmap* map, void* key, int value);
int hashmap_get(hashmap* map, void* key, int* found);
void free_hashmap(hashmap* map);