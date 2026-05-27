package com.ether4o4.morsvitaest

// For desktop, a common way is to use a system property.
// This can be set, for example, in the JVM arguments when running in debug mode: -Dmorsvitaest.debug=true
actual val isDebugBuild: Boolean = System.getProperty("morsvitaest.debug", "false").toBoolean()
