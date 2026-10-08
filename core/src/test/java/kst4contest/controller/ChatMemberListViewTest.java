package kst4contest.controller;

import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMemberListViewTest {

    private static ChatMember member(String call) {
        ChatMember m = new ChatMember();
        m.setCallSign(call);
        return m;
    }

    private static List<String> calls(List<ChatMember> members) {
        return members.stream().map(ChatMember::getCallSign).toList();
    }

    @Test
    void withoutFiltersOrOrderTheInputIsReturnedUnchanged() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"), member("DL1ABC"));

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(), null);

        assertEquals(List.of("DN9APW", "DO5AMF", "DL1ABC"), calls(result));
    }

    @Test
    void severalFiltersAreCombinedWithAnd() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"), member("DL1ABC"));
        Predicate<ChatMember> startsWithD = m -> m.getCallSign().startsWith("D");
        Predicate<ChatMember> endsWithF = m -> m.getCallSign().endsWith("F");

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(startsWithD, endsWithF), null);

        assertEquals(List.of("DO5AMF"), calls(result));
    }

    @Test
    void anEmptyFilterListLetsEverythingThrough() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"));

        assertEquals(2, ChatMemberListView.compute(input, List.of(), null).size());
    }

    @Test
    void theOrderIsStableForEqualRanks() {
        List<ChatMember> input = List.of(member("BBB"), member("AAA"), member("CCC"), member("AAA"));
        Comparator<ChatMember> allEqual = (a, b) -> 0;

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(), allEqual);

        assertEquals(List.of("BBB", "AAA", "CCC", "AAA"), calls(result),
                "equal ranks must keep their incoming order. This is deliberately not what "
                        + "SortedList did: it inserted single elements by binary search and left "
                        + "them wherever the search ended, so the position within a tie was "
                        + "undefined and drifted as stations came and went.");
    }

    @Test
    void theComparatorDecidesTheOrder() {
        List<ChatMember> input = List.of(member("CCC"), member("AAA"), member("BBB"));

        List<ChatMember> result = ChatMemberListView.compute(
                input, List.of(), Comparator.comparing(ChatMember::getCallSign));

        assertEquals(List.of("AAA", "BBB", "CCC"), calls(result));
    }

    @Test
    void filteringHappensBeforeOrdering() {
        List<ChatMember> input = List.of(member("CCC"), member("AAA"), member("BBB"));
        Predicate<ChatMember> notB = m -> !m.getCallSign().equals("BBB");

        List<ChatMember> result = ChatMemberListView.compute(
                input, List.of(notB), Comparator.comparing(ChatMember::getCallSign));

        assertEquals(List.of("AAA", "CCC"), calls(result));
    }

    @Test
    void theResultIsImmutable() {
        List<ChatMember> result = ChatMemberListView.compute(List.of(member("DN9APW")), List.of(), null);

        assertThrows(UnsupportedOperationException.class, () -> result.add(member("DL1ABC")));
    }

    @Test
    void aFilterThrowingDoesNotDiscardTheWholeList() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"));
        Predicate<ChatMember> broken = m -> { throw new IllegalStateException("boom"); };

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(broken), null);

        assertEquals(List.of("DN9APW", "DO5AMF"), calls(result),
                "a broken filter must not empty the station list mid-contest");
    }

    @Test
    void aSecondOrderIsDerivedFromTheCanonicalListNotFromThePreviousResult() {
        /*
         * SortedList always ordered the unsorted source. Sorting an already sorted
         * result again would make equally ranked stations keep the previous
         * column's order instead of their arrival order, which is what the
         * operator sees as rows jumping when switching the sort column.
         */
        List<ChatMember> canonical = List.of(
                member("CCC"), member("AAA"), member("BBB"), member("DDD"));

        Comparator<ChatMember> byLastCharacter =
                Comparator.comparing(m -> m.getCallSign().substring(2));
        Comparator<ChatMember> allEqual = (a, b) -> 0;

        List<ChatMember> afterFirstOrder = ChatMemberListView.compute(
                canonical, List.of(), byLastCharacter);
        assertEquals(List.of("AAA", "BBB", "CCC", "DDD"), calls(afterFirstOrder));

        List<ChatMember> afterSecondOrder = ChatMemberListView.compute(
                canonical, List.of(), allEqual);

        assertEquals(List.of("CCC", "AAA", "BBB", "DDD"), calls(afterSecondOrder),
                "a new order must be derived from the canonical list, "
                        + "never from the previously shown order");
        assertEquals(calls(ChatMemberListView.compute(canonical, List.of(), allEqual)),
                calls(afterSecondOrder),
                "computing the visible list must not depend on what was computed before");
    }

    @Test
    void countMatchingAgreesWithComputeWithoutOrdering() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"), member("DL1ABC"));
        Predicate<ChatMember> endsWithF = m -> m.getCallSign().endsWith("F");

        assertEquals(
                ChatMemberListView.compute(input, List.of(endsWithF), null).size(),
                ChatMemberListView.countMatching(input, List.of(endsWithF)),
                "the status line only wants a number; it must not pay for sorting thousands of rows");
        assertEquals(3, ChatMemberListView.countMatching(input, List.of()));
    }

    @Test
    void countMatchingKeepsAMemberWhenAFilterThrows() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"));
        Predicate<ChatMember> broken = m -> { throw new IllegalStateException("boom"); };

        assertEquals(2, ChatMemberListView.countMatching(input, List.of(broken)));
    }

    @Test
    void aNullMemberDoesNotBreakTheComputation() {
        List<ChatMember> input = java.util.Arrays.asList(member("DN9APW"), null);

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(), null);

        assertEquals(2, result.size(),
                "the JavaFX list this replaces carried nulls; throwing here would freeze the station list");
    }
}
