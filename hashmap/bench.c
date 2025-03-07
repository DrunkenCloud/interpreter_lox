#include <stdio.h>
#include <time.h>
#include "hashmap.h"

#define NUM_ENTRIES 10000000

int main() {
    hashmap* map = create_hashmap(NUM_ENTRIES * 2);
    clock_t start, end;
    double cpu_time_used;

    start = clock();
    for (int i = 0; i < NUM_ENTRIES; i++) {
        hashmap_insert(map, (void*)(uintptr_t)i, i * 2);
    }
    end = clock();
    cpu_time_used = ((double)(end - start)) / CLOCKS_PER_SEC;
    printf("Insertion Time: %f seconds\n", cpu_time_used);

    start = clock();
    int found, sum = 0;
    for (int i = 0; i < NUM_ENTRIES; i++) {
        sum += hashmap_get(map, (void*)(uintptr_t)i, &found);
    }
    end = clock();
    cpu_time_used = ((double)(end - start)) / CLOCKS_PER_SEC;
    printf("Retrieval Time: %f seconds\n", cpu_time_used);
    printf("Sum of values: %d\n", sum);

    free_hashmap(map);
    return 0;
}