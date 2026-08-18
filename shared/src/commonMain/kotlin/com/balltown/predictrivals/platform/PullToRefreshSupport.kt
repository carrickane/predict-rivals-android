package com.balltown.predictrivals.platform

/** Whether pull-to-refresh should be offered at all on this platform — false on web (no touch-first
 * gesture convention there, and no dependable way to distinguish mobile-web from desktop-web). */
expect fun isPullToRefreshSupported(): Boolean
