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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.skydoves.androidribbon.RibbonRecyclerView
import com.skydoves.androidribbon.ribbonView
import com.skydoves.bundler.bundleNonNull
import com.skydoves.bundler.intentOf
import com.skydoves.pokedex.R
import com.skydoves.pokedex.core.PokedexViewsViewModelProviderFactory
import com.skydoves.pokedex.core.di.ModuleLocator
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.core.model.PokemonInfo
import com.skydoves.pokedex.databinding.ActivityDetailBinding
import com.skydoves.pokedex.ui.main.PokemonGlideRequestListener
import com.skydoves.pokedex.utils.PokemonTypeUtils
import com.skydoves.pokedex.utils.SpacesItemDecoration
import com.skydoves.progressview.ProgressView
import com.skydoves.rainbow.Rainbow
import com.skydoves.rainbow.RainbowOrientation
import com.skydoves.rainbow.color
import com.skydoves.transformationlayout.TransformationCompat
import com.skydoves.transformationlayout.TransformationCompat.onTransformationEndContainerApplyParams
import com.skydoves.transformationlayout.TransformationLayout
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity(R.layout.activity_detail) {

  private lateinit var binding: ActivityDetailBinding

  internal val viewModel: DetailViewModel by viewModels {
    PokedexViewsViewModelProviderFactory(ModuleLocator.repositoryModule)
  }

  private val pokemon: Pokemon by bundleNonNull(EXTRA_POKEMON)

  override fun onCreate(savedInstanceState: Bundle?) {
    ModuleLocator.attach { application }
    onTransformationEndContainerApplyParams(this)
    super.onCreate(savedInstanceState)
    binding = ActivityDetailBinding.inflate(layoutInflater)
    setContentView(binding.root)

    bindViews()
    observeViewModel()

    // Trigger initial data loading by setting the pokemon name
    viewModel.pokemonName.value = pokemon.name
  }

  private fun bindViews() {
    binding.arrow.setOnClickListener {
      onBackPressedDispatcher.onBackPressed()
    }
    binding.name.text = pokemon.name
    binding.bindPokemonImage(pokemon.imageUrl)
  }

  private fun observeViewModel() {
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.pokemonInfo.collect { pokemonInfo ->
            if (pokemonInfo != null) {
              binding.index.text = pokemonInfo.getIdString()
              binding.weight.text = pokemonInfo.getWeightString()
              binding.height.text = pokemonInfo.getHeightString()
              binding.ribbonRecyclerView.bindPokemonTypes(pokemonInfo.types)
              binding.bindProgressBars(pokemonInfo)
            }
          }
        }
        launch {
          viewModel.isLoading.collect { isLoading ->
            binding.progressbar.isVisible = isLoading
          }
        }
        launch {
          viewModel.toastMessage.collect { message ->
            if (!message.isNullOrEmpty()) {
              Toast.makeText(this@DetailActivity, message, Toast.LENGTH_SHORT).show()
            }
          }
        }
      }
    }
  }

  private fun ProgressView.bind(
    labelText: String,
    max: Float,
    progress: Float
  ) {
    this.labelText = labelText
    this.max = max
    this.progress = progress
  }

  private fun ActivityDetailBinding.bindProgressBars(pokemonInfo: PokemonInfo) {
    progressHp.bind(
      labelText = pokemonInfo.getHpString(),
      max = PokemonInfo.MAX_HP.toFloat(),
      progress = pokemonInfo.hp.toFloat()
    )
    attackProgress.bind(
      labelText = pokemonInfo.getAttackString(),
      max = PokemonInfo.MAX_ATTACK.toFloat(),
      progress = pokemonInfo.attack.toFloat()
    )
    defenseProgress.bind(
      labelText = pokemonInfo.getDefenseString(),
      max = PokemonInfo.MAX_DEFENSE.toFloat(),
      progress = pokemonInfo.defense.toFloat()
    )
    speedProgress.bind(
      labelText = pokemonInfo.getSpeedString(),
      max = PokemonInfo.MAX_SPEED.toFloat(),
      progress = pokemonInfo.speed.toFloat()
    )
    expProgress.bind(
      labelText = pokemonInfo.getExpString(),
      max = PokemonInfo.MAX_EXP.toFloat(),
      progress = pokemonInfo.exp.toFloat()
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    ModuleLocator.detach()
  }

  companion object {
    internal const val EXTRA_POKEMON = "EXTRA_POKEMON"

    fun startActivity(transformationLayout: TransformationLayout, pokemon: Pokemon) =
      transformationLayout.context.intentOf<DetailActivity> {
        putExtra(EXTRA_POKEMON to pokemon)
        TransformationCompat.startActivity(transformationLayout, intent)
      }
  }
}

private fun RibbonRecyclerView.bindPokemonTypes(types: List<PokemonInfo.TypeResponse>?) {
  if (!types.isNullOrEmpty()) {
    this.clear()
    for (type in types) {
      addRibbon(
        ribbonView(context) {
          setText(type.type.name)
          setTextColor(Color.WHITE)
          setPaddingLeft(84f)
          setPaddingRight(84f)
          setPaddingTop(2f)
          setPaddingBottom(10f)
          setTextSize(16f)
          setRibbonRadius(120f)
          setTextStyle(Typeface.BOLD)
          setRibbonBackgroundColorResource(
            PokemonTypeUtils.getTypeColor(type.type.name),
          )
        }.apply {
          maxLines = 1
          gravity = Gravity.CENTER
        },
      )
      addItemDecoration(SpacesItemDecoration())
    }
  }
}

private fun ActivityDetailBinding.bindPokemonImage(url: String) {
  Glide.with(root.context)
    .load(url)
    .listener(
      PokemonGlideRequestListener(
        onResourceReady = { resource ->
          if (resource !is BitmapDrawable) return@PokemonGlideRequestListener
          header.bindPalette(
            resource.bitmap,
            onBackgroundColorReady = { color ->
              val window = (root.context as? AppCompatActivity)?.window
              if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = color
              }
            }
          )
        }
      )
    )
    .into(this.header)
}

private fun ShapeableImageView.bindPalette(
  bitmap: Bitmap,
  onBackgroundColorReady: (Int) -> Unit = {}
) {
  Palette.Builder(bitmap).generate { palette ->
    val light = palette?.lightVibrantSwatch?.rgb
    val dominantColor = palette?.dominantSwatch?.rgb
    if (dominantColor != null) {
      if (light != null) {
        Rainbow(this).palette {
          +color(dominantColor)
          +color(light)
        }.background(orientation = RainbowOrientation.TOP_BOTTOM)
      } else {
        this.setBackgroundColor(dominantColor)
      }
      onBackgroundColorReady(dominantColor)
    }
  }
}

