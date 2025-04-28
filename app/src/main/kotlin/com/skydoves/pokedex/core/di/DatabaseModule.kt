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

package com.skydoves.pokedex.core.di

import android.content.Context
import com.skydoves.pokedex.core.database.PokemonInfoDao
import androidx.room.Room
import com.skydoves.pokedex.core.database.PokedexDatabase
import com.skydoves.pokedex.core.database.PokemonDao
import com.skydoves.pokedex.core.database.StatsResponseConverter
import com.skydoves.pokedex.core.database.TypeResponseConverter
import kotlin.getValue
import kotlinx.serialization.json.Json

class DatabaseModule(private val context: () -> Context, private val json: Json) {
    val typeResponseConverter: TypeResponseConverter by lazy { TypeResponseConverter(json) }

    val statsResponseConverter: StatsResponseConverter by lazy { StatsResponseConverter(json) }

    val pokedexDatabase: PokedexDatabase by lazy {
        // fallbackToDestructiveMigration requires a parameter in Room 2.7 that's not available in
        //  2.6. Forward-compatibility checks like androidx_max_dep_versions will fail without this.
        @Suppress("DEPRECATION")
        Room.databaseBuilder(context(), PokedexDatabase::class.java, "Pokedex.db")
            .fallbackToDestructiveMigration()
            .addTypeConverter(typeResponseConverter)
            .addTypeConverter(statsResponseConverter)
            .build()
    }

    val pokemonInfoDao: PokemonInfoDao by lazy { pokedexDatabase.pokemonInfoDao() }

    val pokemonDao: PokemonDao by lazy { pokedexDatabase.pokemonDao() }
}
