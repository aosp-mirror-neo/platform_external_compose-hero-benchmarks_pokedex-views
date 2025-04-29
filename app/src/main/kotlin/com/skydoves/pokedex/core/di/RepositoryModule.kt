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

import com.skydoves.pokedex.core.database.PokemonDao
import com.skydoves.pokedex.core.database.PokemonInfoDao
import com.skydoves.pokedex.core.repository.details.DetailsRepository
import com.skydoves.pokedex.core.repository.details.DetailsRepositoryImpl
import com.skydoves.pokedex.core.repository.home.HomeRepository
import com.skydoves.pokedex.core.repository.home.HomeRepositoryImpl
import com.skydoves.pokedex.core.service.PokedexClient
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.HttpUrl

class RepositoryModule(
    private val pokedexClient: PokedexClient,
    private val pokemonDao: PokemonDao,
    private val pokemonInfoDao: PokemonInfoDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val apiUrl: HttpUrl
) {
    val detailsRepository: DetailsRepository by lazy {
        DetailsRepositoryImpl(pokedexClient, pokemonInfoDao, ioDispatcher)
    }

    val homeRepository: HomeRepository by lazy {
        HomeRepositoryImpl(pokedexClient, pokemonDao, ioDispatcher, apiUrl)
    }
}
