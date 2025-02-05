package com.mylosoftworks.kotllms.features.flagsimpl

import com.mylosoftworks.kotllms.shared.AttachedImage

// Flags which allow attaching certain media formats

interface FlagAttachedImages {
    var images: List<AttachedImage>?
}