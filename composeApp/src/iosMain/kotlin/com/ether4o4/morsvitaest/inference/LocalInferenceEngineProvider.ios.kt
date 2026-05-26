package com.ether4o4.morsvitaest.inference

actual fun createLocalInferenceEngine(): LocalInferenceEngine? = IosLiteRTInferenceEngine()
