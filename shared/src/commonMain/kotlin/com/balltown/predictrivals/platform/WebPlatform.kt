package com.balltown.predictrivals.platform

/** True only for the js/wasmJs web targets — used to show web-specific chrome (the top header
 * with logout) and hide the equivalent native affordance (Profile screen's own Logout button). */
expect fun isWebPlatform(): Boolean
