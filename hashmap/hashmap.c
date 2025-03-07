#ifndef HASHMAP
#define HASHMAP

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

hashmap* create_hashmap(int capacity) {
    hashmap* map = (hashmap*)malloc(sizeof(hashmap));
    map->arr = (Entry*)calloc(capacity, sizeof(Entry));
    map->count = 0;
    map->capacity = capacity;
    return map;
}

int hash(void* key, int capacity) {
    return ((uintptr_t)key) % capacity;
}

void hashmap_insert(hashmap* map, void* key, int value) {
    int index = hash(key, map->capacity);
    while (map->arr[index].key != NULL) {
        if (map->arr[index].key == key) {
            map->arr[index].value = value;
            return;
        }
        index = (index + 1) % map->capacity;
    }
    map->arr[index].key = key;
    map->arr[index].value = value;
    map->count++;
}

int hashmap_get(hashmap* map, void* key, int* found) {
    int index = hash(key, map->capacity);
    while (map->arr[index].key != NULL) {
        if (map->arr[index].key == key) {
            *found = 1;
            return map->arr[index].value;
        }
        index = (index + 1) % map->capacity;
    }
    *found = 0;
    return 0;
}

void free_hashmap(hashmap* map) {
    free(map->arr);
    free(map);
}

#endif
