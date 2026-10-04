package kst4contest.view.compose

import androidx.compose.material3.Typography
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TypographyScalingTest {

    @Test
    fun `the base size replaces the body size and the rest keeps its ratio`() {
        val original = Typography()
        val originalBody = original.bodyMedium.fontSize.value
        val originalTitle = original.titleMedium.fontSize.value

        val scaled = original.scaledTo(originalBody * 2f)

        assertEquals(originalBody * 2f, scaled.bodyMedium.fontSize.value, 0.01f)
        assertEquals(originalTitle * 2f, scaled.titleMedium.fontSize.value, 0.01f,
                "every style scales by the same factor, so the design keeps its proportions")
    }

    @Test
    fun `a base size of zero or less is ignored`() {
        val original = Typography()

        assertEquals(original.bodyMedium.fontSize.value,
                original.scaledTo(0f).bodyMedium.fontSize.value, 0.01f)
        assertEquals(original.bodyMedium.fontSize.value,
                original.scaledTo(-5f).bodyMedium.fontSize.value, 0.01f)
    }
}
