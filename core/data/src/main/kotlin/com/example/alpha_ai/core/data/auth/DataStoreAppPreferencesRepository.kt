package com.example.alpha_ai.core.data.auth

import com.example.alpha_ai.core.datastore.AppPreferencesStore
import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** [AppPreferencesRepository] on the plain preferences store. Never throws. */
internal class DataStoreAppPreferencesRepository @Inject constructor(
    private val store: AppPreferencesStore,
) : AppPreferencesRepository {

    override suspend fun isIntroductionCompleted(): Boolean = try {
        store.introductionCompleted.first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("Reading app preferences failed (%s)", e.javaClass.simpleName)
        false
    }

    override suspend fun setIntroductionCompleted() {
        try {
            store.setIntroductionCompleted()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w("Saving app preferences failed (%s)", e.javaClass.simpleName)
        }
    }
}
