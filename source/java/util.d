module java.util;

import std.string;

alias boolean = bool;
alias String = string;

class StringBuilder {
    string buffer; //todo : use Appender
    void append(String s) {
        buffer ~= s;
    }

    override public String toString() {
        return buffer;
    }
}
