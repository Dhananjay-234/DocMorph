package com.docmorph.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper that converts a private [File] path to a content URI suitable for
 * sharing via Android's FileProvider, and builds a standard share [Intent].
 *
 * The FileProvider is declared in [AndroidManifest.xml] with authority
 * [Constants.FILE_PROVIDER_AUTHORITY] and the path config is in
 * `res/xml/file_provider_paths.xml`.
 */
object FileProviderUtils {

    /**
     * Returns a shareable [Uri] for [file] via [FileProvider].
     */
    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)

    /**
     * Creates a standard `ACTION_SEND` [Intent] for [file], with the
     * appropriate MIME type and read-URI grant flags.
     *
     * @param mimeType  e.g. [Constants.MIME_PDF], [Constants.MIME_PNG]
     * @param title     Optional chooser title (defaults to "Share via")
     */
    fun buildShareIntent(
        context: Context,
        file: File,
        mimeType: String,
        title: String = "Share via"
    ): Intent {
        val uri = uriForFile(context, file)
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            title
        )
    }

    /**
     * Returns the app's private cache sub-directory used for temporary
     * export files.  The directory is created if it does not yet exist.
     */
    fun exportCacheDir(context: Context): File =
        File(context.cacheDir, "exports").also { it.mkdirs() }
}
