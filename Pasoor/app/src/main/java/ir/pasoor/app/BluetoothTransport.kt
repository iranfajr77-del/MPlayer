package ir.pasoor.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Offline Bluetooth Classic transport for Pasoor.
 * The host remains authoritative; callers send/receive newline-delimited JSON messages.
 * The Activity must request BLUETOOTH_CONNECT (and BLUETOOTH_SCAN if it performs discovery)
 * at runtime before calling these methods on Android 12+.
 */
class BluetoothTransport(
    private val onMessage: (device: BluetoothDevice, json: String) -> Unit,
    private val onConnection: (device: BluetoothDevice, connected: Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("a74b9f30-5e4d-4f8b-9b7c-6a2f0e1d4c51")
        const val SERVICE_NAME = "Pasoor Offline"
        const val MAX_PEERS = 3
    }

    private val peers = ConcurrentHashMap<String, BluetoothSocket>()
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var closed = false

    @SuppressLint("MissingPermission")
    fun startHost(adapter: BluetoothAdapter) {
        close()
        closed = false
        thread(name = "pasoor-bt-host") {
            try {
                val listening = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                server = listening
                while (!closed && peers.size < MAX_PEERS) {
                    val socket = listening.accept() ?: continue
                    if (closed) {
                        socket.close()
                        break
                    }
                    attach(socket)
                }
                listening.close()
            } catch (e: Exception) {
                if (!closed) onError("خطا در ساخت اتاق بلوتوث: ${e.localizedMessage ?: "نامشخص"}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        thread(name = "pasoor-bt-connect") {
            try {
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                attach(socket)
            } catch (e: Exception) {
                onError("اتصال برقرار نشد: ${e.localizedMessage ?: "نامشخص"}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun attach(socket: BluetoothSocket) {
        if (peers.size >= MAX_PEERS) {
            socket.close()
            return
        }
        val address = socket.remoteDevice.address
        val previous = peers.putIfAbsent(address, socket)
        if (previous != null) {
            socket.close()
            return
        }
        onConnection(socket.remoteDevice, true)
        thread(name = "pasoor-bt-read") {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                while (!closed && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) onMessage(socket.remoteDevice, line)
                }
            } catch (e: Exception) {
                if (!closed) onError("ارتباط با دستگاه قطع شد")
            } finally {
                peers.remove(address)
                try { socket.close() } catch (_: Exception) { }
                onConnection(socket.remoteDevice, false)
            }
        }
    }

    fun send(device: BluetoothDevice, json: String): Boolean {
        val socket = peers[device.address] ?: return false
        return try {
            synchronized(socket) {
                val writer = OutputStreamWriter(socket.outputStream, Charsets.UTF_8)
                writer.write(json.replace("\n", ""))
                writer.write("\n")
                writer.flush()
            }
            true
        } catch (e: Exception) {
            onError("ارسال پیام ناموفق بود")
            false
        }
    }

    fun broadcast(json: String) {
        peers.values.toList().forEach { socket ->
            try {
                synchronized(socket) {
                    val writer = OutputStreamWriter(socket.outputStream, Charsets.UTF_8)
                    writer.write(json.replace("\n", ""))
                    writer.write("\n")
                    writer.flush()
                }
            } catch (_: Exception) {
                try { socket.close() } catch (_: Exception) { }
            }
        }
    }

    fun connectedCount(): Int = peers.size

    fun close() {
        closed = true
        try { server?.close() } catch (_: Exception) { }
        server = null
        peers.values.forEach { try { it.close() } catch (_: Exception) { } }
        peers.clear()
    }
}
