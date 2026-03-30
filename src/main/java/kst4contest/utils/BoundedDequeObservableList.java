package kst4contest.utils;

import javafx.collections.ObservableListBase;

import java.util.Arrays;

/**
 * A bounded ObservableList backed by a circular buffer (ring buffer).
 * <p>
 * Provides O(1) {@link #addFirst} and {@link #addLast} as well as O(1)
 * random access via {@link #get}. When the list reaches {@code maxCapacity},
 * adding a new element at the front automatically evicts the oldest element
 * at the back — and vice versa.
 * <p>
 * This is a drop-in replacement for {@code FXCollections.observableArrayList()}
 * wherever elements are prepended frequently, e.g. chat message lists.
 */
public class BoundedDequeObservableList<E> extends ObservableListBase<E> {

    private final int maxCapacity;
    private final Object[] elements;
    private int head = 0;
    private int size = 0;

    public BoundedDequeObservableList(int maxCapacity) {
        if (maxCapacity <= 0) throw new IllegalArgumentException("maxCapacity must be > 0");
        this.maxCapacity = maxCapacity;
        this.elements = new Object[maxCapacity];
    }

    // ── read access ──────────────────────────────────────────────────────────

    @Override
    public int size() {
        return size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        checkIndex(index);
        return (E) elements[physicalIndex(index)];
    }

    // ── O(1) deque operations ─────────────────────────────────────────────────

    /**
     * Inserts {@code element} at index 0 (newest-first order).
     * If the list is already at capacity the oldest element (last index) is
     * removed first — both changes are reported as a single compound change.
     */
    public void addFirst(E element) {
        beginChange();
        if (size == maxCapacity) {
            // evict last element
            int lastPhysical = physicalIndex(size - 1);
            @SuppressWarnings("unchecked")
            E evicted = (E) elements[lastPhysical];
            elements[lastPhysical] = null;
            size--;
            nextRemove(size, evicted);  // index after decrement == old last index
        }
        head = (head - 1 + maxCapacity) % maxCapacity;
        elements[head] = element;
        size++;
        nextAdd(0, 1);
        endChange();
    }

    /**
     * Appends {@code element} at the last index (oldest-first order).
     * If the list is already at capacity the newest element (index 0) is
     * removed first.
     */
    public void addLast(E element) {
        beginChange();
        if (size == maxCapacity) {
            // evict first element
            @SuppressWarnings("unchecked")
            E evicted = (E) elements[head];
            elements[head] = null;
            head = (head + 1) % maxCapacity;
            size--;
            nextRemove(0, evicted);
        }
        elements[physicalIndex(size)] = element;
        size++;
        nextAdd(size - 1, size);
        endChange();
    }

    // ── standard List mutation (O(n) — use addFirst/addLast for hot path) ─────

    @Override
    public void add(int index, E element) {
        if (index == 0) {
            addFirst(element);
            return;
        }
        if (index == size) {
            addLast(element);
            return;
        }
        checkIndexForAdd(index);
        beginChange();
        if (size == maxCapacity) {
            int lastPhysical = physicalIndex(size - 1);
            @SuppressWarnings("unchecked")
            E evicted = (E) elements[lastPhysical];
            elements[lastPhysical] = null;
            size--;
            nextRemove(size, evicted);
        }
        // shift elements [index .. size-1] one position towards the end
        for (int i = size; i > index; i--) {
            elements[physicalIndex(i)] = elements[physicalIndex(i - 1)];
        }
        elements[physicalIndex(index)] = element;
        size++;
        nextAdd(index, index + 1);
        endChange();
    }

    @Override
    public E remove(int index) {
        checkIndex(index);
        beginChange();
        @SuppressWarnings("unchecked")
        E removed = (E) elements[physicalIndex(index)];
        // shift elements [index+1 .. size-1] one position towards the front
        for (int i = index; i < size - 1; i++) {
            elements[physicalIndex(i)] = elements[physicalIndex(i + 1)];
        }
        elements[physicalIndex(size - 1)] = null;
        size--;
        nextRemove(index, removed);
        endChange();
        return removed;
    }

    @Override
    public E set(int index, E element) {
        checkIndex(index);
        beginChange();
        @SuppressWarnings("unchecked")
        E old = (E) elements[physicalIndex(index)];
        elements[physicalIndex(index)] = element;
        nextSet(index, old);
        endChange();
        return old;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int physicalIndex(int virtualIndex) {
        return (head + virtualIndex) % maxCapacity;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    private void checkIndexForAdd(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
}
