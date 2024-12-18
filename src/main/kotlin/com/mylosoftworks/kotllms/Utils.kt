package com.mylosoftworks.kotllms

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

fun stripTrailingSlash(string: String) = string.removeSuffix("/")

fun <T : Any> T?.safeStructurize(alternative: T): T = this ?: alternative

// Converts an image to a regular base64 string
fun BufferedImage.toBase64(): String {
    val os = ByteArrayOutputStream()
    ImageIO.write(this, "png", os)
    return Base64.getEncoder().encodeToString(os.toByteArray())
}

// Attempts to convert a base64 string to an image
fun base64ToImage(base64: String): BufferedImage {
    val ins = ByteArrayInputStream(Base64.getDecoder().decode(base64))
    return ImageIO.read(ins)
}