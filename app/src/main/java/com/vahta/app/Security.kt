
package com.vahta.app

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

private const val SECURITY_PREFS = "vahta_security"

private fun pinHash(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun findFragmentActivity(context: Context): FragmentActivity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun SecurityScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
    }

    var savedPin by remember {
        mutableStateOf(prefs.getString("pin_hash", null))
    }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var biometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("biometric", false))
    }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateBiometric() {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            error = "Для биометрии требуется FragmentActivity"
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onUnlocked()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    error = errString.toString()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ВАХТА")
            .setSubtitle("Подтвердите вход отпечатком пальца")
            .setNegativeButtonText("Войти по PIN-коду")
            .build()

        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("ВАХТА", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))

        if (savedPin == null) {
            Text("Создайте PIN-код для защиты приложения")
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("PIN-код (4–6 цифр)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it.filter(Char::isDigit).take(6) },
                label = { Text("Повторите PIN-код") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        pin.length !in 4..6 -> error = "PIN должен содержать 4–6 цифр"
                        pin != confirmPin -> error = "PIN-коды не совпадают"
                        else -> {
                            val hash = pinHash(pin)
                            prefs.edit().putString("pin_hash", hash).apply()
                            savedPin = hash
                            pin = ""
                            confirmPin = ""
                            error = ""
                            onUnlocked()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Создать PIN и войти")
            }
        } else {
            Text("Введите PIN-код")
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("PIN-код") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (pinHash(pin) == savedPin) {
                        pin = ""
                        error = ""
                        onUnlocked()
                    } else {
                        error = "Неверный PIN-код"
                        pin = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти")
            }

            if (biometricEnabled && biometricAvailable) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { authenticateBiometric() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти по отпечатку")
                }
            }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        if (savedPin != null && biometricAvailable && !biometricEnabled) {
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = {
                prefs.edit().putBoolean("biometric", true).apply()
                biometricEnabled = true
            }) {
                Text("Включить вход по отпечатку")
            }
        }
    }
}
