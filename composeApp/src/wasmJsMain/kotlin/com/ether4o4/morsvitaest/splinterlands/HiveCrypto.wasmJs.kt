package com.ether4o4.morsvitaest.splinterlands

actual fun signMessage(message: String, postingKeyWif: String): String = throw UnsupportedOperationException("Splinterlands is not supported on Web")

actual suspend fun buildSignedCustomJson(
    username: String,
    postingKeyWif: String,
    opId: String,
    jsonPayload: String,
): String = throw UnsupportedOperationException("Splinterlands is not supported on Web")
