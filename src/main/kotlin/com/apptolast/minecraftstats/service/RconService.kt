package com.apptolast.minecraftstats.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minecraft RCON client implementation
 * Based on: https://minecraft.wiki/w/RCON
 */
@Service
class RconService {
    
    private val logger = LoggerFactory.getLogger(RconService::class.java)

    companion object {
        private const val PACKET_TYPE_AUTH = 3
        private const val PACKET_TYPE_AUTH_RESPONSE = 2
        private const val PACKET_TYPE_COMMAND = 2
        private const val PACKET_TYPE_COMMAND_RESPONSE = 0
    }

    fun executeCommand(host: String, port: Int, password: String, command: String): String? {
        return try {
            Socket(host, port).use { socket ->
                socket.soTimeout = 5000
                val input = DataInputStream(socket.getInputStream())
                val output = DataOutputStream(socket.getOutputStream())
                
                // Authenticate
                sendPacket(output, 1, PACKET_TYPE_AUTH, password)
                val authResponse = readPacket(input)
                
                if (authResponse?.requestId == -1) {
                    logger.error("RCON authentication failed")
                    return null
                }
                
                // Send command
                sendPacket(output, 2, PACKET_TYPE_COMMAND, command)
                val response = readPacket(input)
                
                response?.payload
            }
        } catch (e: Exception) {
            logger.error("RCON error: ${e.message}")
            null
        }
    }

    private fun sendPacket(output: DataOutputStream, requestId: Int, type: Int, payload: String) {
        val payloadBytes = payload.toByteArray(Charsets.US_ASCII)
        val packetSize = 4 + 4 + payloadBytes.size + 2 // requestId + type + payload + null terminators
        
        val buffer = ByteBuffer.allocate(4 + packetSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(packetSize)
        buffer.putInt(requestId)
        buffer.putInt(type)
        buffer.put(payloadBytes)
        buffer.put(0) // null terminator for payload
        buffer.put(0) // null terminator for packet
        
        output.write(buffer.array())
        output.flush()
    }

    private fun readPacket(input: DataInputStream): RconPacket? {
        return try {
            val sizeBytes = ByteArray(4)
            input.readFully(sizeBytes)
            val size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).int
            
            val packetData = ByteArray(size)
            input.readFully(packetData)
            
            val buffer = ByteBuffer.wrap(packetData).order(ByteOrder.LITTLE_ENDIAN)
            val requestId = buffer.int
            val type = buffer.int
            
            val payloadSize = size - 10 // minus requestId (4) + type (4) + 2 null bytes
            val payload = if (payloadSize > 0) {
                val payloadBytes = ByteArray(payloadSize)
                buffer.get(payloadBytes)
                String(payloadBytes, Charsets.US_ASCII)
            } else ""
            
            RconPacket(requestId, type, payload)
        } catch (e: Exception) {
            logger.error("Error reading RCON packet: ${e.message}")
            null
        }
    }

    private data class RconPacket(
        val requestId: Int,
        val type: Int,
        val payload: String
    )
}
