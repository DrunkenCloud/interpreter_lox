JAVAC = javac
JAVA = java
SRC = lox
BIN = ./
MAIN_CLASS = Lox

SOURCES = $(wildcard $(SRC)/*.java)

.PHONY: all run clean

all:
	$(JAVAC) -d $(BIN) $(SOURCES)

run: all
	$(JAVA) lox.Lox ./trial.lox

clean:
	del /s /q $(BIN)\* 2>nul || rmdir /s /q $(BIN) 2>nul