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

package com.skydoves.pokedex.core.di

import android.content.Context
import kotlin.getValue

object ModuleLocator {

    private var context: (() -> Context)? = null

    fun attach(context: () -> Context) {
        this.context = context
    }

    fun detach() {
        context = null
    }

    val serializationModule by lazy { SerializationModule() }
    val dispatchersModule by lazy { DispatchersModule() }
    val networkModule by lazy {
        NetworkModule(
            json = serializationModule.json,
            networkCoroutineContext = dispatchersModule.io
        )
    }
    val databaseModule by lazy {
        DatabaseModule(
            context = requireNotNull(context) { "Please attach the context using attach" },
            json = serializationModule.json
        )
    }
    val repositoryModule by lazy {
        RepositoryModule(
            networkModule.pokedexClient,
            databaseModule.pokemonDao,
            databaseModule.pokemonInfoDao,
            dispatchersModule.io,
            networkModule.baseUrl
        )
    }
}
