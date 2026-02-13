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
     * Whether to enable shared element transitions between the activities.
     * [EnableTransformationLayout] must be set to true, otherwise this flag will be false.
     */
    var EnableSharedElementTransitions = true
        get() = EnableTransformationLayout && field

    object Keys {
        const val POKEDEX_ENABLE_TRANSFORMATION_LAYOUT = "enableSharedTransitionScope"
        const val POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS = "enableSharedElementTransitions"
        const val POKEDEX_API_URL = "apiUrl"
    }
}
