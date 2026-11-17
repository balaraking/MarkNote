package com.soulx.marknote.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProjectPathUtilsTest {

    @Test
    fun `returns relative path for file inside project base directory`() {
        assertEquals(
            "src/main/Main.kt",
            ProjectPathUtils.toProjectRelativePath("/workspace/my-project", "/workspace/my-project/src/main/Main.kt")
        )
    }

    @Test
    fun `rejects sibling directory sharing string prefix with project base path`() {
        assertNull(
            ProjectPathUtils.toProjectRelativePath("/workspace/my-project", "/workspace/my-project-other/Main.kt")
        )
    }

    @Test
    fun `rejects project root directory itself and external paths`() {
        assertNull(ProjectPathUtils.toProjectRelativePath("/workspace/my-project", "/workspace/my-project"))
        assertNull(ProjectPathUtils.toProjectRelativePath("/workspace/my-project", "/tmp/scratch.kt"))
    }
}
