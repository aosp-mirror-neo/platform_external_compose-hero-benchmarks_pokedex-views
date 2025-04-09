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
import com.android.build.gradle.internal.tasks.databinding.DataBindingGenBaseClassesTask
import com.skydoves.pokedex.Configuration
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.hilt.plugin)
}

android {
  namespace = "com.skydoves.pokedex"

  defaultConfig {
    applicationId = "com.skydoves.pokedex"
    testInstrumentationRunner = "com.skydoves.pokedex.AppTestRunner"

    // The schemas directory contains a schema file for each version of the Room database.
    // This is required to enable Room auto migrations.
    // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
    ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
    }
  }

  buildFeatures {
    dataBinding = true
    buildConfig = true
  }

  hilt {
    enableAggregatingTask = true
  }

  kotlin {
    sourceSets.configureEach {
      kotlin.srcDir(layout.buildDirectory.files("generated/ksp/$name/kotlin/"))
    }
    sourceSets.all {
      languageSettings {
        languageVersion = "2.0"
      }
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
}

androidComponents {
  onVariants(selector().all()) { variant ->
    afterEvaluate {
      val dataBindingTask =
        project.tasks.findByName("dataBindingGenBaseClasses" + variant.name.capitalized()) as? DataBindingGenBaseClassesTask
      if (dataBindingTask != null) {
        project.tasks.getByName("ksp" + variant.name.capitalized() + "Kotlin") {
          (this as AbstractKotlinCompileTool<*>).setSource(dataBindingTask.sourceOutFolder)
        }
      }
    }
  }
}

dependencies {
  // modules
  implementation(projects.coreData)

  // modules for unit test
  testImplementation(projects.coreNetwork)
  testImplementation(projects.coreDatabase)
  testImplementation(projects.coreTest)

  // androidx
  implementation(libs.material)
  implementation(libs.androidx.fragment)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.palette)

  // data binding
  implementation(libs.bindables)

  // di
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  androidTestImplementation(libs.hilt.testing)
  kspAndroidTest(libs.hilt.compiler)

  // coroutines
  implementation(libs.coroutines)

  // whatIf
  implementation(libs.whatif)

  // image loading
  implementation(libs.glide)

  // bundler
  implementation(libs.bundler)

  // transformation animation
  implementation(libs.transformationLayout)

  // recyclerView
  implementation(libs.recyclerview)
  implementation(libs.baseAdapter)

  // custom views
  implementation(libs.rainbow)
  implementation(libs.androidRibbon)
  implementation(libs.progressView)

  // coroutines
  implementation(libs.coroutines)
  testImplementation(libs.coroutines)
  testImplementation(libs.coroutines.test)

  // network
  implementation(libs.sandwich)
  implementation(libs.retrofit)
  implementation(libs.retrofit.moshi)
  implementation(libs.okhttp.interceptor)

  // json parsing
  implementation(libs.moshi)
  ksp(libs.moshi.codegen)

  // di
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // From core-model
  // json parsing
  implementation(libs.moshi)
  ksp(libs.moshi.codegen)

  // ----
  // From core-database
  // Room
  // coroutines
  implementation(libs.coroutines)
  testImplementation(libs.coroutines)
  testImplementation(libs.coroutines.test)

  // database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  testImplementation(libs.androidx.arch.core)

  // json parsing
  implementation(libs.moshi)
  ksp(libs.moshi.codegen)

  // di
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // ---
  // From core-data
  implementation(projects.coreNetwork)
  implementation(projects.coreDatabase)
  testImplementation(projects.coreTest)

  // coroutines
  implementation(libs.coroutines)
  testImplementation(libs.coroutines)
  testImplementation(libs.coroutines.test)

  // network
  implementation(libs.sandwich)

  // di
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

}
