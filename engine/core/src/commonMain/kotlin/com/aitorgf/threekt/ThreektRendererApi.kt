package com.aitorgf.threekt

/**
 * Marks core declarations that exist for renderers, such as the storage behind an interleaved buffer.
 * They are public because renderers live in their own modules, where `internal` declarations of the core
 * are not visible, but applications should not need them and they may change without notice. Renderer
 * code opts in with `@file:OptIn(ThreektRendererApi::class)`.
 */
@RequiresOptIn(
    message = "This declaration is meant for renderer implementations and may change without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
annotation class ThreektRendererApi
