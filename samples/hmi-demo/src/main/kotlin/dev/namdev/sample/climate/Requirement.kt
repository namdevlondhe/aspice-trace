package dev.namdev.sample.climate

/** Links code to the requirements it implements. Source-only: it costs nothing at runtime. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Requirement(vararg val ids: String)
