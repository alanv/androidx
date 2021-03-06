/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture

/**
 * [ListenableFuture]-based compatibility wrapper around [PagingSource]'s suspending APIs.
 *
 * @sample androidx.paging.samples.listenableFuturePagingSourceSample
 */
abstract class ListenableFuturePagingSource<Key : Any, Value : Any> : PagingSource<Key, Value>() {
    /**
     * Loading API for [PagingSource].
     *
     * Implement this method to trigger your async load (e.g. from database or network).
     */
    abstract fun loadFuture(params: LoadParams<Key>): ListenableFuture<LoadResult<Key, Value>>

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        return loadFuture(params).await()
    }
}
