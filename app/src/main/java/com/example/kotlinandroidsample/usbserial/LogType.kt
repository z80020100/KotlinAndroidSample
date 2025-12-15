package com.example.kotlinandroidsample.usbserial

enum class LogType {
    RECEIVED,  // Received data (green)
    SENT,      // Sent data (blue)
    ERROR,     // Error messages (red)
    STATUS,    // Status messages (gray)
    INFO       // General info (default text color)
}
