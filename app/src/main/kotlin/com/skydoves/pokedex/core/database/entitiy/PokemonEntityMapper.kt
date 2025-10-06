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

/*
 * Designed and developed by 2024 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.core.database.entitiy

import com.skydoves.pokedex.core.di.ModuleLocator
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.core.model.PokemonNetworkModel
import java.io.File
import okhttp3.HttpUrl

fun List<PokemonNetworkModel>.asDatabaseEntity(): List<PokemonEntity> = map { pokemon ->
    PokemonEntity(name = pokemon.name)
}

fun List<PokemonEntity>.asPresentationModel(apiUrl: HttpUrl): List<Pokemon> = map { entity ->
    Pokemon(
        name = entity.name.replaceFirstChar { it.uppercase() },
        imageUrl = getPokemonImageUrlByName(name = entity.name, apiUrl = apiUrl).toString(),
    )
}

fun getPokemonImageUrlByName(name: String, apiUrl: HttpUrl? = null): HttpUrl {
    val baseApiUrl = apiUrl ?: ModuleLocator.networkModule.baseUrl
    return baseApiUrl
        .newBuilder()
        .addPathSegment("pokemon")
        .addPathSegment(name)
        .addPathSegment("image")
        .build()
}

fun getPokemonImageFileByName(name: String, filesDir: String): File = File(filesDir, "${name}.png")
