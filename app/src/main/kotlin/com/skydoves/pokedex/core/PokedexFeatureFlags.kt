/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.core

import com.skydoves.pokedex.core.PokedexFeatureFlags.EnableTransformationLayout

/** Contains feature flags for the Pokedex hero benchmark target */
object PokedexFeatureFlags {
    /**
     * Whether [com.skydoves.transformationlayout.TransformationLayout] should be used or replaced
     * by simpler layouts instead. If false, shared element transitions will be off too.
     */
    var EnableTransformationLayout = true

    /**
     * Whether to enable Palette color extraction from loaded bitmaps. Disabled by default for
     * benchmark performance and stability.
     */
    var EnablePaletteExtraction = false

    /**
     * Whether to enable shared element transitions between the activities.
     * [EnableTransformationLayout] must be set to true, otherwise this flag will be false.
     */
    var EnableSharedElementTransitions = true
        get() = EnableTransformationLayout && field

    /**
     * Whether to explicitly disable the overscroll effect for the hierarchy. Overscroll relies on
     * shaders on newer API levels, which need to be compiled. In benchmarks, we kill the shader
     * cache, which means that we incur a significant cost when initially compiling the shader.
     *
     * Inconsistencies in adb input injection mean that we sometimes end up hitting the bounds of
     * the list. We disable overscroll by default to stabilize benchmark results.
     */
    var DisableOverscrollEffect = true

    object Keys {
        const val POKEDEX_ENABLE_TRANSFORMATION_LAYOUT = "enableSharedTransitionScope"
        const val POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS = "enableSharedElementTransitions"
        const val POKEDEX_API_URL = "apiUrl"
    }
}
