
package com.vahta.app

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.util.concurrent.Executor

private val securityBg = Color(0xFF0B1728)
private val securityCard = Color(0xFF172B43)
private val securityAccent = Color(0xFF56E4C0)
private val securityMuted = Color(0xFFB4C6D9)

private fun findFragmentActivity(context: Context): FragmentActivity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}

private fun pinHash(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
fun SecurityScreen(onUnlocked: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            "vahta_security",
            Context.MODE_PRIVATE
        )
    }

    var savedPin by remember {
        mutableStateOf(prefs.getString("pin_hash", null))
    }
    var biometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("biometric", false))
    }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    var authenticating by remember { mutableStateOf(false) }

    fun authenticateBiometric() {
        if (!biometricAvailable || !biometricEnabled ||
            savedPin == null || authenticating
        ) return

        val activity = findFragmentActivity(context)
        if (activity == null) {
            error = "Биометрия недоступна"
            return
        }

        authenticating = true

        val executor = Executor { command ->
            activity.runOnUiThread(command)
        }

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    authenticating = false
                    onUnlocked()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    authenticating = false
                }

                override fun onAuthenticationFailed() {
                    error = "Отпечаток не распознан"
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ВАХТА")
            .setSubtitle("Подтвердите вход по отпечатку")
            .setNegativeButtonText("Войти по PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .build()

        prompt.authenticate(info)
    }

    // Автоматический запрос отпечатка при открытии экрана
    LaunchedEffect(savedPin, biometricEnabled, biometricAvailable) {
        if (
            savedPin != null &&
            biometricEnabled &&
            biometricAvailable
        ) {
            authenticateBiometric()
        }
    }

    fun submitPin() {
        if (pin.length < 4) {
            error = "Введите минимум 4 цифры"
            return
        }

        if (savedPin != null) {
            if (pinHash(pin) == savedPin) {
                pin = ""
                onUnlocked()
            } else {
                error = "Неверный PIN-код"
                pin = ""
            }
            return
        }

        if (firstPin == null) {
            firstPin = pin
            pin = ""
            error = ""
        } else {
            if (pin == firstPin) {
                val hash = pinHash(pin)
                prefs.edit()
                    .putString("pin_hash", hash)
                    .putBoolean(
                        "biometric",
                        biometricAvailable
                    )
                    .apply()

                savedPin = hash
                biometricEnabled = biometricAvailable
                pin = ""
                firstPin = null
                onUnlocked()
            } else {
                error = "PIN-коды не совпадают"
                firstPin = null
                pin = ""
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = securityAccent,
            background = securityBg,
            surface = securityCard,
            onSurface = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(securityBg)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.7f))

            Text(
                text = "ВАХТА",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 5.sp
            )

            Text(
                text = "ТВОЙ РИТМ. ТВОИ ПРАВИЛА.",
                fontSize = 11.sp,
                color = securityAccent,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(42.dp))

            Text(
                text = when {
                    savedPin != null -> "С возвращением"
                    firstPin != null -> "Повторите PIN-код"
                    else -> "Создайте PIN-код"
                },
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (savedPin != null)
                    "Введите PIN-код для входа"
                else
                    "Придумайте код из 4–6 цифр",
                fontSize = 14.sp,
                color = securityMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (index < pin.length)
                                    securityAccent
                                else
                                    Color(0xFF354C65),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = error,
                color = Color(0xFFFF8A8A),
                fontSize = 13.sp,
                minLines = 1
            )

            Spacer(Modifier.height(18.dp))

            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", "✓")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .size(74.dp)
                                .background(
                                    if (key == "✓")
                                        securityAccent
                                    else
                                        securityCard,
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable {
                                    error = ""
                                    when (key) {
                                        "⌫" -> {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                            }
                                        }
                                        "✓" -> submitPin()
                                        else -> {
                                            if (pin.length < 6) {
                                                pin += key
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (key == "✓")
                                    securityBg
                                else
                                    Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            if (
                savedPin != null &&
                biometricEnabled &&
                biometricAvailable
            ) {
                TextButton(
                    onClick = { authenticateBiometric() }
                ) {
                    Text(
                        "◎  Войти по отпечатку",
                        color = securityAccent,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "ЗАЩИЩЁННЫЙ ВХОД",
                fontSize = 10.sp,
                color = securityMuted,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
