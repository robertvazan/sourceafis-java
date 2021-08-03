module java.lang;

class IndexOutOfBoundsException : Exception {
    this() {
        super("Index is out of bounds");
    }
    this(string message) {
        super(message);
    }
}

class IllegalArgumentException : Exception {
    this() {
        super("Argument is illegal");
    }
    this(string message) {
        super(message);
    }
}
