package com.semyqofu.aliasyou.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semyqofu.aliasyou.ui.AppStrings
import com.semyqofu.aliasyou.ui.components.Md3ExpressiveShape
import com.semyqofu.aliasyou.ui.components.bouncyClickable
import com.semyqofu.aliasyou.ui.components.computeExpressiveRadius
import com.semyqofu.aliasyou.ui.components.isWideLandscape
import kotlin.math.cos
import kotlin.math.sin

private data class BlobTarget(
    val topCx: Float,
    val topCy: Float,
    val topRadius: Float,
    val topPhaseDeg: Float,
    val bottomCx: Float,
    val bottomCy: Float,
    val bottomRadius: Float,
    val bottomPhaseDeg: Float
)

private data class ApiKeyStepSpec(
    val keyLabel: String,
    val placeholder: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val guideTitle: String,
    val guideSteps: List<String>,
    val linkLabel: String,
    val linkUrl: String,
    val expanded: Boolean,
    val onToggleExpanded: () -> Unit,
    val confirmLabel: String,
    val onSkip: () -> Unit,
    val onConfirm: () -> Unit
)

@Composable
fun LanguageOnboardingScreen(
    initialLanguage: String = "ru",
    initialGeminiKey: String = "",
    initialGigaChatKey: String = "",
    onLanguageSelected: (String) -> Unit,
    onContinue: (language: String, geminiKey: String, gigachatKey: String) -> Unit
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) } // 0: Language, 1: Gemini, 2: GigaChat
    var selectedLanguage by rememberSaveable { mutableStateOf(initialLanguage) }
    var geminiKey by rememberSaveable { mutableStateOf(initialGeminiKey) }
    var gigachatKey by rememberSaveable { mutableStateOf(initialGigaChatKey) }
    var geminiGuideExpanded by rememberSaveable { mutableStateOf(false) }
    var gigachatGuideExpanded by rememberSaveable { mutableStateOf(false) }

    val strings = remember(selectedLanguage) { AppStrings.forLanguage(selectedLanguage) }

    BackHandler(enabled = currentStep > 0) {
        currentStep = (currentStep - 1).coerceAtLeast(0)
    }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val triggerVibration = { duration: Long ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pixelOnboardingShapes")
    val topBlobRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "topBlobRotation"
    )
    val bottomBlobRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bottomBlobRotation"
    )

    // Smoothly glide background organic blobs across onboarding steps for Pixel-like parallax depth
    val targetBlob = when (currentStep) {
        0 -> BlobTarget(
            topCx = 0.88f, topCy = 0.06f, topRadius = 0.44f, topPhaseDeg = 0f,
            bottomCx = 0.42f, bottomCy = 0.90f, bottomRadius = 0.78f, bottomPhaseDeg = 0f
        )
        1 -> BlobTarget(
            topCx = 0.14f, topCy = 0.10f, topRadius = 0.48f, topPhaseDeg = 42f,
            bottomCx = 0.84f, bottomCy = 0.84f, bottomRadius = 0.70f, bottomPhaseDeg = -36f
        )
        else -> BlobTarget(
            topCx = 0.82f, topCy = 0.14f, topRadius = 0.50f, topPhaseDeg = 84f,
            bottomCx = 0.18f, bottomCy = 0.86f, bottomRadius = 0.74f, bottomPhaseDeg = -72f
        )
    }

    val blobSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    val topCxFactor by animateFloatAsState(targetBlob.topCx, animationSpec = blobSpring, label = "topCx")
    val topCyFactor by animateFloatAsState(targetBlob.topCy, animationSpec = blobSpring, label = "topCy")
    val topRadiusFactor by animateFloatAsState(targetBlob.topRadius, animationSpec = blobSpring, label = "topR")
    val topPhaseDeg by animateFloatAsState(targetBlob.topPhaseDeg, animationSpec = blobSpring, label = "topPhase")

    val bottomCxFactor by animateFloatAsState(targetBlob.bottomCx, animationSpec = blobSpring, label = "botCx")
    val bottomCyFactor by animateFloatAsState(targetBlob.bottomCy, animationSpec = blobSpring, label = "botCy")
    val bottomRadiusFactor by animateFloatAsState(targetBlob.bottomRadius, animationSpec = blobSpring, label = "botR")
    val bottomPhaseDeg by animateFloatAsState(targetBlob.bottomPhaseDeg, animationSpec = blobSpring, label = "botPhase")

    val topBlobColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val bottomBlobColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pixel-style slowly rotating & parallax-shifting scalloped organic background shapes
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawScallopedBlob(
                    cx = size.width * topCxFactor,
                    cy = size.height * topCyFactor,
                    baseRadius = size.minDimension * topRadiusFactor,
                    rotationDegrees = topBlobRotation + topPhaseDeg,
                    shape = Md3ExpressiveShape.COOKIE_7,
                    color = topBlobColor
                )

                drawScallopedBlob(
                    cx = size.width * bottomCxFactor,
                    cy = size.height * bottomCyFactor,
                    baseRadius = size.minDimension * bottomRadiusFactor,
                    rotationDegrees = bottomBlobRotation + bottomPhaseDeg,
                    shape = Md3ExpressiveShape.COOKIE_7,
                    color = bottomBlobColor
                )
            }

            val topHeaderBar: @Composable () -> Unit = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(visible = currentStep > 0) {
                            Surface(
                                onClick = {
                                    triggerVibration(15)
                                    currentStep = (currentStep - 1).coerceAtLeast(0)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 2.dp,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = strings.onboardingBackCd,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        val badgeIcon: ImageVector = when (currentStep) {
                            0 -> Icons.Rounded.Casino
                            1 -> Icons.Rounded.AutoAwesome
                            else -> Icons.Rounded.SmartToy
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = badgeIcon,
                                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                                    label = "badgeIcon"
                                ) { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Alias You",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    OnboardingStepDots(
                        totalSteps = 3,
                        currentStep = currentStep
                    )
                }
            }

            val stepTitle: @Composable (TextAlign) -> Unit = { alignment ->
                AnimatedContent(
                    targetState = Triple(currentStep, selectedLanguage, strings),
                    transitionSpec = {
                        val forward = targetState.first >= initialState.first
                        val slideInOffset = { fullWidth: Int -> if (forward) fullWidth / 4 else -fullWidth / 4 }
                        val slideOutOffset = { fullWidth: Int -> if (forward) -fullWidth / 4 else fullWidth / 4 }
                        (fadeIn(tween(280)) + slideInHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy), initialOffsetX = slideInOffset))
                            .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(200), targetOffsetX = slideOutOffset))
                    },
                    label = "stepTitleTransition"
                ) { (step, _, s) ->
                    val (headline, subtitle) = when (step) {
                        0 -> s.onboardingGreeting to s.onboardingSubtitle
                        1 -> s.onboardingGeminiTitle to s.onboardingGeminiSubtitle
                        else -> s.onboardingGigaChatTitle to s.onboardingGigaChatSubtitle
                    }
                    Column(
                        horizontalAlignment = if (alignment == TextAlign.Start) Alignment.Start else Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = headline,
                            style = if (step == 0) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = alignment
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = alignment
                        )
                    }
                }
            }

            val stepContentAndActions: @Composable () -> Unit = {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val forward = targetState >= initialState
                        val slideInOffset = { fullWidth: Int -> if (forward) fullWidth / 3 else -fullWidth / 3 }
                        val slideOutOffset = { fullWidth: Int -> if (forward) -fullWidth / 3 else fullWidth / 3 }
                        (fadeIn(tween(300)) + slideInHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy), initialOffsetX = slideInOffset))
                            .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(220), targetOffsetX = slideOutOffset))
                    },
                    label = "stepBodyTransition"
                ) { step ->
                    when (step) {
                        0 -> LanguageStepBody(
                            selectedLanguage = selectedLanguage,
                            strings = strings,
                            onSelectLanguage = { lang ->
                                triggerVibration(20)
                                selectedLanguage = lang
                                onLanguageSelected(lang)
                            },
                            onToggleLanguage = {
                                triggerVibration(15)
                                val next = if (selectedLanguage == "ru") "en" else "ru"
                                selectedLanguage = next
                                onLanguageSelected(next)
                            },
                            onContinue = {
                                triggerVibration(45)
                                currentStep = 1
                            }
                        )

                        1 -> ApiKeyStepBody(
                            spec = ApiKeyStepSpec(
                                keyLabel = strings.geminiKeyLabel,
                                placeholder = "AIzaSy...",
                                value = geminiKey,
                                onValueChange = { geminiKey = it },
                                guideTitle = strings.onboardingGeminiGuideTitle,
                                guideSteps = strings.onboardingGeminiGuideSteps,
                                linkLabel = strings.onboardingGeminiLinkLabel,
                                linkUrl = "https://aistudio.google.com/api-keys",
                                expanded = geminiGuideExpanded,
                                onToggleExpanded = {
                                    triggerVibration(10)
                                    geminiGuideExpanded = !geminiGuideExpanded
                                },
                                confirmLabel = strings.onboardingContinue,
                                onSkip = {
                                    triggerVibration(25)
                                    geminiKey = ""
                                    currentStep = 2
                                },
                                onConfirm = {
                                    triggerVibration(45)
                                    currentStep = 2
                                }
                            ),
                            skipLabel = strings.onboardingSkip
                        )

                        else -> ApiKeyStepBody(
                            spec = ApiKeyStepSpec(
                                keyLabel = strings.gigachatKeyLabel,
                                placeholder = "ODQ2...",
                                value = gigachatKey,
                                onValueChange = { gigachatKey = it },
                                guideTitle = strings.onboardingGigaChatGuideTitle,
                                guideSteps = strings.onboardingGigaChatGuideSteps,
                                linkLabel = strings.onboardingGigaChatLinkLabel,
                                linkUrl = "https://developers.sber.ru/studio/login",
                                expanded = gigachatGuideExpanded,
                                onToggleExpanded = {
                                    triggerVibration(10)
                                    gigachatGuideExpanded = !gigachatGuideExpanded
                                },
                                confirmLabel = strings.onboardingDone,
                                onSkip = {
                                    triggerVibration(25)
                                    onContinue(selectedLanguage, geminiKey.trim(), "")
                                },
                                onConfirm = {
                                    triggerVibration(45)
                                    onContinue(selectedLanguage, geminiKey.trim(), gigachatKey.trim())
                                }
                            ),
                            skipLabel = strings.onboardingSkip
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (isWideLandscape(maxWidth, maxHeight)) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            topHeaderBar()
                            stepTitle(TextAlign.Start)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 460.dp)
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                stepContentAndActions()
                            }
                        }
                    }
                } else {
                    val viewportHeight = maxHeight
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = viewportHeight),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                topHeaderBar()

                                Spacer(modifier = Modifier.height(20.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(22.dp)
                                ) {
                                    stepTitle(TextAlign.Center)
                                    stepContentAndActions()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepDots(
    totalSteps: Int,
    currentStep: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val width by animateDpAsState(
                targetValue = if (isActive) 22.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "stepDotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "stepDotColor"
            )
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
            ) {}
        }
    }
}

@Composable
private fun LanguageStepBody(
    selectedLanguage: String,
    strings: AppStrings,
    onSelectLanguage: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LanguageChoicePill(
                label = "Русский",
                subLabel = "5 000+ слов",
                selected = selectedLanguage == "ru",
                onClick = { onSelectLanguage("ru") },
                modifier = Modifier.weight(1f)
            )
            LanguageChoicePill(
                label = "English",
                subLabel = "5,300+ words",
                selected = selectedLanguage == "en",
                onClick = { onSelectLanguage("en") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onToggleLanguage,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .width(84.dp)
                    .height(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = strings.onboardingSwitchLanguageCd,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            PrimaryPillButton(
                text = strings.onboardingContinue,
                enabled = true,
                onClick = onContinue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ApiKeyStepBody(
    spec: ApiKeyStepSpec,
    skipLabel: String
) {
    val uriHandler = LocalUriHandler.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (spec.expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "guideChevron"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = spec.value,
            onValueChange = spec.onValueChange,
            label = { Text(spec.keyLabel) },
            placeholder = { Text(spec.placeholder) },
            singleLine = true,
            trailingIcon = {
                if (spec.value.isNotEmpty()) {
                    IconButton(onClick = { spec.onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Expandable Setup Guide Card
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { spec.onToggleExpanded() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = spec.guideTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }

                AnimatedVisibility(
                    visible = spec.expanded,
                    enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        spec.guideSteps.forEach { stepText ->
                            Text(
                                text = stepText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(spec.linkUrl)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = spec.linkLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Row: Skip pill + Primary Continue/Done pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = spec.onSkip,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .height(58.dp)
                    .widthIn(min = 120.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = skipLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            PrimaryPillButton(
                text = spec.confirmLabel,
                enabled = spec.value.isNotBlank(),
                onClick = spec.onConfirm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PrimaryPillButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(58.dp)
            .then(
                if (enabled) {
                    Modifier.bouncyClickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LanguageChoicePill(
    label: String,
    subLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
        },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .height(68.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScallopedBlob(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    rotationDegrees: Float,
    shape: Md3ExpressiveShape,
    color: androidx.compose.ui.graphics.Color
) {
    val numPoints = 180
    val rotRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
    val path = Path()
    for (i in 0 until numPoints) {
        val angle = (i.toFloat() / numPoints) * (2f * Math.PI.toFloat())
        val r = computeExpressiveRadius(shape, angle) * baseRadius
        val a = angle + rotRad
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}
