package com.mylosoftworks.kotllms.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.*
import javax.imageio.ImageIO

actual suspend fun AttachedImage.Companion.fromUrl(url: String): AttachedImage {
    return withContext(Dispatchers.IO) {
        ImageIO.read(URI.create(url).toURL())
    }.toAttached()
}

fun BufferedImage.toAttached(): AttachedImage {
    val os = ByteArrayOutputStream()
    ImageIO.write(this, "png", os)
    return AttachedImage(Base64.getEncoder().encodeToString(os.toByteArray()))
}