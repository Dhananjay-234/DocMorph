package com.docmorph.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

/**
 * DocMorph Application class.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation and
 * serve as the application-level dependency container.
 */
@HiltAndroidApp
class DocMorphApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialise PdfBox-Android font resources
        PDFBoxResourceLoader.init(applicationContext)
    }
}
