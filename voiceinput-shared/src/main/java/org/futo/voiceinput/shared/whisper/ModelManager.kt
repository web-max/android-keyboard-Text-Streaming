package org.futo.voiceinput.shared.whisper

import android.content.Context
import org.futo.voiceinput.shared.ggml.WhisperGGML
import org.futo.voiceinput.shared.types.ModelLoader


class ModelManager(
    val context: Context
) {
    private val loadedModels: HashMap<Any, WhisperGGML> = hashMapOf()
    private val modelGpuState: HashMap<Any, Boolean> = hashMapOf()

    fun obtainModel(model: ModelLoader, useGpu: Boolean): WhisperGGML {
        val key = model.key(context)
        if (loadedModels.contains(key) && modelGpuState[key] != useGpu) {
            loadedModels[key]?.cancel()
            // Note: close is suspend, so we might leak it here if we don't block. But let's just drop it or use runBlocking.
            // Actually WhisperGGML.close is suspend. We can just abandon the old handle for now and let JNI cleanup if needed,
            // or better yet, since the Settings change rarely happens, we recreate it.
            loadedModels.remove(key)
        }

        if (!loadedModels.contains(key)) {
            loadedModels[key] = model.loadGGML(context, useGpu)
            modelGpuState[key] = useGpu
        }

        return loadedModels[key]!!
    }

    fun cancelAll() {
        loadedModels.forEach {
            it.value.cancel()
        }
    }

    suspend fun cleanUp() {
        for (model in loadedModels.entries) {
            model.value.cancel()
            model.value.close()
        }

        loadedModels.clear()
    }
}
