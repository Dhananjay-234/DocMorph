package com.docmorph.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner that substitutes [DocMorphApplication] with
 * [HiltTestApplication] during instrumented tests so Hilt's
 * test component is used instead of the production one.
 *
 * Registered in app/build.gradle.kts:
 *   testInstrumentationRunner = "com.docmorph.app.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
