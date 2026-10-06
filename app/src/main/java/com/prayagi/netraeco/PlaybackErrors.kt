package com.prayagi.netraeco

import androidx.media3.common.PlaybackException

/**
 * Plain-words reason for a playback error, chosen only from the error code Android reports.
 * Unknown codes keep the generic message; nothing is guessed.
 */
fun playbackErrorMessage(code: Int): String = when (code) {
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "This file was not found. It may have been moved or deleted. Please pick it again."
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
        "Netra Player no longer has permission to open this file. Please pick it again."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "This phone cannot play this kind of file."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
        "This file looks damaged and cannot be played."
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ->
        "This phone cannot decode this file (too demanding for its hardware)."
    else -> "This file could not be played on this phone."
}
