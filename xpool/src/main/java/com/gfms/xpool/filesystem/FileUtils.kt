package com.gfms.xpool.filesystem

/**
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.util.Comparator;
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

class FileUtils {

    @Throws(IOException::class)
    fun createDirectoryIfNotExists(directory: File) {
        if (directory.exists() && !directory.isDirectory) {
            if (!directory.isDirectory) {
                throw IOException("${directory.name} is not a directory")
            }
            return;
        }
        if (!directory.mkdirs()) {
            throw IOException("Cannot create directory ${directory.name}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(IOException::class)
    fun deleteDirectory(dir: File) {
        val files: Array<File>? = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    deleteDirectory(file);
                } else {
                    file.delete()
                }
            }
        }
        Files.deleteIfExists(dir.toPath())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun listIndexFiles(directory: File): MutableList<File>? {
        val files: Array<out File> =
            directory.listFiles() { file -> Constants.INDEX_FILE_PATTERN.matcher(file.name).matches() }
                ?: return Collections.emptyList()

        return Arrays.stream(files).sorted(Comparator.comparingInt() { f -> getFileId(f, Constants.INDEX_FILE_PATTERN)}).collect(Collectors.toList())
    }

    private fun getFileId(file: File, pattern: Pattern): String {
        val matcher: Matcher = pattern.matcher(file.name)
        if (matcher.find()) {
            return matcher.group(1)!!
        }
        throw IllegalArgumentException("Cannot extract file id for file ${file.path}")
    }
}
        **/