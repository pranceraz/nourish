package com.nourish.app.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun ConnectionSettings() {
    val context = LocalContext.current
    val service = remember { DraftConnection(context) }
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf(service.origin) }
    var token by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(service.origin.isNotEmpty()) }
    var receiveChat by remember { mutableStateOf(service.chatEnabled) }
    var busy by remember { mutableStateOf(false) }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { status = (e as? ServiceError)?.message ?: "Couldn’t complete the change. Check the HTTPS server address and access token." }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { expanded = !expanded }) { Text("AI connection & personal USDA key") }
        if (expanded) {
            Text("Common-food search works offline. Connect your trusted Nourish server for AI drafts.")
            if (!connected) {
                OutlinedTextField(address,{ address = it },label = { Text("HTTPS server address") }, singleLine = true)
                OutlinedTextField(token,{ token = it },label = { Text("Nourish app access token") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), singleLine = true)
                Button(enabled = !busy, onClick = { run {
                    service.connect(address,token.trim())
                    receiveChat = false
                    connected = true
                    token = ""
                    status = "Connection settings saved."
                } }) { Text("Save connection") }
            } else {
                Text(service.origin)
                Text("ChatGPT diary", style = com.nourish.app.ui.theme.NourishType.heading)
                Text("Connect ChatGPT to ${service.origin}/chat/mcp using OAuth. Confirm a draft in chat, then open Nourish to receive it.")
                OutlinedButton(enabled = !busy,onClick = {
                    if (receiveChat) status = "Incoming meals paused."
                    service.chatEnabled = !receiveChat
                    receiveChat = service.chatEnabled
                    if (receiveChat) run {
                        val count = ChatSync.receive(context)
                        status = "Received $count confirmed meals."
                    }
                }) { Text(if (receiveChat) "Pause incoming meals" else "Receive confirmed meals on this device") }
                if (receiveChat) TextButton(enabled = !busy,onClick = { run {
                    val count = ChatSync.receive(context)
                    status = "Up to date · $count new meals."
                } }) { Text("Check for meals now") }
                Text("Optional USDA key for authorized GPT food lookup. Saving sends it to this server for encrypted storage. Meal drafts use the bundled USDA catalog.")
                OutlinedTextField(key, { key = it }, label = { Text("Personal USDA API key") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), singleLine = true)
                Button(enabled = !busy && key.trim().length in 8..128, onClick = { run {
                    service.request("/v1/settings/usda","PUT",JSONObject().put("key",key.trim()))
                    key = ""
                    status = "Personal key saved for GPT lookup."
                } }) { Text("Save USDA key") }
                TextButton(enabled = !busy, onClick = { run {
                    val configured = service.request("/v1/settings/usda","GET").getBoolean("configured")
                    status = if (configured) "A personal key is stored." else "No personal key is stored."
                } }) { Text("Check key status") }
                TextButton(enabled = !busy, onClick = { run {
                    service.request("/v1/settings/usda","DELETE")
                    key = ""
                    status = "Personal key removed from the server."
                } }) { Text("Remove USDA key") }
                TextButton(enabled = !busy, onClick = {
                    service.disconnect(); connected = false; key = ""; status = "Device disconnected. Server keys can be removed while connected."
                }) { Text("Disconnect device") }
            }
            if (status.isNotBlank()) Text(status)
        }
    }
}
