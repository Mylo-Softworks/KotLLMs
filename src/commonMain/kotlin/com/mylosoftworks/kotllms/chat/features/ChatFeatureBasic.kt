package com.mylosoftworks.kotllms.chat.features

import com.mylosoftworks.kotllms.features.impl.ChatRole

interface ChatFeatureRole {
    var role: ChatRole?
}

interface ChatFeatureContent {
    var content: String?
}
