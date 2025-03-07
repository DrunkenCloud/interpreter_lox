CC = gcc
CFLAGS = -Wall -Wextra -g
SRC = $(wildcard *.c)
OBJ = $(SRC:.c=.o)
EXEC = clox

all: $(EXEC)

$(EXEC): $(OBJ)
	$(CC) $(CFLAGS) -o $(EXEC) $(OBJ)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -f $(OBJ) $(EXEC)

run: $(EXEC)
	./$(EXEC)

.PHONY: all clean run
