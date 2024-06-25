/*
 * Copyright 2024 Google LLC
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

package com.google.android.gms.example.jetpackcomposedemo.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A composable function to create a status text box.
 *
 * @param messageColor The background color of the box.
 * @param messageText The text to be displayed in the box.
 * @param modifier The Modifier to be applied to this button.
 */
@Composable
fun StatusText(messageColor: Color, messageText: String, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth().background(messageColor)) {
    Text(text = messageText, style = MaterialTheme.typography.bodyLarge)
  }
}
