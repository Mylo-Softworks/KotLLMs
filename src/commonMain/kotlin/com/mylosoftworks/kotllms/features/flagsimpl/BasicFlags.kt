package com.mylosoftworks.kotllms.features.flagsimpl

// Flags for the absolute basics, which pretty much any endpoint will support

interface FlagPrompt {
    var prompt: String?
}

interface FlagContextSize {
    var contextSize: Int?
}

interface FlagMaxLength {
    var maxLength: Int?
}

// Merged for less writing code

interface FlagsAllBasic: FlagPrompt, FlagContextSize, FlagMaxLength