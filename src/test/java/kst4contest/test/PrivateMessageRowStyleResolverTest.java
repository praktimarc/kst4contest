package kst4contest.test;

import kst4contest.view.PrivateMessageRowStyleResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PrivateMessageRowStyleResolverTest {

    @ParameterizedTest
    @CsvSource({
            "0, messageHighlight30-column",
            "30, messageHighlight30-column",
            "31, messageHighlight60-column",
            "60, messageHighlight60-column",
            "61, messageHighlight90-column",
            "90, messageHighlight90-column",
            "91, messageHighlight120-column",
            "120, messageHighlight120-column",
            "121, messageHighlight180-column",
            "180, messageHighlight180-column",
            "181, messageHighlight300-column",
            "300, messageHighlight300-column"
    })
    void selectsAgeStyleClassAtEveryBoundary(
            long ageSeconds,
            String expectedStyleClass
    ) {
        assertEquals(
                expectedStyleClass,
                PrivateMessageRowStyleResolver.resolveStyleClass(
                        false,
                        ageSeconds
                )
        );
    }

    @Test
    void returnsNoAgeStyleClassAfterFiveMinutes() {
        assertNull(
                PrivateMessageRowStyleResolver.resolveStyleClass(
                        false,
                        301
                )
        );
    }

    @Test
    void keepsOwnMessageStyleAfterFiveMinutes() {
        assertEquals(
                PrivateMessageRowStyleResolver.OWN_STYLE_CLASS,
                PrivateMessageRowStyleResolver.resolveStyleClass(
                        true,
                        301
                )
        );
    }
}
