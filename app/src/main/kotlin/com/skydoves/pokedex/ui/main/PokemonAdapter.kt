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

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.skydoves.pokedex.R
import com.skydoves.pokedex.core.PokedexFeatureFlags
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.databinding.ItemPokemonContentBinding
import com.skydoves.pokedex.databinding.ItemPokemonTransformationLayoutBinding
import com.skydoves.transformationlayout.TransformationLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PokemonAdapter(
    private val onItemClicked: (Pokemon, TransformationLayout?) -> Unit,
    private val fullyDrawnReporter: () -> Unit,
) : ListAdapter<Pokemon, PokemonAdapter.PokemonViewHolder>(diffUtil) {

    private var onClickedAt = 0L
    private var adapterCoroutineScope: CoroutineScope? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val binding: ViewBinding
        val itemBinding: ItemPokemonContentBinding
        var transformationLayoutDuration: Long = 0
        if (PokedexFeatureFlags.EnableTransformationLayout) {
            binding =
                ItemPokemonTransformationLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            itemBinding = binding.pokemonItemContent
            transformationLayoutDuration = binding.transformationLayout.duration
        } else {
            binding =
                ItemPokemonContentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            itemBinding = binding
        }
        return PokemonViewHolder(
            root = binding.root,
            binding = itemBinding,
            onItemClicked = { pokemon ->
                val currentClickedAt = SystemClock.elapsedRealtime()
                if (currentClickedAt - onClickedAt > transformationLayoutDuration) {
                    onItemClicked(
                        pokemon,
                        (binding as? ItemPokemonTransformationLayoutBinding)?.transformationLayout,
                    )
                    onClickedAt = currentClickedAt
                }
            },
        )
    }

    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapterCoroutineScope = MainScope()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterCoroutineScope?.cancel()
        adapterCoroutineScope = null
    }

    inner class PokemonViewHolder(
        root: View,
        private val binding: ItemPokemonContentBinding,
        private val onItemClicked: (Pokemon) -> Unit,
    ) : RecyclerView.ViewHolder(root) {

        private val glideRequestListener =
            PokemonGlideRequestListener(
                onResourceReady = { drawable ->
                    if (!PokedexFeatureFlags.EnablePaletteExtraction)
                        return@PokemonGlideRequestListener
                    if (drawable !is BitmapDrawable) return@PokemonGlideRequestListener
                    val bitmap = drawable.bitmap
                    adapterCoroutineScope!!.launch(Dispatchers.IO) {
                        val palette = Palette.from(bitmap).generate()
                        val rgb = palette.dominantSwatch?.rgb
                        if (rgb != null) {
                            withContext(Dispatchers.Main) {
                                binding.cardView.setCardBackgroundColor(rgb)
                            }
                        }
                    }
                }
            )

        init {
            binding.root.setOnClickListener {
                val position =
                    bindingAdapterPosition.takeIf { it != NO_POSITION } ?: return@setOnClickListener
                onItemClicked(getItem(position))
            }
        }

        fun bind(pokemon: Pokemon) {
            var requestBuilder =
                Glide.with(binding.root.context)
                    .load(pokemon.imageUrl)
                    .dontAnimate()
                    .placeholder(R.drawable.pokemon_preview)
            if (PokedexFeatureFlags.EnablePaletteExtraction) {
                requestBuilder = requestBuilder.listener(glideRequestListener)
            }
            requestBuilder.into(binding.image)
            binding.name.text = pokemon.name

            fullyDrawnReporter.invoke()
        }
    }

    companion object {
        private val diffUtil =
            object : DiffUtil.ItemCallback<Pokemon>() {

                override fun areItemsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean =
                    oldItem.name == newItem.name

                override fun areContentsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean =
                    oldItem == newItem
            }
    }
}

internal class PokemonGlideRequestListener(
    private val onLoadFailed: ((e: GlideException?) -> Unit)? = null,
    private val onResourceReady: ((resource: Drawable) -> Unit)? = null,
) : RequestListener<Drawable> {
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean,
    ): Boolean {
        e?.printStackTrace()
        onLoadFailed?.invoke(e)
        return false
    }

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean,
    ): Boolean {
        onResourceReady?.invoke(resource)
        return false
    }
}
