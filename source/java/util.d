module java.util;

import std.string;

alias boolean = bool;
alias String = string;

interface List(T) {
    boolean add(T item);
    void add(int index, T item);
    boolean addAll(Collection!T collection);
    boolean addAll(int index, Collection!T collection);
    void clear();
    boolean contains(Object item);
    boolean containsAll(Collection!Object collection);
    boolean equals(Object obj);
    T get(int index);
    int hashCode();
    int indexOf(Object item);
    boolean isEmpty();
    Iterator!T iterator();
    int lastIndexOf(Object item);
    ListIterator!T listIterator();
    ListIterator!T listIterator(int index);
    T remove(int index);
    boolean remove(Object item);
    boolean removeAll(Collection!Object collection);
    boolean retainAll(Collection!Object collection);
    T set(int index, T element);
    int size();
    List!T subList(int fromIndex, int toIndex);
    Object[] toArray();
    U[] toArray(U)(U[] array);
    String toString();
}


interface ListIterator(T) {
    void add(T e);
    boolean hasNext();
    T next();
    int nextIndex();
    boolean hasPrevious();
    T previous();
    int previousIndex();
    void remove();
    void set(T e);
}

interface Collection(T) {
}

bool instanceof(T)(auto object, T type)
{
}

class StringBuilder {
    string buffer; //todo : use Appender
    void append(String s) {
        buffer ~= s;
    }

    override public String toString() {
        return buffer;
    }
}
