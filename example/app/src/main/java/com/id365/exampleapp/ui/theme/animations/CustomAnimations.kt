package com.id365.exampleapp.ui.theme.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.id365.idverification.IdVerification
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * CustomAnimations contains two loading spinners that can be used to override the default animations in the SDK.
 */
@Composable
fun ExampleLoadingSpinner(
    modifier: Modifier = Modifier
) {
    val animationValue by rememberInfiniteTransition(label = "animationValue").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animateFloat"
    )

    Box(modifier = Modifier
        .aspectRatio(3 / 2F)
        .padding(horizontal = 60.dp)
    )
    {
        Canvas(modifier = modifier
            .fillMaxSize()
            .aspectRatio(1F)
        ) {
            val x = size.height/2 + sin(animationValue * PI * 2) * size.height
            val y = size.width/2 + cos(animationValue * PI * 2) * size.width
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Green,
                        Color.Blue,
                        Color.Red,
                    ),
                    center = Offset(x=x.toFloat(), y=y.toFloat())
                ),
                startAngle = 0F,
                sweepAngle = 360F,
                useCenter = false,
                style = Fill
            )
        }
    }
}



@Composable
fun DifferentExampleLoadingSpinner(modifier: Modifier = Modifier) {
    val animatedValue = rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    ).value


    Box(modifier = Modifier
        .aspectRatio(3/2F)
        .padding(horizontal = 60.dp)
    )
    {
        val tintColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1F)
        ) {

            // Get strokeWidth of the circle
            // The minimum width or height divided into a smaller part
            val strokeWidth = size.minDimension / 20

            // Get values of start/sweep-angle
            val startValue = ((-cos(Math.PI * animatedValue)) / 2) + 0.51
            val endValue = ((-cos(Math.PI * animatedValue.pow(3))) / 2) + 0.5
            val startAngle = startValue * 360 - 90
            val endAngle = endValue * 360 - 90
            val sweepAngle = endAngle - startAngle

            drawArc(
                color = tintColor,
                startAngle = startAngle.toFloat(),
                sweepAngle = sweepAngle.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth,
                    cap = StrokeCap.Round,
                )
            )
        }
    }
}