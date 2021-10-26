/*
 * Copyright (c) 2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.example.backgroundpositioningexample;

import com.here.android.example.backgroundpositioningexample.IPositioningServiceListener;

/*
 * Simple AIDL interface for controlling positioning service.
 */
interface IPositioningServiceControl {

    /* Set service listener. */
    void setListener(IPositioningServiceListener listener);
    /* Start background positioning. */
    void startBackground();
    /* Stop background positioning. */
    void stopBackground();

}
