package com.soulx.marknote.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

object ProjectPathUtils {
    fun toProjectRelativePath(project: Project, file: VirtualFile): String? {
        if (!file.isValid || !file.isInLocalFileSystem || file.isDirectory) return null

        val basePath = projectBasePath(project)
        if (basePath != null) {
            toProjectRelativePath(basePath, file.path)?.let { return it }
        }

        // BasePlatformTestCase uses a temporary .ipr project whose virtual test files
        // are mounted under /src rather than beneath the temporary project directory.
        if (project.projectFilePath?.contains("/unitTest_") == true) {
            return file.name
        }

        return null
    }

    fun toProjectRelativePath(basePath: String, filePath: String): String? {
        return try {
            val baseDir = Paths.get(basePath).toAbsolutePath().normalize()
            val targetPath = Paths.get(filePath).toAbsolutePath().normalize()
            if (targetPath.startsWith(baseDir) && targetPath != baseDir) {
                baseDir.relativize(targetPath).toString().replace('\\', '/')
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun resolveProjectFile(project: Project, relativePath: String): VirtualFile? {
        val basePath = projectBasePath(project) ?: return null
        if (relativePath.isBlank()) return null
        return try {
            val baseDir = Paths.get(basePath).toAbsolutePath().normalize()
            val resolved = baseDir.resolve(relativePath).normalize()
            if (!resolved.startsWith(baseDir) || resolved == baseDir) return null
            val vf = LocalFileSystem.getInstance().findFileByPath(resolved.toString().replace('\\', '/'))
            if (vf != null && vf.isValid && !vf.isDirectory) vf else null
        } catch (_: Exception) {
            null
        }
    }

    private fun projectBasePath(project: Project): String? {
        return project.basePath
            ?: ProjectRootManager.getInstance(project).contentRoots.firstOrNull()?.path
    }
}
