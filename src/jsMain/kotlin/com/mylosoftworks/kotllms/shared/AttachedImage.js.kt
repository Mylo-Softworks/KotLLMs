package com.mylosoftworks.kotllms.shared

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual suspend fun AttachedImage.Companion.fromUrl(url: String): AttachedImage {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    val img = document.createElement("img") as HTMLImageElement
    img.src = url

    return suspendCoroutine { cont ->
        img.onload = {
            canvas.width = img.width
            canvas.height = img.height
            context.drawImage(img, 0.0, 0.0)
            val dataUrl = canvas.toDataURL("image/png")
            cont.resume(AttachedImage(dataUrl.substring(dataUrl.indexOf(",") + 1)))
        }
        img.onerror = { b: dynamic, s: String, i: Int, i1: Int, any: Any? ->
            cont.resumeWithException(Exception("Failed to load image"))
        }
    }
}