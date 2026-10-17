package com.example.firstapp

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

/*
 * All game physics use normalized units where the screen HEIGHT == 1.0.
 * The screen width is therefore `aspect` (= width / height).
 * This makes the game play identically on every screen size.
 */
private const val GRAVITY = 2.6f          // units / s^2
private const val FLAP_VELOCITY = -0.75f  // units / s
private const val MAX_FALL_SPEED = 1.6f
private const val PIPE_SPEED = 0.32f      // units / s
private const val PIPE_WIDTH = 0.13f
private const val PIPE_GAP = 0.27f
private const val PIPE_SPACING = 0.55f
private const val BIRD_RADIUS = 0.028f
private const val GROUND_TOP = 0.88f
private const val START_Y = 0.42f

enum class GameState { Ready, Playing, GameOver }

class Pipe(var x: Float, val gapCenter: Float, var passed: Boolean = false)

class TahaBirdEngine(initialHighScore: Int, private val onHighScore: (Int) -> Unit) {
    var state by mutableStateOf(GameState.Ready)
        private set
    var score by mutableIntStateOf(0)
        private set
    var highScore by mutableIntStateOf(initialHighScore)
        private set

    /** Incremented every frame so the Canvas redraws. */
    var frame by mutableLongStateOf(0L)
        private set

    var birdY = START_Y
        private set
    var velocity = 0f
        private set
    var time = 0f
        private set
    var groundOffset = 0f
        private set
    val pipes = ArrayList<Pipe>()
    var aspect by mutableFloatStateOf(0.5f)

    val birdX: Float get() = aspect * 0.28f

    private var gameOverTime = 0f

    fun tap() {
        when (state) {
            GameState.Ready -> {
                state = GameState.Playing
                pipes.add(Pipe(aspect + 0.25f, randomGap()))
                flap()
            }
            GameState.Playing -> flap()
            GameState.GameOver -> if (time - gameOverTime > 0.6f) reset()
        }
    }

    private fun flap() {
        velocity = FLAP_VELOCITY
    }

    private fun reset() {
        pipes.clear()
        birdY = START_Y
        velocity = 0f
        score = 0
        state = GameState.Ready
    }

    private fun randomGap(): Float {
        val min = 0.12f + PIPE_GAP / 2
        val max = GROUND_TOP - 0.10f - PIPE_GAP / 2
        return min + Random.nextFloat() * (max - min)
    }

    fun update(dt: Float) {
        time += dt
        when (state) {
            GameState.Ready -> {
                birdY = START_Y + sin(time * 5f) * 0.012f
                groundOffset += PIPE_SPEED * dt
            }

            GameState.Playing -> {
                velocity = (velocity + GRAVITY * dt).coerceAtMost(MAX_FALL_SPEED)
                birdY += velocity * dt
                groundOffset += PIPE_SPEED * dt

                // Move, spawn and recycle pipes
                pipes.forEach { it.x -= PIPE_SPEED * dt }
                if (pipes.last().x < aspect - PIPE_SPACING) {
                    pipes.add(Pipe(aspect + 0.02f, randomGap()))
                }
                pipes.removeAll { it.x + PIPE_WIDTH < -0.05f }

                // Scoring
                for (p in pipes) {
                    if (!p.passed && p.x + PIPE_WIDTH < birdX) {
                        p.passed = true
                        score++
                    }
                }

                // Ceiling
                if (birdY - BIRD_RADIUS < 0f) {
                    birdY = BIRD_RADIUS
                    velocity = 0f
                }

                // Ground & pipe collisions
                if (birdY + BIRD_RADIUS >= GROUND_TOP) {
                    birdY = GROUND_TOP - BIRD_RADIUS
                    die()
                } else if (pipes.any { hitsPipe(it) }) {
                    die()
                }
            }

            GameState.GameOver -> {
                // Let the bird fall to the ground
                if (birdY + BIRD_RADIUS < GROUND_TOP) {
                    velocity = (velocity + GRAVITY * dt).coerceAtMost(MAX_FALL_SPEED)
                    birdY = (birdY + velocity * dt).coerceAtMost(GROUND_TOP - BIRD_RADIUS)
                }
            }
        }
        frame++
    }

    private fun hitsPipe(p: Pipe): Boolean {
        val r = BIRD_RADIUS * 0.85f // slightly forgiving hitbox
        val topBottom = p.gapCenter - PIPE_GAP / 2
        val bottomTop = p.gapCenter + PIPE_GAP / 2
        return circleHitsRect(birdX, birdY, r, p.x, 0f, p.x + PIPE_WIDTH, topBottom) ||
            circleHitsRect(birdX, birdY, r, p.x, bottomTop, p.x + PIPE_WIDTH, GROUND_TOP)
    }

    private fun circleHitsRect(
        cx: Float, cy: Float, r: Float,
        left: Float, top: Float, right: Float, bottom: Float,
    ): Boolean {
        val nx = cx.coerceIn(left, right)
        val ny = cy.coerceIn(top, bottom)
        val dx = cx - nx
        val dy = cy - ny
        return dx * dx + dy * dy < r * r
    }

    private fun die() {
        state = GameState.GameOver
        gameOverTime = time
        if (score > highScore) {
            highScore = score
            onHighScore(score)
        }
    }
}

@Composable
fun TahaBirdGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("taha_bird", Context.MODE_PRIVATE) }
    val engine = remember {
        TahaBirdEngine(prefs.getInt("high_score", 0)) { best ->
            prefs.edit().putInt("high_score", best).apply()
        }
    }

    // Game loop synced to the display refresh rate
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(1f / 30f)
                last = now
                engine.update(dt)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(engine) {
                detectTapGestures(onPress = { engine.tap() })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            engine.frame // read so we redraw every frame
            engine.aspect = size.width / size.height
            drawGame(engine)
        }

        GameOverlay(engine)
    }
}

private val titleStyle = TextStyle(
    color = Color.White,
    fontWeight = FontWeight.ExtraBold,
    shadow = Shadow(Color(0xFF3E2723), Offset(5f, 5f), 2f),
)

@Composable
private fun GameOverlay(engine: TahaBirdEngine) {
    when (engine.state) {
        GameState.Ready -> Column(
            modifier = Modifier.fillMaxSize().padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TAHA BIRD", style = titleStyle.copy(fontSize = 52.sp, color = Color(0xFFFFEB3B)))
            Spacer(Modifier.height(260.dp))
            Text("Tap to flap!", style = titleStyle.copy(fontSize = 26.sp))
            if (engine.highScore > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Best: ${engine.highScore}", style = titleStyle.copy(fontSize = 20.sp))
            }
        }

        GameState.Playing -> Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 40.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text("${engine.score}", style = titleStyle.copy(fontSize = 64.sp))
        }

        GameState.GameOver -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("GAME OVER", style = titleStyle.copy(fontSize = 46.sp, color = Color(0xFFFF7043)))
                Column(
                    modifier = Modifier
                        .background(Color(0xFFDED895), RoundedCornerShape(16.dp))
                        .padding(horizontal = 48.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("SCORE", color = Color(0xFFE86101), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${engine.score}", style = titleStyle.copy(fontSize = 40.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("BEST", color = Color(0xFFE86101), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${engine.highScore}", style = titleStyle.copy(fontSize = 40.sp))
                }
                Text("Tap to play again", style = titleStyle.copy(fontSize = 22.sp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------

private val PipeGreen = Color(0xFF73BF2E)
private val PipeDark = Color(0xFF3B6E14)
private val PipeLight = Color(0xFFA8E05F)

private fun DrawScope.drawGame(e: TahaBirdEngine) {
    val s = size.height // 1 unit in pixels

    // Sky
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF4EC0CA), Color(0xFFB6EAF0))),
        size = Size(size.width, GROUND_TOP * s),
    )

    // Parallax clouds
    val cloudShift = (e.groundOffset * 0.25f) % (e.aspect + 0.4f)
    for (i in 0..3) {
        val cx = ((i * 0.38f - cloudShift + 2f * (e.aspect + 0.4f)) % (e.aspect + 0.4f)) - 0.2f
        val cy = 0.12f + (i % 2) * 0.1f
        drawCloud(cx * s, cy * s, 0.045f * s)
    }

    // City/bushes silhouette
    val bushShift = (e.groundOffset * 0.5f) % 0.12f
    var bx = -bushShift
    while (bx < e.aspect + 0.12f) {
        drawCircle(Color(0xFF85D66B), radius = 0.07f * s, center = Offset(bx * s, (GROUND_TOP + 0.02f) * s))
        bx += 0.12f
    }

    // Pipes
    for (p in e.pipes) drawPipe(p, s)

    // Ground
    drawGround(e, s)

    // Bird
    drawBird(e, s)
}

private fun DrawScope.drawCloud(x: Float, y: Float, r: Float) {
    val c = Color.White.copy(alpha = 0.9f)
    drawCircle(c, r, Offset(x, y))
    drawCircle(c, r * 0.8f, Offset(x - r * 1.1f, y + r * 0.25f))
    drawCircle(c, r * 0.85f, Offset(x + r * 1.1f, y + r * 0.2f))
}

private fun DrawScope.drawPipe(p: Pipe, s: Float) {
    val left = p.x * s
    val w = PIPE_WIDTH * s
    val topEnd = (p.gapCenter - PIPE_GAP / 2) * s
    val bottomStart = (p.gapCenter + PIPE_GAP / 2) * s
    val groundY = GROUND_TOP * s
    val capH = 0.035f * s
    val capExtra = 0.008f * s

    fun body(top: Float, bottom: Float) {
        if (bottom <= top) return
        drawRect(PipeGreen, Offset(left, top), Size(w, bottom - top))
        drawRect(PipeLight, Offset(left + w * 0.12f, top), Size(w * 0.12f, bottom - top))
        drawRect(PipeDark, Offset(left + w * 0.8f, top), Size(w * 0.1f, bottom - top))
        drawRect(PipeDark, Offset(left, top), Size(w, bottom - top), style = Stroke(3f))
    }

    fun cap(top: Float) {
        val capLeft = left - capExtra
        val capW = w + capExtra * 2
        drawRect(PipeGreen, Offset(capLeft, top), Size(capW, capH))
        drawRect(PipeLight, Offset(capLeft + capW * 0.1f, top), Size(capW * 0.12f, capH))
        drawRect(PipeDark, Offset(capLeft, top), Size(capW, capH), style = Stroke(4f))
    }

    body(0f, topEnd - capH)
    cap(topEnd - capH)
    body(bottomStart + capH, groundY)
    cap(bottomStart)
}

private fun DrawScope.drawGround(e: TahaBirdEngine, s: Float) {
    val top = GROUND_TOP * s
    drawRect(Color(0xFFDED895), Offset(0f, top), Size(size.width, size.height - top))

    // Moving striped grass strip
    val stripH = 0.025f * s
    drawRect(Color(0xFF9CE659), Offset(0f, top), Size(size.width, stripH))
    clipRect(top = top, bottom = top + stripH) {
        val stripe = 0.03f * s
        var x = -((e.groundOffset * s) % (stripe * 2))
        while (x < size.width) {
            val path = Path().apply {
                moveTo(x, top + stripH)
                lineTo(x + stripe, top + stripH)
                lineTo(x + stripe * 1.6f, top)
                lineTo(x + stripe * 0.6f, top)
                close()
            }
            drawPath(path, Color(0xFF73BF2E))
            x += stripe * 2
        }
    }
    drawLine(Color(0xFF543847), Offset(0f, top), Offset(size.width, top), strokeWidth = 4f)
    drawLine(
        Color(0xFFD0A65E), Offset(0f, top + stripH), Offset(size.width, top + stripH), strokeWidth = 6f
    )
}

private fun DrawScope.drawBird(e: TahaBirdEngine, s: Float) {
    val cx = e.birdX * s
    val cy = e.birdY * s
    val r = BIRD_RADIUS * s * 1.15f

    val angle = when (e.state) {
        GameState.Ready -> 0f
        else -> (e.velocity / MAX_FALL_SPEED * 90f).coerceIn(-25f, 90f)
    }
    val wingFlap = if (e.state == GameState.GameOver) 0f else sin(e.time * 25f) * r * 0.3f
    val outline = Color(0xFF543847)

    rotate(angle, pivot = Offset(cx, cy)) {
        // Body
        drawCircle(Color(0xFFFAC22B), r, Offset(cx, cy))
        drawCircle(Color(0xFFFDE68A), r * 0.55f, Offset(cx - r * 0.1f, cy + r * 0.35f))
        drawCircle(outline, r, Offset(cx, cy), style = Stroke(r * 0.12f))

        // Wing
        val wingTopLeft = Offset(cx - r * 1.05f, cy - r * 0.05f + wingFlap)
        val wingSize = Size(r * 0.95f, r * 0.6f)
        drawOval(Color(0xFFFFF4C2), wingTopLeft, wingSize)
        drawOval(outline, wingTopLeft, wingSize, style = Stroke(r * 0.1f))

        // Eye
        val eye = Offset(cx + r * 0.38f, cy - r * 0.32f)
        drawCircle(Color.White, r * 0.4f, eye)
        drawCircle(outline, r * 0.4f, eye, style = Stroke(r * 0.08f))
        drawCircle(Color.Black, r * 0.16f, eye + Offset(r * 0.14f, 0f))

        // Beak
        val beak = Path().apply {
            moveTo(cx + r * 0.55f, cy + r * 0.02f)
            lineTo(cx + r * 1.4f, cy + r * 0.2f)
            lineTo(cx + r * 0.55f, cy + r * 0.48f)
            close()
        }
        drawPath(beak, Color(0xFFF46F30))
        drawPath(beak, outline, style = Stroke(r * 0.08f))
    }
}
