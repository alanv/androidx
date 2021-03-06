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

package androidx.compose.foundation

import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DrawModifier
import androidx.ui.core.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.util.annotation.FloatRange

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundColor
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
fun Modifier.background(
    color: Color,
    shape: Shape = RectangleShape
) = this.then(Background(
    shape,
    {
        drawRect(color)
    },
    { outline ->
        drawOutline(outline, color)
    }
))

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundShapedBrush
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 * @param alpha Opacity to be applied to the [brush]
 */
fun Modifier.background(
    brush: Brush,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f
) = this.then(Background(
    shape,
    {
        drawRect(brush = brush, alpha = alpha)
    },
    { outline ->
        drawOutline(outline, brush, alpha = alpha)
    }
))

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundColor
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
@Deprecated(
    "Use Modifier.background instead", replaceWith = ReplaceWith(
        "this.background(color = color, shape = shape)",
        "androidx.compose.foundation.background"
    )
)
@Suppress("UNUSED_PARAMETER")
fun Modifier.drawBackground(
    color: Color,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
) = background(color, shape)

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundShapedBrush
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 */
@Deprecated(
    "Use Modifier.background instead", replaceWith = ReplaceWith(
        "this.background(brush = brush, shape = shape)",
        "androidx.compose.foundation.background"
    )
)
@Suppress("UNUSED_PARAMETER")
fun Modifier.drawBackground(
    brush: Brush,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
) = background(brush, shape, alpha)

private data class Background internal constructor(
    private val shape: Shape,
    private val drawRect: ContentDrawScope.() -> Unit,
    private val drawOutline: ContentDrawScope.(outline: Outline) -> Unit
) : DrawModifier {

    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            val localOutline =
                if (size == lastSize) {
                    lastOutline!!
                } else {
                    shape.createOutline(size, this)
                }
            drawOutline(localOutline)
            lastOutline = localOutline
            lastSize = size
        }
        drawContent()
    }
}