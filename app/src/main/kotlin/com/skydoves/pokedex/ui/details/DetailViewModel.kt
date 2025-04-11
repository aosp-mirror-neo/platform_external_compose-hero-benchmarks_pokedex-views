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

package com.skydoves.pokedex.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.pokedex.core.model.PokemonInfo
import com.skydoves.pokedex.core.repository.DetailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class DetailViewModel(detailRepository: DetailRepository) : ViewModel() {

  private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading

  private val _toastMessage: MutableStateFlow<String?> = MutableStateFlow(null)
  val toastMessage: StateFlow<String?> = _toastMessage

  val pokemonName: MutableStateFlow<String?> = MutableStateFlow(null)

  @OptIn(ExperimentalCoroutinesApi::class)
  val pokemonInfo: StateFlow<PokemonInfo?> =
    pokemonName
      .filterNotNull()
      .flatMapLatest { pokemonName ->
        detailRepository.fetchPokemonInfo(
          name = pokemonName,
          onComplete = { _isLoading.value = false },
          onError = { _toastMessage.value = it },
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}