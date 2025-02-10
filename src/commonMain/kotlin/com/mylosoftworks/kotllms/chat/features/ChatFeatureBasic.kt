package com.mylosoftworks.kotllms.chat.features

import com.mylosoftworks.kotllms.features.impl.ChatGen

interface ChatFeatureRole {
    var role: ChatGen.ChatRole?
}

interface ChatFeatureContent {
    var content: String?
}
