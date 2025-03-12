CC = gcc
CFLAGS = -Wall -Wextra -Wshadow -Werror -g -O2 -fstack-protector-strong -fPIE -pie -z noexecstack -D_FORTIFY_SOURCE=2
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
	./$(EXEC) ./trial.lox

val: $(EXEC)
	valgrind --track-origins=yes --leak-check=full --show-leak-kinds=all -s ./$(EXEC) ./trial.lox

.PHONY: all clean run