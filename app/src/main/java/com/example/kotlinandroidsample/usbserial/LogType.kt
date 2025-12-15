package com.example.kotlinandroidsample.usbserial

enum class LogType {
    RX,     // Received data
    TX,     // Sent data
    DEBUG,  // Debug/diagnostic messages
    INFO,   // General information
    WARN,   // Warnings (potential issues)
    ERROR   // Errors
}
