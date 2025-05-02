/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
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

package com.skydoves.pokedex.ui.main

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.core.repository.home.HomeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

internal class PokedexViewsHomeViewModel(private val homeRepository: HomeRepository) : ViewModel() {

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _toastMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val pokemonFetchingIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    @OptIn(ExperimentalCoroutinesApi::class)
    val pokemonList: StateFlow<List<Pokemon>> =
        pokemonFetchingIndex
            .flatMapLatest { page ->
                homeRepository.fetchPokemonList(
                    page = page,
                    onStart = { _isLoading.value = true },
                    onComplete = { _isLoading.value = false },
                    onError = {
                        _isLoading.value = false
                        _toastMessage.value = it
                    },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @MainThread
    fun fetchNextPokemonList() {
        if (!isLoading.value) {
            pokemonFetchingIndex.value++
        }
    }
}
