package kst4contest.controller;

import kst4contest.model.ChatMember;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Derives the visible station list from the canonical one.
 *
 * <p>This replaces the JavaFX {@code FilteredList}/{@code SortedList} pair that
 * used to sit in {@link ChatController}. Being a pure function, the filtering
 * and ordering rules are testable without a running toolkit for the first time.
 */
public final class ChatMemberListView {

    private ChatMemberListView() {
    }

    /**
     * @param members the canonical list; never modified
     * @param filters combined with AND; an empty list lets everything through
     * @param order   {@code null} keeps the incoming order; sorting is stable
     * @return an immutable result list
     */
    public static List<ChatMember> compute(List<ChatMember> members,
                                           List<Predicate<ChatMember>> filters,
                                           Comparator<ChatMember> order) {
        List<ChatMember> result = new ArrayList<>(members.size());
        for (ChatMember member : members) {
            if (matches(member, filters)) {
                result.add(member);
            }
        }
        if (order != null) {
            // List.sort is stable: within a tie the arrival order is kept.
            result.sort(order);
        }
        /*
         * Collections.unmodifiableList and not List.copyOf: the JavaFX list this
         * replaces carried null entries, and List.copyOf rejects them. A
         * NullPointerException raised here would be swallowed by the listener guard
         * and the station list would stop updating without a word.
         */
        return java.util.Collections.unmodifiableList(result);
    }

    /**
     * How many members pass the filters. Same rules as {@link #compute}, but it
     * neither sorts nor builds a list: the status line only wants the number.
     */
    public static int countMatching(List<ChatMember> members, List<Predicate<ChatMember>> filters) {
        int count = 0;
        for (ChatMember member : members) {
            if (matches(member, filters)) {
                count++;
            }
        }
        return count;
    }

    private static boolean matches(ChatMember member, List<Predicate<ChatMember>> filters) {
        for (Predicate<ChatMember> filter : filters) {
            try {
                if (!filter.test(member)) {
                    return false;
                }
            } catch (RuntimeException e) {
                // A broken filter must not empty the station list during a contest.
                System.err.println("[station list] filter failed, keeping the member: " + e);
            }
        }
        return true;
    }
}
