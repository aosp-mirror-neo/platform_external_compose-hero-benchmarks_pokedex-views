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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Transition
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import androidx.tracing.Trace
import androidx.tracing.trace
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.skydoves.androidribbon.RibbonRecyclerView
import com.skydoves.androidribbon.ribbonView
import com.skydoves.bundler.bundleNonNull
import com.skydoves.bundler.intentOf
import com.skydoves.pokedex.R
import com.skydoves.pokedex.core.PokedexFeatureFlags
import com.skydoves.pokedex.core.PokedexFeatureFlags.Keys.POKEDEX_API_URL
import com.skydoves.pokedex.core.PokedexFeatureFlags.Keys.POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS
import com.skydoves.pokedex.core.PokedexFeatureFlags.Keys.POKEDEX_ENABLE_TRANSFORMATION_LAYOUT
import com.skydoves.pokedex.core.PokedexViewsViewModelProviderFactory
import com.skydoves.pokedex.core.database.entitiy.getPokemonImageUrlByName
import com.skydoves.pokedex.core.di.ModuleLocator
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.core.model.PokemonInfo
import com.skydoves.pokedex.databinding.ActivityDetailBinding
import com.skydoves.pokedex.ui.main.PokemonGlideRequestListener
import com.skydoves.pokedex.utils.PokemonTypeUtils
import com.skydoves.pokedex.utils.SpacesItemDecoration
import com.skydoves.pokedex.utils.requireBooleanExtra
import com.skydoves.progressview.ProgressView
import com.skydoves.rainbow.Rainbow
import com.skydoves.rainbow.RainbowOrientation
import com.skydoves.rainbow.color
import com.skydoves.transformationlayout.TransformationCompat
import com.skydoves.transformationlayout.TransformationCompat.onTransformationEndContainerApplyParams
import com.skydoves.transformationlayout.TransformationLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl

class DetailActivity : AppCompatActivity(R.layout.activity_detail) {

    private lateinit var binding: ActivityDetailBinding

    internal val viewModel: DetailViewModel by viewModels {
        PokedexViewsViewModelProviderFactory(ModuleLocator.repositoryModule)
    }

    private lateinit var _pokemon: Lazy<Pokemon>
    private val pokemon
        get() = _pokemon.value

    /**
     * Cookie for the trace block of the home <-> details transition. Should be set when starting a
     * transition and reset when it ends.
     */
    private var transitionTraceCookie: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        trace("DetailActivity Setup") {
            if (intent.hasExtra(POKEDEX_API_URL)) {
                ModuleLocator.networkModule.baseUrl =
                    intent.getStringExtra(POKEDEX_API_URL)!!.toHttpUrl()
            }

            val startDestination = intent.getStringExtra("startDestination")
            setupSharedElementTransition(startDestination)
            transitionTraceCookie = intent.getIntExtra(TRACE_COOKIE, -1)
            setupPokemonData(startDestination)
        }

        super.onCreate(savedInstanceState)
        trace("ModuleLocator#attach") { ModuleLocator.attach { application } }
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindViews()
        observeViewModel()

        // Trigger initial data loading by setting the pokemon name
        viewModel.pokemonName.value = pokemon.name
    }

    private fun setupPokemonData(startDestination: String?) {
        _pokemon =
            if (startDestination == "details") {
                lazy {
                    val name = "Ablazeon"
                    Pokemon(name = name, imageUrl = getPokemonImageUrlByName(name).toString())
                }
            } else {
                bundleNonNull<Pokemon>(EXTRA_POKEMON)
            }
    }

    private fun setupSharedElementTransition(startDestination: String?) {
        // For startup benchmarks, our start destination will be details. In that case, we won't
        //  have initialized the feature flags yet. If our start destination is something else,
        //  assume we have already initialized the flags.
        if (startDestination == "details") {
            PokedexFeatureFlags.EnableTransformationLayout =
                intent.requireBooleanExtra(POKEDEX_ENABLE_TRANSFORMATION_LAYOUT)
            PokedexFeatureFlags.EnableSharedElementTransitions =
                intent.requireBooleanExtra(POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS)
        } else {
            if (PokedexFeatureFlags.EnableSharedElementTransitions) {
                onTransformationEndContainerApplyParams(this)
                setupSharedElementTransitionListeners()
            }
        }
    }

    private fun bindViews() {
        binding.pokedexDetailsBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.name.text = pokemon.name
        binding.bindPokemonImage(
            model = pokemon.imageUrl,
            onImageReady = { drawable ->
                if (drawable !is BitmapDrawable) return@bindPokemonImage
                lifecycleScope.launch { binding.header.bindPalette(drawable.bitmap) }
            },
        )
    }

    private fun setupSharedElementTransitionListeners() {
        val sharedElementTransitionListener =
            object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) {
                    Log.d("DetailActivity", "onTransitionStart $transition")
                    binding.transitionStatus.text = DETAILS_TRANSITION_ACTIVE
                }

                override fun onTransitionEnd(transition: Transition) {
                    Log.d("DetailActivity", "onTransitionEnd $transition")
                    binding.transitionStatus.text = DETAILS_TRANSITION_INACTIVE
                    endDetailsTransitionTrace()
                }

                override fun onTransitionCancel(transition: Transition?) {}

                override fun onTransitionPause(transition: Transition?) {}

                override fun onTransitionResume(transition: Transition?) {}
            }

        window.sharedElementEnterTransition?.addListener(sharedElementTransitionListener)
        window.sharedElementReturnTransition?.addListener(sharedElementTransitionListener)
    }

    override fun onEnterAnimationComplete() {
        Log.d("DetailActivity", "onEnterAnimationComplete")
        // onEnterAnimationComplete can get called multiple times when we have more than one
        //  transition, which is the case for shared element. For shared element, we rely on the
        //  transition listeners to update the status instead.
        if (!PokedexFeatureFlags.EnableSharedElementTransitions) {
            binding.transitionStatus.text = DETAILS_TRANSITION_INACTIVE
            endDetailsTransitionTrace()
        }
    }

    private fun endDetailsTransitionTrace() {
        if (transitionTraceCookie != -1) {
            Trace.endAsyncSection(NAVIGATION_TRANSITION_TRACE_NAME, transitionTraceCookie)
            transitionTraceCookie = -1
        }
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
                            reportFullyDrawn()
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

    private fun ProgressView.bind(labelText: String, max: Float, progress: Float) {
        this.labelText = labelText
        this.max = max
        this.progress = progress
    }

    private fun ActivityDetailBinding.bindProgressBars(pokemonInfo: PokemonInfo) {
        progressHp.bind(
            labelText = pokemonInfo.getHpString(),
            max = PokemonInfo.MAX_HP.toFloat(),
            progress = pokemonInfo.hp.toFloat(),
        )
        attackProgress.bind(
            labelText = pokemonInfo.getAttackString(),
            max = PokemonInfo.MAX_ATTACK.toFloat(),
            progress = pokemonInfo.attack.toFloat(),
        )
        defenseProgress.bind(
            labelText = pokemonInfo.getDefenseString(),
            max = PokemonInfo.MAX_DEFENSE.toFloat(),
            progress = pokemonInfo.defense.toFloat(),
        )
        speedProgress.bind(
            labelText = pokemonInfo.getSpeedString(),
            max = PokemonInfo.MAX_SPEED.toFloat(),
            progress = pokemonInfo.speed.toFloat(),
        )
        expProgress.bind(
            labelText = pokemonInfo.getExpString(),
            max = PokemonInfo.MAX_EXP.toFloat(),
            progress = pokemonInfo.exp.toFloat(),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ModuleLocator.detach()
    }

    companion object {
        const val NAVIGATION_TRANSITION_TRACE_NAME = "Pokedex Details Navigation Transition"
        internal const val EXTRA_POKEMON = "EXTRA_POKEMON"
        internal const val TRACE_COOKIE = "TRACE_COOKIE"
        private const val DETAILS_TRANSITION_ACTIVE = "pokedex-details-transition-active-true"
        private const val DETAILS_TRANSITION_INACTIVE = "pokedex-details-transition-active-false"

        fun startActivity(context: Context, pokemon: Pokemon, traceCookie: Int) {
            context.intentOf<DetailActivity> {
                putExtra(EXTRA_POKEMON to pokemon)
                putExtra(TRACE_COOKIE to traceCookie)
                startActivity(context)
            }
        }

        fun startActivityWithTransition(
            transformationLayout: TransformationLayout,
            pokemon: Pokemon,
            traceCookie: Int,
        ) =
            transformationLayout.context.intentOf<DetailActivity> {
                putExtra(EXTRA_POKEMON to pokemon)
                putExtra(TRACE_COOKIE to traceCookie)
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
                            PokemonTypeUtils.getTypeColor(type.type.name)
                        )
                    }
                    .apply {
                        maxLines = 1
                        gravity = Gravity.CENTER
                    }
            )
            addItemDecoration(SpacesItemDecoration())
        }
    }
}

private fun ActivityDetailBinding.bindPokemonImage(
    model: Any,
    onImageReady: (Drawable) -> Unit = {},
) {
    Glide.with(root.context)
        .load(model)
        .listener(
            PokemonGlideRequestListener(onResourceReady = { resource -> onImageReady(resource) })
        )
        .into(this.header)
}

private suspend fun ShapeableImageView.bindPalette(
    bitmap: Bitmap,
    onBackgroundColorReady: (Int) -> Unit = {},
) {
    val palette = withContext(Dispatchers.IO) { Palette.Builder(bitmap).generate() }
    val light = palette.lightVibrantSwatch?.rgb
    val dominantColor = palette.dominantSwatch?.rgb
    if (dominantColor != null) {
        withContext(Dispatchers.Main) {
            if (light != null) {
                Rainbow(this@bindPalette)
                    .palette {
                        +color(dominantColor)
                        +color(light)
                    }
                    .background(orientation = RainbowOrientation.TOP_BOTTOM)
            } else {
                this@bindPalette.setBackgroundColor(dominantColor)
            }
        }
        onBackgroundColorReady(dominantColor)
    }
}
