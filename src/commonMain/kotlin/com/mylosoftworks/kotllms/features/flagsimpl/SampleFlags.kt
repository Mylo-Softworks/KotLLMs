package com.mylosoftworks.kotllms.features.flagsimpl

// Flags for sampling settings, sampling settings affect the flow of the generation

// Basic

interface FlagTemperature {
    var temperature: Float?
}

// Less common, supported in KoboldCPP
interface FlagTopA {
    var topA: Float?
}

interface FlagTfs {
    var tfs: Float?
}

interface FlagTypical {
    var typical: Float?
}

// Common
interface FlagTopK {
    var topK: Int?
}

interface FlagTopP {
    var topP: Float?
}

/**
 * Contains [FlagTemperature], [FlagTopK], [FlagTopP].
 *
 * Note: [FlagTopA], [FlagTfs] and [FlagTypical] are not included since they're less common.
 */
interface FlagsCommonSampling: FlagTemperature, FlagTopK, FlagTopP

// Penalties
/**
 * A penalty which helps the LLM avoid repetition.
 */
interface FlagRepetitionPenalty {
    var repetitionPenalty: Float?
}

/**
 * A variant of [FlagRepetitionPenalty] which includes range and slope.
 */
interface FlagRepetitionPenaltyWithRangeSlope: FlagRepetitionPenalty {
    var repetitionPenaltyRange: Int?
    var repetitionPenaltySlope: Float?
}

// Other sampling flags, like early stopping and output trimming

interface FlagStopSequences {
    var stopSequences: List<String>?
}

interface FlagTrimStop {
    var trimStop: Boolean?
}

interface FlagEarlyStopping {
    var earlyStopping: Boolean?
}