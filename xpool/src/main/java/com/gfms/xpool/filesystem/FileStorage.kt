package com.gfms.xpool.filesystem

/**
import java.io.File

interface XPool

class FileStorage(private val delegate: File= File("/tmp")): XPool {

    init {
        delegate.mkdir()
    }
}

 **/