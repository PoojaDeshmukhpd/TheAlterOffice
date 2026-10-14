package com.example.appeventtracker.data.parser

import com.example.appeventtracker.data.local.NewEvent
import com.example.appeventtracker.data.model.EventType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventJsonParser @Inject constructor() {


    fun parse(json: String): List<NewEvent> {
        try {
            val trimmed = json.trim()

            return when {
                trimmed.startsWith("{") -> {
                    listOf(parseEvent(JSONObject(trimmed)))
                }

                trimmed.startsWith("[") -> {
                    val array = JSONArray(trimmed)
                    buildList {
                        for (index in 0 until array.length()) {
                            val value = array.get(index)

                            require(value is JSONObject) {
                                "Each array item must be a JSON object"
                            }

                            add(parseEvent(value))
                        }
                    }
                }

                else -> throw IllegalArgumentException(
                    "Input must be a JSON object or array"
                )
            }
        } catch (exception: JSONException) {
            throw IllegalArgumentException(
                "Invalid JSON: ${exception.message}",
                exception
            )
        }
    }

    private fun parseEvent(json: JSONObject): NewEvent {
        val eventTypeValue = json.optString("eventType")
            .takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(
                "Missing required field: eventType"
            )

        val eventType = try {
            EventType.valueOf(eventTypeValue.uppercase())
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Unsupported event type: $eventTypeValue",
                exception
            )
        }

        val payloadValue = json.opt("payload")
            ?: throw IllegalArgumentException(
                "Missing required field: payload"
            )

        require(payloadValue !== JSONObject.NULL) {
            "Payload cannot be null"
        }

        // Preserve structured JSON payloads as JSON text in Room.
        val payload = when (payloadValue) {
            is JSONObject, is JSONArray -> payloadValue.toString()
            is String -> JSONObject.quote(payloadValue)
            else -> payloadValue.toString()
        }

        return NewEvent(
            eventType = eventType,
            payload = payload
        )
    }
}